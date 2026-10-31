package com.aurora.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** A compact, focusable recommendation card for the horizontal rail. */
public class MovieCardView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private Movie mMovie = MovieCatalog.MOVIES.get(0);
    private int mAccent = 0xFFFFB45E;
    private boolean mAnimateFocus = true;

    public MovieCardView(Context c) {
        super(c);
        init();
    }

    public MovieCardView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
        setWillNotDraw(false);
    }

    public void setMovie(Movie m) {
        if (m == null) return;
        mMovie = m;
        invalidate();
    }

    public Movie getMovie() {
        return mMovie;
    }

    public void setAccent(int accent) {
        mAccent = accent;
        invalidate();
    }

    public void setAnimateFocus(boolean animate) {
        mAnimateFocus = animate;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (mAnimateFocus) {
            animate().scaleX(gainFocus ? 1.028f : 1f)
                    .scaleY(gainFocus ? 1.028f : 1f)
                    .setDuration(130).start();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float radius = Ui.dp(getContext(), 14);
        mRect.set(0, 0, w, h);
        if (mMovie.thumbnail != null && !mMovie.thumbnail.isRecycled()) {
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setAntiAlias(true);
            // Thumbnail replaces procedural art when present
            c.drawBitmap(mMovie.thumbnail, null, mRect, mPaint);
        } else {
            MovieArt.drawPoster(c, mRect, mMovie, radius, mPaint);
        }

        // Hairline border keeps each card crisp against the glass.
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Ui.dp(getContext(), 1));
        mPaint.setColor(0x26FFFFFF);
        c.drawRoundRect(mRect, radius, radius, mPaint);

        float pad = Ui.dp(getContext(), 11);

        // Provider chip, top-left: the card always names the app that
        // supplies the recommendation alongside its artwork.
        if (mMovie.providerName.length() > 0) {
            mText.setTypeface(Ui.MEDIUM);
            mText.setTextSize(Ui.sp(getContext(), 6));
            String provider = Ui.ellipsize(mText, mMovie.providerName, w - pad * 2f);
            float chipW = mText.measureText(provider) + Ui.dp(getContext(), 12);
            float chipH = Ui.dp(getContext(), 12);
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Ui.alpha(mAccent, 46));
            c.drawRoundRect(new RectF(pad, pad, pad + chipW, pad + chipH),
                    chipH / 2f, chipH / 2f, mPaint);
            mText.setColor(mAccent);
            c.drawText(provider, pad + Ui.dp(getContext(), 6),
                    pad + chipH - Ui.dp(getContext(), 3.2f), mText);
        }

        // Copy sits along the bottom edge, like the approved mockup.
        float ratingReserve = Ui.dp(getContext(), 50);
        float textW = w - pad * 2f - ratingReserve;

        mText.setTypeface(Ui.BOLD);
        mText.setTextSize(Ui.sp(getContext(), 10.5f));
        mText.setColor(0xFFF2F4F8);
        String title = Ui.ellipsize(mText, mMovie.title, textW);
        c.drawText(title, pad, h - Ui.dp(getContext(), 23), mText);

        mText.setTypeface(Ui.REGULAR);
        mText.setTextSize(Ui.sp(getContext(), 7));
        mText.setColor(0xFFA5ADBE);
        String meta = Ui.ellipsize(mText, mMovie.year + "  ·  " + mMovie.genre, textW);
        c.drawText(meta, pad, h - Ui.dp(getContext(), 10), mText);

        // Match pill, bottom-right.
        mText.setTypeface(Ui.MEDIUM);
        mText.setTextSize(Ui.sp(getContext(), 6));
        String match = mMovie.match + "%";
        float pillW = mText.measureText(match) + Ui.dp(getContext(), 12);
        float pillH = Ui.dp(getContext(), 12);
        float pillLeft = w - pad - pillW;
        float pillTop = h - Ui.dp(getContext(), 18);
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Ui.alpha(mAccent, 42));
        c.drawRoundRect(new RectF(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH),
                pillH / 2f, pillH / 2f, mPaint);
        mText.setColor(mAccent);
        c.drawText(match, pillLeft + Ui.dp(getContext(), 6),
                pillTop + pillH - Ui.dp(getContext(), 3.2f), mText);

        if (isFocused()) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(Ui.dp(getContext(), 2));
            mPaint.setColor(0xE6FFFFFF);
            float inset = Ui.dp(getContext(), 1);
            c.drawRoundRect(new RectF(inset, inset, w - inset, h - inset),
                    radius, radius, mPaint);
        }
        mPaint.setStyle(Paint.Style.FILL);
    }
}
