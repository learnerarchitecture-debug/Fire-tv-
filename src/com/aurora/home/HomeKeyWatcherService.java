package com.aurora.home;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;

/**
 * Brings OBAA Home back when Fire OS sends the user to the stock launcher.
 *
 * Fire OS 5.1.1 sends a protected CLOSE_SYSTEM_DIALOGS broadcast with
 * reason=home, but the broadcast requires android.permission.DISABLE_KEYGUARD,
 * which this firmware does not define for third-party apps.  The broadcast
 * receiver is kept as a fast path for Fire OS builds that do allow it.
 *
 * The compatible route is a lightweight foreground poll.  Fire OS 5.1.1
 * filters ActivityManager.getRunningAppProcesses() to the caller's own UID,
 * so the service also reads UsageStatsManager (granted once with
 * "adb shell appops set com.aurora.home GET_USAGE_STATS allow").  When the
 * stock launcher is the most recently used app, OBAA Home is started on top.
 * Polling only runs while the screen is on and stops as soon as HomeActivity
 * is resumed, so it is idle whenever OBAA is visible.
 *
 * The stock launcher is never disabled and can be restored by disabling this
 * service.
 */
public class HomeKeyWatcherService extends Service {

    public static final String ACTION_STOP = "com.aurora.home.STOP_HOME_WATCHER";
    private static final String TAG = "HomeKeyWatcherService";
    private static final String PREFS = "aurora";
    private static final String PREF_ENABLED = "home_watcher_enabled";
    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private static final long LAUNCH_DELAY_MS = 450;
    private static final long LAUNCH_RETRY_MS = 1100;
    private static final long LAUNCH_DEBOUNCE_MS = 1200;
    private static final long POLL_INTERVAL_MS = 500;
    private static final long USAGE_WINDOW_MS = 6L * 60L * 60L * 1000L;
    private static final int NOTIFICATION_ID = 42;

    private BroadcastReceiver receiver;
    private long lastTrigger;
    private long lastLaunch;
    private long lastPollLog;
    private boolean polling;
    private boolean launchPending;
    private boolean usageWarningShown;
    private String lastUsageForeground;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable poller = new Runnable() {
        @Override
        public void run() {
            if (!polling) return;
            checkForeground();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_ENABLED, enabled).apply();
        if (enabled) {
            ensureStarted(context);
        } else {
            context.stopService(new Intent(context, HomeKeyWatcherService.class));
        }
    }

    public static void ensureStarted(Context context) {
        if (!isEnabled(context)) return;
        try {
            context.startService(new Intent(context, HomeKeyWatcherService.class));
        } catch (Throwable t) {
            Log.e(TAG, "start failed", t);
        }
    }

    /**
     * True when the one-time ADB app-op grant for usage statistics is present.
     * PACKAGE_USAGE_STATS alone is not enough on Fire OS; the app op must be
     * allowed with:
     *   adb shell appops set com.aurora.home GET_USAGE_STATS allow
     */
    public static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager appOps =
                    (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            Log.w(TAG, "usage access check failed", t);
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());

        // Fast path for Fire OS builds that grant DISABLE_KEYGUARD.
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null
                        || !Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    return;
                }
                String reason = intent.getStringExtra("reason");
                Log.i(TAG, "close system dialogs reason=" + reason);
                if ("home".equals(reason)) scheduleHomeFromBroadcast();
            }
        };
        try {
            registerReceiver(receiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        } catch (Throwable t) {
            Log.w(TAG, "broadcast receiver unavailable", t);
        }

        polling = true;
        handler.post(poller);
        Log.i(TAG, "watching for Home (broadcast + usage stats)");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!isEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        polling = false;
        handler.removeCallbacksAndMessages(null);
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Throwable ignored) {
            }
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleHomeFromBroadcast() {
        final long now = SystemClock.uptimeMillis();
        if (now - lastTrigger < LAUNCH_DEBOUNCE_MS) return;
        lastTrigger = now;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                launchHome("broadcast");
            }
        }, LAUNCH_DELAY_MS);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!HomeActivity.isResumed()) launchHome("broadcast-retry");
            }
        }, LAUNCH_RETRY_MS);
    }

    private void checkForeground() {
        if (HomeActivity.isResumed()) {
            launchPending = false;
            return;
        }

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isScreenOn()) return;

        int launcherImportance = -1;
        int processCount = 0;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            try {
                List<ActivityManager.RunningAppProcessInfo> processes =
                        am.getRunningAppProcesses();
                if (processes != null) {
                    processCount = processes.size();
                    for (int i = 0; i < processes.size(); i++) {
                        ActivityManager.RunningAppProcessInfo process = processes.get(i);
                        if (process != null && AMAZON_LAUNCHER.equals(process.processName)) {
                            launcherImportance = process.importance;
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "process list unavailable", t);
            }
        }

        boolean usageAccess = hasUsageAccess(this);
        String usageForeground = usageAccess ? currentForegroundPackage() : null;

        final long now = SystemClock.uptimeMillis();
        if (now - lastPollLog > 10000) {
            lastPollLog = now;
            Log.i(TAG, "poll: " + processCount + " processes, launcher importance="
                    + launcherImportance + ", usage access=" + usageAccess
                    + ", usage foreground=" + usageForeground);
        }
        if (!usageAccess && !usageWarningShown) {
            usageWarningShown = true;
            Log.w(TAG, "usage access missing; run: adb shell appops set "
                    + getPackageName() + " GET_USAGE_STATS allow");
        }
        if (usageForeground != null && !usageForeground.equals(lastUsageForeground)) {
            lastUsageForeground = usageForeground;
            Log.i(TAG, "usage foreground=" + usageForeground);
        }

        boolean processSaysLauncher =
                launcherImportance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        boolean usageSaysLauncher = AMAZON_LAUNCHER.equals(usageForeground);
        if (!processSaysLauncher && !usageSaysLauncher) return;

        // A launch is already in flight; HomeActivity can take several seconds
        // to become resumed on Fire OS, so stay quiet instead of re-logging.
        if (launchPending && now - lastLaunch < 6000) return;
        if (now - lastTrigger < LAUNCH_DEBOUNCE_MS) return;

        lastTrigger = now;
        Log.i(TAG, "stock launcher foreground (" + (processSaysLauncher ? "process" : "usage")
                + "), importance=" + launcherImportance
                + ", usage=" + usageForeground);
        launchHome(processSaysLauncher ? "poll-process" : "poll-usage");
    }

    /**
     * Most recently used package, from UsageStatsManager.  Returns null when
     * usage access is missing or the query fails.
     */
    private String currentForegroundPackage() {
        try {
            UsageStatsManager usm =
                    (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return null;
            long now = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, now - USAGE_WINDOW_MS, now);
            if (stats == null || stats.isEmpty()) return null;
            UsageStats best = null;
            for (int i = 0; i < stats.size(); i++) {
                UsageStats stat = stats.get(i);
                if (stat == null) continue;
                if (best == null || stat.getLastTimeUsed() > best.getLastTimeUsed()) {
                    best = stat;
                }
            }
            return best == null ? null : best.getPackageName();
        } catch (Throwable t) {
            Log.w(TAG, "usage stats query failed", t);
            return null;
        }
    }

    private void launchHome(String why) {
        final long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < 250) return;
        // HomeActivity can take a few seconds to become resumed on Fire OS.
        // Do not stack more launches while the first one is still in flight.
        if (launchPending && now - lastLaunch < 6000) return;
        lastLaunch = now;
        launchPending = true;
        Intent home = new Intent(HomeKeyWatcherService.this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(home);
            Log.i(TAG, "launching OBAA Home (" + why + ")");
        } catch (Throwable t) {
            Log.e(TAG, "home launch failed", t);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, HomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("OBAA Home")
                .setContentText("Home key ready")
                .setContentIntent(pending)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN);
        return builder.build();
    }
}
