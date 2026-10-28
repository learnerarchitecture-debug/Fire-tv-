package com.aurora.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Switch;

import java.lang.reflect.Method;

/** Small drawing/layout helpers shared by the glass screens. */
public final class Ui {

    public static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    public static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    public static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    private Ui() {}

    /** Full-bleed TV surface: no status or navigation bars. */
    public static void immersive(Activity a) {
        if (a == null) return;
        a.getWindow().getDecorView().setSystemUiVisibility(immersiveFlags());
    }

    /** Keep dialogs on the same full-bleed surface as the launcher. */
    public static void immersive(Dialog d) {
        if (d == null || d.getWindow() == null) return;
        d.getWindow().getDecorView().setSystemUiVisibility(immersiveFlags());
    }

    private static int immersiveFlags() {
        return View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }

    /** Alert dialogs need the same full-bleed treatment as their activity. */
    public static class ImmersiveDialogBuilder extends AlertDialog.Builder {
        private final Context mContext;
        private boolean mTopGravity;

        public ImmersiveDialogBuilder(Context context, int themeResId) {
            super(context, themeResId);
            mContext = context;
        }

        /** Keeps a text-entry dialog above the on-screen keyboard. */
        public ImmersiveDialogBuilder topGravity() {
            mTopGravity = true;
            return this;
        }

        @Override
        public AlertDialog show() {
            AlertDialog dialog = create();
            Window window = dialog.getWindow();
            if (window != null) {
                // Keep dialog buttons above the Fire TV / emulator soft keyboard.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                if (mTopGravity) {
                    window.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.y = (int) dp(mContext, 56);
                    window.setAttributes(lp);
                }
            }
            dialog.show();
            immersive(dialog);
            return dialog;
        }
    }

    public static float dp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                c.getResources().getDisplayMetrics());
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }

    public static int mix(int a, int b, float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        int aa = (int) (((a >>> 24) & 0xFF) * (1f - t) + ((b >>> 24) & 0xFF) * t);
        int rr = (int) (((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
        int gg = (int) (((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
        int bb = (int) ((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    /** Applies the glass slider to every view tagged "glass" in a tree. */
    public static void applyGlassAlpha(View root, int percent) {
        if (root == null) return;
        float f = 0.35f + 0.65f * (Math.max(0, Math.min(100, percent)) / 100f);
        int a = Math.round(255f * f);
        applyGlassAlphaRecursive(root, a);
    }

    private static void applyGlassAlphaRecursive(View v, int alpha) {
        Object tag = v.getTag();
        if ("glass".equals(tag)) {
            Drawable d = v.getBackground();
            if (d != null) {
                d = d.mutate();
                d.setAlpha(alpha);
                v.setBackground(d);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                applyGlassAlphaRecursive(g.getChildAt(i), alpha);
            }
        }
    }

    public static void tintSwitch(Switch sw, int accent) {
        if (sw == null) return;
        int offThumb = 0xFF8A93A6;
        int offTrack = 0x33FFFFFF;
        int onTrack = (accent & 0x00FFFFFF) | 0x66000000;
        ColorStateList thumb = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{accent, offThumb});
        ColorStateList track = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{onTrack, offTrack});
        // thumbTint/trackTint only exist on API 23+. Reflection keeps the
        // APK compatible with Fire OS 5.1 while still tinting on newer boxes.
        try {
            Method thumbMethod = Switch.class.getMethod("setThumbTintList", ColorStateList.class);
            thumbMethod.invoke(sw, thumb);
            Method trackMethod = Switch.class.getMethod("setTrackTintList", ColorStateList.class);
            trackMethod.invoke(sw, track);
        } catch (Throwable ignored) {
        }
    }

    public static String ellipsize(Paint p, String text, float maxWidth) {
        if (text == null) return "";
        if (p.measureText(text) <= maxWidth) return text;
        String ell = "\u2026";
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            String s = text.substring(0, mid) + ell;
            if (p.measureText(s) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return text.substring(0, lo) + ell;
    }

    public static void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
