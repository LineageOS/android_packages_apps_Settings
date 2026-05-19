/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.deviceinfo.AbstractBluetoothAddressPreferenceController;

/**
 * Concrete subclass of bluetooth address preference controller
 */
public class BluetoothAddressPreferenceController extends
        AbstractBluetoothAddressPreferenceController implements PreferenceControllerMixin {
    public BluetoothAddressPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    // This space intentionally left blank

    @Override
    protected void setMacSummary(Preference preference, String summary) {
        preference.setSummary(R.string.device_info_protected_single_press);
        preference.setSelectable(true);
        preference.setOnPreferenceClickListener(p -> {
            super.setMacSummary(preference, summary);
            return true;
        });
    }
}
