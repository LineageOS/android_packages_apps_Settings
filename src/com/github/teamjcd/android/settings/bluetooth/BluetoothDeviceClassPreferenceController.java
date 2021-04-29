package com.github.teamjcd.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class BluetoothDeviceClassPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    protected BluetoothAdapter mBluetoothAdapter;

    private static final String TAG = "BluetoothDeviceClassPrefCtrl";

    private Preference mPreference;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                updateState(mPreference);
            }
        }
    };

    public BluetoothDeviceClassPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void updateState(final Preference preference) {
        preference.setVisible(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
    }

    @Override
    public int getAvailabilityStatus() {
        return mBluetoothAdapter != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(final Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            final Intent intent = new Intent(BluetoothDeviceClassSettings.ACTION_BLUETOOTH_DEVICE_CLASS_SETTINGS);
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
            mContext.startActivity(intent);
            return true;
        }

        return false;
    }
}
