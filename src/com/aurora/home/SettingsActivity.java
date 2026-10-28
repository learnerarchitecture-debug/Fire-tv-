package com.aurora.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SettingsActivity extends Activity {

    private static final String PKG_YOUTUBE = "com.amazon.firetv.youtube";
    private static final String PKG_YOUTUBE_ALT = "com.google.android.youtube";
    private static final String PKG_NETFLIX = "com.netflix.mediaclient";
    private static final String PKG_NETFLIX_ALT = "com.netflix.ninja";
    private static final String PKG_PRIME = "com.amazon.avod";

    private static final String[] NAV_TITLES = {
            "General", "Appearance", "Network", "Display & Sound", "Apps", "Privacy", "About"
    };
    private static final int[] NAV_ICONS = {
            IconView.GEAR, IconView.PALETTE, IconView.WIFI,
            IconView.DISPLAY, IconView.APPS, IconView.LOCK, IconView.INFO
    };

    private AuroraBackgroundView mBg;
    private LinearLayout mNavContainer;
    private LinearLayout mPaneContainer;
    private ScrollView mScroll;
    private LayoutInflater mInflater;
    private IconLoader mLoader;
    private IconView mSettingsLogo;
    private IconView mCloseIcon;
    private final List<View> mNavItems = new ArrayList<View>();
    private LinearLayout mPreviewGlass;
    private int mPane = 0;
    private int mAccent = 0xFFFFB45E;
    private boolean mFirstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.immersive(this);
        setContentView(R.layout.activity_settings);

        mBg = (AuroraBackgroundView) findViewById(R.id.bg);
        mNavContainer = (LinearLayout) findViewById(R.id.nav_container);
        mPaneContainer = (LinearLayout) findViewById(R.id.pane_container);
        mScroll = (ScrollView) findViewById(R.id.content_scroll);
        mSettingsLogo = (IconView) findViewById(R.id.settings_logo);
        mCloseIcon = (IconView) findViewById(R.id.close_icon);
        mInflater = LayoutInflater.from(this);
        mLoader = new IconLoader(this, 96);

        mSettingsLogo.setMode(IconView.AURORA);
        mSettingsLogo.setColor(0xFFFFFFFF, 0);
        mCloseIcon.setMode(IconView.CLOSE);
        mCloseIcon.setColor(0xFFFFFFFF, 0);
        findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.profile_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPane(6, true);
            }
        });

        applyTheme();
        buildNav();
        showPane(0, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Ui.immersive(this);
        if (mFirstResume) {
            mFirstResume = false;
            return;
        }
        applyTheme();
        showPane(mPane, true);
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
        mAccent = Prefs.accent(this);
        applyAccentTheme();
        mBg.setTheme(Prefs.theme(this), mAccent);
        mSettingsLogo.setColor(0xFFFFFFFF, 0);
        mCloseIcon.setColor(0xFFFFFFFF, 0);
    }

    /**
     * API 22 switches and edit fields read the activity theme accent. The
     * overlay keeps those platform widgets in step with the selected accent.
     */
    private void applyAccentTheme() {
        getTheme().applyStyle(accentStyle(), true);
    }

    private int accentStyle() {
        String a = Prefs.accentKey(this);
        if ("coral".equals(a)) return R.style.AccentCoral;
        if ("violet".equals(a)) return R.style.AccentViolet;
        if ("mint".equals(a)) return R.style.AccentMint;
        if ("blue".equals(a)) return R.style.AccentBlue;
        return R.style.AccentAmber;
    }

    private void buildNav() {
        mNavContainer.removeAllViews();
        mNavItems.clear();
        for (int i = 0; i < NAV_TITLES.length; i++) {
            final int index = i;
            View item = mInflater.inflate(R.layout.item_nav, mNavContainer, false);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getLayoutParams();
            if (lp == null) lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(30));
            lp.bottomMargin = dp(2);
            item.setLayoutParams(lp);
            IconView icon = (IconView) item.findViewById(R.id.nav_icon);
            TextView title = (TextView) item.findViewById(R.id.nav_title);
            icon.setMode(NAV_ICONS[i]);
            title.setText(NAV_TITLES[i]);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPane(index, false);
                }
            });
            item.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN
                            && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (index != mPane) showPane(index, false);
                        // The pane is rebuilt synchronously, but its children
                        // only become focusable after the next layout pass.
                        mPaneContainer.post(new Runnable() {
                            @Override
                            public void run() {
                                View target = firstFocusable(mPaneContainer);
                                if (target != null) target.requestFocus();
                            }
                        });
                        return true;
                    }
                    return false;
                }
            });
            item.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    updateNavColors(v, index);
                }
            });
            mNavContainer.addView(item);
            mNavItems.add(item);
        }
        updateNavSelection();
    }

    private View firstFocusable(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            if (child.isFocusable()) return child;
            if (child instanceof ViewGroup) {
                View nested = firstFocusable((ViewGroup) child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private void updateNavSelection() {
        for (int i = 0; i < mNavItems.size(); i++) {
            mNavItems.get(i).setSelected(i == mPane);
            updateNavColors(mNavItems.get(i), i);
        }
    }

    private void updateNavColors(View item, int index) {
        TextView title = (TextView) item.findViewById(R.id.nav_title);
        IconView icon = (IconView) item.findViewById(R.id.nav_icon);
        View dot = item.findViewById(R.id.nav_dot);
        boolean selected = index == mPane;
        boolean focused = item.isFocused();
        title.setTextColor(focused ? 0xFFFFFFFF : (selected ? mAccent : 0xFFA5ADBE));
        icon.setColor(focused ? 0xFFFFFFFF : (selected ? mAccent : 0xFF8A93A6), 0);
        dot.setVisibility(selected ? View.VISIBLE : View.GONE);
        dot.setBackgroundColor(mAccent);
    }

    private void showPane(int index, boolean focusNav) {
        mPane = index;
        updateNavSelection();
        mPaneContainer.removeAllViews();
        mPreviewGlass = null;
        switch (index) {
            case 0: buildGeneralPane(); break;
            case 1: buildAppearancePane(); break;
            case 2: buildNetworkPane(); break;
            case 3: buildDisplayPane(); break;
            case 4: buildAppsPane(); break;
            case 5: buildPrivacyPane(); break;
            default: buildAboutPane(); break;
        }
        mScroll.scrollTo(0, 0);
        if (focusNav && index < mNavItems.size()) {
            mNavItems.get(index).requestFocus();
        }
    }

    private void addHeader(String title, String lead) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(22);
        t.setTextColor(0xFFF2F4F8);
        t.setTypeface(Ui.BOLD);
        t.setIncludeFontPadding(false);
        mPaneContainer.addView(t);

        TextView l = new TextView(this);
        l.setText(lead);
        l.setTextSize(9);
        l.setTextColor(0xFFA5ADBE);
        l.setIncludeFontPadding(false);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(2);
        mPaneContainer.addView(l, llp);

        View bar = new View(this);
        bar.setBackgroundColor(mAccent);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(24), dp(2));
        blp.topMargin = dp(8);
        mPaneContainer.addView(bar, blp);
    }

    private void addSectionLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(7.5f);
        t.setTextColor(0xFF6E788C);
        t.setTypeface(Ui.BOLD);
        t.setLetterSpacing(0.2f);
        t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(13);
        lp.bottomMargin = dp(4);
        mPaneContainer.addView(t, lp);
    }

    private LinearLayout addCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.settings_card_bg);
        card.setPadding(dp(3), dp(3), dp(3), dp(3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(7);
        mPaneContainer.addView(card, lp);
        return card;
    }

    private void addDivider(LinearLayout card) {
        if (card.getChildCount() == 0) return;
        View d = new View(this);
        d.setBackgroundColor(0x1FFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.leftMargin = dp(42);
        card.addView(d, lp);
    }

    private View inflateRow(LinearLayout card, int icon, String title, String summary) {
        addDivider(card);
        View row = mInflater.inflate(R.layout.item_setting, card, false);
        IconView ic = (IconView) row.findViewById(R.id.s_icon);
        ic.setMode(icon);
        ic.setColor(mAccent, Ui.alpha(mAccent, 0x2E));
        ((TextView) row.findViewById(R.id.s_title)).setText(title);
        ((TextView) row.findViewById(R.id.s_summary)).setText(summary);
        Switch sw = (Switch) row.findViewById(R.id.s_switch);
        sw.setVisibility(View.GONE);
        Ui.tintSwitch(sw, mAccent);
        IconView chev = (IconView) row.findViewById(R.id.s_chevron);
        chev.setMode(IconView.CHEVRON);
        chev.setColor(0xFF6E788C, 0);
        card.addView(row);
        return row;
    }

    private void addToggleRow(LinearLayout card, int icon, String title, String summary,
                              final String key, final boolean def) {
        View row = inflateRow(card, icon, title, summary);
        final Switch sw = (Switch) row.findViewById(R.id.s_switch);
        sw.setVisibility(View.VISIBLE);
        sw.setChecked(Prefs.getBool(this, key, def));
        row.findViewById(R.id.s_chevron).setVisibility(View.GONE);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean value = !Prefs.getBool(SettingsActivity.this, key, def);
                Prefs.putBool(SettingsActivity.this, key, value);
                sw.setChecked(value);
            }
        });
    }

    private void addChevronRow(LinearLayout card, int icon, String title, String summary,
                               View.OnClickListener click) {
        View row = inflateRow(card, icon, title, summary);
        row.findViewById(R.id.s_switch).setVisibility(View.GONE);
        row.findViewById(R.id.s_chevron).setVisibility(View.VISIBLE);
        row.setOnClickListener(click);
    }

    private void addSeekRow(LinearLayout card, String title, int progress) {
        addDivider(card);
        View row = mInflater.inflate(R.layout.item_seekbar, card, false);
        final TextView titleView = (TextView) row.findViewById(R.id.seek_title);
        final TextView valueView = (TextView) row.findViewById(R.id.seek_value);
        final SeekBar seek = (SeekBar) row.findViewById(R.id.seek_bar);
        titleView.setText(title);
        valueView.setText(progress + "%");
        seek.setProgress(progress);
        seek.setProgressTintList(ColorStateList.valueOf(mAccent));
        seek.setThumbTintList(ColorStateList.valueOf(mAccent));
        seek.setBackgroundResource(R.drawable.setting_row_bg);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                valueView.setText(value + "%");
                Prefs.putInt(SettingsActivity.this, Prefs.K_GLASS, value);
                if (mPreviewGlass != null) {
                    Ui.applyGlassAlpha(mPreviewGlass, value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        seek.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                titleView.setTextColor(hasFocus ? mAccent : 0xFFF2F4F8);
            }
        });
        card.addView(row);
    }

    private void addAppRow(LinearLayout card, final String primary, final String alt,
                           String title, Drawable fallback, final String prefKey) {
        addDivider(card);
        View row = mInflater.inflate(R.layout.item_app_setting, card, false);
        final ImageView icon = (ImageView) row.findViewById(R.id.a_icon);
        final TextView titleView = (TextView) row.findViewById(R.id.a_title);
        final TextView summaryView = (TextView) row.findViewById(R.id.a_summary);
        final Switch sw = (Switch) row.findViewById(R.id.a_switch);
        final String resolved = isInstalled(primary) ? primary
                : (alt != null && isInstalled(alt) ? alt : null);
        final boolean installed = resolved != null;
        titleView.setText(title);
        if (installed) {
            RoundedIconDrawable rd = new RoundedIconDrawable(Ui.dp(this, 6));
            icon.setImageDrawable(rd);
            icon.setTag(resolved);
            mLoader.queue(resolved, icon);
        } else {
            icon.setImageDrawable(fallback);
        }
        final boolean shown = Prefs.showDockApp(this, prefKey);
        summaryView.setText(appSummary(installed, shown));
        sw.setChecked(shown);
        Ui.tintSwitch(sw, mAccent);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean show = !Prefs.showDockApp(SettingsActivity.this, prefKey);
                Prefs.putBool(SettingsActivity.this, prefKey, show);
                sw.setChecked(show);
                summaryView.setText(appSummary(installed, show));
            }
        });
        card.addView(row);
    }

    private String appSummary(boolean installed, boolean shown) {
        if (!shown) return installed ? "Installed · hidden from dock" : "Not installed · hidden";
        return installed ? "Installed · shown in dock" : "Not installed · shortcut shown";
    }

    private void addNote(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(7.5f);
        t.setTextColor(0xFF6E788C);
        t.setLineSpacing(dp(1), 1f);
        t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(7);
        lp.bottomMargin = dp(4);
        mPaneContainer.addView(t, lp);
    }

    private void addPreviewCard() {
        FrameLayout preview = new FrameLayout(this);
        preview.setBackgroundResource(R.drawable.preview_card_bg);
        preview.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(108));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(7);
        preview.setLayoutParams(lp);

        AuroraBackgroundView mini = new AuroraBackgroundView(this);
        mini.setTheme(Prefs.theme(this), mAccent);
        preview.addView(mini, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout glass = new LinearLayout(this);
        glass.setOrientation(LinearLayout.HORIZONTAL);
        glass.setGravity(android.view.Gravity.CENTER_VERTICAL);
        glass.setBackgroundResource(R.drawable.preview_glass);
        glass.setPadding(dp(13), dp(10), dp(12), dp(10));
        glass.setTag("glass");
        Ui.applyGlassAlpha(glass, Prefs.glassPercent(this));
        mPreviewGlass = glass;
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        glp.setMargins(dp(8), dp(8), dp(8), dp(8));
        preview.addView(glass, glp);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText("OBAA HOME");
        label.setTextSize(6.5f);
        label.setTextColor(0xFFF2F4F8);
        label.setTypeface(Ui.MEDIUM);
        label.setLetterSpacing(0.18f);
        label.setIncludeFontPadding(false);
        brandRow.addView(label);
        TextView live = new TextView(this);
        live.setText("LIVE PREVIEW");
        live.setTextSize(5);
        live.setTextColor(0x99FFFFFF);
        live.setLetterSpacing(0.18f);
        live.setIncludeFontPadding(false);
        LinearLayout.LayoutParams liveLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        liveLp.leftMargin = dp(10);
        brandRow.addView(live, liveLp);
        text.addView(brandRow);

        TextView eyebrow = new TextView(this);
        eyebrow.setText("FEATURED · TRENDING");
        eyebrow.setTextSize(5.5f);
        eyebrow.setTextColor(mAccent);
        eyebrow.setTypeface(Ui.MEDIUM);
        eyebrow.setLetterSpacing(0.12f);
        eyebrow.setIncludeFontPadding(false);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        elp.topMargin = dp(6);
        text.addView(eyebrow, elp);

        TextView title = new TextView(this);
        title.setText("Dune: Part Two");
        title.setTextSize(15);
        title.setTextColor(0xFFF2F4F8);
        title.setTypeface(Ui.BOLD);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ttlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ttlp.topMargin = dp(1);
        text.addView(title, ttlp);

        TextView meta = new TextView(this);
        meta.setText("2024 · Sci-Fi · Epic · 2h 46m");
        meta.setTextSize(6);
        meta.setTextColor(0xFFA5ADBE);
        meta.setIncludeFontPadding(false);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = dp(3);
        text.addView(meta, mlp);

        glass.addView(text, tlp);

        PosterArtView poster = new PosterArtView(this);
        poster.setMovie(MovieCatalog.MOVIES.get(0));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(50), dp(70));
        plp.leftMargin = dp(12);
        glass.addView(poster, plp);

        mPaneContainer.addView(preview);
    }

    private void buildGeneralPane() {
        addHeader("General", getString(R.string.settings_lead_general));
        addSectionLabel("HOME");
        LinearLayout card = addCard();
        addToggleRow(card, IconView.CLOCK, "24-hour clock",
                "Show 18:30 instead of 6:30", Prefs.K_24H, false);
        addToggleRow(card, IconView.CALENDAR, "Show date",
                "Display the date under the clock", Prefs.K_SHOW_DATE, true);
        addToggleRow(card, IconView.CLOUD, "Show weather chip",
                "Keep current conditions in the top bar", Prefs.K_SHOW_WEATHER, true);

        addSectionLabel("WEATHER");
        card = addCard();
        addChevronRow(card, IconView.PIN, "Weather city", citySummary(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCityDialog();
                    }
                });
        addChevronRow(card, IconView.THERMO, "Temperature unit", unitSummary(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showUnitDialog();
                    }
                });
        addChevronRow(card, IconView.REFRESH, "Refresh weather",
                "Fetch the latest conditions now", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refreshWeather(true);
                    }
                });

        addNote(getString(R.string.settings_footer_note));
    }

    private void buildAppearancePane() {
        addHeader("Appearance", getString(R.string.settings_lead_appearance));
        addPreviewCard();
        addSectionLabel("STYLE");
        LinearLayout card = addCard();
        addChevronRow(card, IconView.PALETTE, "Theme",
                "Midnight · Graphite · Aurora  (currently " + Prefs.themeName(this) + ")",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showThemeDialog();
                    }
                });
        addChevronRow(card, IconView.PALETTE, "Accent colour",
                "Amber · Coral · Violet · Mint · Blue  (currently " + Prefs.accentName(this) + ")",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAccentDialog();
                    }
                });
        addSeekRow(card, "Glass intensity", Prefs.glassPercent(this));

        addSectionLabel("MOTION");
        card = addCard();
        addToggleRow(card, IconView.REFRESH, "Reduce motion",
                "Use instant transitions and focus changes", Prefs.K_REDUCE_MOTION, false);
        addNote("The preview updates live. The home screen applies the same theme, "
                + "accent and glass strength.");
    }

    private void buildNetworkPane() {
        addHeader("Network", getString(R.string.settings_lead_network));
        addSectionLabel("CONNECTION");
        LinearLayout card = addCard();
        addChevronRow(card, IconView.WIFI, "Wi-Fi network", wifiName(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNetworkDialog();
                    }
                });
        addChevronRow(card, IconView.INFO, "IP address", ipAddress(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNetworkDialog();
                    }
                });
        addChevronRow(card, IconView.REFRESH, "Link speed", linkSpeed(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNetworkDialog();
                    }
                });
        addChevronRow(card, IconView.WIFI, "Open network settings",
                "System Wi-Fi and network controls", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tryOpen(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS);
                    }
                });

        addSectionLabel("DIAGNOSTICS");
        card = addCard();
        addChevronRow(card, IconView.INFO, "Connection status", connectionSummary(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNetworkDialog();
                    }
                });
        addNote("Weather requests go directly to Open-Meteo. The network name is read "
                + "from the Fire TV connection and is never uploaded.");
    }

    private void buildDisplayPane() {
        addHeader("Display & Sound", getString(R.string.settings_lead_display));
        addSectionLabel("PICTURE");
        LinearLayout card = addCard();
        addChevronRow(card, IconView.INFO, "Resolution", displaySummary(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDisplayDialog();
                    }
                });
        addChevronRow(card, IconView.PALETTE, "Picture mode", "Standard · 1080p",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDisplayDialog();
                    }
                });
        addChevronRow(card, IconView.INFO, "Open display settings",
                "System picture and screen controls", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tryOpen(Settings.ACTION_DISPLAY_SETTINGS, null);
                    }
                });

        addSectionLabel("SOUND");
        card = addCard();
        addChevronRow(card, IconView.INFO, "Audio output", "HDMI · Dolby Digital+",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSoundDialog();
                    }
                });
        addChevronRow(card, IconView.INFO, "Open sound settings",
                "System audio and speaker controls", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tryOpen(Settings.ACTION_SOUND_SETTINGS, null);
                    }
                });
        addChevronRow(card, IconView.INFO, "Screen saver",
                "System screensaver settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tryOpen(Settings.ACTION_DREAM_SETTINGS, null);
                    }
                });
        addNote("Display and sound controls are provided by Fire OS. "
                + "OBAA Home only launches the matching system panel.");
    }

    private void buildAppsPane() {
        addHeader("Apps", getString(R.string.settings_lead_apps));
        addSectionLabel("CURATED DOCK");
        LinearLayout card = addCard();
        addAppRow(card, PKG_YOUTUBE, PKG_YOUTUBE_ALT, "YouTube",
                new BrandIconDrawable(0xFFFF3B30, 0xFFB31217, "", BrandIconDrawable.PLAY),
                Prefs.K_SHOW_YOUTUBE);
        addAppRow(card, PKG_NETFLIX, PKG_NETFLIX_ALT, "Netflix",
                new BrandIconDrawable(0xFFE50914, 0xFF4A0508, "N", BrandIconDrawable.LETTER),
                Prefs.K_SHOW_NETFLIX);
        addAppRow(card, PKG_PRIME, null, "Prime Video",
                new BrandIconDrawable(0xFF1F8FFF, 0xFF0B3C7A, "P", BrandIconDrawable.LETTER),
                Prefs.K_SHOW_PRIME);

        addSectionLabel("SYSTEM");
        card = addCard();
        addChevronRow(card, IconView.APPS, "Manage installed apps",
                "Open the system app list", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tryOpen("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS",
                                Settings.ACTION_APPLICATION_SETTINGS);
                    }
                });
        addChevronRow(card, IconView.INFO, "Default home",
                "How to make OBAA Home the launcher", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDefaultHomeDialog();
                    }
                });
        addChevronRow(card, IconView.APPS, "Appstore",
                "Open the Amazon Appstore", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchPackage("com.amazon.venezia", "Appstore");
                    }
                });
        addNote("The dock is intentionally limited to YouTube, Netflix and Prime Video. "
                + "All other installed and main apps live in the Apps drawer on the home screen. "
                + "Fire OS system apps stay hidden.");
    }

    private void buildPrivacyPane() {
        addHeader("Privacy", getString(R.string.settings_lead_privacy));
        addSectionLabel("LOCAL DATA");
        LinearLayout card = addCard();
        addChevronRow(card, IconView.PIN, "Weather location", citySummary(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCityDialog();
                    }
                });
        addChevronRow(card, IconView.REFRESH, "Clear weather cache",
                "Remove cached conditions and location", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearWeather();
                    }
                });
        addChevronRow(card, IconView.HEART, "Clear watchlist",
                "Remove all saved movie picks", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearWatchlist();
                    }
                });
        addChevronRow(card, IconView.INFO, "Reset all settings",
                "Restore OBAA Home defaults", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetSettings();
                    }
                });

        addSectionLabel("PERMISSIONS");
        card = addCard();
        addChevronRow(card, IconView.WIFI, "Network access",
                "Allowed · weather and app links", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPermissionsDialog();
                    }
                });
        addChevronRow(card, IconView.PIN, "Location",
                "Approximate · weather only", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPermissionsDialog();
                    }
                });
        addChevronRow(card, IconView.STORAGE, "Storage", "Not required",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPermissionsDialog();
                    }
                });
        addToggleRow(card, IconView.INFO, "Anonymous diagnostics",
                "Local only in this build", Prefs.K_DIAGNOSTICS, false);
        addNote("OBAA Home does not collect analytics or send usage data. "
                + "Weather requests go directly to Open-Meteo.");
    }

    private void buildAboutPane() {
        addHeader("About", getString(R.string.settings_lead_about));
        addBrandCard();
        addSectionLabel("DEVICE");
        LinearLayout card = addCard();
        addChevronRow(card, IconView.INFO, "Device", Build.MODEL,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAboutDialog();
                    }
                });
        addChevronRow(card, IconView.INFO, "Android",
                Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAboutDialog();
                    }
                });
        addChevronRow(card, IconView.WIFI, "IP address", ipAddress(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNetworkDialog();
                    }
                });
        addChevronRow(card, IconView.STORAGE, "Storage", storageSummary(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAboutDialog();
                    }
                });

        addSectionLabel("OBAA HOME");
        card = addCard();
        addChevronRow(card, IconView.INFO, "Version", "2.0 · Glass",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAboutDialog();
                    }
                });
        addChevronRow(card, IconView.APPS, "Set as default home",
                "Launcher Manager or ADB", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDefaultHomeDialog();
                    }
                });
        addChevronRow(card, IconView.INFO, "Open source licenses",
                "Libraries and data sources", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLicensesDialog();
                    }
                });
        addNote("Made for Fire TV. All artwork is drawn on-device.");
    }

    private void addBrandCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.settings_card_bg);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(7);

        FrameLayout logo = new FrameLayout(this);
        logo.setBackgroundResource(R.drawable.aurora_logo_bg);
        IconView mark = new IconView(this);
        mark.setMode(IconView.AURORA);
        mark.setColor(0xFFFFFFFF, 0);
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(dp(22), dp(22));
        mlp.gravity = android.view.Gravity.CENTER;
        logo.addView(mark, mlp);
        card.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(12);
        TextView title = new TextView(this);
        title.setText("OBAA HOME");
        title.setTextSize(18);
        title.setTextColor(0xFFF2F4F8);
        title.setTypeface(Ui.BOLD);
        title.setLetterSpacing(0.08f);
        title.setIncludeFontPadding(false);
        text.addView(title);
        TextView version = new TextView(this);
        version.setText("Version 2.0 · Glass");
        version.setTextSize(8);
        version.setTextColor(0xFFA5ADBE);
        version.setIncludeFontPadding(false);
        text.addView(version);
        TextView desc = new TextView(this);
        desc.setText("A calm, glass-first launcher for Fire TV.");
        desc.setTextSize(9);
        desc.setTextColor(0xFFA5ADBE);
        desc.setIncludeFontPadding(false);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(4);
        text.addView(desc, dlp);
        card.addView(text, tlp);

        mPaneContainer.addView(card, lp);
    }

    private String citySummary() {
        String c = Prefs.get(this, Prefs.K_CITY, "");
        return c.length() > 0 ? c : "Auto-detect on first refresh";
    }

    private String unitSummary() {
        return "f".equals(Prefs.get(this, Prefs.K_TEMP_UNIT, "c"))
                ? "Fahrenheit (°F)" : "Celsius (°C)";
    }

    private Ui.ImmersiveDialogBuilder dialogBuilder() {
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

    private CharSequence[] colorBullets(String[] names, int[] colors) {
        CharSequence[] out = new CharSequence[names.length];
        for (int i = 0; i < names.length; i++) {
            SpannableString s = new SpannableString("●  " + names[i]);
            s.setSpan(new ForegroundColorSpan(colors[i]), 0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out[i] = s;
        }
        return out;
    }

    private void showThemeDialog() {
        final String[] names = {"Midnight", "Graphite", "Aurora"};
        final String[] keys = {"midnight", "graphite", "aurora"};
        final String[] accents = {"amber", "blue", "violet"};
        int[] colors = {0xFFFFB45E, 0xFF7FB7FF, 0xFFA99BFF};
        String cur = Prefs.theme(this);
        int sel = "graphite".equals(cur) ? 1 : "aurora".equals(cur) ? 2 : 0;
        dialogBuilder()
                .setTitle("Theme")
                .setSingleChoiceItems(colorBullets(names, colors), sel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Prefs.put(SettingsActivity.this, Prefs.K_THEME, keys[which]);
                        Prefs.put(SettingsActivity.this, Prefs.K_ACCENT, accents[which]);
                        applyTheme();
                        showPane(mPane, true);
                        toast("Theme applied");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAccentDialog() {
        final String[] names = {"Amber", "Coral", "Violet", "Mint", "Blue"};
        final String[] keys = {"amber", "coral", "violet", "mint", "blue"};
        int[] colors = {0xFFFFB45E, 0xFFFF7A6B, 0xFFA99BFF, 0xFF5FE3C0, 0xFF7FB7FF};
        String cur = Prefs.accentKey(this);
        int sel = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(cur)) sel = i;
        dialogBuilder()
                .setTitle("Accent colour")
                .setSingleChoiceItems(colorBullets(names, colors), sel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Prefs.put(SettingsActivity.this, Prefs.K_ACCENT, keys[which]);
                        applyTheme();
                        showPane(mPane, true);
                        toast("Accent applied");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCityDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setHint("e.g. Mumbai, Delhi, London");
        input.setText(Prefs.get(this, Prefs.K_CITY, ""));
        dialogBuilder()
                .topGravity()
                .setTitle("Weather city")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String city = input.getText().toString().trim();
                        if (city.length() == 0) return;
                        toast("Looking up " + city + "…");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Weather.geocodeCity(SettingsActivity.this, city);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("Weather location set");
                                            showPane(mPane, true);
                                            refreshWeather(false);
                                        }
                                    });
                                } catch (final Throwable t) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toast("Couldn't find that city");
                                        }
                                    });
                                }
                            }
                        }, "geocode").start();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUnitDialog() {
        final String[] names = {"Celsius (°C)", "Fahrenheit (°F)"};
        final String[] keys = {"c", "f"};
        int sel = "f".equals(Prefs.get(this, Prefs.K_TEMP_UNIT, "c")) ? 1 : 0;
        dialogBuilder()
                .setTitle("Temperature unit")
                .setSingleChoiceItems(names, sel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Prefs.put(SettingsActivity.this, Prefs.K_TEMP_UNIT, keys[which]);
                        showPane(mPane, true);
                        refreshWeather(false);
                        toast("Temperature unit updated");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshWeather(final boolean rebuild) {
        toast("Refreshing weather…");
        Weather.refresh(this, new Weather.Callback() {
            @Override
            public void onWeather(Weather.Data d) {
                if (d != null && d.valid()) {
                    toast("Weather updated");
                } else {
                    toast("Couldn't reach the weather service");
                }
                if (rebuild) showPane(mPane, true);
            }
        });
    }

    private void showNetworkDialog() {
        dialogBuilder()
                .setTitle("Network")
                .setMessage("Wi-Fi: " + wifiName()
                        + "\nIP address: " + ipAddress()
                        + "\nLink speed: " + linkSpeed()
                        + "\nBand: " + band()
                        + "\n\nIf Wi-Fi shows as unknown, the network name is hidden.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDisplayDialog() {
        dialogBuilder()
                .setTitle("Display")
                .setMessage("Resolution: " + displaySummary()
                        + "\nRefresh rate: " + Math.round(refreshRate()) + " Hz"
                        + "\n\nPicture and HDR controls live in the Fire TV display settings.")
                .setPositiveButton("Open display settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tryOpen(Settings.ACTION_DISPLAY_SETTINGS, null);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSoundDialog() {
        dialogBuilder()
                .setTitle("Sound")
                .setMessage("Audio output: HDMI\n"
                        + "Supported formats depend on your TV and receiver, including Dolby Atmos passthrough.\n\n"
                        + "Use the Fire TV sound settings to choose the output mode.")
                .setPositiveButton("Open sound settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tryOpen(Settings.ACTION_SOUND_SETTINGS, null);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPermissionsDialog() {
        dialogBuilder()
                .setTitle("Permissions")
                .setMessage("Network access: allowed\n"
                        + "Used for weather and app links.\n\n"
                        + "Location: approximate\n"
                        + "Used only to find your weather city. You can enter a city manually.\n\n"
                        + "Storage: not required\n"
                        + "OBAA Home stores a small preferences file on the device.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDefaultHomeDialog() {
        final boolean enabled = HomeKeyReceiver.isEnabled(this);
        dialogBuilder()
                .setTitle("Default home")
                .setMessage(enabled
                        ? "OBAA Home is handling the Fire TV Home button. The stock launcher remains installed and can be restored here at any time."
                        : getString(R.string.default_home_hint))
                .setPositiveButton(enabled ? "Disable" : "Enable now",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                boolean ok = enabled
                                        ? HomeKeyReceiver.disable(SettingsActivity.this)
                                        : HomeKeyReceiver.enable(SettingsActivity.this);
                                toast(ok
                                        ? (enabled ? "Home key service disabled" : "Home key service enabled")
                                        : "Grant WRITE_SECURE_SETTINGS with ADB first");
                                showPane(mPane, true);
                            }
                        })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showLicensesDialog() {
        dialogBuilder()
                .setTitle("Open source licenses")
                .setMessage(getString(R.string.licenses_body))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        dialogBuilder()
                .setTitle("OBAA Home")
                .setMessage(getString(R.string.about_body)
                        + "\n\nDevice: " + Build.MODEL
                        + "\nAndroid: " + Build.VERSION.RELEASE + " (API "
                        + Build.VERSION.SDK_INT + ")"
                        + "\nIP: " + ipAddress()
                        + "\nStorage: " + storageSummary())
                .setPositiveButton("OK", null)
                .show();
    }

    private void clearWeather() {
        Prefs.put(this, Prefs.K_WX_CACHE, "");
        Prefs.put(this, Prefs.K_LAT, "");
        Prefs.put(this, Prefs.K_LON, "");
        Prefs.put(this, Prefs.K_CITY, "");
        toast("Weather cache cleared");
        showPane(mPane, true);
    }

    private void clearWatchlist() {
        Prefs.putSavedIds(this, new HashSet<String>());
        toast("Watchlist cleared");
        showPane(mPane, true);
    }

    private void resetSettings() {
        dialogBuilder()
                .setTitle("Reset all settings?")
                .setMessage("This restores the default theme, accent, clock, weather and dock choices.")
                .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSharedPreferences("aurora", MODE_PRIVATE).edit().clear().apply();
                        applyTheme();
                        showPane(0, true);
                        toast("Settings reset");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void tryOpen(String action, String fallbackAction) {
        try {
            startActivity(new Intent(action));
        } catch (Throwable t) {
            if (fallbackAction != null) {
                try {
                    startActivity(new Intent(fallbackAction));
                    return;
                } catch (Throwable ignored) {
                }
            }
            toast("Not available on this device");
        }
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
        return "Wi-Fi";
    }

    private String ipAddress() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "—";
    }

    private String storageSummary() {
        try {
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            double free = fs.getAvailableBytes() / 1e9;
            double total = fs.getTotalBytes() / 1e9;
            return String.format(Locale.US, "%.1f GB free of %.1f GB", free, total);
        } catch (Throwable t) {
            return "Unavailable";
        }
    }

    private String linkSpeed() {
        try {
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null && wi.getLinkSpeed() > 0) {
                    return wi.getLinkSpeed() + " Mbps";
                }
            }
        } catch (Throwable ignored) {
        }
        return "—";
    }

    private String band() {
        try {
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null && wi.getFrequency() > 0) {
                    return wi.getFrequency() >= 4900 ? "5 GHz" : "2.4 GHz";
                }
            }
        } catch (Throwable ignored) {
        }
        return "—";
    }

    private String connectionSummary() {
        return "Connected · " + band() + " · " + linkSpeed();
    }

    private String displaySummary() {
        try {
            int w = getWindowManager().getDefaultDisplay().getWidth();
            int h = getWindowManager().getDefaultDisplay().getHeight();
            return w + " × " + h;
        } catch (Throwable t) {
            return "1920 × 1080";
        }
    }

    private float refreshRate() {
        try {
            return getWindowManager().getDefaultDisplay().getRefreshRate();
        } catch (Throwable t) {
            return 60f;
        }
    }

    private int dp(float v) {
        return (int) Ui.dp(this, v);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
