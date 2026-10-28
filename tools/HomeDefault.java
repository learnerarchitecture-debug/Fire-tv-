package com.aurora.tools;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import java.lang.reflect.Method;

/**
 * On-device helper for Fire OS 5 (API 22), which has no `pm
 * set-home-activity` command. Runs via app_process as the shell UID,
 * which is granted SET_PREFERRED_APPLICATIONS on this device.
 *
 * Usage:
 *   app_process ... com.aurora.tools.HomeDefault clear <package>
 *   app_process ... com.aurora.tools.HomeDefault add
 */
public final class HomeDefault {

    private static final String HOME_PKG = "com.aurora.home";
    private static final String HOME_CLS = "com.aurora.home.HomeActivity";

    public static void main(String[] args) throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) serviceManager
                .getMethod("getService", String.class)
                .invoke(null, "package");
        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        Object pm = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);

        String action = args.length > 0 ? args[0] : "add";

        if ("clear".equals(action)) {
            String pkg = args.length > 1 ? args[1] : "com.amazon.tv.launcher";
            Method clear = pm.getClass()
                    .getMethod("clearPackagePreferredActivities", String.class);
            clear.invoke(pm, pkg);
            System.out.println("Cleared preferred activities for " + pkg);
            try {
                Method clearPersistent = pm.getClass()
                        .getMethod("clearPackagePersistentPreferredActivities",
                                String.class, int.class);
                clearPersistent.invoke(pm, pkg, 0);
                System.out.println("Cleared persistent preferred activities for " + pkg);
            } catch (Throwable t) {
                System.out.println("Persistent preferred clear skipped: " + t);
            }
            return;
        }

        if ("add".equals(action)) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            filter.addCategory(Intent.CATEGORY_DEFAULT);

            ComponentName home = new ComponentName(HOME_PKG, HOME_CLS);
            ComponentName stock = new ComponentName(
                    "com.amazon.tv.launcher",
                    "com.amazon.tv.launcher.ui.HomeActivity_vNext");
            ComponentName[] set = new ComponentName[]{home, stock};
            int match = IntentFilter.MATCH_CATEGORY_EMPTY;

            for (Method method : pm.getClass().getMethods()) {
                if (!"addPreferredActivity".equals(method.getName())) continue;
                Class<?>[] p = method.getParameterTypes();
                try {
                    if (p.length == 5
                            && p[0] == IntentFilter.class
                            && p[1] == int.class
                            && p[2] == ComponentName[].class
                            && p[3] == ComponentName.class
                            && p[4] == int.class) {
                        method.invoke(pm, filter, match, set, home, 0);
                        System.out.println("Added preferred HOME activity (5-arg): " + home);
                        return;
                    }
                    if (p.length == 4
                            && p[0] == IntentFilter.class
                            && p[1] == int.class
                            && p[2] == ComponentName[].class
                            && p[3] == ComponentName.class) {
                        method.invoke(pm, filter, match, set, home);
                        System.out.println("Added preferred HOME activity (4-arg): " + home);
                        return;
                    }
                } catch (Throwable t) {
                    System.out.println("addPreferredActivity " + method + " failed: " + t);
                }
            }
            System.out.println("No usable addPreferredActivity method found");
            return;
        }

        System.out.println("Usage: HomeDefault clear [package] | add");
    }
}
