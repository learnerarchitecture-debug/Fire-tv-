package com.aurora.home;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

/**
 * Loads and rasterizes installed app icons on a single background
 * thread with an LruCache. Serial loading keeps the UI thread free on
 * the Fire TV Stick's slow CPU.
 */
public class IconLoader {

    private final LruCache<String, Bitmap> mCache =
            new LruCache<String, Bitmap>(12 * 1024 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getRowBytes() * value.getHeight();
                }
            };

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final HandlerThread mThread = new HandlerThread("icons");
    private final Handler mBg;
    private final PackageManager mPm;
    private final int mSizePx;

    public IconLoader(Context c, int sizePx) {
        mThread.start();
        mBg = new Handler(mThread.getLooper());
        mPm = c.getPackageManager();
        mSizePx = sizePx;
    }

    public Bitmap peek(String pkg) {
        synchronized (mCache) {
            return mCache.get(pkg);
        }
    }

    public void queue(final String pkg, final ImageView target) {
        mBg.post(new Runnable() {
            @Override
            public void run() {
                Bitmap b;
                synchronized (mCache) {
                    b = mCache.get(pkg);
                }
                if (b == null) {
                    b = render(pkg);
                    if (b != null) {
                        synchronized (mCache) {
                            mCache.put(pkg, b);
                        }
                    }
                }
                final Bitmap fb = b;
                mMain.post(new Runnable() {
                    @Override
                    public void run() {
                        if (fb == null) return;
                        if (!pkg.equals(target.getTag())) return;
                        Drawable d = target.getDrawable();
                        if (d instanceof RoundedIconDrawable) {
                            ((RoundedIconDrawable) d).setBitmap(fb);
                        }
                    }
                });
            }
        });
    }

    private Bitmap render(String pkg) {
        try {
            Drawable d = mPm.getApplicationIcon(pkg);
            if (d == null) return null;
            Bitmap b = Bitmap.createBitmap(mSizePx, mSizePx, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            int iw = d.getIntrinsicWidth();
            int ih = d.getIntrinsicHeight();
            if (iw <= 0 || ih <= 0) {
                iw = mSizePx;
                ih = mSizePx;
            }
            float scale = Math.min(mSizePx * 0.96f / iw, mSizePx * 0.96f / ih);
            int w = Math.max(1, (int) (iw * scale));
            int h = Math.max(1, (int) (ih * scale));
            d.setBounds((mSizePx - w) / 2, (mSizePx - h) / 2,
                    (mSizePx - w) / 2 + w, (mSizePx - h) / 2 + h);
            d.draw(c);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    public void destroy() {
        mThread.quit();
    }
}
