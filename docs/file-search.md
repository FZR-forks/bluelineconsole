# Local file search

Blue Line Console includes an optional local file search provider.

## Enable the feature

1. Open **Settings**.
2. Go to **Files**.
3. Enable **Enable local file search**.

## Permissions

- On Android 10 and below, file search uses storage read permission.
- On Android 11 and above, file search requires **All files access**.

Use **Grant access to all files** in the Files settings section to open the system permission page.

This is needed so files in shared folders such as **Downloads** can be found reliably.

## Priority and performance

- File search is **opt-in**.
- File search results are appended after other standard search providers to reduce collisions.
- Search uses a minimum query length and result cap for responsiveness.
