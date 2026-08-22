package net.nhiroki.bluelineconsole.applicationMain;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class SuperKeyAccessibilityPermissionHelper {
    public static final String PREFERENCE_KEY_OPEN_ACCESSIBILITY_SETTINGS = "pref_super_key_accessibility_permission";

    private SuperKeyAccessibilityPermissionHelper() {
    }

    public static boolean isGranted(Context context) {
        final String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null || enabledServices.isEmpty()) {
            return false;
        }

        final String expectedServiceId = new ComponentName(context, SuperKeyAccessibilityService.class).flattenToString();
        final String[] serviceIds = enabledServices.split(":");

        for (String serviceId : serviceIds) {
            if (expectedServiceId.equals(serviceId)) {
                return true;
            }
        }

        return false;
    }

    public static void requestPermission(Context context) {
        final Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

