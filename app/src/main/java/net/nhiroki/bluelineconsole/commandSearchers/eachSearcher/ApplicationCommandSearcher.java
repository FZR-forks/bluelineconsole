package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.commandSearchers.lib.ShortcutQueryMatcher;
import net.nhiroki.bluelineconsole.commandSearchers.lib.StringMatchStrategy;
import net.nhiroki.bluelineconsole.commands.applications.ApplicationDatabase;
import net.nhiroki.bluelineconsole.dataStore.cache.ApplicationInformation;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;
import net.nhiroki.bluelineconsole.wrapperForAndroid.ContactsReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationCommandSearcher implements CommandSearcher {
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";

    private ApplicationDatabase applicationDatabase;
    private List<ShortcutInfoWithAppLabel> shortcutInfoList = new ArrayList<>();
    private List<ContactsReader.Contact> cachedContacts = new ArrayList<>();
    private boolean contactsLoaded = false;

    @Override
    public void refresh(Context context) {
        this.applicationDatabase = new ApplicationDatabase(context);
        this.shortcutInfoList = new ArrayList<>();
        this.cachedContacts = new ArrayList<>();
        this.contactsLoaded = false;
    }

    @Override
    public void close() {
        this.applicationDatabase.close();
    }

    @Override
    public boolean isPrepared() {
        return this.applicationDatabase.isPrepared();
    }

    @Override
    public void waitUntilPrepared() {
        this.applicationDatabase.waitUntilPrepared();
    }

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        final boolean matchAllApplications = query.equalsIgnoreCase("all_apps");

        List<Pair<Integer, CandidateEntry>> appCandidates = new ArrayList<>();
        for (ApplicationInformation applicationInformation : applicationDatabase.getApplicationInformationList()) {
            final String appLabel = applicationInformation.getLabel();
            final ApplicationInfo androidApplicationInfo = applicationDatabase.getAndroidApplicationInfo(applicationInformation.getPackageName());

            if (matchAllApplications) {
                appCandidates.add(new Pair<>(0, new AppOpenCandidateEntry(context, applicationInformation, androidApplicationInfo, appLabel)));
                continue;
            }

            int appLabelMatchResult = StringMatchStrategy.match(context, query, appLabel, false);
            if (appLabelMatchResult != -1) {
                appCandidates.add(new Pair<>(appLabelMatchResult, new AppOpenCandidateEntry(context, applicationInformation, androidApplicationInfo, appLabel)));
                continue;
            }

            int packageNameMatchResult = StringMatchStrategy.match(context, query, applicationInformation.getPackageName(), false);
            if (packageNameMatchResult != -1) {
                appCandidates.add(new Pair<>(100000 + packageNameMatchResult, new AppOpenCandidateEntry(context, applicationInformation, androidApplicationInfo, appLabel)));
                continue;
            }
        }

        appCandidates.addAll(findWhatsAppContactActionCandidates(context, query));

        if (Build.VERSION.SDK_INT >= 25) {
            if (shortcutInfoList.isEmpty()) {
                shortcutInfoList = getAvailableShortcuts(context);
            }

            for (ShortcutInfoWithAppLabel shortcutInfoWithAppLabel : shortcutInfoList) {
                int shortcutMatchResult = ShortcutQueryMatcher.matchScore(
                        query,
                        shortcutInfoWithAppLabel.shortLabel,
                        shortcutInfoWithAppLabel.longLabel,
                        shortcutInfoWithAppLabel.appLabel,
                        shortcutInfoWithAppLabel.shortcutInfo.getPackage()
                );

                if (shortcutMatchResult == -1) {
                    continue;
                }

                appCandidates.add(new Pair<>(50000 + shortcutMatchResult, new AppShortcutCandidateEntry(context, shortcutInfoWithAppLabel)));
            }
        }

        Collections.sort(appCandidates, (o1, o2) -> o1.first.compareTo(o2.first));

        for (Pair<Integer, CandidateEntry> entry : appCandidates) {
            candidates.add(entry.second);
        }

        return candidates;
    }

    private List<Pair<Integer, CandidateEntry>> findWhatsAppContactActionCandidates(Context context, String query) {
        List<Pair<Integer, CandidateEntry>> ret = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return ret;
        }

        if (!isPackageInstalled(context, WHATSAPP_PACKAGE)) {
            return ret;
        }

        ensureContactsLoaded(context);
        for (ContactsReader.Contact contact : cachedContacts) {
            int match = StringMatchStrategy.match(context, query, contact.displayName, false);
            if (match == -1) {
                continue;
            }

            String phoneNumber = firstPhoneNumber(contact);
            if (phoneNumber == null) {
                continue;
            }

            String normalized = normalizePhone(phoneNumber);
            if (normalized.isEmpty()) {
                continue;
            }

            ret.add(new Pair<>(55000 + match, new WhatsAppContactCandidateEntry(contact.displayName, normalized)));
        }

        return ret;
    }

    private void ensureContactsLoaded(Context context) {
        if (contactsLoaded) {
            return;
        }

        contactsLoaded = true;

        if (!ContactsReader.appHasReadContactsPermission(context)) {
            return;
        }

        try {
            cachedContacts = ContactsReader.fetchAllContacts(context);
        } catch (ContactsReader.ContactReadPermissionDenied ignored) {
            cachedContacts = new ArrayList<>();
        }
    }

    private String firstPhoneNumber(ContactsReader.Contact contact) {
        if (contact == null || contact.phoneNumbers == null || contact.phoneNumbers.isEmpty()) {
            return null;
        }
        return contact.phoneNumbers.get(0);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        return phoneNumber.replaceAll("[^0-9+]", "");
    }

    private boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private List<ShortcutInfoWithAppLabel> getAvailableShortcuts(Context context) {
        if (Build.VERSION.SDK_INT < 25) {
            return new ArrayList<>();
        }

        LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
        if (launcherApps == null || !launcherApps.hasShortcutHostPermission()) {
            return new ArrayList<>();
        }

        final LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
        int queryFlags = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;
        if (Build.VERSION.SDK_INT >= 30) {
            queryFlags |= LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED;
        }
        shortcutQuery.setQueryFlags(queryFlags);

        final List<ShortcutInfo> shortcuts;
        try {
            shortcuts = launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle());
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }

        if (shortcuts == null) {
            return new ArrayList<>();
        }

        Map<String, String> appLabelMap = new HashMap<>();
        for (ApplicationInformation appInfo : applicationDatabase.getApplicationInformationList()) {
            appLabelMap.put(appInfo.getPackageName(), appInfo.getLabel());
        }

        List<ShortcutInfoWithAppLabel> ret = new ArrayList<>();
        for (ShortcutInfo shortcutInfo : shortcuts) {
            if (!shortcutInfo.isEnabled()) {
                continue;
            }

            String shortLabel = shortcutInfo.getShortLabel() == null ? "" : shortcutInfo.getShortLabel().toString();
            String longLabel = shortcutInfo.getLongLabel() == null ? "" : shortcutInfo.getLongLabel().toString();

            if (shortLabel.isEmpty() && longLabel.isEmpty()) {
                continue;
            }

            String appLabel = appLabelMap.containsKey(shortcutInfo.getPackage())
                    ? appLabelMap.get(shortcutInfo.getPackage())
                    : shortcutInfo.getPackage();
            ret.add(new ShortcutInfoWithAppLabel(shortcutInfo, shortLabel, longLabel, appLabel));
        }

        return ret;
    }

    private static class ShortcutInfoWithAppLabel {
        private final ShortcutInfo shortcutInfo;
        private final String shortLabel;
        private final String longLabel;
        private final String appLabel;

        ShortcutInfoWithAppLabel(ShortcutInfo shortcutInfo, String shortLabel, String longLabel, String appLabel) {
            this.shortcutInfo = shortcutInfo;
            this.shortLabel = shortLabel;
            this.longLabel = longLabel;
            this.appLabel = appLabel;
        }
    }

    private static class AppOpenCandidateEntry implements CandidateEntry {
        private final ApplicationInformation applicationInformation;
        private final ApplicationInfo androidApplicationInfo;
        private final String title;
        private final boolean displayPackageName;

        AppOpenCandidateEntry(Context context, ApplicationInformation applicationInformation, ApplicationInfo androidApplicationInfo, String appTitle) {
            this.applicationInformation = applicationInformation;
            this.androidApplicationInfo = androidApplicationInfo;
            this.title = appTitle;
            this.displayPackageName = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_apps_show_package_name", false);
        }

        @Override
        @NonNull
        public String getTitle() {
            return title;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            if (!displayPackageName) {
                return null;
            }

            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(applicationInformation.getPackageName());
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return packageNameView;
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public EventLauncher getEventLauncher(final Context context) {
            return activity -> {
                String packageName = applicationInformation.getPackageName();
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
                if (packageName.equals(context.getPackageName())) {
                    activity.finishIfNotHome();
                    activity.startActivity(new Intent(activity, MainActivity.class));
                    return;
                }
                if (intent == null) {
                    Toast.makeText(activity, String.format(activity.getString(R.string.error_failure_not_found_opening_application_with_class), packageName), Toast.LENGTH_LONG).show();
                    return;
                }
                activity.startActivity(intent);
                activity.finishIfNotHome();
            };
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return context.getPackageManager().getApplicationIcon(androidApplicationInfo);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class WhatsAppContactCandidateEntry implements CandidateEntry {
        private final String contactName;
        private final String normalizedPhone;

        WhatsAppContactCandidateEntry(String contactName, String normalizedPhone) {
            this.contactName = contactName;
            this.normalizedPhone = normalizedPhone;
        }

        @Override
        @NonNull
        public String getTitle() {
            return String.format("Message %s on WhatsApp", contactName);
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + Uri.encode(normalizedPhone)));
                intent.setPackage(WHATSAPP_PACKAGE);
                try {
                    activity.startActivity(intent);
                    activity.finishIfNotHome();
                } catch (RuntimeException e) {
                    Toast.makeText(activity, String.format(activity.getString(R.string.error_failure_not_found_opening_application_with_class), WHATSAPP_PACKAGE), Toast.LENGTH_LONG).show();
                }
            };
        }

        @Override
        public Drawable getIcon(Context context) {
            try {
                return context.getPackageManager().getApplicationIcon(WHATSAPP_PACKAGE);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public boolean isSubItem() {
            return true;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class AppShortcutCandidateEntry implements CandidateEntry {
        private final ShortcutInfoWithAppLabel shortcutInfoWithAppLabel;
        private final boolean displayPackageName;

        AppShortcutCandidateEntry(Context context, ShortcutInfoWithAppLabel shortcutInfoWithAppLabel) {
            this.shortcutInfoWithAppLabel = shortcutInfoWithAppLabel;
            this.displayPackageName = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_apps_show_package_name", false);
        }

        @Override
        @NonNull
        public String getTitle() {
            if (shortcutInfoWithAppLabel.shortLabel.isEmpty()) {
                return shortcutInfoWithAppLabel.longLabel + " (" + shortcutInfoWithAppLabel.appLabel + ")";
            }
            return shortcutInfoWithAppLabel.shortLabel + " (" + shortcutInfoWithAppLabel.appLabel + ")";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            if (!displayPackageName) {
                return null;
            }

            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(shortcutInfoWithAppLabel.shortcutInfo.getPackage());
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return packageNameView;
        }

        @Override
        public boolean hasEvent() {
            return Build.VERSION.SDK_INT >= 25;
        }

        @Override
        public EventLauncher getEventLauncher(final Context context) {
            if (Build.VERSION.SDK_INT < 25) {
                return null;
            }

            return activity -> {
                LauncherApps launcherApps = activity.getSystemService(LauncherApps.class);
                if (launcherApps == null) {
                    return;
                }

                try {
                    launcherApps.startShortcut(
                            shortcutInfoWithAppLabel.shortcutInfo.getPackage(),
                            shortcutInfoWithAppLabel.shortcutInfo.getId(),
                            new Rect(),
                            null,
                            shortcutInfoWithAppLabel.shortcutInfo.getUserHandle()
                    );
                    activity.finishIfNotHome();
                } catch (RuntimeException e) {
                    Toast.makeText(activity, String.format(activity.getString(R.string.error_failure_not_found_opening_application_with_class), shortcutInfoWithAppLabel.shortcutInfo.getPackage()), Toast.LENGTH_LONG).show();
                }
            };
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            if (Build.VERSION.SDK_INT < 25) {
                return null;
            }

            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            if (launcherApps == null) {
                return null;
            }
            return launcherApps.getShortcutIconDrawable(shortcutInfoWithAppLabel.shortcutInfo, context.getResources().getDisplayMetrics().densityDpi);
        }

        @Override
        public boolean isSubItem() {
            return true;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }
}
