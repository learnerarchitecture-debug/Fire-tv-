package com.aurora.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * The ambient glass backdrop: a deep gradient with three soft colour
 * clouds and a vignette. It is static, so the cost is one draw per
 * invalidation rather than a per-frame blur.
 */
public class AuroraBackgroundView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private String mTheme = "midnight";
    private int mAccent = 0xFFFFB45E;

    public AuroraBackgroundView(Context c) {
        super(c);
    }

    public AuroraBackgroundView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setTheme(String theme, int accent) {
        mTheme = theme == null ? "midnight" : theme;
        mAccent = accent;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        int baseTop, baseBottom, g1, g2, g3;
        if ("graphite".equals(mTheme)) {
            baseTop = 0xFF101218;
            baseBottom = 0xFF050608;
            g1 = 0x12FFFFFF;
            g2 = 0x0F8FA8C0;
            g3 = 0x0AC0C6D8;
        } else if ("aurora".equals(mTheme)) {
            baseTop = 0xFF0A0A1A;
            baseBottom = 0xFF04050B;
            g1 = 0x33A66BFF;
            g2 = 0x267D5BFF;
            g3 = 0x1A2DD4BF;
        } else {
            baseTop = 0xFF0A0C13;
            baseBottom = 0xFF040509;
            g1 = 0x33FF9E4D;
            g2 = 0x2A6B5BFF;
            g3 = 0x1A2FB8A8;
        }

        mPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{baseTop, baseBottom}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, mPaint);

        drawGlow(c, w * 0.06f, h * 0.02f, Math.max(w, h) * 0.62f, g1);
        drawGlow(c, w * 0.98f, h * 0.18f, Math.max(w, h) * 0.56f, g2);
        drawGlow(c, w * 0.52f, h * 1.05f, Math.max(w, h) * 0.72f, g3);
        drawGlow(c, w * 0.80f, h * 0.78f, Math.max(w, h) * 0.34f,
                Ui.alpha(mAccent, 24));

        mPaint.setShader(new RadialGradient(w * 0.5f, h * 0.44f, Math.max(w, h) * 0.78f,
                new int[]{0x00000000, 0x00000000, 0x66000000},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, mPaint);

        mPaint.setShader(null);
    }

    private void drawGlow(Canvas c, float cx, float cy, float radius, int color) {
        mPaint.setShader(new RadialGradient(cx, cy, radius,
                new int[]{color, Ui.alpha(color, 0)}, new float[]{0f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, radius, mPaint);
    }
}
