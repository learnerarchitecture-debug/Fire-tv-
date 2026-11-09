package com.aurora.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * "Ambient Drift" filmstrip (Option C design lab): the swipe deck becomes a
 * horizontally sliding strip of mini posters along the bottom of the screen.
 * The selected title sits centre, slightly enlarged, and drives the hero
 * text block + AmbientBackdropView through the onSelected callback. D-pad
 * left/right browses without consuming titles; pass/save/undo keep the
 * original deck semantics so the watchlist store behaves identically.
 */
public final class FilmstripView extends View {

    public interface Listener {
        void onSwiped(Movie movie, boolean saved);
        void onUndo(Movie movie, boolean wasSaved);
        void onDeckChanged(int position, int total, boolean canUndo);
        void onOpen(Movie movie);
        void onSelected(Movie movie);
    }

    public interface EdgeListener {
        void onUpEdge();
        void onDownEdge();
    }

    private static final class HistoryEntry {
        final int index;
        final boolean saved;

        HistoryEntry(int index, boolean saved) {
            this.index = index;
            this.saved = saved;
        }
    }

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mScrim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mSrc = new Rect();
    private final RectF mTmp = new RectF();

    private List<Movie> mMovies = new ArrayList<Movie>();
    private final List<HistoryEntry> mHistory = new ArrayList<HistoryEntry>();
    private Set<String> mSavedIds = Collections.emptySet();
    private int mIndex = 0;
    private int mAccent = 0xFFFFB45E;
    private Listener mListener;
    private EdgeListener mEdges;

    public FilmstripView(Context c, AttributeSet a) {
        super(c, a);
        setFilmTexts();
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void setFilmTexts() {
        mText.setColor(0xFFFFFFFF);
        mText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    // ---- public control API (mirrors the old SwipeDeckView surface) ---------

    public void setListener(Listener l) {
        mListener = l;
    }

    public void setEdgeListener(EdgeListener e) {
        mEdges = e;
    }

    public void setAccent(int accent) {
        mAccent = accent;
        invalidate();
    }

    /** Kept for drop-in compatibility with the old deck binding code. */
    public void setAnimationsEnabled(boolean enabled) {
        // Filmstrip never uses drag animation storage; nothing to do.
    }

    public void setSavedIds(Set<String> ids) {
        mSavedIds = ids != null ? ids : Collections.<String>emptySet();
        invalidate();
    }

    public void setMovies(List<Movie> movies) {
        mMovies = movies != null ? new ArrayList<Movie>(movies) : new ArrayList<Movie>();
        mHistory.clear();
        mIndex = 0;
        for (Movie m : mMovies) {
            ThumbnailLoader.load(getContext(), m, this);
        }
        notifyDeckChanged();
        notifySelected();
        invalidate();
    }

    public Movie getCurrentMovie() {
        if (mMovies.isEmpty()) return null;
        return mMovies.get(mIndex % mMovies.size());
    }

    public boolean canUndo() {
        return !mHistory.isEmpty();
    }

    public void swipeLeft() {
        move(false);
    }

    public void swipeRight() {
        move(true);
    }

    public void nextSelection() {
        if (mMovies.size() < 2) return;
        advance(1);
    }

    public void undo() {
        if (mHistory.isEmpty()) return;
        HistoryEntry last = mHistory.remove(mHistory.size() - 1);
        mIndex = last.index % Math.max(1, mMovies.size());
        if (mListener != null && !mMovies.isEmpty()) {
            mListUpdate();
            mListener.onUndo(mMovies.get(mIndex), last.saved);
        } else {
            mListUpdate();
        }
    }

    private void move(boolean saved) {
        if (mMovies.isEmpty()) return;
        Movie m = mMovies.get(mIndex % mMovies.size());
        mHistory.add(new HistoryEntry(mIndex, saved));
        if (mListener != null) mListener.onSwiped(m, saved);
        advance(1);
    }

    private void advance(int d) {
        if (mMovies.isEmpty()) return;
        mIndex = (mIndex + d + mMovies.size()) % mMovies.size();
        mListUpdate();
    }

    private void mListUpdate() {
        notifyDeckChanged();
        notifySelected();
        invalidate();
    }

    private void notifyDeckChanged() {
        if (mListener != null) {
            mListener.onDeckChanged(mIndex % Math.max(1, mMovies.size()), mMovies.size(), !mHistory.isEmpty());
        }
    }

    private void notifySelected() {
        if (mListener != null) {
            mListener.onSelected(getCurrentMovie());
        }
    }

    // ---- input --------------------------------------------------------------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mMovies.size() < 2) return true;
            advance(-1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mMovies.size() < 2) return true;
            advance(1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mEdges != null) mEdges.onUpEdge();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (mEdges != null) mEdges.onDownEdge();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            Movie m = getCurrentMovie();
            if (m != null && mListener != null) mListener.onOpen(m);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ---- drawing ------------------------------------------------------------

    private int dp(float v) {
        return (int) Ui.dp(getContext(), v);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w < 60 || h < 40) return;
        List<Movie> ms = mMovies;
        int n = ms.size();
        if (n == 0) return;

        int gap = dp(10);
        int miniW = dp(72), miniH = dp(104);
        int curW = dp(126), curH = dp(178);
        if (curH > h - dp(8)) {
            float k = (h - dp(8)) / (float) curH;
            curW = Math.round(curW * k);
            curH = Math.round(curH * k);
        }

        // cumulative strip baseline positions with the current mini centred
        int[] xs = new int[n];
        int[] ws = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            int wi = (i == mIndex) ? curW : miniW;
            xs[i] = total;
            ws[i] = wi;
            total += wi;
            if (i < n - 1) total += gap;
        }
        int curLeft = xs[mIndex];
        int x0 = w / 2 - curLeft - ws[mIndex] / 2;

        int base = h - dp(2);
        for (int i = 0; i < n; i++) {
            if (i == mIndex) continue;
            drawMini(c, ms.get(i), x0 + xs[i], base - miniH, ws[i], miniH, false);
        }
        Movie cur = getCurrentMovie();
        if (cur != null) {
            drawMini(c, cur, x0 + xs[mIndex], base - curH, curW, curH, true);
        }
        if (isFocused()) {
            // subtle accent underline across the strip edge while it owns focus
            mRing.setStyle(Paint.Style.STROKE);
            mRing.setStrokeWidth(dp(2));
            mRing.setColor(mAccent);
            mRing.setAlpha(160);
            c.drawLine(dp(6), h - dp(3), w - dp(6), h - dp(3), mRing);
        }
    }

    private void drawMini(Canvas c, Movie m, int left, int top, int mw, int mh, boolean cur) {
        float radius = dp(10);
        mTmp.set(left, top, left + mw, top + mh);

        boolean saved = mSavedIds.contains(m.id);
        int alpha = cur ? 255 : 150;
        if (saved) alpha = Math.max(alpha, 225);

        Bitmap bmp = m.thumbnail;   // volatile read
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(alpha);
        if (bmp != null && !bmp.isRecycled()) {
            Rect src = ThumbnailLoader.coverSrc(mw, mh, bmp.getWidth(), bmp.getHeight());
            c.drawBitmap(bmp, src, mTmp, mPaint);
        } else {
            MovieArt.drawPoster(c, mTmp, m, radius, mPaint);
        }

        // legibility scrim at the poster foot
        mScrim.setShader(new LinearGradient(0, mTmp.bottom - mh * 0.42f, 0, mTmp.bottom,
                0x0005070B, 0xB305070B, Shader.TileMode.CLAMP));
        c.drawRect(mTmp.left, mTmp.bottom - mh * 0.42f, mTmp.right, mTmp.bottom, mScrim);

        // title
        float ts = cur ? dp(9) : dp(6.5f);
        mText.setTextSize(ts);
        mText.setAlpha(255);
        mText.setColor(0xFFF2F4F8);
        String t = m.title;
        while (t.length() > 3 && mText.measureText(t) > mw - dp(10)) {
            t = t.substring(0, t.length() - 2);
        }
        c.drawText(t, mTmp.left + dp(6), mTmp.bottom - dp(6), mText);

        // rounded frame + selection ring
        mRing.setShader(null);
        mRing.setStyle(Paint.Style.STROKE);
        mRing.setStrokeWidth(dp(cur ? 2.5f : 1f));
        mRing.setColor(cur ? 0xFFFFFFFF : (saved ? mAccent : 0x26FFFFFF));
        mRing.setAlpha(255);
        c.drawRoundRect(mTmp, radius, radius, mRing);

        if (saved) {
            mRing.setStyle(Paint.Style.FILL);
            mRing.setColor(mAccent);
            float cr = dp(cur ? 4.5f : 3.5f);
            c.drawCircle(mTmp.right - cr - dp(7), mTmp.top + cr + dp(7), cr, mRing);
        }
    }
}
