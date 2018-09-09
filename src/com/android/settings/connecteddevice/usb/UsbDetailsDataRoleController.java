/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.connecteddevice.usb;

import static android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE;
import static android.hardware.usb.UsbPortStatus.DATA_ROLE_HOST;
import static android.hardware.usb.UsbPortStatus.DATA_ROLE_NONE;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/** This class controls the radio buttons for switching between USB device and host mode. */
public class UsbDetailsDataRoleController extends UsbDetailsController
        implements SelectorWithWidgetPreference.OnClickListener {

    @Nullable private PreferenceCategory mPreferenceCategory;
    @Nullable private SelectorWithWidgetPreference mDevicePref;
    @Nullable private SelectorWithWidgetPreference mHostPref;

    @Nullable private SelectorWithWidgetPreference mNextRolePref;

    private final Runnable mFailureCallback =
            () -> {
                if (mNextRolePref != null) {
                    mNextRolePref.setSummary(R.string.usb_switching_failed);
                    mNextRolePref = null;
                }
            };

    public UsbDetailsDataRoleController(
            Context context, UsbDetailsFragment fragment, UsbBackend backend) {
        super(context, fragment, backend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mHostPref =
                makeRadioPreference(
                        UsbBackend.dataRoleToString(DATA_ROLE_HOST), R.string.usb_control_host);
        mDevicePref =
                makeRadioPreference(
                        UsbBackend.dataRoleToString(DATA_ROLE_DEVICE), R.string.usb_control_device);
    }

    @Override
    protected void refresh(boolean connected, long functions, int powerRole, int dataRole) {
        if (dataRole == DATA_ROLE_DEVICE) {
            requireNonNull(mDevicePref).setChecked(true);
            requireNonNull(mHostPref).setChecked(false);
            requireNonNull(mPreferenceCategory).setEnabled(true);
        } else if (dataRole == DATA_ROLE_HOST) {
            requireNonNull(mDevicePref).setChecked(false);
            requireNonNull(mHostPref).setChecked(true);
            requireNonNull(mPreferenceCategory).setEnabled(true);
        } else if (!connected || dataRole == DATA_ROLE_NONE) {
            requireNonNull(mPreferenceCategory).setEnabled(false);
            if (mNextRolePref == null) {
                // Disconnected with no operation pending, so clear subtexts
                requireNonNull(mHostPref).setSummary("");
                requireNonNull(mDevicePref).setSummary("");
            }
        }

        if (mNextRolePref != null && dataRole != DATA_ROLE_NONE) {
            if (UsbBackend.dataRoleFromString(mNextRolePref.getKey()) == dataRole) {
                // Clear switching text if switch succeeded
                mNextRolePref.setSummary("");
            } else {
                // Set failure text if switch failed
                mNextRolePref.setSummary(R.string.usb_switching_failed);
            }
            mNextRolePref = null;
            mHandler.removeCallbacks(mFailureCallback);
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        int role = UsbBackend.dataRoleFromString(preference.getKey());
        if (role != mUsbBackend.getDataRole()
                && mNextRolePref == null
                && !Utils.isMonkeyRunning()) {
            requireAuthAndExecute(
                    () -> {
                        mUsbBackend.setDataRole(role);
                        mNextRolePref = preference;
                        preference.setSummary(R.string.usb_switching);

                        mHandler.postDelayed(
                                mFailureCallback,
                                mUsbBackend.areAllRolesSupported()
                                        ? UsbBackend.PD_ROLE_SWAP_TIMEOUT_MS
                                        : UsbBackend.NONPD_ROLE_SWAP_TIMEOUT_MS);
                    });
        }
    }

    @Override
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning()
                && !mUsbBackend.isSingleDataRoleSupported();
    }

    @Override
    public String getPreferenceKey() {
        return "usb_details_data_role";
    }

    private SelectorWithWidgetPreference makeRadioPreference(String key, int titleId) {
        SelectorWithWidgetPreference pref =
                new SelectorWithWidgetPreference(requireNonNull(mPreferenceCategory).getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        requireNonNull(mPreferenceCategory).addPreference(pref);
        return pref;
    }
}
