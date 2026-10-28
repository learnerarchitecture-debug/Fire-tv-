package com.aurora.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bottom-sheet app drawer for the home screen. Shows launchable user apps
 * and the curated main apps (YouTube, Netflix and Prime Video). Fire OS
 * system apps stay hidden so the drawer remains a personal app shelf.
 */
public class AppDrawerActivity extends Activity {

    private static final Set<String> MAIN = new HashSet<String>(Arrays.asList(
            "com.amazon.firetv.youtube",
            "com.amazon.youtube",
            "com.google.android.youtube",
            "com.google.android.youtube.tv",
            "com.netflix.mediaclient",
            "com.netflix.ninja",
            "com.amazon.avod",
            "com.amazon.amazonvideo.livingroom"
    ));

    private GridView mGrid;
    private TextView mSubtitle;
    private View mSheet;
    private View mScrim;
    private View mEmpty;
    private AppGridAdapter mAdapter;
    private IconLoader mLoader;
    private final Handler mUi = new Handler(Looper.getMainLooper());
    private boolean mClosing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.immersive(this);
        setContentView(R.layout.activity_app_drawer);

        mSheet = findViewById(R.id.drawer_sheet);
        mScrim = findViewById(R.id.drawer_scrim);
        mGrid = (GridView) findViewById(R.id.drawer_grid);
        mEmpty = findViewById(R.id.drawer_empty);
        mSubtitle = (TextView) findViewById(R.id.drawer_subtitle);

        IconView closeIcon = (IconView) findViewById(R.id.drawer_close_icon);
        closeIcon.setMode(IconView.CLOSE);
        closeIcon.setColor(0xFFFFFFFF, 0);
        findViewById(R.id.drawer_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDrawer();
            }
        });
        mScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDrawer();
            }
        });

        mLoader = new IconLoader(this, 128);
        mAdapter = new AppGridAdapter(this, mLoader);
        mAdapter.setListener(new AppGridAdapter.Listener() {
            @Override
            public void onAppClick(AppGridAdapter.AppEntry entry) {
                openApp(entry);
            }
        });
        mGrid.setAdapter(mAdapter);

        animateIn();
        loadApps();
    }

    private void animateIn() {
        mScrim.setAlpha(0f);
        if (Prefs.reduceMotion(this)) {
            mScrim.setAlpha(1f);
            return;
        }
        mSheet.setVisibility(View.INVISIBLE);
        mSheet.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (mSheet.getHeight() <= 0) return true;
                        mSheet.getViewTreeObserver().removeOnPreDrawListener(this);
                        mSheet.setTranslationY(mSheet.getHeight());
                        mSheet.setVisibility(View.VISIBLE);
                        mSheet.animate()
                                .translationY(0f)
                                .setDuration(220)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                        mScrim.animate()
                                .alpha(1f)
                                .setDuration(220)
                                .start();
                        return true;
                    }
                });
    }

    private void closeDrawer() {
        if (mClosing) return;
        mClosing = true;
        if (Prefs.reduceMotion(this)) {
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        mSheet.animate()
                .translationY(mSheet.getHeight())
                .setDuration(180)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
        mScrim.animate().alpha(0f).setDuration(180).start();
    }

    private void loadApps() {
        final PackageManager pm = getPackageManager();
        final String self = getPackageName();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppGridAdapter.AppEntry> apps = queryApps(pm, self);
                mUi.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        bindApps(apps);
                    }
                });
            }
        }, "app-drawer").start();
    }

    private void bindApps(List<AppGridAdapter.AppEntry> apps) {
        mAdapter.setApps(apps);
        mSubtitle.setText(getString(R.string.apps_lead) + " \u00B7 " + apps.size());
        if (apps.isEmpty()) {
            mGrid.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
            findViewById(R.id.drawer_close).requestFocus();
            return;
        }
        mGrid.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mGrid.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || mAdapter.getCount() == 0) return;
                if (mGrid.getChildCount() > 0) {
                    mGrid.getChildAt(0).requestFocus();
                } else {
                    mGrid.requestFocus();
                }
            }
        }, 60);
    }

    private List<AppGridAdapter.AppEntry> queryApps(PackageManager pm, String self) {
        List<AppGridAdapter.AppEntry> out = new ArrayList<AppGridAdapter.AppEntry>();
        List<ApplicationInfo> all;
        try {
            all = pm.getInstalledApplications(0);
        } catch (Throwable t) {
            return out;
        }
        Set<String> seen = new HashSet<String>();
        for (ApplicationInfo ai : all) {
            if (ai == null || ai.packageName == null) continue;
            String pkg = ai.packageName;
            if (pkg.equals(self) || seen.contains(pkg) || !ai.enabled) continue;

            boolean system = (ai.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (system && !MAIN.contains(pkg)) continue;

            Intent launch = launchIntent(pm, pkg);
            if (launch == null) continue;

            CharSequence label = null;
            try {
                label = pm.getApplicationLabel(ai);
            } catch (Throwable ignored) {
            }
            if (label == null || label.length() == 0) label = pkg;

            // Fire OS labels the Prime Video package "FireTV Player". Keep the
            // drawer consistent with the curated dock and the rest of OBAA.
            int rank = priority(pkg);
            if (rank == 0) {
                label = getString(R.string.youtube);
            } else if (rank == 1) {
                label = getString(R.string.netflix);
            } else if (rank == 2) {
                label = getString(R.string.prime_video);
            }

            AppGridAdapter.AppEntry entry = new AppGridAdapter.AppEntry();
            entry.pkg = pkg;
            entry.label = label;
            entry.intent = launch;
            out.add(entry);
            seen.add(pkg);
        }
        Collections.sort(out, new Comparator<AppGridAdapter.AppEntry>() {
            @Override
            public int compare(AppGridAdapter.AppEntry a, AppGridAdapter.AppEntry b) {
                int pa = priority(a.pkg);
                int pb = priority(b.pkg);
                if (pa != pb) return pa - pb;
                return String.valueOf(a.label).compareToIgnoreCase(String.valueOf(b.label));
            }
        });
        return out;
    }

    private static Intent launchIntent(PackageManager pm, String pkg) {
        Intent intent = null;
        // Match the dock and Settings behaviour: prefer the regular launcher
        // activity, then fall back to the leanback entry point.
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

    private static int priority(String pkg) {
        if ("com.amazon.firetv.youtube".equals(pkg)
                || "com.amazon.youtube".equals(pkg)
                || "com.google.android.youtube".equals(pkg)
                || "com.google.android.youtube.tv".equals(pkg)) return 0;
        if ("com.netflix.mediaclient".equals(pkg)
                || "com.netflix.ninja".equals(pkg)) return 1;
        if ("com.amazon.avod".equals(pkg)
                || "com.amazon.amazonvideo.livingroom".equals(pkg)) return 2;
        return 10;
    }

    private void openApp(AppGridAdapter.AppEntry entry) {
        if (entry == null || entry.intent == null) return;
        try {
            Intent intent = new Intent(entry.intent);
            // Keep the app above OBAA in the same task where possible so Back
            // returns to the launcher rather than the Fire TV stock home.
            intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Throwable t) {
            Toast.makeText(this, "Could not open " + entry.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        closeDrawer();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUi.removeCallbacksAndMessages(null);
        if (mLoader != null) mLoader.destroy();
    }
}
