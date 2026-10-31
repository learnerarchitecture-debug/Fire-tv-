package com.aurora.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridView;

/**
 * GridView with deterministic D-pad row navigation.
 *
 * On Fire TV the focused child consumes the key event, so the platform
 * FocusFinder never sees off-screen rows and the grid appears stuck at the
 * last visible row.  This subclass moves focus itself and calls setSelection
 * first, which makes AdapterView scroll the target row into view.
 */
public class TvGridView extends GridView {

    public interface EdgeListener {
        /** Focus is on the last row and DOWN was pressed. */
        void onDownEdge();
        /** Focus is on the first row and UP was pressed. */
        void onUpEdge();
    }

    private EdgeListener mEdgeListener;

    public TvGridView(Context c) {
        super(c);
    }

    public TvGridView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setEdgeListener(EdgeListener listener) {
        mEdgeListener = listener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int key = event.getKeyCode();
            if (key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_UP) {
                View focused = getFocusedChild();
                if (focused != null) {
                    int position = getPositionForView(focused);
                    if (position != INVALID_POSITION) {
                        int columns = Math.max(1, getNumColumns());
                        int count = getCount();
                        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                            int target = position + columns;
                            if (target >= count) {
                                int lastRowStart = ((count - 1) / columns) * columns;
                                // A partial last row still has an item below
                                // this column: jump to its nearest item.
                                target = position < lastRowStart ? count - 1 : INVALID_POSITION;
                            }
                            if (target != INVALID_POSITION) {
                                focusPosition(target);
                                return true;
                            }
                            if (mEdgeListener != null) {
                                mEdgeListener.onDownEdge();
                                return true;
                            }
                        } else {
                            int target = position - columns;
                            if (target >= 0) {
                                focusPosition(target);
                                return true;
                            }
                            if (mEdgeListener != null) {
                                mEdgeListener.onUpEdge();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** Scrolls to a position and focuses its child once layout has run. */
    public void focusPosition(final int position) {
        if (position < 0 || position >= getCount()) return;
        setSelection(position);
        post(new Runnable() {
            @Override
            public void run() {
                if (position >= getCount()) return;
                View child = getChildAt(position - getFirstVisiblePosition());
                if (child != null) {
                    child.requestFocus();
                } else {
                    setSelection(position);
                    post(this);
                }
            }
        });
    }

    public void focusFirst() {
        focusPosition(0);
    }

    public void focusLast() {
        focusPosition(Math.max(0, getCount() - 1));
    }
}
