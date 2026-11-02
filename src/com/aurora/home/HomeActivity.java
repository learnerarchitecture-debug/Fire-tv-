package com.aurora.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeActivity extends Activity {

    private static final long WX_STALE_MS = 30L * 60L * 1000L;
    private static final String PKG_YOUTUBE = "com.amazon.firetv.youtube";
    private static final String PKG_YOUTUBE_ALT = "com.google.android.youtube";
    private static final String PKG_NETFLIX = "com.netflix.mediaclient";
    private static final String PKG_NETFLIX_ALT = "com.netflix.ninja";
    private static final String PKG_PRIME = "com.amazon.avod";

    private View mRoot;
    private AuroraBackgroundView mBg;
    private TextView mHour, mColon, mMinute, mAmpm, mDate;
    private TextView mWxTemp, mWxCond, mWxLoc;
    private WeatherIconView mWxIcon;
    private LinearLayout mWeatherChip;
    private PosterArtView mHeroArt;
    private TextView mHeroEyebrow, mHeroTitle, mHeroMeta, mHeroDesc;
    private TextView mHeroSaveLabel;
    private LinearLayout mHeroPlay, mHeroSave;
    private IconView mPlayIcon, mSaveIcon, mSettingsIcon, mBrandIcon;
    private IconView mDockSettingsIcon, mDockAppsIcon;
    private TextView mDockStatus;
    private LinearLayout mDockAddApps;
    private LinearLayout mDockUpdate;
    private IconView mDockAddIcon;
    private IconLoader mLoader;
    private final Handler mUi = new Handler();
    private Runnable mTick;
    private Runnable mRotationTick;
    private long mLastWxRefresh = 0;
    private Movie mFeatured;
    private List<Movie> mRecommendations = new ArrayList<Movie>();
    private String mProviderSignature = "";
    private int mRotation = Integer.MIN_VALUE;
    private Set<String> mSaved = new HashSet<String>();
    private int mAccent = 0xFFFFB45E;
    private boolean mFirstResume = true;
    private static volatile boolean sResumed;

    /** Used by the Home watcher to avoid a duplicate launch when OBAA is already visible. */
    public static boolean isResumed() {
        return sResumed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.immersive(this);
        setContentView(R.layout.activity_home);
        HomeKeyWatcherService.ensureStarted(this);

        mRoot = findViewById(R.id.root);
        mBg = (AuroraBackgroundView) findViewById(R.id.bg);
        mHour = (TextView) findViewById(R.id.clock_hour);
        mColon = (TextView) findViewById(R.id.clock_colon);
        mMinute = (TextView) findViewById(R.id.clock_minute);
        mAmpm = (TextView) findViewById(R.id.clock_ampm);
        mDate = (TextView) findViewById(R.id.clock_date);
        mWxTemp = (TextView) findViewById(R.id.wx_temp);
        mWxCond = (TextView) findViewById(R.id.wx_cond);
        mWxLoc = (TextView) findViewById(R.id.wx_loc);
        mWxIcon = (WeatherIconView) findViewById(R.id.wx_icon);
        mWeatherChip = (LinearLayout) findViewById(R.id.weather_chip);
        mHeroArt = (PosterArtView) findViewById(R.id.hero_art);
        mHeroEyebrow = (TextView) findViewById(R.id.hero_eyebrow);
        mHeroTitle = (TextView) findViewById(R.id.hero_title);
        mHeroMeta = (TextView) findViewById(R.id.hero_meta);
        mHeroDesc = (TextView) findViewById(R.id.hero_desc);
        mHeroSaveLabel = (TextView) findViewById(R.id.hero_save_label);
        mHeroPlay = (LinearLayout) findViewById(R.id.hero_play);
        mHeroSave = (LinearLayout) findViewById(R.id.hero_save);
        mPlayIcon = (IconView) findViewById(R.id.play_icon);
        mSaveIcon = (IconView) findViewById(R.id.save_icon);
        mSettingsIcon = (IconView) findViewById(R.id.settings_icon);
        mBrandIcon = (IconView) findViewById(R.id.brand_icon);
        mDockSettingsIcon = (IconView) findViewById(R.id.dock_settings_icon);
        mDockAppsIcon = (IconView) findViewById(R.id.dock_apps_icon);
        mDockStatus = (TextView) findViewById(R.id.dock_status);
        mDockAddApps = (LinearLayout) findViewById(R.id.dock_add_apps);
        if (mDockAddApps != null) {
            mDockAddIcon = (IconView) findViewById(R.id.dock_add_icon);
            if (mDockAddIcon != null) {
                mDockAddIcon.setMode(IconView.PLUS);
                mDockAddIcon.setColor(Prefs.accent(this), 0);
            }
            mDockAddApps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(HomeActivity.this, AppPickerActivity.class);
                    i.putExtra(AppPickerActivity.EXTRA_MODE, AppPickerActivity.MODE_HOME);
                    startActivity(i);
                    overridePendingTransition(0, 0);
                }
            });
        }

        mDockUpdate = (LinearLayout) findViewById(R.id.dock_update);
        if (mDockUpdate != null) {
            mDockUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUpdateDialog();
                }
            });
        }
        if (mDockUpdate != null) {
            mDockUpdate.setVisibility(View.GONE);
        }

        icon(mBrandIcon, IconView.AURORA, 0xFFFFFFFF);
        icon(mPlayIcon, IconView.PLAY, 0xFF12151C);
        icon(mSaveIcon, IconView.HEART, 0xFFFFFFFF);
        icon(mDockSettingsIcon, IconView.GEAR, 0);
        icon(mDockAppsIcon, IconView.APPS, 0);
        icon(mSettingsIcon, IconView.GEAR, 0);

        mLoader = new IconLoader(this, 96);
        mSaved = Prefs.savedIds(this);
        bindHomeApps();
        loadRecommendations(true);
        if (Prefs.getBool(this, Prefs.K_AUTO_UPDATE, true)) {
            startAutoUpdateCheck();
        }

        View.OnClickListener openSettings = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        };
        click(R.id.settings_btn, openSettings);
        click(R.id.dock_settings, openSettings);
        click(R.id.dock_apps, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppDrawer();
            }
        });
        click(R.id.avatar, openSettings);
        if (mWeatherChip != null) mWeatherChip.setOnClickListener(openSettings);

        if (mHeroPlay != null) mHeroPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeatured != null) showMovieDetails(mFeatured);
            }
        });
        if (mHeroSave != null) mHeroSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeatured == null) return;
                boolean saved = toggleSaved(mFeatured.id);
                toast(saved ? "Saved to watchlist" : "Removed from watchlist");
            }
        });
        // Hero next recommendation
        click(R.id.hero_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecommendations.size() > 1) {
                    setFeatured(mRecommendations.get(1), false);
                }
            }
        });
        applyTheme();
        updateClock();
        updateDockStatus();
        startClock();

        Weather.Data d = Weather.loadCache(this);
        if (d != null) renderWeather(d);
        maybeRefreshWeather();
    }

    private void startRotationWatch() {
        stopRotationWatch();
        mRotationTick = new Runnable() {
            @Override
            public void run() {
                loadRecommendations(false);
                mUi.postDelayed(this, 30000);
            }
        };
        mUi.postDelayed(mRotationTick, 30000);
    }

    private void stopRotationWatch() {
        if (mRotationTick != null) {
            mUi.removeCallbacks(mRotationTick);
            mRotationTick = null;
        }
    }

    private void loadRecommendations(boolean force) {
        int rotation = Recommendations.rotationForNow(this);
        String signature = Recommendations.signature(this);
        if (!force && rotation == mRotation && signature.equals(mProviderSignature)) return;
        mRotation = rotation;
        mProviderSignature = signature;
        mRecommendations = Recommendations.build(this, rotation);
        if (!mRecommendations.isEmpty()) {
            setFeatured(mRecommendations.get(0), false);
        }
    }

    private void bindHomeApps() {
        // Home dock apps bound via layout; picker saves to Prefs.K_HOME_DOCK.
    }

    private String loadLabel(String pkg) {
        if (pkg == null) return "";
        try {
            return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable t) { return pkg; }
    }

    private void updateDockVisibility() {
        // Home dock now handled by bindHomeApps; footer apps removed.
    }


    @Override
    protected void onResume() {
        super.onResume();
        sResumed = true;
        Ui.immersive(this);
        mSaved = Prefs.savedIds(this);
        loadRecommendations(false);
        applyTheme();
        updateClock();
        updateDockStatus();
        startClock();
        startRotationWatch();
        Weather.Data d = Weather.loadCache(this);
        if (d != null) renderWeather(d);
        maybeRefreshWeather();
        if (mFirstResume) {
            mFirstResume = false;
            if (mHeroPlay != null) mHeroPlay.requestFocus();
        } else {
            View focus = getCurrentFocus();
            if (focus == null || !focus.isShown()) {
                if (mHeroPlay != null) mHeroPlay.requestFocus();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sResumed = false;
        stopClock();
        stopRotationWatch();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoader != null) mLoader.destroy();
    }

    private void applyTheme() {
        String theme = Prefs.theme(this);
        mAccent = Prefs.accent(this);
        if (mBg != null) mBg.setTheme(theme, mAccent);
        if (mSettingsIcon != null) mSettingsIcon.setColor(mAccent, 0);
        if (mDockSettingsIcon != null) mDockSettingsIcon.setColor(mAccent, 0);
        if (mDockAppsIcon != null) mDockAppsIcon.setColor(mAccent, 0);
        if (mHeroEyebrow != null) mHeroEyebrow.setTextColor(mAccent);
        if (mHeroPlay != null) mHeroPlay.setBackgroundDrawable(primaryButtonBackground());
        if (mWeatherChip != null) mWeatherChip.setVisibility(Prefs.showWeather(this) ? View.VISIBLE : View.GONE);
        if (mRoot != null) Ui.applyGlassAlpha(mRoot, Prefs.glassPercent(this));
        updateFeaturedSave();
    }

    private Drawable primaryButtonBackground() {
        StateListDrawable states = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.RECTANGLE);
        focused.setCornerRadius(Ui.dp(this, 999));
        focused.setColor(0xFFFFFFFF);
        focused.setStroke((int) Ui.dp(this, 2), mAccent);
        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setCornerRadius(Ui.dp(this, 999));
        normal.setColor(0xFFE8EBF2);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    private Drawable actionButtonBackground() {
        StateListDrawable states = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.RECTANGLE);
        focused.setCornerRadius(Ui.dp(this, 999));
        focused.setColor(0x2EFFFFFF);
        focused.setStroke((int) Ui.dp(this, 1.5f), mAccent);
        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setCornerRadius(Ui.dp(this, 999));
        normal.setColor(0x12FFFFFF);
        normal.setStroke((int) Ui.dp(this, 1), 0x2AFFFFFF);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    private void setFeatured(Movie movie, boolean fromRail) {
        if (movie == null) return;
        mFeatured = movie;
        mHeroArt.setMovie(movie);
        String eyebrow = "FEATURED · " + movie.tag;
        if (movie.providerName.length() > 0) eyebrow += " · " + movie.providerName;
        mHeroEyebrow.setText(eyebrow.toUpperCase(Locale.US));
        mHeroTitle.setText(movie.title);
        mHeroMeta.setText(movie.meta());
        mHeroDesc.setText(movie.description);
        updateFeaturedSave();
    }

    private void updateFeaturedSave() {
        if (mHeroSaveLabel == null || mFeatured == null) return;
        boolean saved = mSaved.contains(mFeatured.id);
        mHeroSaveLabel.setText(saved ? "Saved" : "Watchlist");
        mHeroSaveLabel.setTextColor(saved ? mAccent : 0xFFF2F4F8);
        mSaveIcon.setColor(saved ? mAccent : 0xFFFFFFFF, 0);
    }

    private boolean toggleSaved(String id) {
        boolean saved;
        if (mSaved.contains(id)) {
            mSaved.remove(id);
            saved = false;
        } else {
            mSaved.add(id);
            saved = true;
        }
        Prefs.putSavedIds(this, mSaved);
        updateFeaturedSave();
        return saved;
    }

    private void startClock() {
        stopClock();
        mTick = new Runnable() {
            @Override
            public void run() {
                updateClock();
                mUi.postDelayed(this, 15000);
            }
        };
        mUi.postDelayed(mTick, 15000);
    }

    private void stopClock() {
        if (mTick != null) {
            mUi.removeCallbacks(mTick);
            mTick = null;
        }
    }

    private void updateClock() {
        boolean h24 = Prefs.getBool(this, Prefs.K_24H, false);
        Date now = new Date();
        int hour = now.getHours();
        String ampm = "";
        if (!h24) {
            ampm = hour >= 12 ? "PM" : "AM";
            hour = hour % 12;
            if (hour == 0) hour = 12;
        }
        mHour.setText(String.valueOf(hour));
        mMinute.setText(String.format(Locale.getDefault(), "%02d", now.getMinutes()));
        mAmpm.setText(ampm);
        mAmpm.setVisibility(h24 ? View.GONE : View.VISIBLE);
        mDate.setVisibility(Prefs.showDate(this) ? View.VISIBLE : View.GONE);
        mDate.setText(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now));
    }

    private void updateDockStatus() {
        String ssid = wifiName();
        String free = freeStorageGb();
        String status = (ssid.length() > 0 ? ssid : "Wi-Fi")
                + (free.length() > 0 ? "    ·    " + free + " GB FREE" : "");
        mDockStatus.setText(status.toUpperCase(Locale.US));
    }

    private void maybeRefreshWeather() {
        long now = System.currentTimeMillis();
        Weather.Data d = Weather.loadCache(this);
        boolean stale = d == null || !d.valid()
                || now - d.fetched > WX_STALE_MS
                || !d.unit.equals(Prefs.get(this, Prefs.K_TEMP_UNIT, "c"));
        if (!stale || now - mLastWxRefresh < 10000L) return;
        mLastWxRefresh = now;
        Weather.refresh(this, new Weather.Callback() {
            @Override
            public void onWeather(Weather.Data data) {
                if (data != null) renderWeather(data);
            }
        });
    }

    private void renderWeather(Weather.Data d) {
        if (d == null || !d.valid()) {
            mWxTemp.setText("--°");
            mWxCond.setText("Weather unavailable");
            mWxLoc.setText("Set your city in Settings");
            mWxIcon.setKind(WeatherIconView.CLOUD);
            return;
        }
        String unit = "f".equals(d.unit) ? "F" : "C";
        mWxTemp.setText(Math.round(d.temp) + "°" + unit);
        mWxCond.setText(Weather.condition(d.code));
        mWxLoc.setText(d.place);
        mWxIcon.setKind(Weather.iconKind(d.code, d.day));
    }

    private AlertDialog.Builder dialogBuilder() {
        return new Ui.ImmersiveDialogBuilder(this, dialogTheme());
    }

    private int dialogTheme() {
        String a = Prefs.accentKey(this);
        if ("coral".equals(a)) return R.style.GlassDialogTheme_Coral;
        if ("violet".equals(a)) return R.style.GlassDialogTheme_Violet;
        if ("mint".equals(a)) return R.style.GlassDialogTheme_Mint;
        if ("blue".equals(a)) return R.style.GlassDialogTheme_Blue;
        return R.style.GlassDialogTheme;
    }

    private void showMovieDetails(final Movie movie) {
        String watchlist = mSaved.contains(movie.id) ? "Remove from Watchlist" : "Add to Watchlist";
        final boolean hasProvider = movie.providerPkg.length() > 0;
        final String providerName = hasProvider ? movie.providerName : "Prime Video";
        final String providerPkg = hasProvider ? movie.providerPkg : PKG_PRIME;
        String message = movie.meta() + "\n\n" + movie.description
                + "\n\n" + movie.match + "% match for you";
        if (hasProvider) message += "\nAvailable in " + providerName;
        dialogBuilder()
                .setTitle(movie.title)
                .setMessage(message)
                .setPositiveButton("Open " + providerName, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchPackage(providerPkg, providerName);
                    }
                })
                .setNeutralButton(watchlist, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean saved = toggleSaved(movie.id);
                        toast(saved ? "Saved to watchlist" : "Removed from watchlist");
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showUpdateDialog() {
        UpdateChecker.check(this, new UpdateChecker.Callback() {
            @Override
            public void onResult(final UpdateChecker.Update update) {
                mUi.post(new Runnable() {
                    @Override
                    public void run() {
                        if (update == null || !update.available) {
                            dialogBuilder().setTitle("No updates").setMessage("OBAA Home is up to date.").setPositiveButton("OK", null).show();
                            return;
                        }
                        dialogBuilder()
                                .setTitle("Update found")
                                .setMessage("A new build is ready: " + update.version + ".\n\nInstall now?")
                                .setPositiveButton("Install now", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int w) {
                                        installUpdate(update);
                                    }
                                })
                                .setNegativeButton("Later", null)
                                .show();
                    }
                });
            }
            @Override
            public void onError(String msg) {
                toast("Couldn't check for updates");
            }
        });
    }

    private void installUpdate(UpdateChecker.Update update) {
        if (update == null || update.file == null || !update.file.exists()) {
            toast("Update file missing");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(Uri.fromFile(update.file));
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.setDataAndType(Uri.fromFile(update.file), "application/vnd.android.package-archive");
                view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(view);
            } catch (Throwable t2) { toast("Couldn't open installer"); }
        }
    }

    private void openAppDrawer() {
        startActivity(new Intent(this, AppDrawerActivity.class));
        overridePendingTransition(0, 0);
    }

    private void startAutoUpdateCheck() {
        UpdateChecker.check(this, new UpdateChecker.Callback() {
            @Override
            public void onResult(final UpdateChecker.Update update) {
                if (update != null && update.available && !isFinishing()) {
                    mUi.post(new Runnable() {
                        @Override
                        public void run() {
                            toast("Update available: " + update.version + " — open Settings to install");
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                // Silent background check.
            }
        });
    }

    private void launchPackage(String pkg, String label) {
        Intent intent = null;
        try {
            intent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            }
        } catch (Throwable ignored) {
        }
        if (intent == null) {
            toast("Could not open " + label);
            return;
        }
        // Stay in OBAA's task when the target allows it so Back returns home.
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Throwable t) {
            toast("Could not open " + label);
        }
    }

    private boolean isInstalled(String pkg) {
        if (pkg == null) return false;
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private String wifiName() {
        try {
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null && wi.getSSID() != null) {
                    String s = wi.getSSID().replace("\"", "");
                    if (s.length() > 0 && !"<unknown ssid>".equals(s)) return s;
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private String freeStorageGb() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            double free = fs.getAvailableBytes() / 1e9;
            return String.format(Locale.US, "%.1f", free);
        } catch (Throwable t) {
            return "";
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /** Safe click binding: missing views never crash the home screen. */
    private void click(int id, View.OnClickListener l) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(l);
    }

    private void icon(IconView v, int mode, int color) {
        if (v == null) return;
        v.setMode(mode);
        if (color != 0) v.setColor(color, 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openSettings();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Home screen: swallow BACK so we never fall through to a black screen.
    }
}
