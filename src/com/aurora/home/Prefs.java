package com.aurora.home;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Small, dependency-free preference store for OBAA Home.
 * The underlying file keeps its original name so existing installs
 * retain their weather city and clock choices.
 */
public final class Prefs {
    public static final String K_CITY = "city";
    public static final String K_LAT = "lat";
    public static final String K_LON = "lon";
    public static final String K_TEMP_UNIT = "unit";
    public static final String K_24H = "h24";
    public static final String K_THEME = "theme";
    public static final String K_ACCENT = "accent";
    public static final String K_GLASS = "glass";
    public static final String K_SHOW_DATE = "show_date";
    public static final String K_SHOW_WEATHER = "show_weather";
    public static final String K_REDUCE_MOTION = "reduce_motion";
    public static final String K_SAVED = "saved";
    public static final String K_SHOW_YOUTUBE = "show_youtube";
    public static final String K_SHOW_NETFLIX = "show_netflix";
    public static final String K_SHOW_PRIME = "show_prime";
    public static final String K_DIAGNOSTICS = "diagnostics";
    public static final String K_WX_CACHE = "wx_cache";
    /** App tray customisation. Empty/absent means "show every launchable app". */
    public static final String K_DRAWER_CUSTOM = "drawer_custom";
    public static final String K_DRAWER_APPS = "drawer_apps";
    /** Preferred fallback apps for the home screen dock. */
    public static final String[] PREFERRED_HOME_APPS = {
            "com.amazon.firetv.youtube", "com.google.android.youtube",
            "com.netflix.mediaclient", "com.netflix.ninja",
            "com.amazon.avod", "com.spotify.tv.android",
            "com.plexapp.android", "com.disney.disneyplus"
    };
    /** Recommendation rotation controls. */
    public static final String K_REC_AUTO = "rec_auto";
    public static final String K_REC_ROTATION = "rec_rotation";
    public static final String K_REC_LAST = "rec_last";
    public static final String K_REC_FORCE = "rec_force";
    public static final String K_AUTO_UPDATE = "auto_update";
    public static final String K_HOME_DOCK = "home_dock";
    /** Continue Watching: the app most recently launched from home. */
    public static final String K_LAST_PKG = "last_launch_pkg";
    public static final String K_LAST_LABEL = "last_launch_label";
    /** UI accents follow the featured artwork's sampled palette. */
    public static final String K_PALETTE = "palette_theme";
    /** Learned taste: "g:action:3|p:com.pkg:2" — bumped on every open so
     *  recommendations follow what the household actually plays. */
    public static final String K_AFFINITY = "affinity";
    /** Continue Watching at title level: the last title deep-linked from home. */
    public static final String K_LAST_TITLE_KIND = "last_title_kind";
    public static final String K_LAST_TITLE_ID = "last_title_id";
    public static final String K_LAST_TITLE = "last_title";
    public static final String K_LAST_TITLE_PKG = "last_title_pkg";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("aurora", Context.MODE_PRIVATE);
    }

    public static String get(Context c, String k, String d) {
        return sp(c).getString(k, d);
    }

    public static void put(Context c, String k, String v) {
        sp(c).edit().putString(k, v).apply();
    }

    public static boolean getBool(Context c, String k, boolean d) {
        return sp(c).getBoolean(k, d);
    }

    public static void putBool(Context c, String k, boolean v) {
        sp(c).edit().putBoolean(k, v).apply();
    }

    public static int getInt(Context c, String k, int d) {
        return sp(c).getInt(k, d);
    }

    public static void putInt(Context c, String k, int v) {
        sp(c).edit().putInt(k, v).apply();
    }

    public static long getLong(Context c, String k, long d) {
        return sp(c).getLong(k, d);
    }

    public static void putLong(Context c, String k, long v) {
        sp(c).edit().putLong(k, v).apply();
    }

    /** Packages explicitly chosen for the app tray (only used when custom). */
    public static Set<String> drawerPackages(Context c) {
        Set<String> out = new HashSet<String>();
        String raw = get(c, K_DRAWER_APPS, "");
        if (raw.length() == 0) return out;
        String[] parts = raw.split(",");
        for (String p : parts) {
            if (p.length() > 0) out.add(p);
        }
        return out;
    }

    public static void putDrawerPackages(Context c, Set<String> packages) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            if (pkg == null || pkg.length() == 0) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(pkg);
        }
        sp(c).edit().putString(K_DRAWER_APPS, sb.toString()).apply();
    }

    public static boolean drawerCustom(Context c) {
        return getBool(c, K_DRAWER_CUSTOM, false);
    }

    public static void putDrawerCustom(Context c, boolean custom) {
        sp(c).edit().putBoolean(K_DRAWER_CUSTOM, custom).apply();
    }

    /** Current theme key, with the old ocean/ember names migrated in-place. */
    public static String theme(Context c) {
        String t = get(c, K_THEME, "midnight");
        if ("ocean".equals(t)) return "aurora";
        if ("ember".equals(t)) return "midnight";
        if ("graphite".equals(t) || "aurora".equals(t)) return t;
        return "midnight";
    }

    public static String themeName(Context c) {
        String t = theme(c);
        if ("graphite".equals(t)) return "Graphite";
        if ("aurora".equals(t)) return "Aurora";
        return "Midnight";
    }

    public static String accentKey(Context c) {
        String a = get(c, K_ACCENT, "");
        if ("amber".equals(a) || "coral".equals(a) || "violet".equals(a)
                || "mint".equals(a) || "blue".equals(a)) {
            return a;
        }
        String t = theme(c);
        if ("graphite".equals(t)) return "blue";
        if ("aurora".equals(t)) return "violet";
        return "amber";
    }

    public static String accentName(Context c) {
        String a = accentKey(c);
        if ("coral".equals(a)) return "Coral";
        if ("violet".equals(a)) return "Violet";
        if ("mint".equals(a)) return "Mint";
        if ("blue".equals(a)) return "Blue";
        return "Amber";
    }

    public static int accent(Context c) {
        String a = accentKey(c);
        if ("coral".equals(a)) return 0xFFFF7A6B;
        if ("violet".equals(a)) return 0xFFA99BFF;
        if ("mint".equals(a)) return 0xFF5FE3C0;
        if ("blue".equals(a)) return 0xFF7FB7FF;
        return 0xFFFFB45E;
    }

    /** Accent at roughly 20% alpha, used for tinted icon boxes. */
    public static int accentDim(Context c) {
        return (accent(c) & 0x00FFFFFF) | 0x33000000;
    }

    public static int glassPercent(Context c) {
        int v = getInt(c, K_GLASS, 62);
        if (v < 0) v = 0;
        if (v > 100) v = 100;
        return v;
    }

    public static boolean showDate(Context c) {
        return getBool(c, K_SHOW_DATE, true);
    }

    public static boolean showWeather(Context c) {
        return getBool(c, K_SHOW_WEATHER, true);
    }

    public static boolean reduceMotion(Context c) {
        return getBool(c, K_REDUCE_MOTION, false);
    }

    /** When false, accents stay on the fixed theme colour (no artwork sampling). */
    public static boolean paletteTheme(Context c) {
        return getBool(c, K_PALETTE, true);
    }

    public static Set<String> savedIds(Context c) {
        Set<String> out = new HashSet<String>();
        String raw = get(c, K_SAVED, "");
        if (raw.length() == 0) return out;
        String[] parts = raw.split(",");
        for (String p : parts) {
            if (p.length() > 0) out.add(p);
        }
        return out;
    }

    public static void putSavedIds(Context c, Set<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        put(c, K_SAVED, sb.toString());
    }

    /** Package of the app launched last from OBAA Home ("" if none yet). */
    public static String lastPackage(Context c) {
        return get(c, K_LAST_PKG, "");
    }

    public static String lastLabel(Context c) {
        return get(c, K_LAST_LABEL, "");
    }

    public static boolean showDockApp(Context c, String key) {
        return getBool(c, key, true);
    }

    public static Set<String> homeDockPackages(Context c) {
        Set<String> out = new HashSet<String>();
        String raw = get(c, K_HOME_DOCK, "");
        if (raw.length() == 0) return out;
        String[] parts = raw.split(",");
        for (String p : parts) if (p.length() > 0) out.add(p);
        return out;
    }

    public static void putHomeDockPackages(Context c, Set<String> packages) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : packages) {
            if (pkg == null || pkg.length() == 0) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(pkg);
        }
        sp(c).edit().putString(K_HOME_DOCK, sb.toString()).apply();
    }

    public static void resetHomeDock(Context c) {
        sp(c).edit().putString(K_HOME_DOCK, "").apply();
    }

    /** Installed packages among the preferred home dock apps. */
    public static Set<String> defaultHomeDockApps(Context c) {
        Set<String> out = new HashSet<String>();
        if (c == null) return out;
        android.content.pm.PackageManager pm = c.getPackageManager();
        for (String pkg : PREFERRED_HOME_APPS) {
            try {
                pm.getPackageInfo(pkg, 0);
                out.add(pkg);
            } catch (Exception ignored) {
                // not installed
            }
        }
        return out;
    }

    // Learned affinity -----------------------------------------------------------

    /** Parses "g:key:n|p:key:n" into an ordered map (bad entries skipped). */
    static java.util.LinkedHashMap<String, Integer> parseAffinity(String raw) {
        java.util.LinkedHashMap<String, Integer> out =
                new java.util.LinkedHashMap<String, Integer>();
        if (raw == null || raw.length() == 0) return out;
        for (String part : raw.split("\\|")) {
            String[] seg = part.split(":");
            if (seg.length != 3 || seg[0].length() == 0 || seg[1].length() == 0) continue;
            try {
                int n = Integer.parseInt(seg[2]);
                if (n > 0) out.put(seg[0] + ":" + seg[1], Integer.valueOf(n));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    /** Serializes the affinity map back to the stored format ("" if empty). */
    static String serializeAffinity(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            if (sb.length() > 0) sb.append('|');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    /** Copy of map with key bumped by one, clamped at cap (package-private
     *  so SelfTest can exercise the maths without touching real prefs). */
    static Map<String, Integer> bumpAffinityMap(Map<String, Integer> map,
                                                String key, int cap) {
        java.util.LinkedHashMap<String, Integer> out =
                new java.util.LinkedHashMap<String, Integer>();
        if (map != null) out.putAll(map);
        if (key == null || key.length() == 0) return out;
        Integer cur = out.get(key);
        int next = (cur == null ? 0 : cur.intValue()) + 1;
        if (next > cap) next = cap;
        out.put(key, Integer.valueOf(next));
        return out;
    }

    /** The household's learned taste: counts per "g:<genre>" / "p:<pkg>". */
    public static Map<String, Integer> affinity(Context c) {
        return parseAffinity(get(c, K_AFFINITY, ""));
    }

    /** Records one more open of a genre ("g:action") or provider ("p:<pkg>"). */
    public static void bumpAffinity(Context c, String key) {
        if (key == null || key.length() == 0) return;
        Map<String, Integer> m = bumpAffinityMap(affinity(c), key, 99);
        put(c, K_AFFINITY, serializeAffinity(m));
    }

    public static int affinityOf(Context c, String key) {
        Integer v = affinity(c).get(key);
        return v == null ? 0 : v.intValue();
    }

    // Title-level Continue Watching ------------------------------------------------

    public static String lastTitleKind(Context c) {
        return get(c, K_LAST_TITLE_KIND, "");
    }

    public static String lastTitleId(Context c) {
        return get(c, K_LAST_TITLE_ID, "");
    }

    public static String lastTitle(Context c) {
        return get(c, K_LAST_TITLE, "");
    }

    public static String lastTitlePkg(Context c) {
        return get(c, K_LAST_TITLE_PKG, "");
    }

    /** Remembers the last title deep-linked from home (""). */
    public static void putLastTitle(Context c, String kind, String id,
                                    String title, String pkg) {
        put(c, K_LAST_TITLE_KIND, kind == null ? "" : kind);
        put(c, K_LAST_TITLE_ID, id == null ? "" : id);
        put(c, K_LAST_TITLE, title == null ? "" : title);
        put(c, K_LAST_TITLE_PKG, pkg == null ? "" : pkg);
    }

    /** A direct app launch supersedes any remembered title. */
    public static void clearLastTitle(Context c) {
        put(c, K_LAST_TITLE_KIND, "");
        put(c, K_LAST_TITLE_ID, "");
        put(c, K_LAST_TITLE, "");
        put(c, K_LAST_TITLE_PKG, "");
    }
}
