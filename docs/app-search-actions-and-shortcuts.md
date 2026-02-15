# App search actions and app shortcuts

Blue Line Console supports two Android app-integrated behaviors in the application searcher.

## 1) App search providers (`!provider query`)

You can use apps as search providers similarly to web search engine shortcuts.

- Preferred syntax: `!<provider> <query>`
- Legacy syntax is still supported: `<provider> <query>`

Examples:

- `!wa mom`
- `!yt lo-fi mix`
- `youtube lo-fi mix`

`<provider>` is matched fuzzily against app label/package/aliases, so you do **not** need to type a full app name.

When the matched app supports `ACTION_SEARCH` or `ACTION_WEB_SEARCH`, Blue Line Console shows a candidate that launches in-app search with your query.

If `!provider` matches an app that does not expose in-app search actions, Blue Line Console falls back to opening the app.

## 2) App shortcuts (quick actions)

Blue Line Console indexes launcher shortcuts (dynamic/manifest/pinned and cached where available) from installed apps, and surfaces them in search.

This enables flows like:

- typing a contact name and seeing a shortcut such as **“Message Alice (WhatsApp)”**
- launching app-provided quick actions directly from command search

### Important Android behavior

Shortcut visibility depends on Android launcher permissions:

- `LauncherApps` shortcuts are available only when shortcut host permission is granted (typically for launcher apps / default home role).
- If host permission is unavailable, launcher shortcuts from other apps cannot be listed by Android.

To improve messaging use-cases, Blue Line Console additionally provides a WhatsApp contact action candidate from contacts (`Message <name> on WhatsApp`) when contacts permission and WhatsApp are available.

## Notes

- Shortcut availability depends on what each app publishes.
- Search-action availability depends on whether an app handles `ACTION_SEARCH` or `ACTION_WEB_SEARCH`.
- On older Android versions, unavailable APIs/flags are skipped safely.
