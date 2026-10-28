package com.aurora.home;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * Procedural movie poster art: a coloured nebula, a ringed planet and
 * a field of stars. No bitmaps are decoded, which keeps the Fire TV
 * Stick's memory and CPU free.
 */
public final class MovieArt {

    private MovieArt() {}

    public static void drawPoster(Canvas c, RectF r, Movie m, float radius, Paint p) {
        if (r.width() <= 0 || r.height() <= 0) return;
        // The deck dims background cards by changing the paint alpha;
        // every new poster must start fully opaque again.
        p.setAlpha(255);
        Path clip = new Path();
        clip.addRoundRect(r, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(clip);

        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                new int[]{m.c1, m.c2}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(r, p);

        float min = Math.min(r.width(), r.height());
        boolean wide = r.width() > r.height() * 1.65f;
        float cx = r.left + r.width() * (wide ? 0.75f : 0.78f);
        float cy = r.top + r.height() * (wide ? 0.44f : 0.42f);
        float rad = min * (wide ? 0.34f : 0.37f);

        p.setShader(new RadialGradient(cx, cy, rad * 2.15f,
                new int[]{Ui.alpha(m.c3, 95), Ui.alpha(m.c1, 40), 0x00000000},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, rad * 2.15f, p);

        p.setShader(new RadialGradient(cx - rad * 0.32f, cy - rad * 0.36f, rad * 1.35f,
                new int[]{m.c3, m.c1, m.c2},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, rad, p);

        // A tilted planetary ring.
        c.save();
        c.rotate(-18f, cx, cy);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, rad * 0.035f));
        p.setColor(Ui.alpha(m.c3, 145));
        c.drawOval(new RectF(cx - rad * 1.55f, cy - rad * 0.42f,
                cx + rad * 1.55f, cy + rad * 0.42f), p);
        p.setStrokeWidth(Math.max(1f, rad * 0.018f));
        p.setColor(Ui.alpha(m.c3, 75));
        c.drawOval(new RectF(cx - rad * 1.82f, cy - rad * 0.56f,
                cx + rad * 1.82f, cy + rad * 0.56f), p);
        c.restore();

        // Stars, seeded so a movie always has the same sky.
        Random rnd = new Random(m.id.hashCode() * 31L + 7L);
        int count = Math.max(8, Math.min(34, (int) (r.width() * r.height() / 4800f)));
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < count; i++) {
            float x = r.left + rnd.nextFloat() * r.width();
            float y = r.top + rnd.nextFloat() * r.height() * 0.78f;
            float sr = Math.max(0.5f, min * (0.004f + rnd.nextFloat() * 0.006f));
            p.setColor(Ui.alpha(0xFFFFFFFF, 55 + rnd.nextInt(130)));
            c.drawCircle(x, y, sr, p);
        }

        // A soft aurora ribbon.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, min * 0.012f));
        p.setColor(Ui.alpha(m.c3, 38));
        Path ribbon = new Path();
        ribbon.moveTo(r.left - r.width() * 0.05f, r.top + r.height() * 0.72f);
        ribbon.cubicTo(r.left + r.width() * 0.28f, r.top + r.height() * 0.35f,
                r.left + r.width() * 0.62f, r.top + r.height() * 0.58f,
                r.right + r.width() * 0.05f, r.top + r.height() * 0.22f);
        c.drawPath(ribbon, p);

        // Bottom shade for titles. The ribbon above left the paint in
        // STROKE mode, so switch back to FILL before drawing the shade.
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, r.top + r.height() * 0.42f,
                0, r.bottom, new int[]{0x0005070B, 0xB305070B, 0xE605070B},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(r.left, r.top + r.height() * 0.35f, r.right, r.bottom, p);

        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        c.restore();
    }
}
