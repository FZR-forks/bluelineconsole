# Local file search

Blue Line Console includes an optional local file search provider.

## Enable the feature

1. Open **Settings**.
2. Go to **Files**.
3. Enable **Enable local file search**.

On Android 12 and below, enabling file search requests storage read permission.

## Grant access to common folders

On newer Android versions, not every local file appears in `MediaStore`.
To improve coverage (for example files in **Downloads**), the Files settings category has actions to grant persistent folder access:

- **Grant access to Downloads folder**
- **Grant access to Documents folder**
- **Grant access to Pictures folder**

After granting, Blue Line Console can search file names in those folders using Android's Storage Access Framework.

## Priority and performance

- File search is **opt-in**.
- File search results are appended after other standard search providers to reduce collisions.
- Search uses a minimum query length and result cap for responsiveness.
