package com.aurora.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Small setup helper for the Fire TV Home-key service.  Fire OS does not
 * expose a normal default-home picker, so the one-time setup writes the
 * accessibility component into the secure settings.  The same operation is
 * available from Settings > Apps > Home key service once
 * WRITE_SECURE_SETTINGS has been granted with ADB.
 */
public class HomeKeyReceiver extends BroadcastReceiver {

    public static final String ACTION_ENABLE = "com.aurora.home.ENABLE_HOME_KEY";
    public static final String ACTION_DISABLE = "com.aurora.home.DISABLE_HOME_KEY";

    private static final String COMPONENT =
            "com.aurora.home/com.aurora.home.HomeAccessibilityService";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        boolean ok = false;
        if (ACTION_ENABLE.equals(action)) {
            ok = enable(context);
            Toast.makeText(context, ok ? "OBAA Home key enabled" : "ADB permission required",
                    Toast.LENGTH_SHORT).show();
        } else if (ACTION_DISABLE.equals(action)) {
            ok = disable(context);
            Toast.makeText(context, ok ? "OBAA Home key disabled" : "Could not disable",
                    Toast.LENGTH_SHORT).show();
        }
        Log.i("HomeKeyReceiver", "action=" + action + " ok=" + ok);
    }

    public static boolean isEnabled(Context context) {
        try {
            String value = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return value != null && value.contains(COMPONENT);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean enable(Context context) {
        try {
            String current = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String next;
            if (current == null || current.length() == 0) {
                next = COMPONENT;
            } else if (current.contains(COMPONENT)) {
                next = current;
            } else {
                next = current + ":" + COMPONENT;
            }
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next);
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            return true;
        } catch (Throwable t) {
            Log.e("HomeKeyReceiver", "enable failed", t);
            return false;
        }
    }

    public static boolean disable(Context context) {
        try {
            String current = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (current == null || current.length() == 0) return true;
            StringBuilder out = new StringBuilder();
            for (String part : current.split(":")) {
                if (part.length() == 0 || COMPONENT.equals(part)) continue;
                if (out.length() > 0) out.append(':');
                out.append(part);
            }
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, out.toString());
            if (out.length() == 0) {
                Settings.Secure.putString(context.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, "0");
            }
            return true;
        } catch (Throwable t) {
            Log.e("HomeKeyReceiver", "disable failed", t);
            return false;
        }
    }
}
