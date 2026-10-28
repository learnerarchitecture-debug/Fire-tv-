package com.aurora.home;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Draws a bitmap clipped to a rounded rectangle (modern squircle-style
 * app icon). Keeps the shader/matrix work in onBoundsChange so the
 * per-draw cost is a single drawRoundRect.
 */
public class RoundedIconDrawable extends Drawable {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix mMatrix = new Matrix();
    private Bitmap mBitmap;
    private final float mRadius;

    public RoundedIconDrawable(float radiusPx) {
        mRadius = radiusPx;
    }

    public void setBitmap(Bitmap b) {
        mBitmap = b;
        if (b != null) {
            mPaint.setShader(new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        } else {
            mPaint.setShader(null);
        }
        updateMatrix();
        invalidateSelf();
    }

    private void updateMatrix() {
        if (mBitmap == null || mPaint.getShader() == null) return;
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float scale = Math.min((float) b.width() / mBitmap.getWidth(),
                (float) b.height() / mBitmap.getHeight());
        float dx = (b.width() - mBitmap.getWidth() * scale) / 2f;
        float dy = (b.height() - mBitmap.getHeight() * scale) / 2f;
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate(b.left + dx, b.top + dy);
        mPaint.getShader().setLocalMatrix(mMatrix);
    }

    @Override
    protected void onBoundsChange(Rect r) {
        super.onBoundsChange(r);
        updateMatrix();
    }

    @Override
    public void draw(Canvas c) {
        if (mPaint.getShader() == null) return;
        Rect b = getBounds();
        c.drawRoundRect(b.left, b.top, b.right, b.bottom, mRadius, mRadius, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap != null ? mBitmap.getWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap != null ? mBitmap.getHeight() : -1;
    }
}
