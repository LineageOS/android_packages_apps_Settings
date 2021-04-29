package com.github.teamjcd.android.settings.bluetooth;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassContentProvider;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassData;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassDatabaseHelper;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassStore;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class BluetoothDeviceClassEditor extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnKeyListener {
    public static final int METRICS_CATEGORY_BLUETOOTH_DEVICE_CLASS_EDITOR = 26;
    public static final int METRICS_CATEGORY_DIALOG_BLUETOOTH_DEVICE_CLASS_EDITOR_ERROR = 610;

    private final static String TAG = BluetoothDeviceClassEditor.class.getSimpleName();

    private static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;

    private EditTextPreference mName;
    private EditTextPreference mClass;

    private BluetoothDeviceClassData mBluetoothDeviceClassData;

    private boolean mNewBluetoothDeviceClass;
    private boolean mReadOnlyBluetoothDeviceClass;

    private Uri mBluetoothDeviceClassUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (TextUtils.isEmpty(action)) {
            finish();
            return;
        }

        initBluetoothDeviceClassEditorUi();

        Uri uri = null;
        if (action.equals(Intent.ACTION_EDIT)) {
            uri = intent.getData();
            if (!uri.isPathPrefixMatch(BluetoothDeviceClassContentProvider.DEVICE_CLASS_URI)) {
                Log.e(TAG, "Edit request not for device class table. Uri: " + uri);
                finish();
                return;
            }
        } else if (action.equals(Intent.ACTION_INSERT)) {
            mBluetoothDeviceClassUri = intent.getData();
            if (!mBluetoothDeviceClassUri.isPathPrefixMatch(BluetoothDeviceClassContentProvider.DEVICE_CLASS_URI)) {
                Log.e(TAG, "Insert request not for device class table. Uri: " + mBluetoothDeviceClassUri);
                finish();
                return;
            }
            mNewBluetoothDeviceClass = true;
        } else {
            finish();
            return;
        }

        if (uri != null) {
            BluetoothDeviceClassStore store = BluetoothDeviceClassStore.getBluetoothDeviceClassStore(getPrefContext());
            mBluetoothDeviceClassData = store.get(uri);
        } else {
            mBluetoothDeviceClassData = new BluetoothDeviceClassData();
        }

        if (!mBluetoothDeviceClassData.isUserEditable()) {
            mReadOnlyBluetoothDeviceClass = true;
            mClass.setEnabled(false);
        }

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        fillUI(savedInstanceState == null);
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_BLUETOOTH_DEVICE_CLASS_EDITOR;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary(newValue != null ? String.valueOf(newValue) : null);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!mNewBluetoothDeviceClass && !mReadOnlyBluetoothDeviceClass) {
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete)
                .setIcon(R.drawable.ic_delete);
        }

        menu.add(0, MENU_SAVE, 0, R.string.menu_save)
            .setIcon(android.R.drawable.ic_menu_save);

        menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DELETE:
                deleteBluetoothDeviceClass();
                finish();
                return true;
            case MENU_SAVE:
                if (validateAndSaveBluetoothDeviceClassData()) {
                    finish();
                }
                return true;
            case MENU_CANCEL:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnKeyListener(this);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (validateAndSaveBluetoothDeviceClassData()) {
                    finish();
                }

                return true;
            }
        }

        return false;
    }

    private void initBluetoothDeviceClassEditorUi() {
        addPreferencesFromResource(R.xml.bluetooth_device_class_editor);

        mName = (EditTextPreference) findPreference("bluetooth_device_class_name");
        mClass = (EditTextPreference) findPreference("bluetooth_device_class_class");
    }

    private void fillUI(boolean firstTime) {
        if (firstTime) {
            mName.setText(mBluetoothDeviceClassData.getName());
            mClass.setText(Integer.toHexString(mBluetoothDeviceClassData.getDeviceClass()));
        }

        mName.setSummary(mName.getText());
        mClass.setSummary(mClass.getText());
    }

    private void deleteBluetoothDeviceClass() {
        if (mBluetoothDeviceClassData.getUri() != null) {
            getContentResolver().delete(mBluetoothDeviceClassData.getUri(), null, null);
            mBluetoothDeviceClassData = new BluetoothDeviceClassData();
        }
    }

    private boolean validateAndSaveBluetoothDeviceClassData() {
        if (mReadOnlyBluetoothDeviceClass) {
            return true;
        }

        final String name = mName.getText();
        final String cod = mClass.getText();

        final String errorMsg = validateBluetoothDeviceClassData();
        if (errorMsg != null) {
            showError();
            return false;
        }

        // TODO - better use BluetoothDeviceClassStore instead
        final ContentValues values = new ContentValues();
        boolean callUpdate = mNewBluetoothDeviceClass;

        callUpdate = setStringValueAndCheckIfDiff(values,
                BluetoothDeviceClassDatabaseHelper.DEVICE_CLASS_NAME,
                name,
                mBluetoothDeviceClassData.getName());

        callUpdate = setIntValueAndCheckIfDiff(values,
                BluetoothDeviceClassDatabaseHelper.DEVICE_CLASS_VALUE,
                cod,
                mBluetoothDeviceClassData.getDeviceClass());

        values.put(BluetoothDeviceClassDatabaseHelper.DEVICE_CLASS_USER_EDITABLE, 1);

        if (callUpdate) {
            final Uri uri = mBluetoothDeviceClassData.getUri() == null ? mBluetoothDeviceClassUri : mBluetoothDeviceClassData.getUri();
            updateBluetoothDeviceClassDataToDatabase(uri, values);
        }

        return true;
    }

    private String validateBluetoothDeviceClassData() {
        String errMsg = null;

        final String name = mName.getText();
        final String cod = mClass.getText();

        if (TextUtils.isEmpty(name)) {
            errMsg = getResources().getString(R.string.error_name_empty);
        } else if (TextUtils.isEmpty(cod)) {
            errMsg = getResources().getString(R.string.error_device_class_empty);
        }

        if (errMsg == null) {
            try {
                Integer.parseUnsignedInt(cod, 16);
            } catch (Exception e) {
                errMsg = getResources().getString(R.string.error_device_class_invalid);
            }
        }

        return errMsg;
    }

    private void showError() {
        ErrorDialog.showError(this);
    }

    private boolean setStringValueAndCheckIfDiff(
            ContentValues cv, String key, String value, boolean assumeDiff, final String valueFromLocalCache) {
        final boolean isDiff = assumeDiff
                || !((TextUtils.isEmpty(value) && TextUtils.isEmpty(valueFromLocalCache))
                || (value != null && value.equals(valueFromLocalCache)));

        if (isDiff && value != null) {
            cv.put(key, value);
        }
        return isDiff;
    }

    private boolean setIntValueAndCheckIfDiff(
            ContentValues cv, String key, int value, boolean assumeDiff, final Integer valueFromLocalCache) {
        final boolean isDiff = assumeDiff || value != valueFromLocalCache;
        if (isDiff) {
            cv.put(key, value);
        }
        return isDiff;
    }

    private void updateApnDataToDatabase(Uri uri, ContentValues values) {
        // TODO - better use BluetoothDeviceClassStore
    }

    public static class ErrorDialog extends InstrumentedDialogFragment {
        public static void showError(BluetoothDeviceClassEditor editor) {
            final ErrorDialog dialog = new ErrorDialog();
            dialog.setTargetFragment(editor, 0);
            dialog.show(editor.getFragmentManager(), "error");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String msg = ((BluetoothDeviceClassEditor) getTargetFragment()).validateBluetoothDeviceClassData();

            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(msg)
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return METRICS_CATEGORY_DIALOG_BLUETOOTH_DEVICE_CLASS_EDITOR_ERROR;
        }
    }
}
