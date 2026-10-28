package com.aurora.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Large procedural poster used behind the featured movie. */
public class PosterArtView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF mRect = new RectF();
    private Movie mMovie = MovieCatalog.MOVIES.get(0);

    public PosterArtView(Context c) {
        super(c);
    }

    public PosterArtView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setMovie(Movie m) {
        if (m == null) return;
        mMovie = m;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float radius = Ui.dp(getContext(), 21);
        mRect.set(0, 0, getWidth(), getHeight());
        MovieArt.drawPoster(c, mRect, mMovie, radius, mPaint);

        // A very soft highlight along the top edge.
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Ui.dp(getContext(), 1));
        mPaint.setColor(0x22FFFFFF);
        c.drawRoundRect(new RectF(0.5f, 0.5f, getWidth() - 0.5f, getHeight() - 0.5f),
                radius, radius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
    }
}
