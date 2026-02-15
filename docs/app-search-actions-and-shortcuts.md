# App search actions and app shortcuts

Blue Line Console now supports two Android app-integrated behaviors in the application searcher:

## 1) App search actions

You can search inside supported apps by typing:

- `<app> <query>`

Examples:

- `youtube lo-fi`
- `maps coffee`

When an installed app exposes `ACTION_SEARCH` or `ACTION_WEB_SEARCH`, Blue Line Console now shows a candidate that launches the app-specific search with your query.

## 2) Android app shortcuts (quick actions)

Blue Line Console now indexes launcher shortcuts (dynamic/manifest/pinned and cached where available) from installed apps.

This enables flows like:

- typing a contact name to see a shortcut such as **“Message Alice (WhatsApp)”**
- launching app-provided actions directly from command search

## Notes

- Shortcut availability depends on what each app publishes to the launcher.
- Search-action availability depends on whether each app handles `ACTION_SEARCH` or `ACTION_WEB_SEARCH`.
- On older Android versions, unavailable APIs are skipped safely.
