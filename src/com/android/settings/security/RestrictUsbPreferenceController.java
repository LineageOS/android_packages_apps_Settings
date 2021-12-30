/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.security;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

import lineageos.providers.LineageSettings;
import lineageos.trust.TrustInterface;

public class RestrictUsbPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener{

    public RestrictUsbPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        DevicePolicyManager policyManager = mContext.getSystemService(DevicePolicyManager.class);
        if (policyManager != null && !policyManager.canUsbDataSignalingBeDisabled()) {
            if (!TrustInterface.getInstance(mContext).hasUsbRestrictor()) {
                return UNSUPPORTED_ON_DEVICE;
            }
        }
        return UserManager.get(mContext).isAdminUser() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference restrictUsbPreference = (ListPreference) preference;
        final long currentTimeout = LineageSettings.Global.getInt(mContext.getContentResolver(),
                LineageSettings.Global.TRUST_RESTRICT_USB, 1);
        restrictUsbPreference.setValue(String.valueOf(currentTimeout));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mPreferenceKey.equals(preference.getKey())) {
            int value = Integer.parseInt((String) newValue);
            LineageSettings.Global.putInt(mContext.getContentResolver(),
                    LineageSettings.Global.TRUST_RESTRICT_USB, value);
        }
        return true;
    }
}
