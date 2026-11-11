package com.aurora.home;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Samples a real artwork bitmap down to its dominant bright accent colour,
 * so the ambient palette follows the picture's pixels rather than only the
 * curated poster colours. Pure Java (no AndroidX Palette): one 32x32 sample
 * and an 18-bucket hue histogram — accurate enough for UI tinting and
 * cheap enough for a Fire TV Stick.
 */
public final class ArtPalette {

    private ArtPalette() {}

    /**
     * Returns a representative bright accent colour for the artwork, or 0
     * when the bitmap has too little usable signal to be trusted.
     */
    public static int dominant(Bitmap src) {
        if (src == null || src.isRecycled()) return 0;
        try {
            Bitmap small = Bitmap.createScaledBitmap(src, 32, 32, true);
            try {
                float[] hsv = new float[3];
                int[] count = new int[18];
                float[] sumV = new float[18];
                float[] bestS = new float[18];
                int w = small.getWidth(), h = small.getHeight();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int c = small.getPixel(x, y);
                        if ((c >>> 24) < 200) continue;
                        Color.RGBToHSV((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, hsv);
                        float hue = hsv[0];
                        float sat = hsv[1];
                        float val = hsv[2];
                        if (val < 0.16f) continue;            // too dark to tint with
                        int bucket;
                        if (sat < 0.18f) {
                            bucket = 17;                       // neutral/light bucket
                        } else {
                            bucket = Math.min(11, (int) (hue / 30f));
                        }
                        count[bucket]++;
                        sumV[bucket] += val;
                        if (sat > bestS[bucket]) bestS[bucket] = sat;
                    }
                }
                int best = -1;
                int bestWeight = 0;
                for (int i = 0; i < count.length; i++) {
                    // Frequency first, with a slight bias toward saturated hues.
                    int weight = count[i];
                    if (i < 12 && bestS[i] > 0.55f) weight += count[i] / 4;
                    if (weight > bestWeight) {
                        bestWeight = weight;
                        best = i;
                    }
                }
                // Reject weak samples: mostly-dark or mostly-transparent art.
                if (best < 0 || bestWeight < 24) return 0;
                float val = Math.max(0.62f, Math.min(0.94f, sumV[best] / count[best]));
                float sat = best == 17
                        ? 0.10f
                        : Math.min(0.9f, Math.max(0.45f, bestS[best]));
                float hue = best == 17 ? 0f : best * 30f + 15f;
                return Color.HSVToColor(new float[]{hue, sat, val});
            } finally {
                if (small != src && !small.isRecycled()) small.recycle();
            }
        } catch (Throwable t) {
            return 0;
        }
    }
}
