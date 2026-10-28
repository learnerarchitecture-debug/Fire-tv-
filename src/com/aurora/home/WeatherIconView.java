package com.aurora.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Canvas-drawn weather glyphs (sun, moon, cloud, rain, snow, storm, fog).
 * No image assets, no decode cost — ideal for a low-power TV stick.
 */
public class WeatherIconView extends View {

    public static final int SUN = 0;
    public static final int MOON = 1;
    public static final int PARTLY_DAY = 2;
    public static final int PARTLY_NIGHT = 3;
    public static final int CLOUD = 4;
    public static final int FOG = 5;
    public static final int RAIN = 6;
    public static final int SNOW = 7;
    public static final int STORM = 8;

    private static final int COL_SUN = 0xFFFFD75E;
    private static final int COL_MOON = 0xFFD7E2F2;
    private static final int COL_CLOUD = 0xFFAEBDD3;
    private static final int COL_CLOUD_DARK = 0xFF8494AC;
    private static final int COL_DROP = 0xFF6EC9F5;
    private static final int COL_SNOW = 0xFFEAF4FF;

    private int mKind = CLOUD;

    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final Path mPath2 = new Path();
    private final RectF mRect = new RectF();

    public WeatherIconView(Context c) {
        super(c);
    }

    public WeatherIconView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setKind(int kind) {
        mKind = kind;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cx = w / 2f, cy = h / 2f, s = Math.min(w, h);

        mFill.setStyle(Paint.Style.FILL);
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setStrokeCap(Paint.Cap.ROUND);

        switch (mKind) {
            case SUN: drawSun(c, cx, cy, s, 1f); break;
            case MOON: drawMoon(c, cx, cy, s, 1f); break;
            case PARTLY_DAY:
                drawSun(c, cx - s * 0.10f, cy - s * 0.13f, s, 0.72f);
                drawCloud(c, cx + s * 0.04f, cy + s * 0.10f, s * 0.92f, COL_CLOUD);
                break;
            case PARTLY_NIGHT:
                drawMoon(c, cx - s * 0.12f, cy - s * 0.14f, s, 0.70f);
                drawCloud(c, cx + s * 0.04f, cy + s * 0.10f, s * 0.92f, COL_CLOUD);
                break;
            case CLOUD: drawCloud(c, cx, cy, s, COL_CLOUD); break;
            case FOG:
                drawCloud(c, cx, cy - s * 0.10f, s * 0.82f, COL_CLOUD);
                mStroke.setColor(COL_CLOUD);
                mStroke.setStrokeWidth(s * 0.055f);
                c.drawLine(cx - s * 0.26f, cy + s * 0.16f, cx + s * 0.26f, cy + s * 0.16f, mStroke);
                c.drawLine(cx - s * 0.18f, cy + s * 0.29f, cx + s * 0.18f, cy + s * 0.29f, mStroke);
                break;
            case RAIN:
                drawCloud(c, cx, cy - s * 0.06f, s * 0.94f, COL_CLOUD_DARK);
                mStroke.setColor(COL_DROP);
                mStroke.setStrokeWidth(s * 0.055f);
                for (int i = 0; i < 3; i++) {
                    float x = cx - s * 0.18f + i * s * 0.18f;
                    c.drawLine(x, cy + s * 0.16f, x - s * 0.06f, cy + s * 0.34f, mStroke);
                }
                break;
            case SNOW:
                drawCloud(c, cx, cy - s * 0.06f, s * 0.94f, COL_CLOUD);
                mFill.setColor(COL_SNOW);
                for (int i = 0; i < 3; i++) {
                    c.drawCircle(cx - s * 0.18f + i * s * 0.18f, cy + s * 0.26f, s * 0.035f, mFill);
                }
                break;
            case STORM:
                drawCloud(c, cx, cy - s * 0.10f, s * 0.94f, COL_CLOUD_DARK);
                mPath.reset();
                mPath.moveTo(cx + s * 0.07f, cy + s * 0.06f);
                mPath.lineTo(cx - s * 0.11f, cy + s * 0.30f);
                mPath.lineTo(cx - s * 0.01f, cy + s * 0.30f);
                mPath.lineTo(cx - s * 0.08f, cy + s * 0.48f);
                mPath.lineTo(cx + s * 0.13f, cy + s * 0.22f);
                mPath.lineTo(cx + s * 0.03f, cy + s * 0.22f);
                mPath.lineTo(cx + s * 0.13f, cy + s * 0.06f);
                mPath.close();
                mFill.setColor(COL_SUN);
                c.drawPath(mPath, mFill);
                break;
            default: drawCloud(c, cx, cy, s, COL_CLOUD); break;
        }
    }

    private void drawSun(Canvas c, float cx, float cy, float s, float scale) {
        float r = s * 0.17f * scale;
        mFill.setColor(COL_SUN);
        c.drawCircle(cx, cy, r, mFill);
        mStroke.setColor(COL_SUN);
        mStroke.setStrokeWidth(s * 0.055f * scale);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cx + cos * r * 1.35f, cy + sin * r * 1.35f,
                    cx + cos * r * 1.75f, cy + sin * r * 1.75f, mStroke);
        }
    }

    private void drawMoon(Canvas c, float cx, float cy, float s, float scale) {
        float r = s * 0.27f * scale;
        mPath.reset();
        mPath.addCircle(cx, cy, r, Path.Direction.CW);
        mPath2.reset();
        mPath2.addCircle(cx + r * 0.55f, cy - r * 0.30f, r * 0.85f, Path.Direction.CW);
        mPath.op(mPath2, Path.Op.DIFFERENCE);
        mFill.setColor(COL_MOON);
        c.drawPath(mPath, mFill);
    }

    private void drawCloud(Canvas c, float cx, float cy, float s, int color) {
        mPath.reset();
        mPath.addCircle(cx - s * 0.16f, cy + s * 0.02f, s * 0.15f, Path.Direction.CW);
        mPath.addCircle(cx + s * 0.07f, cy - s * 0.06f, s * 0.19f, Path.Direction.CW);
        mRect.set(cx - s * 0.34f, cy + s * 0.02f, cx + s * 0.30f, cy + s * 0.17f);
        mPath.addRoundRect(mRect, s * 0.08f, s * 0.08f, Path.Direction.CW);
        mFill.setColor(color);
        c.drawPath(mPath, mFill);
    }
}
