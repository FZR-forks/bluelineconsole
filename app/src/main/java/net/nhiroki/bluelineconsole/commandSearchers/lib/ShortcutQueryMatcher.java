package net.nhiroki.bluelineconsole.commandSearchers.lib;

public class ShortcutQueryMatcher {
    private ShortcutQueryMatcher() {}

    public static int matchScore(String query, String shortLabel, String longLabel, String appLabel, String packageName) {
        if (query == null || query.isEmpty()) {
            return -1;
        }

        final String queryLower = query.toLowerCase();

        int shortLabelScore = containsScore(shortLabel, queryLower);
        if (shortLabelScore != -1) {
            return shortLabelScore;
        }

        int longLabelScore = containsScore(longLabel, queryLower);
        if (longLabelScore != -1) {
            return 1000 + longLabelScore;
        }

        int appLabelScore = containsScore(appLabel, queryLower);
        if (appLabelScore != -1) {
            return 2000 + appLabelScore;
        }

        int packageScore = containsScore(packageName, queryLower);
        if (packageScore != -1) {
            return 3000 + packageScore;
        }

        return -1;
    }

    private static int containsScore(String target, String queryLower) {
        if (target == null) {
            return -1;
        }

        return target.toLowerCase().indexOf(queryLower);
    }
}
