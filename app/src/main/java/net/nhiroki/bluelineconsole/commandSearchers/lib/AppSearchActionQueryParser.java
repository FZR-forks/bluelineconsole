package net.nhiroki.bluelineconsole.commandSearchers.lib;

import androidx.annotation.Nullable;

public class AppSearchActionQueryParser {
    private AppSearchActionQueryParser() {
    }

    @Nullable
    public static ParsedQuery parse(String query) {
        if (query == null) {
            return null;
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (!trimmed.startsWith("!")) {
            return null;
        }

        trimmed = trimmed.substring(1).trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        int separator = trimmed.indexOf(' ');
        if (separator <= 0 || separator >= trimmed.length() - 1) {
            return null;
        }

        String appSelector = trimmed.substring(0, separator).trim();
        String searchText = trimmed.substring(separator + 1).trim();

        if (appSelector.isEmpty() || searchText.isEmpty()) {
            return null;
        }

        return new ParsedQuery(appSelector, searchText);
    }

    public static class ParsedQuery {
        public final String appSelector;
        public final String searchText;
        public ParsedQuery(String appSelector, String searchText) {
            this.appSelector = appSelector;
            this.searchText = searchText;
        }
    }
}
