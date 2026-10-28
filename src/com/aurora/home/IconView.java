package com.aurora.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Tiny canvas-drawn glyph icons (no bitmaps, no assets) used for the
 * settings rows, buttons and chevrons. Everything is geometric so it
 * stays crisp and costs almost nothing to draw.
 */
public class IconView extends View {

    public static final int GEAR = 0;
    public static final int CLOCK = 1;
    public static final int THERMO = 2;
    public static final int PALETTE = 3;
    public static final int WIFI = 4;
    public static final int STORAGE = 5;
    public static final int INFO = 6;
    public static final int APPS = 7;
    public static final int PIN = 8;
    public static final int REFRESH = 9;
    public static final int CHEVRON = 10;
    public static final int BACK = 11;
    public static final int PLAY = 12;
    public static final int HEART = 13;
    public static final int UNDO = 14;
    public static final int CLOSE = 15;
    public static final int AURORA = 16;
    public static final int CLOUD = 17;
    public static final int DISPLAY = 18;
    public static final int LOCK = 19;
    public static final int CALENDAR = 20;

    private int mMode = GEAR;
    private int mColor = 0xFFEAF2FF;
    private int mBoxColor = 0x00000000;

    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final RectF mRect = new RectF();

    public IconView(Context c) {
        super(c);
    }

    public IconView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setMode(int mode) {
        mMode = mode;
        invalidate();
    }

    public void setColor(int color, int boxColor) {
        mColor = color;
        mBoxColor = boxColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (mBoxColor != 0) {
            mFill.setStyle(Paint.Style.FILL);
            mFill.setColor(mBoxColor);
            float br = Math.min(w, h) * 0.28f;
            c.drawRoundRect(0, 0, w, h, br, br, mFill);
        }

        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(mColor);
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setColor(mColor);
        mStroke.setStrokeCap(Paint.Cap.ROUND);

        float cx = w / 2f, cy = h / 2f, s = Math.min(w, h);

        switch (mMode) {
            case GEAR: drawGear(c, cx, cy, s); break;
            case CLOCK: drawClock(c, cx, cy, s); break;
            case THERMO: drawThermo(c, cx, cy, s); break;
            case PALETTE: drawPalette(c, cx, cy, s); break;
            case WIFI: drawWifi(c, cx, cy, s); break;
            case STORAGE: drawStorage(c, cx, cy, s); break;
            case INFO: drawInfo(c, cx, cy, s); break;
            case APPS: drawApps(c, cx, cy, s); break;
            case PIN: drawPin(c, cx, cy, s); break;
            case REFRESH: drawRefresh(c, cx, cy, s); break;
            case CHEVRON: drawChevron(c, cx, cy, s, false); break;
            case BACK: drawChevron(c, cx, cy, s, true); break;
            case PLAY: drawPlay(c, cx, cy, s); break;
            case HEART: drawHeart(c, cx, cy, s); break;
            case UNDO: drawUndo(c, cx, cy, s); break;
            case CLOSE: drawClose(c, cx, cy, s); break;
            case AURORA: drawAurora(c, cx, cy, s); break;
            case CLOUD: drawCloud(c, cx, cy, s); break;
            case DISPLAY: drawDisplay(c, cx, cy, s); break;
            case LOCK: drawLock(c, cx, cy, s); break;
            case CALENDAR: drawCalendar(c, cx, cy, s); break;
            default: break;
        }
    }

    private void drawGear(Canvas c, float cx, float cy, float s) {
        float r = s * 0.30f;
        mStroke.setStrokeWidth(s * 0.10f);
        c.save();
        c.translate(cx, cy);
        for (int i = 0; i < 8; i++) {
            c.rotate(45f);
            c.drawLine(0, -r * 1.25f, 0, -r * 0.92f, mStroke);
        }
        c.restore();
        c.drawCircle(cx, cy, r * 0.86f, mStroke);
        c.drawCircle(cx, cy, r * 0.30f, mFill);
    }

    private void drawClock(Canvas c, float cx, float cy, float s) {
        float r = s * 0.36f;
        mStroke.setStrokeWidth(s * 0.07f);
        c.drawCircle(cx, cy, r, mStroke);
        mStroke.setStrokeWidth(s * 0.06f);
        c.drawLine(cx, cy, cx, cy - r * 0.62f, mStroke);
        c.drawLine(cx, cy, cx + r * 0.42f, cy + r * 0.10f, mStroke);
    }

    private void drawThermo(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.10f);
        c.drawLine(cx - s * 0.02f, cy - s * 0.30f, cx - s * 0.02f, cy + s * 0.08f, mStroke);
        c.drawCircle(cx - s * 0.02f, cy + s * 0.22f, s * 0.145f, mFill);
        mStroke.setStrokeWidth(s * 0.045f);
        c.drawLine(cx + s * 0.09f, cy - s * 0.18f, cx + s * 0.17f, cy - s * 0.18f, mStroke);
        c.drawLine(cx + s * 0.09f, cy - s * 0.05f, cx + s * 0.17f, cy - s * 0.05f, mStroke);
    }

    private void drawPalette(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.07f);
        c.drawCircle(cx, cy, s * 0.35f, mStroke);
        float d = s * 0.055f;
        c.drawCircle(cx - s * 0.14f, cy - s * 0.10f, d, mFill);
        c.drawCircle(cx + s * 0.12f, cy - s * 0.14f, d, mFill);
        c.drawCircle(cx + s * 0.02f, cy + s * 0.14f, d, mFill);
    }

    private void drawWifi(Canvas c, float cx, float cy, float s) {
        float oy = cy + s * 0.28f;
        mStroke.setStrokeWidth(s * 0.075f);
        float[] radii = {0.16f, 0.30f, 0.44f};
        for (float fr : radii) {
            float r = s * fr;
            mRect.set(cx - r, oy - r, cx + r, oy + r);
            c.drawArc(mRect, 205, 130, false, mStroke);
        }
        c.drawCircle(cx, oy - s * 0.02f, s * 0.06f, mFill);
    }

    private void drawStorage(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.07f);
        mRect.set(cx - s * 0.32f, cy - s * 0.34f, cx + s * 0.32f, cy - s * 0.06f);
        c.drawRoundRect(mRect, s * 0.07f, s * 0.07f, mStroke);
        mRect.set(cx - s * 0.32f, cy + s * 0.06f, cx + s * 0.32f, cy + s * 0.34f);
        c.drawRoundRect(mRect, s * 0.07f, s * 0.07f, mStroke);
    }

    private void drawDisplay(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.07f);
        mRect.set(cx - s * 0.34f, cy - s * 0.29f, cx + s * 0.34f, cy + s * 0.13f);
        c.drawRoundRect(mRect, s * 0.07f, s * 0.07f, mStroke);
        c.drawLine(cx, cy + s * 0.13f, cx, cy + s * 0.29f, mStroke);
        c.drawLine(cx - s * 0.14f, cy + s * 0.29f, cx + s * 0.14f, cy + s * 0.29f, mStroke);
    }

    private void drawCalendar(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.065f);
        mRect.set(cx - s * 0.30f, cy - s * 0.25f, cx + s * 0.30f, cy + s * 0.31f);
        c.drawRoundRect(mRect, s * 0.06f, s * 0.06f, mStroke);
        c.drawLine(cx - s * 0.30f, cy - s * 0.05f, cx + s * 0.30f, cy - s * 0.05f, mStroke);
        c.drawLine(cx - s * 0.14f, cy - s * 0.34f, cx - s * 0.14f, cy - s * 0.18f, mStroke);
        c.drawLine(cx + s * 0.14f, cy - s * 0.34f, cx + s * 0.14f, cy - s * 0.18f, mStroke);
        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(mColor);
        c.drawCircle(cx - s * 0.13f, cy + s * 0.10f, s * 0.045f, mFill);
        c.drawCircle(cx + s * 0.05f, cy + s * 0.10f, s * 0.045f, mFill);
    }

    private void drawLock(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.08f);
        mRect.set(cx - s * 0.16f, cy - s * 0.32f, cx + s * 0.16f, cy + s * 0.04f);
        c.drawArc(mRect, 180, 180, false, mStroke);
        mRect.set(cx - s * 0.23f, cy - s * 0.03f, cx + s * 0.23f, cy + s * 0.31f);
        c.drawRoundRect(mRect, s * 0.06f, s * 0.06f, mFill);
    }

    private void drawInfo(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.07f);
        c.drawCircle(cx, cy, s * 0.36f, mStroke);
        c.drawCircle(cx, cy - s * 0.17f, s * 0.045f, mFill);
        mStroke.setStrokeWidth(s * 0.06f);
        c.drawLine(cx, cy - s * 0.06f, cx, cy + s * 0.19f, mStroke);
    }

    private void drawApps(Canvas c, float cx, float cy, float s) {
        float q = s * 0.17f, gap = s * 0.07f;
        float x0 = cx - q - gap / 2f, y0 = cy - q - gap / 2f;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                mRect.set(x0 + i * (q + gap), y0 + j * (q + gap),
                        x0 + i * (q + gap) + q, y0 + j * (q + gap) + q);
                c.drawRoundRect(mRect, q * 0.30f, q * 0.30f, mFill);
            }
        }
    }

    private void drawPin(Canvas c, float cx, float cy, float s) {
        mPath.reset();
        mPath.addCircle(cx, cy - s * 0.08f, s * 0.20f, Path.Direction.CW);
        mPath.moveTo(cx, cy + s * 0.36f);
        mPath.lineTo(cx - s * 0.20f, cy - s * 0.06f);
        mPath.lineTo(cx + s * 0.20f, cy - s * 0.06f);
        mPath.close();
        c.drawPath(mPath, mFill);
    }

    private void drawRefresh(Canvas c, float cx, float cy, float s) {
        float r = s * 0.32f;
        mStroke.setStrokeWidth(s * 0.09f);
        mRect.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(mRect, 330, 300, false, mStroke);
        float ay = cy - r;
        c.drawLine(cx - s * 0.09f, ay - s * 0.09f, cx + s * 0.03f, ay, mStroke);
        c.drawLine(cx - s * 0.09f, ay + s * 0.09f, cx + s * 0.03f, ay, mStroke);
    }

    private void drawChevron(Canvas c, float cx, float cy, float s, boolean back) {
        mStroke.setStrokeWidth(s * 0.09f);
        float dir = back ? -1f : 1f;
        c.drawLine(cx - dir * s * 0.10f, cy - s * 0.16f, cx + dir * s * 0.06f, cy, mStroke);
        c.drawLine(cx - dir * s * 0.10f, cy + s * 0.16f, cx + dir * s * 0.06f, cy, mStroke);
    }

    private void drawPlay(Canvas c, float cx, float cy, float s) {
        mPath.reset();
        mPath.moveTo(cx - s * 0.16f, cy - s * 0.24f);
        mPath.lineTo(cx + s * 0.26f, cy);
        mPath.lineTo(cx - s * 0.16f, cy + s * 0.24f);
        mPath.close();
        c.drawPath(mPath, mFill);
    }

    private void drawHeart(Canvas c, float cx, float cy, float s) {
        mPath.reset();
        mPath.moveTo(cx, cy + s * 0.30f);
        mPath.cubicTo(cx - s * 0.62f, cy - s * 0.10f, cx - s * 0.30f, cy - s * 0.55f,
                cx, cy - s * 0.16f);
        mPath.cubicTo(cx + s * 0.30f, cy - s * 0.55f, cx + s * 0.62f, cy - s * 0.10f,
                cx, cy + s * 0.30f);
        mPath.close();
        c.drawPath(mPath, mFill);
    }

    private void drawUndo(Canvas c, float cx, float cy, float s) {
        float r = s * 0.30f;
        mStroke.setStrokeWidth(s * 0.09f);
        mRect.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(mRect, 40, 285, false, mStroke);
        float ax = cx - r * 0.72f, ay = cy - r * 0.70f;
        c.drawLine(ax, ay - s * 0.10f, ax, ay + s * 0.10f, mStroke);
        c.drawLine(ax, ay - s * 0.10f, ax + s * 0.15f, ay, mStroke);
    }

    private void drawClose(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.09f);
        c.drawLine(cx - s * 0.17f, cy - s * 0.17f, cx + s * 0.17f, cy + s * 0.17f, mStroke);
        c.drawLine(cx + s * 0.17f, cy - s * 0.17f, cx - s * 0.17f, cy + s * 0.17f, mStroke);
    }

    private void drawAurora(Canvas c, float cx, float cy, float s) {
        mStroke.setStrokeWidth(s * 0.085f);
        mRect.set(cx - s * 0.36f, cy - s * 0.26f, cx + s * 0.36f, cy + s * 0.26f);
        c.drawArc(mRect, 205, 130, false, mStroke);
        mRect.set(cx - s * 0.27f, cy - s * 0.15f, cx + s * 0.27f, cy + s * 0.37f);
        c.drawArc(mRect, 205, 130, false, mStroke);
        mRect.set(cx - s * 0.18f, cy - s * 0.04f, cx + s * 0.18f, cy + s * 0.48f);
        c.drawArc(mRect, 205, 130, false, mStroke);
    }

    private void drawCloud(Canvas c, float cx, float cy, float s) {
        float r = s * 0.18f;
        c.drawCircle(cx - s * 0.18f, cy + s * 0.02f, r, mFill);
        c.drawCircle(cx + s * 0.02f, cy - s * 0.08f, r * 1.20f, mFill);
        c.drawCircle(cx + s * 0.22f, cy + s * 0.03f, r * 0.90f, mFill);
        mRect.set(cx - s * 0.30f, cy + s * 0.00f, cx + s * 0.32f, cy + s * 0.26f);
        c.drawRoundRect(mRect, s * 0.12f, s * 0.12f, mFill);
    }
}
