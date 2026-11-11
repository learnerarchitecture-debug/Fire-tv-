package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * "Ambient Drift" full-bleed backdrop (Option C design lab):
 * the selected title's artwork fills the whole home screen with a slow
 * Ken Burns pan, circulation-side scrims for text legibility, a soft
 * vignette and a large translucent initials watermark. When no artwork
 * is available the procedural MovieArt gradient fills the same rect, so
 * the screen never goes flat black.
 */
public final class AmbientBackdropView extends View {

    private static final long FRAME_MS = 42L;   // ~24fps is plenty for a slow pan
    private static final long FADE_MS = 420L;

    private Movie mMovie;
    private String mInit = "";
    private boolean mMotion = true;
    private boolean mArtSampled = false;
    private long mStart = SystemClock.uptimeMillis();
    private long mFadeStart;

    /** Re-checks the artwork a beat later so the palette can upgrade from
     *  the curated colours to colours sampled from the real bitmap. */
    private final Runnable mProbe = new Runnable() {
        @Override
        public void run() {
            Movie m = mMovie;
            if (m == null || mArtSampled) return;
            Bitmap b = m.thumbnail;
            if (b == null || b.isRecycled()) return;
            int sampled = ArtPalette.dominant(b);
            if (sampled == 0) return;
            mArtSampled = true;
            if (mPaletteListener != null) mPaletteListener.onPalette(sampled);
        }
    };

    /** Receives the artwork's dominant colour so UI accents can follow the scene. */
    public interface PaletteListener {
        void onPalette(int contentAccent);
    }

    private PaletteListener mPaletteListener;

    /** Someone wants to know whenever the ambient colour mood changes. */
    public void setPaletteListener(PaletteListener l) {
        mPaletteListener = l;
        if (l != null && mMovie != null) l.onPalette(paletteColor(mMovie));
    }

    /** Derived accent from the title's palette colours (no bitmap sampling). */
    private static int paletteColor(Movie m) {
        if (m == null) return 0;
        int base = Ui.mix(m.c1, m.c2, 0.5f);
        int warmed = Ui.mix(base, m.c3, 0.34f);
        return Ui.mix(warmed, 0xFFFFFFFF, 0.18f);
    }

    private void schedulePaletteProbe() {
        removeCallbacks(mProbe);
        postDelayed(mProbe, 700);
        postDelayed(mProbe, 2100);
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mSrc = new Rect();

    public AmbientBackdropView(Context c, AttributeSet a) {
        super(c, a);
        mText.setColor(0xFFFFFFFF);
        mText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    private final Runnable mFrame = new Runnable() {
        @Override
        public void run() {
            invalidate();
            if (mMovie != null && mMotion && isShown()) {
                postDelayed(this, FRAME_MS);
            }
        }
    };

    /** Switches the entire ambient world to a title. Safe to call repeatedly. */
    public void setMovie(Movie m) {
        boolean same = (m == mMovie)
                || (m != null && mMovie != null && m.id != null && m.id.equals(mMovie.id));
        if (same) {
            if (m != null) {
                ThumbnailLoader.load(getContext(), m, this);
                schedulePaletteProbe();
            }
            return;
        }
        mMovie = m;
        mInit = m == null ? "" : initials(m.title);
        mMotion = !Prefs.reduceMotion(getContext());
        mFadeStart = SystemClock.uptimeMillis();
        mArtSampled = false;
        if (m != null) {
            ThumbnailLoader.load(getContext(), m, this);
            schedulePaletteProbe();
        }
        if (mPaletteListener != null) mPaletteListener.onPalette(m == null ? 0 : paletteColor(m));
        restart();
    }

    private static String initials(String t) {
        if (t == null) return "";
        String[] w = t.trim().split(" ");
        int n = Math.min(w.length, 2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) if (w[i].length() > 0) sb.append(Character.toUpperCase(w[i].charAt(0)));
        return sb.toString();
    }

    private void restart() {
        removeCallbacks(mFrame);
        if (mMovie != null && mMotion && isShown()) postDelayed(mFrame, FRAME_MS);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        restart();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mFrame);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        Movie m = mMovie;
        int w = getWidth(), h = getHeight();
        if (w < 12 || h < 12) return;
        if (m == null) return;   // AuroraBackgroundView shows through

        float overX = mMotion ? w * 0.085f : w * 0.015f;
        float overY = mMotion ? h * 0.105f : h * 0.02f;
        float t = (SystemClock.uptimeMillis() - mStart) / 1000f;
        float panX = mMotion ? (float) Math.sin(t * Math.PI * 2.0 / 21.0) * overX : overX * 0.5f;
        float panY = mMotion ? (float) Math.cos(t * Math.PI * 2.0 / 29.0) * overY : overY * 0.5f;
        RectF dst = new RectF(
                -overX * (1 - Math.max(0, panX / Math.max(1, overX))) + panX - overX * 0.5f,
                -overY * (1 - Math.max(0, panY / Math.max(1, overY))) + panY - overY * 0.5f,
                w + overX * 0.5f + panX,
                h + overY * 0.5f + panY);

        long since = SystemClock.uptimeMillis() - mFadeStart;
        float fade = Math.min(1f, since / (float) FADE_MS) * 1f;

        Bitmap bmp = m.thumbnail;   // volatile: written by the art pool
        if (bmp != null && !bmp.isRecycled()) {
            if (fade < 1f) fade = 0.25f + 0.75f * fade;
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAlpha((int) (255 * fade));
            Rect src = ThumbnailLoader.coverSrc(dst.width(), dst.height(), bmp.getWidth(), bmp.getHeight());
            c.drawBitmap(bmp, src, dst, mPaint);
        } else {
            MovieArt.drawPoster(c, dst, m, 0f, mPaint);
            if (fade < 1f) {
                mPaint.setAlpha((int) (255 * fade));
                mPaint.setShader(new LinearGradient(0, 0, 0, h,
                        0xFF05070B, 0xFF05070B, Shader.TileMode.CLAMP));
            }
        }

        scrims(c, w, h);
        watermark(c, w, h);
        mPaint.setAlpha(255);
        mPaint.setShader(null);
    }

    /** Legibility scrims: HUD top, title side, deck corner, vignette. */
    private void scrims(Canvas c, int w, int h) {
        mPaint.setShader(new LinearGradient(0, 0, 0, h * 0.20f,
                0x4D05070B, 0x0005070B, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h * 0.20f, mPaint);

        mPaint.setShader(new LinearGradient(w * 0.66f, 0, 0, 0,
                0x0005070B, 0x9905070B, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w * 0.66f, h, mPaint);

        mPaint.setShader(new LinearGradient(0, h * 0.42f, 0, h,
                0x0005070B, 0xE605070B, Shader.TileMode.CLAMP));
        c.drawRect(0, h * 0.42f, w, h, mPaint);

        mPaint.setShader(new RadialGradient(w * 0.5f, h * 0.44f, Math.max(w, h) * 0.62f,
                0x0005070B, 0x5205070B, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, mPaint);
    }

    /** Large translucent monogram parked right-of-centre. */
    private void watermark(Canvas c, int w, int h) {
        if (mInit.length() == 0) return;
        mPaint.setShader(null);
        String s = mInit.substring(0, 1);
        mText.setTextSize(h * 0.40f);
        mText.setLetterSpacing(0f);
        mText.setAlpha(24);
        float tw = mText.measureText(s);
        c.drawText(s, w - tw - w * 0.07f, h * 0.52f, mText);
        mText.setAlpha(255);
    }
}
