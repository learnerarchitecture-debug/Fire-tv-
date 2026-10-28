package com.aurora.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS 5 pins the Home button to the stock launcher.  This service
 * filters the Home key before the key policy sees it and brings OBAA Home
 * to the front instead.  Holding Menu while pressing Home is left alone so
 * the original Fire TV long-press Home menu remains reachable.
 */
public class HomeAccessibilityService extends AccessibilityService {

    private boolean menuPressed;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
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
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.i("HomeAccessibilityService", "home key intercepted");
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                return true;

            default:
                return false;
        }
    }
}
