package net.nhiroki.bluelineconsole.dataStore.deviceLocal;

import android.content.Context;
import android.content.SharedPreferences;

public class AppUsageHistory {
    private static final String PREF_FILE_NAME = "app_usage_history";
    private static final String PREF_KEY_USAGE_PREFIX = "pref_app_usage_last_opened_";

    private AppUsageHistory() {
    }

    public static long getLastOpenedAt(Context context, String packageName) {
        return getLastOpenedAt(context, packageName, null);
    }

    public static long getLastOpenedAt(Context context, String packageName, String usageTarget) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return 0L;
        }

        final SharedPreferences preferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);

        if (usageTarget == null || usageTarget.isEmpty()) {
            return preferences.getLong(PREF_KEY_USAGE_PREFIX + packageName, 0L);
        }

        final long targetLastOpened = preferences.getLong(PREF_KEY_USAGE_PREFIX + packageName + "_" + usageTarget, 0L);
        if (targetLastOpened != 0L) {
            return targetLastOpened;
        }

        return preferences.getLong(PREF_KEY_USAGE_PREFIX + packageName, 0L);
    }

    public static void recordOpen(Context context, String packageName) {
        recordOpen(context, packageName, null);
    }

    public static void recordOpen(Context context, String packageName, String usageTarget) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return;
        }

        final long openedAt = System.currentTimeMillis();
        final SharedPreferences.Editor preferenceEditor = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE).edit();
        preferenceEditor.putLong(PREF_KEY_USAGE_PREFIX + packageName, openedAt);

        if (usageTarget != null && !usageTarget.isEmpty()) {
            preferenceEditor.putLong(PREF_KEY_USAGE_PREFIX + packageName + "_" + usageTarget, openedAt);
        }

        preferenceEditor.apply();
    }
}
