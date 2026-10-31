package com.aurora.home;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Single source of truth for launchable apps.
 *
 * The drawer used to hide every Fire OS system app.  The new picker can add
 * any launchable package (including system apps) to the tray, so the filter
 * lives here instead of inside the drawer activity.
 */
public final class AppCatalog {

    private static final String[] YOUTUBE = {
            "com.amazon.firetv.youtube",
            "com.amazon.youtube",
            "com.google.android.youtube",
            "com.google.android.youtube.tv"
    };
    private static final String[] NETFLIX = {
            "com.netflix.mediaclient",
            "com.netflix.ninja"
    };
    private static final String[] PRIME = {
            "com.amazon.avod",
            "com.amazon.amazonvideo.livingroom"
    };

    private AppCatalog() {}

    public static boolean isMain(String pkg) {
        return priority(pkg) < 10;
    }

    public static int priority(String pkg) {
        if (pkg == null) return 10;
        for (String p : YOUTUBE) if (p.equals(pkg)) return 0;
        for (String p : NETFLIX) if (p.equals(pkg)) return 1;
        for (String p : PRIME) if (p.equals(pkg)) return 2;
        return 10;
    }

    public static Intent launchIntent(PackageManager pm, String pkg) {
        Intent intent = null;
        try {
            intent = pm.getLaunchIntentForPackage(pkg);
        } catch (Throwable ignored) {
        }
        if (intent == null) {
            try {
                intent = pm.getLeanbackLaunchIntentForPackage(pkg);
            } catch (Throwable ignored) {
            }
        }
        return intent;
    }

    /** Every launchable app. System apps are included when requested. */
    public static List<AppGridAdapter.AppEntry> all(Context context, boolean includeSystem) {
        List<AppGridAdapter.AppEntry> out = new ArrayList<AppGridAdapter.AppEntry>();
        PackageManager pm = context.getPackageManager();
        String self = context.getPackageName();
        List<ApplicationInfo> installed;
        try {
            installed = pm.getInstalledApplications(0);
        } catch (Throwable t) {
            return out;
        }
        Set<String> seen = new HashSet<String>();
        for (ApplicationInfo ai : installed) {
            if (ai == null || ai.packageName == null) continue;
            String pkg = ai.packageName;
            if (pkg.equals(self) || seen.contains(pkg) || !ai.enabled) continue;

            boolean system = (ai.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (system && !includeSystem && !isMain(pkg)) continue;

            Intent launch = launchIntent(pm, pkg);
            if (launch == null) continue;

            AppGridAdapter.AppEntry entry = new AppGridAdapter.AppEntry();
            entry.pkg = pkg;
            entry.label = label(context, pm, ai, pkg);
            entry.intent = launch;
            out.add(entry);
            seen.add(pkg);
        }
        sort(out);
        return out;
    }

    private static CharSequence label(Context context, PackageManager pm,
                                      ApplicationInfo ai, String pkg) {
        CharSequence label = null;
        try {
            label = pm.getApplicationLabel(ai);
        } catch (Throwable ignored) {
        }
        if (label == null || label.length() == 0) label = pkg;
        // Fire OS labels the Prime Video package "FireTV Player"; keep the
        // drawer, dock and picker consistent.
        int rank = priority(pkg);
        if (rank == 0) return context.getString(R.string.youtube);
        if (rank == 1) return context.getString(R.string.netflix);
        if (rank == 2) return context.getString(R.string.prime_video);
        return label;
    }

    private static void sort(List<AppGridAdapter.AppEntry> entries) {
        Collections.sort(entries, new Comparator<AppGridAdapter.AppEntry>() {
            @Override
            public int compare(AppGridAdapter.AppEntry a, AppGridAdapter.AppEntry b) {
                int pa = priority(a.pkg);
                int pb = priority(b.pkg);
                if (pa != pb) return pa - pb;
                return String.valueOf(a.label).compareToIgnoreCase(String.valueOf(b.label));
            }
        });
    }

    /** The automatic tray: all launchable user apps plus the curated main apps. */
    public static Set<String> defaultPackages(Context context) {
        Set<String> out = new HashSet<String>();
        for (AppGridAdapter.AppEntry e : all(context, false)) out.add(e.pkg);
        return out;
    }

    public static boolean isCustom(Context context) {
        return Prefs.drawerCustom(context);
    }

    /** Packages the drawer should currently show. */
    public static Set<String> visiblePackages(Context context) {
        if (!isCustom(context)) return defaultPackages(context);
        return Prefs.drawerPackages(context);
    }

    /** Entries for the drawer, preserving the user's custom selection. */
    public static List<AppGridAdapter.AppEntry> visible(Context context) {
        Set<String> allowed = visiblePackages(context);
        List<AppGridAdapter.AppEntry> all = all(context, true);
        List<AppGridAdapter.AppEntry> out = new ArrayList<AppGridAdapter.AppEntry>();
        for (AppGridAdapter.AppEntry e : all) {
            if (allowed.contains(e.pkg)) out.add(e);
        }
        return out;
    }

    public static void save(Context context, Set<String> packages) {
        Prefs.putDrawerPackages(context, packages);
        Prefs.putDrawerCustom(context, true);
    }

    public static void reset(Context context) {
        Prefs.putDrawerPackages(context, new HashSet<String>());
        Prefs.putDrawerCustom(context, false);
    }

    public static String summary(Context context) {
        int count = visiblePackages(context).size();
        return count + " apps · " + (isCustom(context) ? "custom tray" : "automatic tray");
    }
}
