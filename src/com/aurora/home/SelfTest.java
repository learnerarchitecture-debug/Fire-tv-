package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dependency-free self-test that runs on-device (Settings · Developer ·
 * Run self-test). It exercises the pure logic that ships in the launcher:
 * the artwork match scorer, the palette sampler, deep-link construction,
 * the learned-affinity maths, and the structural integrity of the
 * recommendation pool and catalog. No network access and no test framework —
 * if these invariants hold on the Fire TV, the core of the launcher is sound.
 */
public final class SelfTest {

    private SelfTest() {}

    /** Runs every check; returns PASS/FAIL lines ending with a summary. */
    public static List<String> runAll(Context context) {
        List<String> out = new ArrayList<String>();
        out.add("OBAA Home self-test");
        int pass = 0;
        int total = 0;

        total++; if (testMatchScore(out)) pass++;
        total++; if (testClean(out)) pass++;
        total++; if (testArtPalette(out)) pass++;
        total++; if (testDeepLinks(out)) pass++;
        total++; if (testLinkMap(out)) pass++;
        total++; if (testAffinity(out)) pass++;
        total++; if (testRecommendations(context, out)) pass++;
        total++; if (testCatalog(out)) pass++;

        out.add("RESULT " + pass + "/" + total
                + (pass == total ? " — ALL OK" : " — FAILURES PRESENT"));
        return out;
    }

    private static boolean ok(List<String> out, String name, boolean passed, String detail) {
        out.add((passed ? "PASS " : "FAIL ") + name
                + (detail == null || detail.length() == 0 ? "" : "  (" + detail + ")"));
        return passed;
    }

    // 1. Artwork match scorer ---------------------------------------------------

    private static boolean testMatchScore(List<String> out) {
        try {
            boolean good = ThumbnailLoader.matchScore("kota factory", "kota factory") == 10
                    && ThumbnailLoader.matchScore("kota factory 2019", "kota factory") == 8
                    && ThumbnailLoader.matchScore("the kota factory story", "kota factory") == 6
                    && ThumbnailLoader.matchScore("kota story", "kota") == 1
                    && ThumbnailLoader.matchScore("everything everywhere", "kota") == 0
                    && ThumbnailLoader.matchScore(null, "x") == 0;
            return ok(out, "art match scoring", good,
                    good ? null : "scoring ladder exact=10 prefix=8 contains=6 tokens");
        } catch (Throwable t) {
            return ok(out, "art match scoring", false, String.valueOf(t));
        }
    }

    private static boolean testClean(List<String> out) {
        try {
            boolean good = "dune part two".equals(ThumbnailLoader.clean("Dune: Part Two"))
                    && "multiple spaces".equals(ThumbnailLoader.clean("  Multiple   spaces "))
                    && "".equals(ThumbnailLoader.clean(null));
            return ok(out, "title normalisation", good,
                    good ? null : "clean() must lowercase, strip punctuation, squeeze spaces");
        } catch (Throwable t) {
            return ok(out, "title normalisation", false, String.valueOf(t));
        }
    }

    // 2. Palette sampler ----------------------------------------------------------

    private static boolean testArtPalette(List<String> out) {
        try {
            Bitmap red = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            red.eraseColor(Color.rgb(200, 40, 30));
            int a = ArtPalette.dominant(red);
            red.recycle();
            boolean good = a != 0
                    && ((a >> 16) & 0xFF) > ((a >> 8) & 0xFF)
                    && ((a >> 16) & 0xFF) > (a & 0xFF);

            Bitmap dark = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            dark.eraseColor(Color.rgb(6, 8, 10));
            boolean darkRejected = ArtPalette.dominant(dark) == 0;
            dark.recycle();

            boolean good2 = darkRejected && ArtPalette.dominant(null) == 0;
            return ok(out, "palette sampler", good && good2,
                    (good && good2) ? null : "solid red must tint red; near-black must be rejected");
        } catch (Throwable t) {
            return ok(out, "palette sampler", false, String.valueOf(t));
        }
    }

    // 3. Keyless deep links -------------------------------------------------------

    private static boolean testDeepLinks(List<String> out) {
        try {
            Uri yt = HomeActivity.deepLinkUri("yt", "g2lc9v2mktU");
            Uri nf = HomeActivity.deepLinkUri("nf", "80057281");
            boolean good = yt != null
                    && "https://www.youtube.com/watch?v=g2lc9v2mktU".equals(yt.toString())
                    && nf != null && "nflx://www.netflix.com/title/80057281".equals(nf.toString())
                    && HomeActivity.deepLinkUri("", "") == null
                    && HomeActivity.deepLinkUri(null, "x") == null
                    && "com.amazon.firetv.youtube".equals(HomeActivity.deepLinkPkg("yt"))
                    && "com.netflix.ninja".equals(HomeActivity.deepLinkPkg("nf"))
                    && "".equals(HomeActivity.deepLinkPkg("other"));
            return ok(out, "deep-link construction", good,
                    good ? null : "youtube.com/watch?v=<id> / nflx://www.netflix.com/title/<id>");
        } catch (Throwable t) {
            return ok(out, "deep-link construction", false, String.valueOf(t));
        }
    }

    private static boolean testLinkMap(List<String> out) {
        try {
            String[] yt = Recommendations.linkFor("youtube", "nightofliving");
            String[] nf = Recommendations.linkFor("netflix", "stranger");
            boolean good = yt != null && "yt".equals(yt[0])
                    && yt[1] != null && yt[1].length() == 11
                    && nf != null && "nf".equals(nf[0]) && nf[1] != null && nf[1].length() >= 8
                    && Recommendations.linkFor("youtube", "nope") == null
                    && Recommendations.linkFor("airtel", "jawan") == null;
            return ok(out, "verified video/title ids", good,
                    good ? null : "every baked id must resolve or the map entry is wrong");
        } catch (Throwable t) {
            return ok(out, "verified video/title ids", false, String.valueOf(t));
        }
    }

    // 4. Learned affinity ---------------------------------------------------------

    private static boolean testAffinity(List<String> out) {
        try {
            Map<String, Integer> m = Prefs.parseAffinity("g:action:2|p:com.x:1|bad|g:empty:0");
            boolean good = m.size() == 2
                    && m.containsKey("g:action") && m.get("g:action").intValue() == 2
                    && m.containsKey("p:com.x") && m.get("p:com.x").intValue() == 1;

            m = Prefs.bumpAffinityMap(m, "g:action", 99);
            good = good && m.get("g:action").intValue() == 3;
            m = Prefs.bumpAffinityMap(m, "g:action", 3);
            good = good && m.get("g:action").intValue() == 3;   // cap honoured

            Map<String, Integer> back = Prefs.parseAffinity(Prefs.serializeAffinity(m));
            good = good && back.equals(m);

            good = good && Recommendations.boostedMatch(95, 1, 2) == 99
                    && Recommendations.boostedMatch(80, 0, 0) == 80
                    && Recommendations.boostedMatch(50, 2, 1) == 58;

            good = good && "action".equals(Recommendations.genreKey("Action · Thriller"))
                    && "comedy".equals(Recommendations.genreKey("Comedy"))
                    && "".equals(Recommendations.genreKey(null));
            return ok(out, "affinity maths", good,
                    good ? null : "parse/bump/serialise must round-trip; match must clamp to 99");
        } catch (Throwable t) {
            return ok(out, "affinity maths", false, String.valueOf(t));
        }
    }

    // 5. Recommendation pool --------------------------------------------------------

    private static boolean testRecommendations(Context context, List<String> out) {
        try {
            if (context == null) return ok(out, "recommendation pool", false, "no context");
            List<Movie> recs = Recommendations.build(context, 1);
            boolean good = recs != null && !recs.isEmpty();
            Set<String> ids = new HashSet<String>();
            for (Movie m : recs) {
                if (m.title == null || m.title.length() == 0) good = false;
                if (m.match < 0 || m.match > 99) good = false;
                ids.add(m.id);
            }
            good = good && ids.size() == recs.size();
            return ok(out, "recommendation pool", good,
                    good ? null : "titles non-empty, match 0..99, ids unique");
        } catch (Throwable t) {
            return ok(out, "recommendation pool", false, String.valueOf(t));
        }
    }

    // 6. Curated catalog ------------------------------------------------------------

    private static boolean testCatalog(List<String> out) {
        try {
            boolean good = !MovieCatalog.MOVIES.isEmpty();
            Set<String> ids = new HashSet<String>();
            for (Movie m : MovieCatalog.MOVIES) {
                if (m.title == null || m.title.length() == 0) good = false;
                if (m.description == null || m.description.length() == 0) good = false;
                if (m.match < 0 || m.match > 100) good = false;
                if (!ids.add(m.id)) good = false;
                try {
                    float r = Float.parseFloat(m.rating);
                    if (r < 0f || r > 10f) good = false;
                } catch (NumberFormatException e) {
                    good = false;
                }
            }
            return ok(out, "catalog integrity", good,
                    good ? null : "unique ids, non-empty titles, ratings parse 0..10");
        } catch (Throwable t) {
            return ok(out, "catalog integrity", false, String.valueOf(t));
        }
    }
}
