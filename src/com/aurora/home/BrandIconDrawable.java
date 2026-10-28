package com.aurora.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Small branded fallback icon used when an app is not installed. */
public class BrandIconDrawable extends Drawable {

    public static final int LETTER = 0;
    public static final int PLAY = 1;

    private final int mColor1;
    private final int mColor2;
    private final String mGlyph;
    private final int mMode;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final RectF mRect = new RectF();
    private int mAlpha = 255;

    public BrandIconDrawable(int color1, int color2, String glyph, int mode) {
        mColor1 = color1;
        mColor2 = color2;
        mGlyph = glyph;
        mMode = mode;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float radius = Math.min(b.width(), b.height()) * 0.23f;
        mRect.set(b);
        mPaint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                mColor1, mColor2, Shader.TileMode.CLAMP));
        mPaint.setAlpha(mAlpha);
        mPaint.setStyle(Paint.Style.FILL);
        c.drawRoundRect(mRect, radius, radius, mPaint);
        mPaint.setShader(null);
        mPaint.setColor(0xFFFFFFFF);
        mPaint.setAlpha(mAlpha);
        if (mMode == PLAY) {
            float cx = b.exactCenterX(), cy = b.exactCenterY();
            float s = Math.min(b.width(), b.height()) * 0.23f;
            mPath.reset();
            mPath.moveTo(cx - s * 0.62f, cy - s);
            mPath.lineTo(cx + s, cy);
            mPath.lineTo(cx - s * 0.62f, cy + s);
            mPath.close();
            c.drawPath(mPath, mPaint);
        } else {
            mPaint.setTypeface(Ui.BOLD);
            mPaint.setTextSize(Math.min(b.width(), b.height()) * 0.56f);
            mPaint.setTextAlign(Paint.Align.CENTER);
            float y = b.exactCenterY() - (mPaint.ascent() + mPaint.descent()) / 2f;
            c.drawText(mGlyph, b.exactCenterX(), y, mPaint);
            mPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
