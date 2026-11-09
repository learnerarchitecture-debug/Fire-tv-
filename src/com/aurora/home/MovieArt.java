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
 * Procedural movie backdrop fallback: a cinematic gradient wash with a
 * soft spotlight, a diagonal lens sweep and film grain. No bitmaps are
 * decoded, which keeps the Fire TV Stick's memory and CPU free — and it
 * only shows when no real artwork could be fetched.
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
        float cx = r.left + r.width() * (wide ? 0.72f : 0.60f);
        float cy = r.top + r.height() * (wide ? 0.40f : 0.38f);
        float rad = min * (wide ? 0.55f : 0.52f);

        // A big soft spotlight halo — the feel of a lit scene, not a planet.
        p.setShader(new RadialGradient(cx, cy, rad * 2.1f,
                new int[]{Ui.alpha(m.c3, 110), Ui.alpha(m.c1, 46), 0x00000000},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, rad * 2.1f, p);

        // Diagonal lens sweep: one soft band of light crossing the frame.
        c.save();
        c.rotate(-24f, cx, cy);
        p.setShader(new LinearGradient(cx - rad * 2.2f, cy, cx + rad * 2.2f, cy,
                new int[]{0x00FFFFFF, Ui.alpha(m.c3, 60), 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(cx - rad * 0.14f, r.top, cx + rad * 0.14f, r.bottom, p);
        c.restore();

        // Film grain, seeded so a movie always has the same speckle.
        Random rnd = new Random(m.id.hashCode() * 31L + 7L);
        int count = Math.max(24, Math.min(90, (int) (r.width() * r.height() / 3000f)));
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        for (int i = 0; i < count; i++) {
            float x = r.left + rnd.nextFloat() * r.width();
            float y = r.top + rnd.nextFloat() * r.height();
            float sr = Math.max(0.5f, min * (0.0018f + rnd.nextFloat() * 0.0032f));
            p.setColor(Ui.alpha(0xFFFFFFFF, 22 + rnd.nextInt(85)));
            c.drawCircle(x, y, sr, p);
        }

        // Bottom shade for titles; switch back to FILL before drawing it.
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
