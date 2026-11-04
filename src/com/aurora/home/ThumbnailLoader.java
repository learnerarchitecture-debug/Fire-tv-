package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.JsonReader;
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
 * Fetches real movie artwork from the public iTunes Search API (no key).
 * Memory + disk cached; when a title cannot be matched the caller keeps the
 * procedural poster so the home screen never shows a blank card.
 */
public final class ThumbnailLoader {

    private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(20 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    /** Titles we tried and could not match; retried on the next process. */
    private static final Set<String> MISSED = new HashSet<String>();
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
            if (MISSED.contains(m.id)) return;
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
                    if (bmp == null) MISSED.add(m.id);
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
        Bitmap disk = fromDisk(app, m.id);
        if (disk != null) return disk;
        String title = clean(m.title);
        String year = m.year == null ? "" : m.year.trim();
        String[][] attempts = {
                {"IN", "tvShow", title + " " + year},
                {"US", "tvShow", title + " " + year},
                {"IN", "movie", title + " " + year},
                {"US", "movie", title + " " + year},
        };
        for (String[] a : attempts) {
            try {
                String term = URLEncoder.encode(a[2].trim(), "UTF-8").replace("+", "%20");
                String url = "https://itunes.apple.com/search?term=" + term
                        + "&media=" + a[1] + "&limit=8&country=" + a[0];
                String art = searchArtwork(url, title);
                if (art != null) {
                    Bitmap b = downloadBitmap(art, 640);
                    if (b != null) {
                        saveToDisk(app, m.id, b);
                        return b;
                    }
                }
            } catch (Throwable ignored) {
                // try the next query
            }
        }
        return null;
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

    private static File cacheDir(Context c) {
        if (c == null) return null;
        return new File(c.getCacheDir(), "movieart");
    }

    private static Bitmap fromDisk(Context c, String id) {
        try {
            // "_v2" suffix invalidates art cached by the old (album-permitting) matcher.
            File f = new File(cacheDir(c), id + "_v2.jpg");
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
            File f = new File(dir, id + "_v2.jpg");
            out = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.JPEG, 86, out);
            out.flush();
        } catch (Throwable ignored) {
        } finally {
            if (out != null) try { out.close(); } catch (Throwable ignored) {}
        }
    }
}
