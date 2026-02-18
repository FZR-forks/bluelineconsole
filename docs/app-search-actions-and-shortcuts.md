# App search providers and app shortcuts

Blue Line Console supports Android app-integrated behavior in two ways.

## 1) App search providers via existing search-engine enrollment

App search is now integrated into the **existing custom search engine workflow**.

Configure from preferences the same way you configure custom URL/search providers, then choose an app instead of entering only a URL:

1. Open custom URL/search-engine settings.
2. Add or edit a provider.
3. Use **Select app as search provider**.
4. Save as a search engine (`has_query` enabled).

After enrollment, usage is identical to other search engines:

- `<command> <query>`

Example:

- `wa mom`

This launches `ACTION_SEARCH` / `ACTION_WEB_SEARCH` in the selected app package.

## 2) App actions and shortcuts in normal search

App actions remain separate from app-search providers and appear through normal/classic search without prefixes.

- Launcher shortcuts from installed apps are shown when Android shortcut host permission is available.
- WhatsApp contact action fallback (`Message <name> on WhatsApp`) is also shown when contacts permission and WhatsApp are available.

### Ordering behavior

When multiple app or app-shortcut candidates have the same text match score, Blue Line Console now prioritizes candidates by recent usage:

1. Better text match score first.
2. More recently opened app package first.
3. Alphabetical order as deterministic tiebreaker.

Opening an app, app shortcut, or WhatsApp contact action updates the usage history for that package.

## 3) Keyboard shortcuts and permissions

For physical keyboard users, Blue Line Console supports these native-style shortcuts:

- `Super` / `Windows` key (pressed alone) to open the overview (when accessibility permission is granted).
- `Escape` to close the app window.
- `Ctrl+W`, `Ctrl+Q`, and `Alt+F4` to close the app window.

The settings page includes an explicit **Super key accessibility permission** item that opens Android Accessibility settings and shows whether the permission is granted.

## Notes

- App search providers are stored in the same custom provider storage as URLs/search engines.
- Shortcut visibility still depends on Android launcher shortcut host permission.
