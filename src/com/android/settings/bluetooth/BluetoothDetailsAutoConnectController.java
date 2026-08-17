/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Controller that adds a "Connect automatically" dropdown to the Bluetooth device details page.
 *
 * <p>The selected mode is persisted to {@link android.provider.Settings.Secure} via {@link
 * AutoConnectMode} and enforced by the Bluetooth stack.
 */
public class BluetoothDetailsAutoConnectController extends BluetoothDetailsController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "BtAutoConnectCtrl";

    private static final String KEY_AUTO_CONNECT_GROUP = "bluetooth_auto_connect";
    private static final String KEY_AUTO_CONNECT = "bluetooth_auto_connect_mode";

    private PreferenceCategory mAutoConnectContainer;
    private ListPreference mAutoConnectPreference;

    public BluetoothDetailsAutoConnectController(
            Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_CONNECT_GROUP;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(newValue instanceof String)) {
            return false;
        }
        int mode = Integer.parseInt((String) newValue);
        if (mode < AutoConnectMode.MANUAL_ONLY || mode > AutoConnectMode.MAX_MODE) {
            return false;
        }
        AutoConnectMode.setMode(mContext, mCachedDevice.getDevice(), mode);
        if (mAutoConnectPreference != null) {
            mAutoConnectPreference.setSummary(mAutoConnectPreference.getEntry());
        }
        return true;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mAutoConnectContainer = screen.findPreference(getPreferenceKey());
        if (mAutoConnectContainer == null) {
            return;
        }
        mAutoConnectContainer.setLayoutResource(
                R.layout.preference_category_bluetooth_no_padding);
        refresh();
    }

    @Override
    protected void refresh() {
        if (mAutoConnectContainer == null) {
            return;
        }
        mAutoConnectPreference = mAutoConnectContainer.findPreference(KEY_AUTO_CONNECT);
        if (mAutoConnectPreference == null) {
            createAutoConnectPreference(mAutoConnectContainer.getContext());
            mAutoConnectContainer.addPreference(mAutoConnectPreference);
        } else {
            updateAutoConnectPreference();
        }
    }

    private void createAutoConnectPreference(Context context) {
        mAutoConnectPreference = new ListPreference(context);
        mAutoConnectPreference.setKey(KEY_AUTO_CONNECT);
        mAutoConnectPreference.setTitle(R.string.bluetooth_auto_connect_title);
        mAutoConnectPreference.setDialogTitle(R.string.bluetooth_auto_connect_title);
        mAutoConnectPreference.setEntries(
                new CharSequence[] {
                    context.getString(R.string.bluetooth_auto_connect_mode_manual),
                    context.getString(R.string.bluetooth_auto_connect_mode_pairing),
                    context.getString(R.string.bluetooth_auto_connect_mode_range),
                    context.getString(R.string.bluetooth_auto_connect_mode_always),
                });
        mAutoConnectPreference.setEntryValues(
                new CharSequence[] {
                    Integer.toString(AutoConnectMode.MANUAL_ONLY),
                    Integer.toString(AutoConnectMode.AFTER_PAIRING),
                    Integer.toString(AutoConnectMode.ON_RANGE),
                    Integer.toString(AutoConnectMode.ALWAYS),
                });
        mAutoConnectPreference.setOnPreferenceChangeListener(this);
        updateAutoConnectPreference();
    }

    private void updateAutoConnectPreference() {
        mAutoConnectPreference.setValue(
                Integer.toString(AutoConnectMode.getMode(mContext, mCachedDevice.getDevice())));
        mAutoConnectPreference.setSummary(mAutoConnectPreference.getEntry());
    }
}
