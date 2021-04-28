package com.github.teamjcd.android.settings.bluetooth;

import com.android.settings.SettingsActivity;

public class BluetoothDeviceClassSettingsActivity extends SettingsActivity {
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return BluetoothDeviceClassSettings.class.getName().equals(fragmentName);
    }
}
