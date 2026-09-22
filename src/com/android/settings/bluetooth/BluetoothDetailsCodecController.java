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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothCodecType;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/** Controller for choosing the A2DP codec a single paired device is played with. */
public class BluetoothDetailsCodecController extends BluetoothDetailsController
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothDetailsCodecController";

    private static final String KEY_BT_AUDIO_DEVICE_TYPE_GROUP =
            "bluetooth_audio_device_type_group";
    private static final String KEY_BT_AUDIO_CODEC = "bluetooth_audio_codec";

    private final LocalBluetoothProfileManager mProfileManager;

    @VisibleForTesting PreferenceGroup mCodecContainer;

    private final BroadcastReceiver mCodecConfigReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    refresh();
                }
            };

    @Nullable private ListPreference mCodecPreference;

    public BluetoothDetailsCodecController(
            Context context,
            PreferenceFragmentCompat fragment,
            LocalBluetoothManager manager,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mProfileManager = manager.getProfileManager();
    }

    @Override
    public boolean isAvailable() {
        A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        return a2dpProfile != null && a2dpProfile.isEnabled(mCachedDevice.getDevice());
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
        mContext.registerReceiver(mCodecConfigReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mContext.unregisterReceiver(mCodecConfigReceiver);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BT_AUDIO_DEVICE_TYPE_GROUP;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mCodecContainer = screen.findPreference(getPreferenceKey());
        refresh();
    }

    @Override
    protected void refresh() {
        if (mCodecContainer == null) {
            return;
        }
        if (mCodecPreference == null) {
            mCodecPreference = new ListPreference(mCodecContainer.getContext());
            mCodecPreference.setKey(KEY_BT_AUDIO_CODEC);
            mCodecPreference.setTitle(R.string.bluetooth_details_audio_codec_title);
            mCodecPreference.setDialogTitle(R.string.bluetooth_details_audio_codec_title);
            mCodecPreference.setOnPreferenceChangeListener(this);
            mCodecContainer.addPreference(mCodecPreference);
        }
        updateCodecPreference(mCodecPreference);
    }

    private void updateCodecPreference(ListPreference codecPreference) {
        BluetoothCodecStatus codecStatus = getCodecStatus();
        if (codecStatus == null) {
            codecPreference.setEnabled(false);
            codecPreference.setSummary(R.string.bluetooth_details_audio_codec_unavailable);
            return;
        }

        List<CharSequence> names = new ArrayList<>();
        List<CharSequence> ids = new ArrayList<>();
        for (BluetoothCodecConfig config : codecStatus.getCodecsSelectableCapabilities()) {
            BluetoothCodecType codecType = config.getExtendedCodecType();
            if (codecType == null) {
                continue;
            }
            names.add(codecType.getCodecName());
            ids.add(Long.toString(codecType.getCodecId()));
        }
        if (ids.isEmpty()) {
            codecPreference.setEnabled(false);
            codecPreference.setSummary(R.string.bluetooth_details_audio_codec_unavailable);
            return;
        }

        codecPreference.setEntries(names.toArray(new CharSequence[0]));
        codecPreference.setEntryValues(ids.toArray(new CharSequence[0]));
        codecPreference.setEnabled(true);

        BluetoothCodecType current =
                codecStatus.getCodecConfig() == null
                        ? null
                        : codecStatus.getCodecConfig().getExtendedCodecType();
        if (current != null && ids.contains(Long.toString(current.getCodecId()))) {
            codecPreference.setValue(Long.toString(current.getCodecId()));
            codecPreference.setSummary(current.getCodecName());
        } else {
            codecPreference.setSummary(R.string.bluetooth_details_audio_codec_unavailable);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(preference instanceof ListPreference) || !(newValue instanceof String)) {
            return false;
        }
        if (!KEY_BT_AUDIO_CODEC.equals(preference.getKey())) {
            return false;
        }
        A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        BluetoothCodecStatus codecStatus = getCodecStatus();
        if (a2dpProfile == null || codecStatus == null) {
            return false;
        }
        long codecId = Long.parseLong((String) newValue);
        for (BluetoothCodecConfig config : codecStatus.getCodecsSelectableCapabilities()) {
            BluetoothCodecType codecType = config.getExtendedCodecType();
            if (codecType == null || codecType.getCodecId() != codecId) {
                continue;
            }
            Log.d(TAG, "onPreferenceChange: selecting " + codecType.getCodecName());
            a2dpProfile.setCodecConfigPreference(
                    mCachedDevice.getDevice(),
                    new BluetoothCodecConfig.Builder()
                            .setExtendedCodecType(codecType)
                            .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                            .build());
            return false;
        }
        Log.e(TAG, "onPreferenceChange: codec " + codecId + " is not selectable");
        return false;
    }

    @Nullable
    private BluetoothCodecStatus getCodecStatus() {
        A2dpProfile a2dpProfile = mProfileManager.getA2dpProfile();
        return a2dpProfile == null ? null : a2dpProfile.getCodecStatus(mCachedDevice.getDevice());
    }

    @VisibleForTesting
    @Nullable
    ListPreference getCodecPreference() {
        return mCodecPreference;
    }
}
