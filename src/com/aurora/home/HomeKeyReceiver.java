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

        if (isStartTrigger(action)) {
            HomeKeyWatcherService.ensureStarted(context);
            Log.i("HomeKeyReceiver", "start trigger (" + action + "): home watcher started");
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

    /**
     * Boot-time broadcasts that Fire OS may deliver before BOOT_COMPLETED.
     * Starting the watcher on any of them removes most of the post-reboot
     * wait.  Unknown/hidden actions are compared as strings because they are
     * not public constants on API 22.
     */
    private static boolean isStartTrigger(String action) {
        if (action == null) return false;
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_MEDIA_MOUNTED.equals(action)
                || Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)
                || Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)
                || Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)
                || "android.net.conn.CONNECTIVITY_CHANGE".equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)
                || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)
                || "android.intent.action.PRE_BOOT_COMPLETED".equals(action)
                || "android.intent.action.USER_STARTED".equals(action)
                || "android.intent.action.USER_INITIALIZE".equals(action)
                || "com.amazon.shouldlaunchhome".equals(action)
                || "com.amazon.tv.networkmonitor.INTERNET_UP".equals(action)
                || "com.amazon.tv.networkmonitor.POST_BOOT_COMPLETED".equals(action)
                || "android.intent.action.DEVICE_STORAGE_OK".equals(action)
                || "android.intent.action.DEVICE_STORAGE_LOW".equals(action)
                || "android.intent.action.USER_SWITCHED".equals(action)
                || "android.intent.action.LOCALE_CHANGED".equals(action)
                || "android.intent.action.CONFIGURATION_CHANGED".equals(action)
                || "android.accounts.LOGIN_ACCOUNTS_CHANGED".equals(action)
                || "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)
                || "com.amazon.dcp.sso.action.account.added".equals(action)
                || "com.amazon.dcp.sso.action.AmazonAccountPropertyService.property.changed".equals(action)
                || "amazon.intent.action.USER_ACTIVE".equals(action)
                || "amazon.intent.action.LAUNCHING_HOME_ON_SLEEP".equals(action)
                || "com.amazon.tv.networkmonitor.CONNECTIVITY_CHECK".equals(action)
                || "amazon.intent.action.HOME_PRESSED".equals(action)
                || "amazon.intent.action.HOME_DOUBLE_PRESSED".equals(action);
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
