package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.commandSearchers.lib.StringMatchStrategy;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileSystemSearchCommandSearcher implements CommandSearcher {
    public static final String PREF_FILE_SEARCH_ENABLED_KEY = "pref_file_search_enabled";
    public static final String PREF_FILE_SEARCH_TREE_URIS_KEY = "pref_file_search_tree_uris";

    public static final String PREF_FILE_SEARCH_GRANT_DOWNLOADS_KEY = "pref_file_search_grant_downloads";
    public static final String PREF_FILE_SEARCH_GRANT_DOCUMENTS_KEY = "pref_file_search_grant_documents";
    public static final String PREF_FILE_SEARCH_GRANT_PICTURES_KEY = "pref_file_search_grant_pictures";

    public static final String[] COMMON_FOLDER_IDS = new String[]{"Download", "Documents", "Pictures"};

    private static final int MAX_CANDIDATES_PER_QUERY = 40;
    private static final int MAX_TREE_FILES_TO_SCAN = 1500;

    @Override
    public void refresh(Context context) {
        // No cache to refresh. Query lazily in searchCandidateEntries().
    }

    @Override
    public void close() {
        // No persistent resources.
    }

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    public void waitUntilPrepared() {
        // No-op.
    }

    @NonNull
    @Override
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (!pref.getBoolean(PREF_FILE_SEARCH_ENABLED_KEY, false)) {
            return new ArrayList<>();
        }

        final boolean mediaStorePermissionGranted = hasRequiredPermissionForMediaStore(context);
        if (Build.VERSION.SDK_INT <= 32 && !mediaStorePermissionGranted) {
            pref.edit().putBoolean(PREF_FILE_SEARCH_ENABLED_KEY, false).apply();
            return new ArrayList<>();
        }

        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return new ArrayList<>();
        }

        List<ScoredFileResult> scoredResults = new ArrayList<>();
        Set<String> uriSet = new HashSet<>();

        if (mediaStorePermissionGranted) {
            searchUsingMediaStore(context, normalizedQuery, scoredResults, uriSet);
        }
        searchUsingGrantedFolders(context, normalizedQuery, scoredResults, uriSet);

        Collections.sort(scoredResults, (left, right) -> Integer.compare(left.score, right.score));

        List<CandidateEntry> ret = new ArrayList<>();
        int limit = Math.min(MAX_CANDIDATES_PER_QUERY, scoredResults.size());
        for (int i = 0; i < limit; ++i) {
            ret.add(scoredResults.get(i).entry);
        }

        return ret;
    }

    public static Intent createFolderPickerIntent(String commonFolderId) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        if (Build.VERSION.SDK_INT >= 26) {
            Uri initial = DocumentsContract.buildRootUri("com.android.externalstorage.documents", "primary");
            if (commonFolderId != null && !commonFolderId.isEmpty()) {
                initial = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:" + commonFolderId);
            }
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);
        }
        return intent;
    }

    public static void saveTreeUri(Context context, Uri uri) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> current = pref.getStringSet(PREF_FILE_SEARCH_TREE_URIS_KEY, new HashSet<>());
        Set<String> updated = new HashSet<>(current);
        updated.add(uri.toString());
        pref.edit().putStringSet(PREF_FILE_SEARCH_TREE_URIS_KEY, updated).apply();
    }

    private boolean hasRequiredPermissionForMediaStore(Context context) {
        if (Build.VERSION.SDK_INT <= 32) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        // MediaStore Files query is intentionally disabled on Android 13+ for now.
        // SAF tree-based search still works with user-granted folder access.
        return false;
    }

    private void searchUsingMediaStore(Context context, String query, List<ScoredFileResult> scoredResults, Set<String> uriSet) {
        final Uri collectionUri = MediaStore.Files.getContentUri("external");

        List<String> projectionList = new ArrayList<>();
        projectionList.add(MediaStore.Files.FileColumns._ID);
        projectionList.add(MediaStore.Files.FileColumns.DISPLAY_NAME);
        projectionList.add(MediaStore.Files.FileColumns.MIME_TYPE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Files.FileColumns.RELATIVE_PATH);
        }
        final String[] projection = projectionList.toArray(new String[0]);

        final String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?";
        final String[] selectionArgs = new String[]{"%" + query + "%"};
        final String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

        ContentResolver contentResolver = context.getContentResolver();

        try (Cursor cursor = contentResolver.query(collectionUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor == null) {
                return;
            }

            int idIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
            int nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
            int relativePathIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH);

            while (cursor.moveToNext()) {
                if (idIndex < 0 || nameIndex < 0) {
                    continue;
                }

                long id = cursor.getLong(idIndex);
                String fileName = cursor.getString(nameIndex);
                if (fileName == null || fileName.trim().isEmpty()) {
                    continue;
                }

                int score = StringMatchStrategy.match(context, query, fileName, false);
                if (score < 0) {
                    continue;
                }

                Uri fileUri = Uri.withAppendedPath(collectionUri, String.valueOf(id));
                String uriString = fileUri.toString();
                if (uriSet.contains(uriString)) {
                    continue;
                }

                String mimeType = mimeTypeIndex < 0 ? null : cursor.getString(mimeTypeIndex);
                String relativePath = relativePathIndex < 0 ? null : cursor.getString(relativePathIndex);

                scoredResults.add(new ScoredFileResult(score, new FileCandidateEntry(fileName, relativePath, mimeType, fileUri)));
                uriSet.add(uriString);
            }

        } catch (RuntimeException ignored) {
        }
    }

    private void searchUsingGrantedFolders(Context context, String query, List<ScoredFileResult> scoredResults, Set<String> uriSet) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> treeUris = pref.getStringSet(PREF_FILE_SEARCH_TREE_URIS_KEY, new HashSet<>());

        for (String treeUriString : treeUris) {
            if (treeUriString == null || treeUriString.isEmpty()) {
                continue;
            }

            Uri treeUri = Uri.parse(treeUriString);
            DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
            if (root == null || !root.canRead()) {
                continue;
            }

            scanTree(root, query, context, scoredResults, uriSet);
        }
    }

    private void scanTree(DocumentFile root, String query, Context context, List<ScoredFileResult> scoredResults, Set<String> uriSet) {
        ArrayDeque<DocumentFile> queue = new ArrayDeque<>();
        queue.add(root);

        int scannedFiles = 0;

        while (!queue.isEmpty() && scannedFiles < MAX_TREE_FILES_TO_SCAN) {
            DocumentFile current = queue.removeFirst();
            DocumentFile[] children;

            try {
                children = current.listFiles();
            } catch (RuntimeException e) {
                continue;
            }

            for (DocumentFile child : children) {
                if (child == null || !child.canRead()) {
                    continue;
                }

                if (child.isDirectory()) {
                    queue.addLast(child);
                    continue;
                }

                ++scannedFiles;

                String name = child.getName();
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                int score = StringMatchStrategy.match(context, query, name, false);
                if (score < 0) {
                    continue;
                }

                Uri fileUri = child.getUri();
                String uriString = fileUri.toString();
                if (uriSet.contains(uriString)) {
                    continue;
                }

                String mimeType = child.getType();
                String detail = root.getName() == null ? "" : root.getName();

                scoredResults.add(new ScoredFileResult(score, new FileCandidateEntry(name, detail, mimeType, fileUri)));
                uriSet.add(uriString);
            }
        }
    }

    private static class ScoredFileResult {
        private final int score;
        private final FileCandidateEntry entry;

        ScoredFileResult(int score, FileCandidateEntry entry) {
            this.score = score;
            this.entry = entry;
        }
    }

    private static class FileCandidateEntry implements CandidateEntry {
        private final String fileName;
        private final String detail;
        private final String mimeType;
        private final Uri uri;

        FileCandidateEntry(String fileName, String relativePath, String mimeType, Uri uri) {
            this.fileName = fileName;
            this.detail = relativePath == null ? "" : relativePath;
            this.mimeType = mimeType;
            this.uri = uri;
        }

        @NonNull
        @Override
        public String getTitle() {
            return fileName;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            if (detail.isEmpty()) {
                return null;
            }
            TextView detailView = new TextView(mainActivity);
            detailView.setText(detail);
            detailView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            return detailView;
        }

        @Override
        public boolean hasLongView() {
            return true;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, mimeType != null ? mimeType : "*/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(activity, activity.getString(R.string.error_failure_not_found_opening_application), Toast.LENGTH_LONG).show();
                }
            };
        }

        @Override
        public Drawable getIcon(Context context) {
            String extension = "";
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < fileName.length()) {
                extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
            }

            int iconRes;
            if (extension.equals("apk")) {
                iconRes = android.R.drawable.sym_def_app_icon;
            } else if (extension.equals("pdf")) {
                iconRes = android.R.drawable.ic_menu_save;
            } else if (isImage(extension, mimeType)) {
                iconRes = android.R.drawable.ic_menu_gallery;
            } else if (isVideo(extension, mimeType)) {
                iconRes = android.R.drawable.ic_media_play;
            } else if (isAudio(extension, mimeType)) {
                iconRes = android.R.drawable.ic_media_ff;
            } else if (isArchive(extension)) {
                iconRes = android.R.drawable.ic_menu_upload;
            } else if (isText(extension, mimeType)) {
                iconRes = android.R.drawable.ic_menu_edit;
            } else {
                iconRes = android.R.drawable.ic_menu_agenda;
            }

            return ContextCompat.getDrawable(context, iconRes);
        }

        private boolean isImage(String extension, String mimeType) {
            return (mimeType != null && mimeType.startsWith("image/"))
                    || extension.equals("png")
                    || extension.equals("jpg")
                    || extension.equals("jpeg")
                    || extension.equals("gif")
                    || extension.equals("webp");
        }

        private boolean isVideo(String extension, String mimeType) {
            return (mimeType != null && mimeType.startsWith("video/"))
                    || extension.equals("mp4")
                    || extension.equals("mkv")
                    || extension.equals("webm")
                    || extension.equals("mov");
        }

        private boolean isAudio(String extension, String mimeType) {
            return (mimeType != null && mimeType.startsWith("audio/"))
                    || extension.equals("mp3")
                    || extension.equals("wav")
                    || extension.equals("ogg")
                    || extension.equals("m4a");
        }

        private boolean isArchive(String extension) {
            return extension.equals("zip") || extension.equals("7z") || extension.equals("rar") || extension.equals("tar") || extension.equals("gz");
        }

        private boolean isText(String extension, String mimeType) {
            return (mimeType != null && mimeType.startsWith("text/"))
                    || extension.equals("txt")
                    || extension.equals("md")
                    || extension.equals("json")
                    || extension.equals("xml")
                    || extension.equals("csv");
        }

        @Override
        public boolean hasEvent() {
            return true;
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
}
