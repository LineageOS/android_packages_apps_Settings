package com.github.teamjcd.android.settings.bluetooth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassData;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

public class BluetoothDeviceClassEditor extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnKeyListener {
    public static final int METRICS_CATEGORY_BLUETOOTH_DEVICE_CLASS_EDITOR = 26;

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
            mBluetoothDeviceClassData = new BluetoothDeviceClassData();
        }

        if (mBluetoothDeviceClassData.isUserEditable()) {
            // entry is user editable
        } else {
            // entry is read-only
        }
    }

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY_BLUETOOTH_DEVICE_CLASS_EDITOR;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO
        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                //if (validateAndSaveApnData()) {
                    finish();
                //}
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
}
