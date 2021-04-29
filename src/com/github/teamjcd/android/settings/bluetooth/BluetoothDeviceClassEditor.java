package com.github.teamjcd.android.settings.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View.OnKeyListener;

import com.android.settings.SettingsPreferenceFragment;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class BluetoothDeviceClassEditor extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnKeyListener {
    private final static String TAG = BluetoothDeviceClassEditor.class.getSimpleName();

    private static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;

    private EditTextPreference mName;
    private EditTextPreference mClass;

    private BluetoothDeviceClassData mBluetoothDeviceClassData;

    private boolean mNewBluetoothDeviceClass;

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
        } else if (action.equals(Intent.ACTION_INSERT)) {
            mNewBluetoothDeviceClass = true;
        } else {
            finish();
            return;
        }

        if (uri != null) {
            //mBluetoothDeviceClassData = getBluetoothDeviceClassDataFromUri(uri);
        } else {
            //mBluetoothDeviceClassData = new BluetoothDeviceClassData();
        }

        if (mBluetoothDeviceClassData.isUserEditable()) {
            // entry is user editable
        } else {
            // entry is read-only
        }
    }
}
