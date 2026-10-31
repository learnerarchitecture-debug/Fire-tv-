package com.aurora.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
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
    private IconView mUndoIcon, mPassIcon, mDeckSaveIcon, mDockSettingsIcon, mDockAppsIcon;
    private SwipeDeckView mDeck;
    private TextView mDeckCount;
    private FrameLayout mUndoBtn;
    private LinearLayout mRail;
    private final List<MovieCardView> mRailCards = new ArrayList<MovieCardView>();
    private LinearLayout mFooterApp1, mFooterApp2, mFooterApp3;
    private ImageView mFooterIcon1, mFooterIcon2, mFooterIcon3;
    private TextView mFooterLabel1, mFooterLabel2, mFooterLabel3;
    private TextView mDockStatus;
    private LinearLayout mDockAddApps;
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
        mUndoIcon = (IconView) findViewById(R.id.undo_icon);
        mPassIcon = (IconView) findViewById(R.id.pass_icon);
        mDeckSaveIcon = (IconView) findViewById(R.id.deck_save_icon);
        mDockSettingsIcon = (IconView) findViewById(R.id.dock_settings_icon);
        mDockAppsIcon = (IconView) findViewById(R.id.dock_apps_icon);
        mDeck = (SwipeDeckView) findViewById(R.id.swipe_deck);
        mDeckCount = (TextView) findViewById(R.id.deck_count);
        mUndoBtn = (FrameLayout) findViewById(R.id.deck_undo);
        mRail = (LinearLayout) findViewById(R.id.rail);
        mFooterApp1 = (LinearLayout) findViewById(R.id.footer_app1);
        mFooterApp2 = (LinearLayout) findViewById(R.id.footer_app2);
        mFooterApp3 = (LinearLayout) findViewById(R.id.footer_app3);
        mFooterIcon1 = (ImageView) findViewById(R.id.footer_app1_icon);
        mFooterIcon2 = (ImageView) findViewById(R.id.footer_app2_icon);
        mFooterIcon3 = (ImageView) findViewById(R.id.footer_app3_icon);
        mFooterLabel1 = (TextView) findViewById(R.id.footer_app1_label);
        mFooterLabel2 = (TextView) findViewById(R.id.footer_app2_label);
        mFooterLabel3 = (TextView) findViewById(R.id.footer_app3_label);
        mDockStatus = (TextView) findViewById(R.id.dock_status);
        mDockAddApps = (LinearLayout) findViewById(R.id.dock_add_apps);
        mDockAddIcon = (IconView) findViewById(R.id.dock_add_icon);
        if (mDockAddIcon != null) {
            mDockAddIcon.setMode(IconView.PLUS);
            mDockAddIcon.setColor(Prefs.accent(this), 0);
        }
        if (mDockAddApps != null) {
            mDockAddApps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, AppPickerActivity.class));
                    overridePendingTransition(0, 0);
                }
            });
        }

        mBrandIcon.setMode(IconView.AURORA);
        mBrandIcon.setColor(0xFFFFFFFF, 0);
        mPlayIcon.setMode(IconView.PLAY);
        mPlayIcon.setColor(0xFF12151C, 0);
        mSaveIcon.setMode(IconView.HEART);
        mSaveIcon.setColor(0xFFFFFFFF, 0);
        mUndoIcon.setMode(IconView.UNDO);
        mUndoIcon.setColor(0xFFA5ADBE, 0);
        mPassIcon.setMode(IconView.CLOSE);
        mPassIcon.setColor(0xFFFF9A8E, 0);
        mDeckSaveIcon.setMode(IconView.HEART);
        mDeckSaveIcon.setColor(0xFF6FE3B0, 0);
        mDockSettingsIcon.setMode(IconView.GEAR);
        mDockAppsIcon.setMode(IconView.APPS);
        mSettingsIcon.setMode(IconView.GEAR);

        mLoader = new IconLoader(this, 96);
        mSaved = Prefs.savedIds(this);
        mDeck.setSavedIds(mSaved);
        mDeck.setListener(new SwipeDeckView.Listener() {
            @Override
            public void onSwiped(Movie movie, boolean saved) {
                if (saved) {
                    if (mSaved.add(movie.id)) {
                        Prefs.putSavedIds(HomeActivity.this, mSaved);
                        mDeck.setSavedIds(mSaved);
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
                    mDeck.setSavedIds(mSaved);
                    updateFeaturedSave();
                    toast("Undone · removed from watchlist");
                } else {
                    toast("Undone");
                }
            }

            @Override
            public void onDeckChanged(int position, int total, boolean canUndo) {
                if (mDeckCount != null) {
                    mDeckCount.setText((position + 1) + " / " + total);
                }
                if (mUndoBtn != null) {
                    boolean undoFocused = mUndoBtn.isFocused();
                    mUndoBtn.setEnabled(canUndo);
                    mUndoBtn.setFocusable(canUndo);
                    mUndoBtn.setClickable(canUndo);
                    mUndoBtn.setAlpha(canUndo ? 1f : 0.35f);
                    mUndoIcon.setColor(canUndo ? 0xFFF2F4F8 : 0xFFA5ADBE, 0);
                    if (!canUndo && undoFocused) {
                        mDeck.requestFocus();
                    }
                }
            }

            @Override
            public void onOpen(Movie movie) {
                setFeatured(movie, false);
                showMovieDetails(movie);
            }
        });

        setupDockApps();
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
        findViewById(R.id.settings_btn).setOnClickListener(openSettings);
        findViewById(R.id.dock_settings).setOnClickListener(openSettings);
        findViewById(R.id.dock_apps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppDrawer();
            }
        });
        findViewById(R.id.avatar).setOnClickListener(openSettings);
        mWeatherChip.setOnClickListener(openSettings);

        mHeroPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeatured != null) showMovieDetails(mFeatured);
            }
        });
        mHeroSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeatured == null) return;
                boolean saved = toggleSaved(mFeatured.id);
                toast(saved ? "Saved to watchlist" : "Removed from watchlist");
            }
        });
        mUndoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeck.undo();
            }
        });
        findViewById(R.id.deck_pass).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeck.swipeLeft();
            }
        });
        findViewById(R.id.deck_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeck.swipeRight();
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

    private void buildRail() {
        mRailCards.clear();
        mRail.removeAllViews();
        if (mRecommendations.size() < 2) return;
        int count = Math.min(4, mRecommendations.size() - 1);
        for (int i = 0; i < count; i++) {
            final Movie movie = mRecommendations.get(i + 1);
            MovieCardView card = new MovieCardView(this);
            card.setMovie(movie);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i < count - 1) lp.rightMargin = (int) Ui.dp(this, 8);
            card.setLayoutParams(lp);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setFeatured(movie, true);
                }
            });
            mRail.addView(card);
            mRailCards.add(card);
        }
    }

    /**
     * Rebuilds the featured pick, rail and deck when the five-minute window
     * advances or the set of installed OTT providers changes.
     */
    private void loadRecommendations(boolean force) {
        int rotation = Recommendations.rotationForNow(this);
        String signature = Recommendations.signature(this);
        if (!force && rotation == mRotation && signature.equals(mProviderSignature)) return;
        mRotation = rotation;
        mProviderSignature = signature;
        mRecommendations = Recommendations.build(this, rotation);
        mDeck.setMovies(mRecommendations);
        buildRail();
        if (!mRecommendations.isEmpty()) {
            setFeatured(mRecommendations.get(0), false);
        }
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

    private void setupDockApps() {
        bindDockApp(mFooterApp1, mFooterIcon1, mFooterLabel1,
                PKG_YOUTUBE, PKG_YOUTUBE_ALT,
                new BrandIconDrawable(0xFFFF3B30, 0xFFB31217, "", BrandIconDrawable.PLAY),
                "YouTube", Prefs.K_SHOW_YOUTUBE);
        bindDockApp(mFooterApp2, mFooterIcon2, mFooterLabel2,
                PKG_NETFLIX, PKG_NETFLIX_ALT,
                new BrandIconDrawable(0xFFE50914, 0xFF4A0508, "N", BrandIconDrawable.LETTER),
                "Netflix", Prefs.K_SHOW_NETFLIX);
        bindDockApp(mFooterApp3, mFooterIcon3, mFooterLabel3,
                PKG_PRIME, null,
                new BrandIconDrawable(0xFF1F8FFF, 0xFF0B3C7A, "P", BrandIconDrawable.LETTER),
                "Prime Video", Prefs.K_SHOW_PRIME);
    }

    private void bindDockApp(LinearLayout item, ImageView icon, TextView label,
                             String primary, String alt, Drawable fallback,
                             final String displayName, final String prefKey) {
        final String resolved = isInstalled(primary) ? primary
                : (alt != null && isInstalled(alt) ? alt : null);
        label.setText(displayName);
        if (resolved != null) {
            RoundedIconDrawable rd = new RoundedIconDrawable(Ui.dp(this, 7));
            icon.setImageDrawable(rd);
            icon.setTag(resolved);
            mLoader.queue(resolved, icon);
            label.setTextColor(0xFFA5ADBE);
        } else {
            icon.setImageDrawable(fallback);
            label.setTextColor(0xFF6E788C);
        }
        item.setVisibility(Prefs.showDockApp(this, prefKey) ? View.VISIBLE : View.GONE);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resolved == null) {
                    toast(displayName + " isn't installed on this Fire TV");
                } else {
                    launchPackage(resolved, displayName);
                }
            }
        });
    }

    private void updateDockVisibility() {
        mFooterApp1.setVisibility(Prefs.showDockApp(this, Prefs.K_SHOW_YOUTUBE)
                ? View.VISIBLE : View.GONE);
        mFooterApp2.setVisibility(Prefs.showDockApp(this, Prefs.K_SHOW_NETFLIX)
                ? View.VISIBLE : View.GONE);
        mFooterApp3.setVisibility(Prefs.showDockApp(this, Prefs.K_SHOW_PRIME)
                ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sResumed = true;
        Ui.immersive(this);
        mSaved = Prefs.savedIds(this);
        mDeck.setSavedIds(mSaved);
        loadRecommendations(false);
        applyTheme();
        updateDockVisibility();
        updateClock();
        updateDockStatus();
        startClock();
        startRotationWatch();
        Weather.Data d = Weather.loadCache(this);
        if (d != null) renderWeather(d);
        maybeRefreshWeather();
        if (mFirstResume) {
            mFirstResume = false;
            mHeroPlay.requestFocus();
        } else {
            View focus = getCurrentFocus();
            if (focus == null || !focus.isShown()) {
                mHeroPlay.requestFocus();
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
        mBg.setTheme(theme, mAccent);
        mSettingsIcon.setColor(mAccent, 0);
        mDockSettingsIcon.setColor(mAccent, 0);
        mDockAppsIcon.setColor(mAccent, 0);
        mHeroEyebrow.setTextColor(mAccent);
        mHeroPlay.setBackgroundDrawable(primaryButtonBackground());
        findViewById(R.id.deck_undo).setBackgroundDrawable(actionButtonBackground());
        findViewById(R.id.deck_pass).setBackgroundDrawable(actionButtonBackground());
        findViewById(R.id.deck_save).setBackgroundDrawable(actionButtonBackground());
        mDeck.setAccent(mAccent);
        mDeck.setAnimationsEnabled(!Prefs.reduceMotion(this));
        for (MovieCardView c : mRailCards) {
            c.setAccent(mAccent);
            c.setAnimateFocus(!Prefs.reduceMotion(this));
        }
        mWeatherChip.setVisibility(Prefs.showWeather(this) ? View.VISIBLE : View.GONE);
        Ui.applyGlassAlpha(mRoot, Prefs.glassPercent(this));
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
        mDeck.setSavedIds(mSaved);
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
