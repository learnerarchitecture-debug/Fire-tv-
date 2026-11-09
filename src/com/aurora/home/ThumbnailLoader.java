package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.LruCache;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches real movie artwork. Sources, in order: the iTunes Search API
 * (movies then TV), TVMaze for series, and finally the Wikipedia REST
 * summary endpoint (its lead image is usually the film poster). All are
 * public and keyless. Memory + disk cached; when nothing matches the
 * caller keeps the procedural backdrop so the home screen never blanks.
 */
public final class ThumbnailLoader {

    private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(20 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    /** id -> fail timestamp; entries expire so art can be re-attempted. */
    private static final Map<String, Long> MISSED = new HashMap<String, Long>();
    private static final long MISSED_TTL_MS = 30L * 60L * 1000L;
    /** movie id -> views waiting for art. */
    private static final Map<String, List<View>> INFLIGHT = new HashMap<String, List<View>>();
    private static final Object LOCK = new Object();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);

    private ThumbnailLoader() {}

    /**
     * Loads artwork for the movie asynchronously and invalidates every view
     * that asked for it once it arrives. Safe to call repeatedly.
     */
    public static void load(Context c, final Movie m, final View target) {
        if (m == null || target == null) return;
        if (m.thumbnail != null && !m.thumbnail.isRecycled()) return;

        Bitmap cached = MEM.get(m.id);
        if (cached != null && !cached.isRecycled()) {
            m.thumbnail = cached;
            target.invalidate();
            return;
        }
        synchronized (LOCK) {
            Long miss = MISSED.get(m.id);
            if (miss != null) {
                if (SystemClock.uptimeMillis() - miss < MISSED_TTL_MS) return;
                MISSED.remove(m.id);   // TTL expired: give this title another shot
            }
            List<View> waiting = INFLIGHT.get(m.id);
            if (waiting != null) {
                if (!waiting.contains(target)) waiting.add(target);
                return;
            }
            List<View> list = new ArrayList<View>();
            list.add(target);
            INFLIGHT.put(m.id, list);
        }

        final Context app = c != null ? c.getApplicationContext() : target.getContext().getApplicationContext();
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                List<View> waiting;
                try {
                    bmp = fetch(app, m);
                } catch (Throwable ignored) {
                    bmp = null;
                }
                synchronized (LOCK) {
                    waiting = INFLIGHT.remove(m.id);
                    if (bmp == null) MISSED.put(m.id, SystemClock.uptimeMillis());
                }
                if (bmp != null) {
                    MEM.put(m.id, bmp);
                    m.thumbnail = bmp;
                }
                if (waiting != null) {
                    for (View v : waiting) {
                        final View fv = v;
                        fv.postInvalidate();
                    }
                }
            }
        });
    }

    /** Cover-crop source rect so bitmaps never stretch inside cards. */
    public static Rect coverSrc(float dstW, float dstH, float bw, float bh) {
        if (bw <= 1 || bh <= 1 || dstW <= 1 || dstH <= 1) {
            return new Rect(0, 0, (int) Math.max(1, bw), (int) Math.max(1, bh));
        }
        float dstRatio = dstW / dstH;
        float bmpRatio = bw / bh;
        int x = 0, y = 0, w = (int) bw, h = (int) bh;
        if (bmpRatio > dstRatio) {
            w = (int) (bh * dstRatio);
            x = (int) ((bw - w) / 2f);
        } else {
            h = (int) (bw / dstRatio);
            y = (int) ((bh - h) / 2f);
        }
        if (w < 1) w = 1;
        if (h < 1) h = 1;
        return new Rect(x, y, x + w, y + h);
    }

    // ---- fetch pipeline ----------------------------------------------------

    private static Bitmap fetch(Context app, Movie m) {
        pruneDisk(app);
        Bitmap disk = fromDisk(app, m.id);
        if (disk != null) return disk;

        boolean series = m.duration != null && m.duration.contains("season");
        String title = clean(m.title);
        String year = m.year == null ? "" : m.year.trim();
        String want = m.title == null ? "" : m.title.trim();

        // iTunes: movies (with + without year) before shows. TV-only sources
        // are skipped for films so a same-named series never steals art.
        String[][] attempts = {
                {"IN", "movie", (title + " " + year).trim()},
                {"US", "movie", (title + " " + year).trim()},
                {"IN", "movie", title},
                {"US", "movie", title},
                {"IN", "tvShow", title},
                {"US", "tvShow", title},
        };
        for (String[] a : attempts) {
            Bitmap b = itunesArt(a[0], a[1], a[2], want);
            if (b != null) {
                saveToDisk(app, m.id, b);
                return b;
            }
        }
        if (series) {
            Bitmap b = tvMazeArt(want, year);
            if (b != null) {
                saveToDisk(app, m.id, b);
                return b;
            }
        }
        Bitmap w = wikiArt(want, year, series);
        if (w != null) {
            saveToDisk(app, m.id, w);
            return w;
        }
        return null;
    }

    private static Bitmap itunesArt(String country, String media, String term, String want) {
        if (term == null || term.length() == 0 || want == null || want.length() == 0) return null;
        try {
            String url = "https://itunes.apple.com/search?term="
                    + URLEncoder.encode(term, "UTF-8")
                    + "&media=" + media + "&limit=6&country=" + country;
            String art = searchArtwork(url, want);
            if (art == null) return null;
            return downloadBitmap(art, 640);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Poster image for a TV series via TVMaze singlesearch (no key). */
    private static Bitmap tvMazeArt(String rawTitle, String year) {
        HttpURLConnection conn = null;
        try {
            if (rawTitle == null || rawTitle.trim().length() == 0) return null;
            String url = "https://api.tvmaze.com/singlesearch/shows?q="
                    + URLEncoder.encode(rawTitle.trim(), "UTF-8");
            conn = httpGet(url, 7000, 9000);
            if (conn.getResponseCode() != 200) return null;
            JsonReader jr = new JsonReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String art = null;
            String premiered = null;
            jr.beginObject();
            while (jr.hasNext()) {
                String name = jr.nextName();
                if ("image".equals(name)) {
                    if (jr.peek() == JsonToken.NULL) { jr.skipValue(); continue; }
                    jr.beginObject();
                    while (jr.hasNext()) {
                        String f = jr.nextName();
                        String v = jr.nextString();
                        if ("original".equals(f) || "medium".equals(f)) {
                            if (art == null) art = v;
                        }
                    }
                    jr.endObject();
                } else if ("premiered".equals(name)) {
                    premiered = jr.nextString();
                } else {
                    jr.skipValue();
                }
            }
            jr.endObject();
            jr.close();
            if (art == null) return null;
            // Same-name different-era series guard ("The Boys" 2019 vs 1960s).
            if (year != null && year.length() >= 4 && premiered != null && premiered.length() >= 4) {
                int wantYear = Integer.parseInt(year.substring(0, 4));
                int gotYear = Integer.parseInt(premiered.substring(0, 4));
                if (Math.abs(wantYear - gotYear) > 2) return null;
            }
            return downloadBitmap(art, 720);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    /** Film posters via the Wikipedia REST summary endpoint (CC BY-SA). */
    private static Bitmap wikiArt(String rawTitle, String year, boolean series) {
        String t = rawTitle == null ? "" : rawTitle.trim();
        if (t.length() == 0) return null;
        List<String> variants = new ArrayList<String>();
        variants.add(t);
        if (series) {
            variants.add(t + " (TV series)");
            variants.add(t + " (" + year + " TV series)");
        } else {
            variants.add(t + " (film)");
            variants.add(t + " (" + year + " film)");
            variants.add(t + " " + year);
        }
        for (String v : variants) {
            String art = wikiPageArt(v);
            if (art == null) continue;
            Bitmap b = downloadBitmap(art, 640);
            if (b != null) return b;
        }
        return null;
    }

    private static String wikiPageArt(String pageTitle) {
        HttpURLConnection conn = null;
        try {
            String url = "https://en.wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(pageTitle.replace(' ', '_'), "UTF-8")
                    .replace("%2F", "/").replace("%3A", ":");
            conn = httpGet(url, 5000, 7000);
            if (conn.getResponseCode() != 200) return null;
            JsonReader jr = new JsonReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String art = null;
            boolean useless = false;
            jr.beginObject();
            while (jr.hasNext()) {
                String name = jr.nextName();
                if ("type".equals(name)) {
                    String ty = jr.nextString();
                    if ("disambiguation".equals(ty)) useless = true;
                } else if ("originalimage".equals(name) || "thumbnail".equals(name)) {
                    if (jr.peek() == JsonToken.NULL) { jr.skipValue(); continue; }
                    jr.beginObject();
                    while (jr.hasNext()) {
                        String f = jr.nextName();
                        if ("source".equals(f)) {
                            String v = jr.nextString();
                            if (art == null) art = v;
                        } else {
                            jr.skipValue();
                        }
                    }
                    jr.endObject();
                } else {
                    jr.skipValue();
                }
            }
            jr.endObject();
            jr.close();
            if (useless || art == null) return null;
            return art;
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    private static HttpURLConnection httpGet(String urlStr, int connectMs, int readMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(connectMs);
        conn.setReadTimeout(readMs);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) OBAAHome");
        return conn;
    }

    /** Queries iTunes Search and picks the best-matching artwork URL. */
    private static String searchArtwork(String urlStr, final String wantTitle) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(9000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) OBAAHome");
            if (conn.getResponseCode() != 200) return null;
            InputStream in = new BufferedInputStream(conn.getInputStream());
            JsonReader jr = new JsonReader(new InputStreamReader(in, "UTF-8"));
            String best = null;
            int bestScore = 0;
            jr.beginObject();
            while (jr.hasNext()) {
                String name = jr.nextName();
                if ("results".equals(name)) {
                    jr.beginArray();
                    while (jr.hasNext()) {
                        String trackName = null;
                        String art = null;
                        jr.beginObject();
                        while (jr.hasNext()) {
                            String f = jr.nextName();
                            if ("trackName".equals(f)) {
                                trackName = jr.nextString();
                            } else if ("artworkUrl100".equals(f)) {
                                art = jr.nextString();
                            } else {
                                jr.skipValue();
                            }
                        }
                        jr.endObject();
                        if (trackName == null || art == null) continue;
                        int score = matchScore(clean(trackName), clean(wantTitle));
                        if (score > bestScore) {
                            bestScore = score;
                            best = art;
                        }
                    }
                    jr.endArray();
                } else {
                    jr.skipValue();
                }
            }
            jr.endObject();
            jr.close();
            if (bestScore < 4) return null;   // reject loose matches (generic titles)
            return best.replace("100x100bb", "600x600bb");
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    /** Loose token containment scoring; >= 4 means the name really matches. */
    private static int matchScore(String found, String want) {
        if (found == null || want == null || want.length() == 0) return 0;
        if (found.equals(want)) return 10;
        if (found.startsWith(want)) return 8;
        if (found.contains(want)) return 6;
        String[] tokens = want.split(" ");
        int hits = 0;
        for (String t : tokens) {
            if (t.length() >= 3 && found.contains(t)) hits++;
        }
        return hits;
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim().replaceAll(" +", " ");
    }

    private static Bitmap downloadBitmap(String urlStr, int maxDim) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) OBAAHome");
            if (conn.getResponseCode() != 200) return null;
            InputStream in = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            byte[] bytes = out.toByteArray();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / sample > maxDim) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    // ---- disk cache --------------------------------------------------------

    private static final long DISK_CACHE_MAX = 40L * 1024L * 1024L; // 40 MB
    private static volatile boolean sPruned;

    /**
     * Runs once per process: deletes older-generation cache files (v1 and
     * v2 predate the multi-source matcher) and keeps the cache bounded by
     * evicting the oldest files.
     */
    private static void pruneDisk(Context c) {
        if (sPruned) return;
        sPruned = true;
        try {
            File dir = cacheDir(c);
            File[] files = dir == null ? null : dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                String n = f.getName();
                if (n.endsWith(".jpg") && !n.endsWith("_v3.jpg")) f.delete();
            }
            files = dir.listFiles();
            long total = 0;
            if (files != null) for (File f : files) total += f.length();
            if (files == null || total <= DISK_CACHE_MAX) return;
            java.util.Arrays.sort(files, new java.util.Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return Long.valueOf(a.lastModified()).compareTo(b.lastModified());
                }
            });
            for (File f : files) {
                if (total <= DISK_CACHE_MAX * 0.85f) break;
                long len = f.length();
                if (f.delete()) total -= len;
            }
        } catch (Throwable ignored) {
        }
    }

    private static File cacheDir(Context c) {
        if (c == null) return null;
        return new File(c.getCacheDir(), "movieart");
    }

    private static Bitmap fromDisk(Context c, String id) {
        try {
            // "_v3" invalidates art cached before the multi-source matcher.
            File f = new File(cacheDir(c), id + "_v3.jpg");
            if (!f.exists() || f.length() < 1000) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / sample > 640) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void saveToDisk(Context c, String id, Bitmap b) {
        FileOutputStream out = null;
        try {
            File dir = cacheDir(c);
            if (dir == null) return;
            dir.mkdirs();
            File f = new File(dir, id + "_v3.jpg");
            out = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.JPEG, 86, out);
            out.flush();
        } catch (Throwable ignored) {
        } finally {
            if (out != null) try { out.close(); } catch (Throwable ignored) {}
        }
    }
}
