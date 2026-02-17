package net.nhiroki.bluelineconsole.applicationMain;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.commandSearchers.eachSearcher.ContactSearchCommandSearcher;
import net.nhiroki.bluelineconsole.commandSearchers.eachSearcher.FileSystemSearchCommandSearcher;
import net.nhiroki.bluelineconsole.wrapperForAndroid.ContactsReader;

public class PreferencesActivity extends BaseWindowActivity {
    private static final int READ_CONTACT_PERMISSION_GRANT_REQUEST_ID = 1;
    private static final int POST_NOTIFICATIONS_PERMISSION_GRANT_REQUEST_ID = 2;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_GRANT_REQUEST_ID = 3;
    private static final int OPEN_DOCUMENT_TREE_REQUEST_DOWNLOADS = 11;
    private static final int OPEN_DOCUMENT_TREE_REQUEST_DOCUMENTS = 12;
    private static final int OPEN_DOCUMENT_TREE_REQUEST_PICTURES = 13;

    private boolean _comingBack = false;
    private PreferencesFragmentWithOnChangeListener preferenceFragment = null;
    public PreferencesActivity() {
        super(R.layout.preferences_activity_body, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceStates) {
        super.onCreate(savedInstanceStates);

        this.setHeaderFooterTexts(getString(R.string.preferences_title_for_header_and_footer), null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 1);

        this.setWindowLocationGravity(Gravity.CENTER_VERTICAL);

        this.preferenceFragment = new PreferencesFragmentWithOnChangeListener();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.main_preference_fragment, preferenceFragment).commit();

        setResult(RESULT_OK, new Intent(this, MainActivity.class));

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; ++i) {
            if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    prefEdit.putBoolean(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY, false);
                    prefEdit.apply();

                    ((SwitchPreference)this.preferenceFragment.findPreference(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY)).setChecked(false);
                }
            }
            if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    AppNotification.update(PreferencesActivity.this);

                } else {
                    SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    prefEdit.putBoolean(AppNotification.PREF_KEY_ALWAYS_SHOW_NOTIFICATION, false);
                    prefEdit.apply();

                    ((SwitchPreference)this.preferenceFragment.findPreference(AppNotification.PREF_KEY_ALWAYS_SHOW_NOTIFICATION)).setChecked(false);
                }
            }
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    prefEdit.putBoolean(FileSystemSearchCommandSearcher.PREF_FILE_SEARCH_ENABLED_KEY, false);
                    prefEdit.apply();

                    ((SwitchPreference)this.preferenceFragment.findPreference(FileSystemSearchCommandSearcher.PREF_FILE_SEARCH_ENABLED_KEY)).setChecked(false);
                }
            }
        }
    }

    protected void setComingBackFlag() {
        this._comingBack = true;
        MainActivity.setIsComingBack(true);
    }

    protected void requestCommonFolderAccess(String prefKey) {
        int requestCode;
        String folderId;

        if (FileSystemSearchCommandSearcher.PREF_FILE_SEARCH_GRANT_DOWNLOADS_KEY.equals(prefKey)) {
            requestCode = OPEN_DOCUMENT_TREE_REQUEST_DOWNLOADS;
            folderId = "Download";
        } else if (FileSystemSearchCommandSearcher.PREF_FILE_SEARCH_GRANT_DOCUMENTS_KEY.equals(prefKey)) {
            requestCode = OPEN_DOCUMENT_TREE_REQUEST_DOCUMENTS;
            folderId = "Documents";
        } else {
            requestCode = OPEN_DOCUMENT_TREE_REQUEST_PICTURES;
            folderId = "Pictures";
        }

        Intent intent = FileSystemSearchCommandSearcher.createFolderPickerIntent(folderId);
        this.setComingBackFlag();
        this.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        if (requestCode != OPEN_DOCUMENT_TREE_REQUEST_DOWNLOADS
                && requestCode != OPEN_DOCUMENT_TREE_REQUEST_DOCUMENTS
                && requestCode != OPEN_DOCUMENT_TREE_REQUEST_PICTURES) {
            return;
        }

        Uri treeUri = data.getData();
        final int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            getContentResolver().takePersistableUriPermission(treeUri, flags);
            FileSystemSearchCommandSearcher.saveTreeUri(this, treeUri);
            Toast.makeText(this, getString(R.string.preferences_item_files_grant_success), Toast.LENGTH_SHORT).show();
        } catch (SecurityException ignored) {
        }
    }

    public static class PreferencesFragmentWithOnChangeListener extends PreferencesFragment {
        SharedPreferences.OnSharedPreferenceChangeListener preferenceChangedListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            preferenceChangedListener = (sharedPreferences, key) -> {
                if (key.equals(AppNotification.PREF_KEY_ALWAYS_SHOW_NOTIFICATION)) {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(PreferencesFragmentWithOnChangeListener.this.getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        PreferencesFragmentWithOnChangeListener.this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                POST_NOTIFICATIONS_PERMISSION_GRANT_REQUEST_ID);
                    } else {
                        AppNotification.update(PreferencesFragmentWithOnChangeListener.this.getActivity());
                    }
                }
                if (key.equals(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY) &&
                        sharedPreferences.getBoolean(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY, false)) {
                    if (! ContactsReader.appHasReadContactsPermission(PreferencesFragmentWithOnChangeListener.this.getContext())) {
                        PreferencesFragmentWithOnChangeListener.this.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                                READ_CONTACT_PERMISSION_GRANT_REQUEST_ID);
                    }
                }

                if (key.equals(FileSystemSearchCommandSearcher.PREF_FILE_SEARCH_ENABLED_KEY) &&
                        sharedPreferences.getBoolean(FileSystemSearchCommandSearcher.PREF_FILE_SEARCH_ENABLED_KEY, false) &&
                        Build.VERSION.SDK_INT <= 32 &&
                        ContextCompat.checkSelfPermission(PreferencesFragmentWithOnChangeListener.this.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    PreferencesFragmentWithOnChangeListener.this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            READ_EXTERNAL_STORAGE_PERMISSION_GRANT_REQUEST_ID);
                }
            };
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangedListener);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangedListener);
            super.onPause();
        }
    }

    @Override
    public void onUserLeaveHint() {
        setResult(RESULT_OK, new Intent(this, MainActivity.class));
        super.onUserLeaveHint();
    }

    @Override
    public void onResume() {
        super.onResume();
        this._comingBack = false;
        MainActivity.setIsComingBack(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        this.changeBaseWindowElementSizeForAnimation(true);
    }

    @Override
    protected void onStop() {
        // This app should be as stateless as possible. When app disappears most activities should finish.
        super.onStop();
        if (! this._comingBack) {
            this.finish();
        }
    }
}
