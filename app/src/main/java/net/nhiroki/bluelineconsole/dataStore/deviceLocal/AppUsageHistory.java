package net.nhiroki.bluelineconsole.dataStore.deviceLocal;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import android.content.Context;

public class AppUsageHistory {
    private static final String PREF_KEY_USAGE_PREFIX = "pref_app_usage_last_opened_";

    private AppUsageHistory() {
    }

    public static long getLastOpenedAt(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return 0L;
        }

        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(PREF_KEY_USAGE_PREFIX + packageName, 0L);
    }

    public static void recordOpen(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return;
        }

        final SharedPreferences.Editor preferenceEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferenceEditor.putLong(PREF_KEY_USAGE_PREFIX + packageName, System.currentTimeMillis());
        preferenceEditor.apply();
    }
}
