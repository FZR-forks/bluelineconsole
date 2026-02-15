package net.nhiroki.bluelineconsole.applicationMain;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class OverlayPermissionHelper {
    public static final String PREFERENCE_KEY_OPEN_OVERLAY_PERMISSION_SETTINGS = "pref_overlay_permission";

    private OverlayPermissionHelper() { }

    public static boolean canDrawOverOtherApps(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static void requestDrawOverOtherAppsPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || canDrawOverOtherApps(context)) {
            return;
        }

        final String packageName = context.getPackageName();
        Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName));
        settingsIntent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (settingsIntent.resolveActivity(context.getPackageManager()) == null) {
            settingsIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            settingsIntent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if (settingsIntent.resolveActivity(context.getPackageManager()) == null) {
            settingsIntent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + packageName)
            );
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(settingsIntent);
    }
}
