package com.aurora.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS 5 pins the Home button to the stock launcher.  This service uses
 * both routes available to an accessibility service:
 *
 *  - it consumes the remote's Home key when Fire OS passes key events to the
 *    service, and
 *  - it watches for the stock launcher coming to the front (Home, boot, or an
 *    app exiting) and immediately brings OBAA Home back on top.
 *
 * Holding Menu while pressing Home is left alone so the original Fire TV
 * long-press Home menu remains reachable.
 */
public class HomeAccessibilityService extends AccessibilityService {

    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private static final long LAUNCH_DEBOUNCE_MS = 800;

    private boolean menuPressed;
    private long lastLaunch;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && AMAZON_LAUNCHER.contentEquals(event.getPackageName())) {
            Log.i("HomeAccessibilityService", "stock launcher window detected");
            openHome();
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i("HomeAccessibilityService", "connected");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        final int action = event.getAction();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MENU:
                menuPressed = action == KeyEvent.ACTION_DOWN;
                return false;

            case KeyEvent.KEYCODE_HOME:
                if (menuPressed) {
                    return false;
                }
                if (action == KeyEvent.ACTION_UP) {
                    Log.i("HomeAccessibilityService", "home key intercepted");
                    openHome();
                }
                return true;

            default:
                return false;
        }
    }

    private void openHome() {
        final long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < LAUNCH_DEBOUNCE_MS) return;
        lastLaunch = now;

        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
