package com.aurora.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Large procedural poster used behind the featured movie. */
public class PosterArtView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF mRect = new RectF();
    private Movie mMovie = MovieCatalog.first();

    public PosterArtView(Context c) {
        super(c);
    }

    public PosterArtView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setMovie(Movie m) {
        if (m == null) return;
        mMovie = m;
        ThumbnailLoader.load(getContext(), m, this);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (mMovie == null) return;
        float radius = Ui.dp(getContext(), 21);
        mRect.set(0, 0, getWidth(), getHeight());
        // Use thumbnail bitmap if present; otherwise procedural poster.
        if (mMovie.thumbnail != null && !mMovie.thumbnail.isRecycled()) {
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);
            Rect src = ThumbnailLoader.coverSrc(mRect.width(), mRect.height(),
                    mMovie.thumbnail.getWidth(), mMovie.thumbnail.getHeight());
            c.drawBitmap(mMovie.thumbnail, src, mRect, mPaint);
        } else {
            MovieArt.drawPoster(c, mRect, mMovie, radius, mPaint);
        }
        // Soft highlight edge (works on both bitmap and poster)
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Ui.dp(getContext(), 1));
        mPaint.setColor(0x22FFFFFF);
        c.drawRoundRect(new RectF(0.5f, 0.5f, getWidth() - 0.5f, getHeight() - 0.5f),
                radius, radius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
    }
}
