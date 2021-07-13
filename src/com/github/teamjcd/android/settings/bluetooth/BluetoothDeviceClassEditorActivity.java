package com.github.teamjcd.android.settings.bluetooth;

import com.android.settings.SettingsActivity;

public class BluetoothDeviceClassEditorActivity extends SettingsActivity {
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return BluetoothDeviceClassEditor.class.getName().equals(fragmentName);
    }
}
