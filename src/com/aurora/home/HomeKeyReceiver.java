package com.aurora.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Setup/control receiver for the Fire TV Home-key watcher.
 *
 * Fire OS 5.1.1 blocks third-party accessibility providers, so the effective
 * Home route is the protected CLOSE_SYSTEM_DIALOGS broadcast handled by
 * HomeKeyWatcherService.  This receiver provides explicit enable/disable
 * commands for ADB and the Settings > Apps > Default home dialog, and starts
 * the watcher after boot.
 */
public class HomeKeyReceiver extends BroadcastReceiver {

    public static final String ACTION_ENABLE = "com.aurora.home.ENABLE_HOME_KEY";
    public static final String ACTION_DISABLE = "com.aurora.home.DISABLE_HOME_KEY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            HomeKeyWatcherService.ensureStarted(context);
            Log.i("HomeKeyReceiver", "boot: home watcher started");
            return;
        }

        boolean ok = false;
        if (ACTION_ENABLE.equals(action)) {
            HomeKeyWatcherService.setEnabled(context, true);
            ok = true;
            Toast.makeText(context, "OBAA Home key enabled", Toast.LENGTH_SHORT).show();
        } else if (ACTION_DISABLE.equals(action)) {
            HomeKeyWatcherService.setEnabled(context, false);
            ok = true;
            Toast.makeText(context, "OBAA Home key disabled", Toast.LENGTH_SHORT).show();
        }
        Log.i("HomeKeyReceiver", "action=" + action + " ok=" + ok);
    }

    public static boolean isEnabled(Context context) {
        return HomeKeyWatcherService.isEnabled(context);
    }

    public static boolean enable(Context context) {
        HomeKeyWatcherService.setEnabled(context, true);
        return true;
    }

    public static boolean disable(Context context) {
        HomeKeyWatcherService.setEnabled(context, false);
        return true;
    }
}
