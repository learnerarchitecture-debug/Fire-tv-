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
    private AmbientBackdropView mAmb;
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
    private FilmstripView mDeck;
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
    private LinearLayout mShell;
    private LinearLayout mHeroResume;
    private TextView mHeroResumeLabel;
    private IconView mResumeIcon;
    private TextView mSaverClock;
    private int mContentAccent = 0;
    // Ambient screensaver state
    private boolean mSaver;
    private int mSaverIndex;
    private Movie mSaverFeatured;
    private Runnable mIdleCheck;
    private Runnable mSaverCycle;
    private static final long IDLE_TO_SAVER_MS = 10L * 60L * 1000L;
    private static final long SAVER_CYCLE_MS = 18000L;

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
        mAmb = (AmbientBackdropView) findViewById(R.id.amb_back);
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
        // Full-bleed hero art now lives in AmbientBackdropView (Option C).
        mHeroEyebrow = (TextView) findViewById(R.id.hero_eyebrow);
        mHeroTitle = (TextView) findViewById(R.id.hero_title);
        mHeroMeta = (TextView) findViewById(R.id.hero_meta);
        mHeroDesc = (TextView) findViewById(R.id.hero_desc);
        mHeroSaveLabel = (TextView) findViewById(R.id.hero_save_label);
        mHeroPlay = (LinearLayout) findViewById(R.id.hero_play);
        mHeroSave = (LinearLayout) findViewById(R.id.hero_save);
        mShell = (LinearLayout) findViewById(R.id.shell);
        mSaverClock = (TextView) findViewById(R.id.saver_clock);
        mHeroResume = (LinearLayout) findViewById(R.id.hero_resume);
        mHeroResumeLabel = (TextView) findViewById(R.id.hero_resume_label);
        mResumeIcon = (IconView) findViewById(R.id.hero_resume_icon);
        if (mResumeIcon != null) {
            mResumeIcon.setMode(IconView.PLAY);
            mResumeIcon.setColor(mAccent, 0);
        }
        if (mHeroResume != null) {
            mHeroResume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resumeLast();
                }
            });
        }
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
        // Film panel action glyphs (undo / pass / save / next)
        View undo = findViewById(R.id.undo_icon);
        if (undo instanceof IconView) {
            IconView i = (IconView) undo;
            i.setMode(IconView.UNDO);
            i.setColor(0xFFF2F4F8, 0);
        }
        View pass = findViewById(R.id.pass_icon);
        if (pass instanceof IconView) {
            IconView i = (IconView) pass;
            i.setMode(IconView.CLOSE);
            i.setColor(0xFFF2F4F8, 0);
        }
        View heart = findViewById(R.id.deck_save_icon);
        if (heart instanceof IconView) {
            IconView i = (IconView) heart;
            i.setMode(IconView.HEART);
            i.setColor(Prefs.accent(this), 0);
        }
        View next = findViewById(R.id.hero_next_icon);
        if (next instanceof IconView) {
            IconView i = (IconView) next;
            i.setMode(IconView.CHEVRON);
            i.setColor(0xFFF2F4F8, 0);
        }

        mLoader = new IconLoader(this, 96);
        mSaved = Prefs.savedIds(this);
        mDeck = (FilmstripView) findViewById(R.id.swipe_film);
        if (mDeck != null) {
            mDeck.setSavedIds(mSaved);
            mDeck.setAccent(Prefs.accent(this));
            mDeck.setAnimationsEnabled(!Prefs.reduceMotion(this));
            mDeck.setEdgeListener(new FilmstripView.EdgeListener() {
                @Override
                public void onUpEdge() {
                    if (mHeroPlay != null && mHeroPlay.isShown()) {
                        mHeroPlay.requestFocus();
                    } else if (mHeroSave != null && mHeroSave.isShown()) {
                        mHeroSave.requestFocus();
                    }
                }

                @Override
                public void onDownEdge() {
                    View shelf = findViewById(R.id.saved_shelf);
                    if (shelf != null && shelf.isShown()) {
                        LinearLayout tiles = (LinearLayout) findViewById(R.id.saved_tiles);
                        if (tiles != null && tiles.getChildCount() > 0) {
                            tiles.getChildAt(0).requestFocus();
                            return;
                        }
                    }
                    LinearLayout row = (LinearLayout) findViewById(R.id.home_apps_row);
                    if (row != null && row.getChildCount() > 0) {
                        row.getChildAt(0).requestFocus();
                    }
                }
            });
            mDeck.setListener(new FilmstripView.Listener() {
                @Override
                public void onSwiped(Movie movie, boolean saved) {
                    if (saved) {
                        if (mSaved.add(movie.id)) {
                            Prefs.putSavedIds(HomeActivity.this, mSaved);
                            if (mDeck != null) mDeck.setSavedIds(mSaved);
                            refreshSavedShelf();
                            toast("Saved to watchlist");
                        } else {
                            toast("Already in your watchlist");
                        }
                    } else {
                        toast("Passed");
                    }
                }

                @Override
                public void onUndo(Movie movie, boolean wasSaved) {
                    if (wasSaved && movie != null && mSaved.remove(movie.id)) {
                        Prefs.putSavedIds(HomeActivity.this, mSaved);
                        if (mDeck != null) mDeck.setSavedIds(mSaved);
                        refreshSavedShelf();
                        updateFeaturedSave();
                    }
                }

                @Override
                public void onDeckChanged(int position, int total, boolean canUndo) {
                    TextView count = (TextView) findViewById(R.id.deck_count);
                    if (count != null) count.setText((position + 1) + " / " + total);
                    View undo = findViewById(R.id.deck_undo);
                    if (undo != null) {
                        undo.setEnabled(canUndo);
                        undo.setFocusable(canUndo);
                        undo.setClickable(canUndo);
                        undo.setAlpha(canUndo ? 1f : 0.35f);
                    }
                }

                @Override
                public void onOpen(Movie movie) {
                    if (movie != null) {
                        setFeatured(movie, false);
                        showMovieDetails(movie);
                    }
                }

                @Override
                public void onSelected(Movie movie) {
                    setFeatured(movie, false);
                }
            });
        }
        if (mAmb != null) {
            mAmb.setPaletteListener(new AmbientBackdropView.PaletteListener() {
                @Override
                public void onPalette(int contentAccent) {
                    mContentAccent = Prefs.paletteTheme(HomeActivity.this)
                            ? contentAccent : 0;
                    applyPalette();
                }
            });
        }
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
        // Hero next recommendation (amen: steps the filmstrip selection)
        click(R.id.hero_next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeck != null) mDeck.nextSelection();
            }
        });
        // Deck action buttons (right-side swipe card panel)
        click(R.id.deck_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeck != null) mDeck.undo();
            }
        });
        click(R.id.deck_pass, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeck != null) mDeck.swipeLeft();
            }
        });
        click(R.id.deck_save, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeck != null) mDeck.swipeRight();
            }
        });
        // Quick appearance toggles in the footer dock
        click(R.id.dock_theme, new View.OnClickListener() {
            @Override
            public void onClick(View v) { cycleTheme(); }
        });
        click(R.id.dock_accent, new View.OnClickListener() {
            @Override
            public void onClick(View v) { cycleAccent(); }
        });
        click(R.id.dock_glass, new View.OnClickListener() {
            @Override
            public void onClick(View v) { cycleGlass(); }
        });
        applyTheme();
        updateClock();
        updateDockStatus();
        startClock();

        Weather.Data d = Weather.loadCache(this);
        if (d != null) renderWeather(d);
        maybeRefreshWeather();
        scheduleIdleWatch();
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
        if (mDeck != null) mDeck.setMovies(mRecommendations);
        if (!mRecommendations.isEmpty()) {
            setFeatured(mRecommendations.get(0), false);
        }
        refreshSavedShelf();
    }

    private void bindHomeApps() {
        LinearLayout row = (LinearLayout) findViewById(R.id.home_apps_row);
        if (row == null) return;
        row.removeAllViews();
        List<String> pkgs = homeAppPackages();
        for (final String pkg : pkgs) {
            View cell = getLayoutInflater().inflate(R.layout.item_home_app, row, false);
            cell.setLayoutParams(new LinearLayout.LayoutParams((int) Ui.dp(this, 108),
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            ImageView icon = (ImageView) cell.findViewById(R.id.home_app_icon);
            TextView label = (TextView) cell.findViewById(R.id.home_app_label);
            if (label != null) {
                label.setText(loadLabel(pkg));
                label.setTextColor(0xFFA5ADBE);
            }
            if (icon != null) {
                RoundedIconDrawable rd = new RoundedIconDrawable(Ui.dp(this, 14));
                icon.setImageDrawable(rd);
                icon.setTag(pkg);
                mLoader.queue(pkg, icon);
            }
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPackage(pkg, loadLabel(pkg));
                }
            });
            row.addView(cell);
        }
        // Add-apps tile (Apple TV style "+")
        View add = getLayoutInflater().inflate(R.layout.item_home_app, row, false);
        add.setLayoutParams(new LinearLayout.LayoutParams((int) Ui.dp(this, 108),
                LinearLayout.LayoutParams.WRAP_CONTENT));
        ImageView addIcon = (ImageView) add.findViewById(R.id.home_app_icon);
        TextView addLabel = (TextView) add.findViewById(R.id.home_app_label);
        if (addIcon != null) addIcon.setImageBitmap(plusIcon());
        if (addLabel != null) {
            addLabel.setText("Add apps");
            addLabel.setTextColor(0xFF6E788C);
        }
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, AppPickerActivity.class);
                i.putExtra(AppPickerActivity.EXTRA_MODE, AppPickerActivity.MODE_HOME);
                startActivity(i);
            }
        });
        row.addView(add);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(spacer);
    }

    private List<String> homeAppPackages() {
        List<String> out = new ArrayList<String>();
        String raw = Prefs.get(this, Prefs.K_HOME_DOCK, "");
        if (raw != null && raw.length() > 0) {
            for (String p : raw.split(",")) {
                String t = p.trim();
                if (t.length() > 0 && isInstalled(t) && !out.contains(t)) out.add(t);
            }
        }
        if (out.isEmpty()) {
            for (String p : Prefs.PREFERRED_HOME_APPS) {
                if (isInstalled(p) && !out.contains(p)) out.add(p);
            }
        }
        if (out.size() > 8) out = new ArrayList<String>(out.subList(0, 8));
        return out;
    }

    private android.graphics.Bitmap plusIcon() {
        int s = (int) Ui.dp(this, 40);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(s, s,
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFB8C0D0);
        p.setStrokeWidth(s * 0.10f);
        p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        c.drawLine(s * 0.30f, s * 0.50f, s * 0.70f, s * 0.50f, p);
        c.drawLine(s * 0.50f, s * 0.30f, s * 0.50f, s * 0.70f, p);
        return bmp;
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
        if (mDeck != null) mDeck.setSavedIds(mSaved);
        loadRecommendations(false);
        applyTheme();
        updateClock();
        updateDockStatus();
        startClock();
        startRotationWatch();
        Weather.Data d = Weather.loadCache(this);
        if (d != null) renderWeather(d);
        maybeRefreshWeather();
        bindResumePill();
        refreshSavedShelf();
        if (mSaver) {
            exitAmbientSaver();
        } else {
            scheduleIdleWatch();
        }
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
        stopIdleWatch();
        if (mSaver) {
            // Snap the shell back so a relaunch never shows a bare screen.
            mSaver = false;
            mUi.removeCallbacks(mSaverCycle);
            mSaverFeatured = null;
            if (mShell != null) {
                mShell.setAlpha(1f);
                mShell.setVisibility(View.VISIBLE);
            }
        }
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
        if (mDeck != null) {
            mDeck.setAccent(mAccent);
            mDeck.setAnimationsEnabled(!Prefs.reduceMotion(this));
        }
        updateFeaturedSave();
        updateQuickToggles();
        applyPalette();
        refreshSavedShelf();
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
        if (mAmb != null) mAmb.setMovie(movie);
        if (mHeroArt != null) mHeroArt.setMovie(movie);
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
        mHeroSaveLabel.setTextColor(saved ? contentAccent() : 0xFFF2F4F8);
        mSaveIcon.setColor(saved ? contentAccent() : 0xFFFFFFFF, 0);
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
        if (mDeck != null) mDeck.setSavedIds(mSaved);
        updateFeaturedSave();
        refreshSavedShelf();
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
        // Screensaver clock overlay: shown only while drifting.
        if (mSaverClock != null) {
            if (mSaver) {
                mSaverClock.setText(String.valueOf(hour) + ":"
                        + String.format(Locale.getDefault(), "%02d", now.getMinutes())
                        + (h24 ? "" : " " + ampm));
                mSaverClock.setVisibility(View.VISIBLE);
            } else {
                mSaverClock.setVisibility(View.GONE);
            }
        }
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
        final boolean deepLinkable = movie.linkKind.length() > 0 && movie.videoId.length() > 0;
        String message = movie.meta() + "\n\n" + movie.description
                + "\n\n" + movie.match + "% match for you";
        if (hasProvider) message += "\nAvailable in " + providerName;
        if (deepLinkable) message += "\nPlays the actual title";
        dialogBuilder()
                .setTitle(movie.title)
                .setMessage(message)
                .setPositiveButton(deepLinkable ? "Play now · " + providerName
                                : "Open " + providerName,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchTitle(movie);
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
        // Launch apps in their own task, exactly like the stock launcher does.
        // Stripping NEW_TASK pulled apps into OBAA's task, and some apps —
        // notably Fire TV YouTube — unwind the whole task (including OBAA's
        // Home screen) when their activity finishes.
        // Continue Watching: remember the last app launched from home.
        if (pkg.length() > 0) {
            Prefs.put(this, Prefs.K_LAST_PKG, pkg);
            Prefs.put(this, Prefs.K_LAST_LABEL, label);
            // Learning: which providers the household opens, and the pill
            // falls back to app-level resume (no stale title kept).
            Prefs.bumpAffinity(this, "p:" + pkg);
            Prefs.clearLastTitle(this);
            HomeKeyWatcherService.noteAppLaunch();
        }
        try {
            startActivity(intent);
        } catch (Throwable t) {
            toast("Could not open " + label);
        }
    }

    // Real metadata: keyless deep links -----------------------------------------

    /** Opens the actual title when a verified keyless deep link exists
     *  (public-domain YouTube classics, Netflix title pages), falling back
     *  to the provider app. Also learns the household's taste. */
    private void launchTitle(Movie m) {
        if (m == null) return;
        Prefs.bumpAffinity(this, "g:" + Recommendations.genreKey(m.genre));
        String pkg = m.providerPkg.length() > 0 ? m.providerPkg : PKG_PRIME;
        Prefs.bumpAffinity(this, "p:" + pkg);
        Prefs.putLastTitle(this, m.linkKind, m.videoId, m.title, pkg);
        Uri uri = deepLinkUri(m.linkKind, m.videoId);
        if (uri != null) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(uri);
            String linkPkg = deepLinkPkg(m.linkKind);
            if (linkPkg.length() > 0 && isInstalled(linkPkg)) i.setPackage(linkPkg);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            HomeKeyWatcherService.noteAppLaunch();
            try {
                startActivity(i);
                return;
            } catch (Throwable ignored) {
                // fall through to the provider app below
            }
        }
        launchPackage(pkg, m.providerName.length() > 0 ? m.providerName : "Prime Video");
    }

    /** Re-opens whatever was last played: title deep link first, then app. */
    private void resumeLast() {
        String kind = Prefs.lastTitleKind(this);
        String id = Prefs.lastTitleId(this);
        Uri uri = deepLinkUri(kind, id);
        if (uri != null) {
            String linkPkg = deepLinkPkg(kind);
            if (linkPkg.length() == 0 || isInstalled(linkPkg)) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(uri);
                if (linkPkg.length() > 0) i.setPackage(linkPkg);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                HomeKeyWatcherService.noteAppLaunch();
                try {
                    startActivity(i);
                    return;
                } catch (Throwable ignored) {
                    // fall back below
                }
            }
            String pkg = Prefs.lastTitlePkg(this);
            if (pkg.length() > 0) {
                String title = Prefs.lastTitle(this);
                launchPackage(pkg, title.length() > 0 ? title : Prefs.lastLabel(this));
                return;
            }
        }
        String pkg = Prefs.lastPackage(this);
        if (pkg.length() > 0) {
            launchPackage(pkg, Prefs.lastLabel(this));
        } else {
            toast("Nothing to resume yet");
        }
    }

    /** https://www.youtube.com/watch?v=<id> / nflx://www.netflix.com/title/<id>;
     *  null when absent. Package-private static so SelfTest can assert the
     *  exact URIs. */
    static Uri deepLinkUri(String kind, String videoId) {
        if (kind == null || videoId == null
                || kind.length() == 0 || videoId.length() == 0) {
            return null;
        }
        if ("yt".equals(kind)) {
            // Fire TV's cobalt YouTube app does not register the vnd.youtube:
            // scheme, but does handle watch URLs; pinned to its package so
            // the full-film page opens directly.
            return Uri.parse("https://www.youtube.com/watch?v=" + videoId);
        }
        if ("nf".equals(kind)) return Uri.parse("nflx://www.netflix.com/title/" + videoId);
        return null;
    }

    /** The package that owns each deep-link kind ("" lets any handler open). */
    static String deepLinkPkg(String kind) {
        if ("yt".equals(kind)) return "com.amazon.firetv.youtube";
        if ("nf".equals(kind)) return "com.netflix.ninja";
        return "";
    }

    /** UI accent that leans toward the current scene's palette (feature 1). */
    private int contentAccent() {
        if (mContentAccent != 0) return Ui.mix(mAccent, mContentAccent, 0.45f);
        return mAccent;
    }

    /** Pushes the ambient scene colour into every accent-tinted surface. */
    private void applyPalette() {
        int accent = contentAccent();
        if (mHeroEyebrow != null) mHeroEyebrow.setTextColor(accent);
        if (mDeck != null) mDeck.setAccent(accent);
        View heart = findViewById(R.id.deck_save_icon);
        if (heart instanceof IconView) ((IconView) heart).setColor(accent, 0);
        TextView count = (TextView) findViewById(R.id.deck_count);
        if (count != null) count.setTextColor((accent & 0x00FFFFFF) | 0xB2000000);
        View shelfLabel = findViewById(R.id.saved_shelf_label);
        if (shelfLabel instanceof TextView) ((TextView) shelfLabel).setTextColor(accent);
        LinearLayout tiles = (LinearLayout) findViewById(R.id.saved_tiles);
        if (tiles != null) {
            for (int i = 0; i < tiles.getChildCount(); i++) {
                View v = tiles.getChildAt(i);
                if (v instanceof MovieCardView) ((MovieCardView) v).setAccent(accent);
            }
        }
        if (mResumeIcon != null && mHeroResume != null && mHeroResume.isShown()) {
            mResumeIcon.setColor(accent, 0);
        }
        if (mDockAddIcon != null) mDockAddIcon.setColor(accent, 0);
    }

    /** Saved ids resolved to titles (recommendation pool first, then catalog). */
    private List<Movie> resolveSavedMovies() {
        List<Movie> out = new ArrayList<Movie>();
        Set<String> seen = new HashSet<String>();
        for (String id : mSaved) {
            Movie m = null;
            for (Movie r : mRecommendations) {
                if (r.id.equals(id)) { m = r; break; }
            }
            if (m == null) {
                for (Movie cm : MovieCatalog.MOVIES) {
                    if (cm.id.equals(id)) { m = cm; break; }
                }
            }
            if (m != null && seen.add(m.title.toLowerCase(Locale.US))) out.add(m);
        }
        return out;
    }

    /** Feature 2: the watchlist shelf above the app tiles. */
    private void refreshSavedShelf() {
        LinearLayout tiles = (LinearLayout) findViewById(R.id.saved_tiles);
        if (tiles == null) return;
        View shelf = findViewById(R.id.saved_shelf);
        List<Movie> items = resolveSavedMovies();
        if (items.isEmpty()) {
            tiles.removeAllViews();
            if (shelf != null) shelf.setVisibility(View.GONE);
            return;
        }
        if (shelf != null) shelf.setVisibility(View.VISIBLE);
        tiles.removeAllViews();
        int accent = contentAccent();
        for (Movie m : items) {
            MovieCardView card = new MovieCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) Ui.dp(this, 112), (int) Ui.dp(this, 142));
            if (tiles.getChildCount() > 0) lp.leftMargin = (int) Ui.dp(this, 10);
            card.setLayoutParams(lp);
            card.setMovie(m);
            card.setAccent(accent);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v instanceof MovieCardView) {
                        showMovieDetails(((MovieCardView) v).getMovie());
                    }
                }
            });
            tiles.addView(card);
        }
    }

    /** Feature 3: resume pill — last deep-linked title, else last app. */
    private void bindResumePill() {
        if (mHeroResume == null) return;
        String kind = Prefs.lastTitleKind(this);
        String id = Prefs.lastTitleId(this);
        Uri titleUri = deepLinkUri(kind, id);
        if (titleUri != null && Prefs.lastTitle(this).length() > 0) {
            String linkPkg = deepLinkPkg(kind);
            if (linkPkg.length() == 0 || isInstalled(linkPkg)) {
                mHeroResume.setVisibility(View.VISIBLE);
                mHeroResumeLabel.setText("RESUME · "
                        + Prefs.lastTitle(this).toUpperCase(Locale.US));
                mHeroResumeLabel.setTextColor(0xFFF2F4F8);
                if (mResumeIcon != null) mResumeIcon.setColor(contentAccent(), 0);
                return;
            }
        }
        String pkg = Prefs.lastPackage(this);
        String label = Prefs.lastLabel(this);
        if (pkg.length() == 0 || !isInstalled(pkg)) {
            mHeroResume.setVisibility(View.GONE);
            return;
        }
        String name = label.length() > 0 ? label : loadLabel(pkg);
        mHeroResume.setVisibility(View.VISIBLE);
        mHeroResumeLabel.setText("RESUME · " + name.toUpperCase(Locale.US));
        mHeroResumeLabel.setTextColor(0xFFF2F4F8);
        if (mResumeIcon != null) mResumeIcon.setColor(contentAccent(), 0);
    }

    // Feature 5: quick appearance toggles --------------------------------------

    private void cycleTheme() {
        String[] keys = {"midnight", "graphite", "aurora"};
        String[] accents = {"amber", "blue", "violet"};
        int next = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(Prefs.theme(this))) { next = (i + 1) % keys.length; break; }
        }
        Prefs.put(this, Prefs.K_THEME, keys[next]);
        Prefs.put(this, Prefs.K_ACCENT, accents[next]);
        applyTheme();
        toast(Prefs.themeName(this) + " theme");
    }

    private void cycleAccent() {
        String[] keys = {"amber", "coral", "violet", "mint", "blue"};
        int next = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(Prefs.accentKey(this))) { next = (i + 1) % keys.length; break; }
        }
        Prefs.put(this, Prefs.K_ACCENT, keys[next]);
        applyTheme();
        toast(Prefs.accentName(this) + " accent");
    }

    private void cycleGlass() {
        int[] steps = {0, 45, 62, 80, 100};
        int cur = Prefs.glassPercent(this);
        int next = steps[0];
        int found = -1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == cur) { found = i; break; }
        }
        if (found >= 0) {
            next = steps[(found + 1) % steps.length];
        } else {
            for (int s : steps) {
                if (s > cur) { next = s; break; }
            }
        }
        Prefs.putInt(this, Prefs.K_GLASS, next);
        applyTheme();
        toast("Glass " + next);
    }

    private void updateQuickToggles() {
        TextView t = (TextView) findViewById(R.id.dock_theme_label);
        if (t != null) t.setText(Prefs.themeName(this).toUpperCase(Locale.US));
        IconView ti = (IconView) findViewById(R.id.dock_theme_icon);
        if (ti != null) { ti.setMode(IconView.DISPLAY); ti.setColor(mAccent, 0); }
        TextView a = (TextView) findViewById(R.id.dock_accent_label);
        if (a != null) a.setText(Prefs.accentName(this).toUpperCase(Locale.US));
        IconView ai = (IconView) findViewById(R.id.dock_accent_icon);
        if (ai != null) { ai.setMode(IconView.PALETTE); ai.setColor(mAccent, 0); }
        TextView g = (TextView) findViewById(R.id.dock_glass_label);
        if (g != null) g.setText(Prefs.glassPercent(this) + "%");
        IconView gi = (IconView) findViewById(R.id.dock_glass_icon);
        if (gi != null) { gi.setMode(IconView.AURORA); gi.setColor(mAccent, 0); }
    }

    // Feature 4: ambient screensaver -------------------------------------------

    private void scheduleIdleWatch() {
        stopIdleWatch();
        mIdleCheck = new Runnable() {
            @Override
            public void run() {
                if (sResumed && !mSaver) enterAmbientSaver();
            }
        };
        mUi.postDelayed(mIdleCheck, IDLE_TO_SAVER_MS);
    }

    private void stopIdleWatch() {
        if (mIdleCheck != null) {
            mUi.removeCallbacks(mIdleCheck);
            mIdleCheck = null;
        }
    }

    @Override
    public void onUserInteraction() {
        // Any key/touch wakes the saver and resets the idle timer.
        if (mSaver) exitAmbientSaver();
        else scheduleIdleWatch();
    }

    private void enterAmbientSaver() {
        if (mSaver || Prefs.reduceMotion(this) || mShell == null || mAmb == null) return;
        final List<Movie> pool = saverPool();
        if (pool.isEmpty()) {
            scheduleIdleWatch();
            return;
        }
        mSaver = true;
        mSaverFeatured = mFeatured;
        mSaverIndex = 0;
        mShell.animate().alpha(0f).setDuration(650).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (mSaver && mShell != null) mShell.setVisibility(View.GONE);
            }
        }).start();
        updateClock();
        cycleSaver(pool);
    }

    private void cycleSaver(final List<Movie> pool) {
        if (!mSaver || pool.isEmpty()) return;
        if (mSaverIndex >= pool.size()) mSaverIndex = 0;
        setFeatured(pool.get(mSaverIndex), false);
        mSaverIndex++;
        mUi.removeCallbacks(mSaverCycle);
        mSaverCycle = new Runnable() {
            @Override
            public void run() { cycleSaver(pool); }
        };
        mUi.postDelayed(mSaverCycle, SAVER_CYCLE_MS);
    }

    private void exitAmbientSaver() {
        if (!mSaver) {
            scheduleIdleWatch();
            return;
        }
        mSaver = false;
        mUi.removeCallbacks(mSaverCycle);
        if (mShell != null) {
            mShell.setVisibility(View.VISIBLE);
            mShell.setAlpha(0f);
            mShell.animate().alpha(1f).setDuration(300).start();
        }
        updateClock();
        if (mSaverFeatured != null) setFeatured(mSaverFeatured, false);
        mSaverFeatured = null;
        scheduleIdleWatch();
    }

    /** Drift order for the saver: saved titles first, then recommendations. */
    private List<Movie> saverPool() {
        List<Movie> pool = new ArrayList<Movie>();
        Set<String> seen = new HashSet<String>();
        for (Movie m : resolveSavedMovies()) {
            if (m != null && seen.add(m.title.toLowerCase(Locale.US))) pool.add(m);
        }
        for (Movie m : mRecommendations) {
            if (m != null && seen.add(m.title.toLowerCase(Locale.US))) pool.add(m);
        }
        return pool;
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
