package net.nhiroki.bluelineconsole.commandSearchers.lib;

import java.util.ArrayList;
import java.util.List;

public class AppSearchProviderMatcher {
    private AppSearchProviderMatcher() {
    }

    public static int matchScore(String selector, String appLabel, String packageName) {
        if (selector == null || selector.isEmpty()) {
            return -1;
        }

        String normalizedSelector = normalize(selector);
        if (normalizedSelector.isEmpty()) {
            return -1;
        }

        int labelIndex = normalize(appLabel).indexOf(normalizedSelector);
        if (labelIndex != -1) {
            return labelIndex;
        }

        int packageIndex = normalize(packageName).indexOf(normalizedSelector);
        if (packageIndex != -1) {
            return 1000 + packageIndex;
        }

        int aliasRank = 0;
        for (String alias : aliases(appLabel, packageName)) {
            int aliasIndex = alias.indexOf(normalizedSelector);
            if (aliasIndex != -1) {
                return 2000 + aliasRank * 100 + aliasIndex;
            }
            aliasRank++;
        }

        return -1;
    }

    private static List<String> aliases(String appLabel, String packageName) {
        List<String> ret = new ArrayList<>();

        String normalizedLabel = normalize(appLabel);
        if (!normalizedLabel.isEmpty()) {
            ret.add(normalizedLabel);
            if (normalizedLabel.length() >= 2) {
                ret.add(normalizedLabel.substring(0, 2));
            }
        }

        if (appLabel != null) {
            String[] words = appLabel.toLowerCase().split("\\s+");
            StringBuilder initials = new StringBuilder();
            for (String word : words) {
                String normalizedWord = normalize(word);
                if (!normalizedWord.isEmpty()) {
                    initials.append(normalizedWord.charAt(0));
                }
            }
            if (initials.length() >= 2) {
                ret.add(initials.toString());
            }
        }

        if (packageName != null) {
            String normalizedPackage = normalize(packageName);
            if (!normalizedPackage.isEmpty()) {
                ret.add(normalizedPackage);
            }

            int lastDot = packageName.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < packageName.length() - 1) {
                String tail = normalize(packageName.substring(lastDot + 1));
                if (!tail.isEmpty()) {
                    ret.add(tail);
                }
            }
        }

        return ret;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
