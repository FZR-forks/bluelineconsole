package net.nhiroki.bluelineconsole.commands.urls;

import androidx.annotation.Nullable;

public class AppSearchProviderUri {
    private static final String PREFIX = "bluelineconsole-app-search://";

    private AppSearchProviderUri() {
    }

    public static String fromPackageName(String packageName) {
        return PREFIX + packageName;
    }

    public static boolean isAppSearchProviderUri(String value) {
        return value != null && value.startsWith(PREFIX) && value.length() > PREFIX.length();
    }

    @Nullable
    public static String getPackageName(String value) {
        if (!isAppSearchProviderUri(value)) {
            return null;
        }

        return value.substring(PREFIX.length());
    }
}
