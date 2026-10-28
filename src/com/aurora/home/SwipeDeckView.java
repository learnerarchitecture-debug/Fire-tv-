package com.aurora.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A small Tinder-style deck. It supports touch drags, the remote's
 * left/right keys, and the on-screen pass/save buttons. Cards are
 * rendered directly to the canvas so the deck stays smooth on the
 * Fire TV Stick's modest GPU.
 */
public class SwipeDeckView extends View {

    public interface Listener {
        void onSwiped(Movie movie, boolean saved);
        void onUndo(Movie movie, boolean wasSaved);
        void onDeckChanged(int position, int total, boolean canUndo);
        void onOpen(Movie movie);
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
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBase = new RectF();
    private final RectF mTmp = new RectF();
    private final Path mHeart = new Path();

    private List<Movie> mDeck = MovieCatalog.MOVIES;
    private final List<HistoryEntry> mHistory = new ArrayList<HistoryEntry>();
    private final Set<String> mSaved = new HashSet<String>();
    private int mIndex = 0;
    private int mAccent = 0xFFFFB45E;

    private float mDragX, mDragY;
    private boolean mDragging;
    private float mDownX, mDownY;
    private boolean mAnimating;
    private boolean mAnimRight;
    private float mAnimProgress;
    private ValueAnimator mAnimator;
    private boolean mAnimations = true;

    private Listener mListener;

    public SwipeDeckView(Context c) {
        super(c);
        init();
    }

    public SwipeDeckView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    public void setAccent(int accent) {
        mAccent = accent;
        invalidate();
    }

    public void setAnimationsEnabled(boolean enabled) {
        mAnimations = enabled;
    }

    public void setListener(Listener l) {
        mListener = l;
        notifyDeckChanged();
    }

    public void setSavedIds(Set<String> ids) {
        mSaved.clear();
        if (ids != null) mSaved.addAll(ids);
        invalidate();
    }

    public Movie getCurrentMovie() {
        if (mDeck.isEmpty()) return null;
        return mDeck.get(mIndex % mDeck.size());
    }

    public boolean canUndo() {
        return !mHistory.isEmpty();
    }

    public void swipeLeft() {
        animateOff(false);
    }

    public void swipeRight() {
        animateOff(true);
    }

    public void undo() {
        if (mAnimating || mHistory.isEmpty()) return;
        HistoryEntry last = mHistory.remove(mHistory.size() - 1);
        mIndex = last.index;
        mDragX = 0;
        mDragY = 0;
        if (mListener != null && mIndex >= 0 && mIndex < mDeck.size()) {
            mListener.onUndo(mDeck.get(mIndex), last.saved);
        }
        invalidate();
        notifyDeckChanged();
    }

    private void notifyDeckChanged() {
        if (mListener != null) {
            mListener.onDeckChanged(mIndex, mDeck.size(), canUndo());
        }
    }

    private Movie movieAt(int offset) {
        if (mDeck.isEmpty()) return null;
        return mDeck.get((mIndex + offset) % mDeck.size());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || mDeck.isEmpty()) return;

        float cardW = Math.min(w * 0.54f, Ui.dp(getContext(), 160));
        float cardH = Math.min(h - Ui.dp(getContext(), 4), Ui.dp(getContext(), 170));
        float left = (w - cardW) / 2f;
        float top = (h - cardH) / 2f + Ui.dp(getContext(), 2);
        mBase.set(left, top, left + cardW, top + cardH);

        // Two cards behind the active one.
        for (int i = 2; i >= 1; i--) {
            Movie m = movieAt(i);
            if (m == null) continue;
            float scale = 1f - i * 0.045f;
            float cw = cardW * scale;
            float ch = cardH * scale;
            float cx = mBase.centerX() + (mDragX * 0.06f * i);
            float cy = mBase.centerY() + Ui.dp(getContext(), 6 * i);
            mTmp.set(cx - cw / 2f, cy - ch / 2f, cx + cw / 2f, cy + ch / 2f);
            drawCard(c, m, mTmp, i == 1 ? 150 : 80, false, 0f);
        }

        // The active card follows the finger or animates away.
        Movie topMovie = getCurrentMovie();
        if (topMovie != null) {
            mTmp.set(mBase);
            float rotation = (mDragX / Math.max(1f, w)) * 13f;
            drawCard(c, topMovie, mTmp, 255, true, rotation);
            drawStamps(c, mBase);
        }

    }

    private void drawCard(Canvas c, Movie m, RectF r, int alpha, boolean active, float rotation) {
        mPaint.setAlpha(255);
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.FILL);
        c.save();
        if (active) {
            c.translate(mDragX, mDragY);
            c.rotate(rotation, r.centerX(), r.centerY());
        }
        float radius = Ui.dp(getContext(), 15);
        MovieArt.drawPoster(c, r, m, radius, mPaint);

        // A permanent hairline keeps the card defined even when unfocused.
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Ui.dp(getContext(), 1));
        mPaint.setColor(0x26FFFFFF);
        c.drawRoundRect(r, radius, radius, mPaint);

        if (alpha < 255) {
            mPaint.setShader(null);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor((255 - alpha) << 24 | 0x0005070B);
            c.drawRoundRect(r, radius, radius, mPaint);
        }

        float pad = Ui.dp(getContext(), 11);
        // Tag pill.
        mText.setTypeface(Ui.MEDIUM);
        mText.setTextSize(Ui.sp(getContext(), 6.3f));
        mText.setColor(mAccent);
        String tag = m.tag;
        float tagW = mText.measureText(tag) + Ui.dp(getContext(), 12);
        float tagH = Ui.dp(getContext(), 12);
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Ui.alpha(mAccent, 38));
        c.drawRoundRect(new RectF(r.left + pad, r.top + pad,
                r.left + pad + tagW, r.top + pad + tagH), tagH / 2f, tagH / 2f, mPaint);
        c.drawText(tag, r.left + pad + Ui.dp(getContext(), 6),
                r.top + pad + tagH - Ui.dp(getContext(), 3.2f), mText);

        if (mSaved.contains(m.id)) {
            float hx = r.right - Ui.dp(getContext(), 18);
            float hy = r.top + Ui.dp(getContext(), 18);
            mPaint.setColor(0xCC05070B);
            c.drawCircle(hx, hy, Ui.dp(getContext(), 9), mPaint);
            drawHeart(c, hx, hy, Ui.dp(getContext(), 5.5f), mAccent, true);
        }

        // Title, metadata and match.
        float textW = r.width() - pad * 2f;
        mText.setTypeface(Ui.BOLD);
        mText.setTextSize(Ui.sp(getContext(), 14));
        mText.setColor(0xFFF2F4F8);
        String title = Ui.ellipsize(mText, m.title, textW);
        c.drawText(title, r.left + pad, r.bottom - Ui.dp(getContext(), 47), mText);

        mText.setTypeface(Ui.REGULAR);
        mText.setTextSize(Ui.sp(getContext(), 7.5f));
        mText.setColor(0xFFA5ADBE);
        String meta = Ui.ellipsize(mText, m.year + "  ·  " + m.genre + "  ·  " + m.duration, textW);
        c.drawText(meta, r.left + pad, r.bottom - Ui.dp(getContext(), 33), mText);

        mText.setTypeface(Ui.MEDIUM);
        mText.setTextSize(Ui.sp(getContext(), 6.8f));
        String match = m.match + "% MATCH";
        float matchW = mText.measureText(match) + Ui.dp(getContext(), 12);
        float matchH = Ui.dp(getContext(), 13);
        float matchTop = r.bottom - Ui.dp(getContext(), 24);
        mPaint.setColor(Ui.alpha(mAccent, 45));
        c.drawRoundRect(new RectF(r.left + pad, matchTop,
                r.left + pad + matchW, matchTop + matchH), matchH / 2f, matchH / 2f, mPaint);
        mText.setColor(mAccent);
        c.drawText(match, r.left + pad + Ui.dp(getContext(), 6),
                matchTop + matchH - Ui.dp(getContext(), 3.4f), mText);

        if (active && isFocused()) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(Ui.dp(getContext(), 2));
            mPaint.setColor(0xE6FFFFFF);
            float inset = Ui.dp(getContext(), 1);
            mTmp.set(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset);
            c.drawRoundRect(mTmp, radius, radius, mPaint);
            mPaint.setStyle(Paint.Style.FILL);
        }
        c.restore();
    }

    private void drawStamps(Canvas c, RectF r) {
        float progress = mAnimating ? mAnimProgress
                : Math.min(1f, Math.abs(mDragX) / Math.max(1f, getWidth() * 0.32f));
        if (progress < 0.03f) return;
        boolean right = mAnimating ? mAnimRight : mDragX > 0;
        String text = right ? "SAVE" : "PASS";
        int color = right ? 0xFF6FE3B0 : 0xFFFF7A6B;
        mText.setTypeface(Ui.BOLD);
        mText.setTextSize(Ui.sp(getContext(), 17));
        mText.setColor(color);
        float tw = mText.measureText(text);
        float pw = tw + Ui.dp(getContext(), 18);
        float ph = Ui.dp(getContext(), 28);
        float left = right ? r.left + Ui.dp(getContext(), 12)
                : r.right - pw - Ui.dp(getContext(), 12);
        float top = r.top + Ui.dp(getContext(), 34);
        c.save();
        c.rotate(right ? -12f : 12f, left + pw / 2f, top + ph / 2f);
        mPaint.setShader(null);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Ui.dp(getContext(), 2));
        mPaint.setColor(Ui.alpha(color, (int) (255 * progress)));
        c.drawRoundRect(new RectF(left, top, left + pw, top + ph),
                Ui.dp(getContext(), 6), Ui.dp(getContext(), 6), mPaint);
        mText.setColor(Ui.alpha(color, (int) (255 * progress)));
        c.drawText(text, left + Ui.dp(getContext(), 9), top + ph - Ui.dp(getContext(), 8), mText);
        c.restore();
    }

    private void drawHeart(Canvas c, float cx, float cy, float s, int color, boolean filled) {
        mHeart.reset();
        mHeart.moveTo(cx, cy + s * 0.42f);
        mHeart.cubicTo(cx - s * 1.05f, cy - s * 0.18f, cx - s * 0.48f, cy - s * 0.92f,
                cx, cy - s * 0.26f);
        mHeart.cubicTo(cx + s * 0.48f, cy - s * 0.92f, cx + s * 1.05f, cy - s * 0.18f,
                cx, cy + s * 0.42f);
        mHeart.close();
        mPaint.setShader(null);
        mPaint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
        mPaint.setStrokeWidth(Math.max(1f, s * 0.22f));
        mPaint.setColor(color);
        c.drawPath(mHeart, mPaint);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mAnimating) return true;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestFocus();
                mDownX = e.getX();
                mDownY = e.getY();
                mDragX = 0;
                mDragY = 0;
                mDragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mDragging) {
                    mDragX = e.getX() - mDownX;
                    mDragY = e.getY() - mDownY;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mDragging) {
                    mDragging = false;
                    float dx = mDragX;
                    if (Math.abs(dx) > getWidth() * 0.22f) {
                        animateOff(dx > 0);
                    } else if (Math.abs(dx) < Ui.dp(getContext(), 8)
                            && Math.abs(mDragY) < Ui.dp(getContext(), 8)) {
                        mDragX = 0;
                        mDragY = 0;
                        performClick();
                        if (mListener != null && getCurrentMovie() != null) {
                            mListener.onOpen(getCurrentMovie());
                        }
                        invalidate();
                    } else {
                        animateBack();
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                // A cancelled gesture must never count as a swipe.
                if (mDragging) {
                    mDragging = false;
                    animateBack();
                }
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            swipeLeft();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            swipeRight();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mListener != null && getCurrentMovie() != null) {
                mListener.onOpen(getCurrentMovie());
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void animateOff(final boolean right) {
        if (mAnimating || mDeck.isEmpty()) return;
        mAnimating = true;
        mAnimRight = right;
        mAnimProgress = 0f;
        final float startX = mDragX;
        final float startY = mDragY;
        final float endX = (right ? 1f : -1f) * (getWidth() * 1.18f);
        final float endY = startY * 1.3f;
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(mAnimations ? 250 : 0);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                mAnimProgress = t;
                mDragX = startX + (endX - startX) * t;
                mDragY = startY + (endY - startY) * t;
                invalidate();
            }
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                mAnimating = false;
                mAnimProgress = 0f;
                mDragX = 0f;
                mDragY = 0f;
                commit(right);
                invalidate();
            }
        });
        mAnimator.start();
    }

    private void animateBack() {
        if (mAnimating) return;
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        mAnimating = true;
        final float startX = mDragX;
        final float startY = mDragY;
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(mAnimations ? 150 : 0);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                mDragX = startX * (1f - t);
                mDragY = startY * (1f - t);
                invalidate();
            }
        });
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                mAnimating = false;
                mDragX = 0f;
                mDragY = 0f;
                invalidate();
            }
        });
        mAnimator.start();
    }

    private void commit(boolean saved) {
        if (mDeck.isEmpty()) return;
        Movie m = getCurrentMovie();
        mHistory.add(new HistoryEntry(mIndex, saved));
        mIndex++;
        boolean cycled = false;
        if (mIndex >= mDeck.size()) {
            mIndex = 0;
            mHistory.clear();
            cycled = true;
        }
        if (mListener != null && m != null) {
            mListener.onSwiped(m, saved);
            mListener.onDeckChanged(mIndex, mDeck.size(), canUndo());
        }
        if (cycled) {
            // The listener shows a small toast; the deck simply loops.
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        mAnimating = false;
        mDragging = false;
        mDragX = 0f;
        mDragY = 0f;
    }
}
