/*
 * Copyright (C) 2024 The LineageOS Project
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

package com.android.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class LidBehaviorPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public LidBehaviorPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        Resources res = mContext.getResources();
        if (res.getBoolean(com.android.internal.R.bool.config_lidControlsScreenLock) ||
                res.getBoolean(com.android.internal.R.bool.config_lidControlsSleep)) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        screen.findPreference(getPreferenceKey()).setOnPreferenceChangeListener(this);
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setValue(String.valueOf(Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.LID_BEHAVIOR,
                0 /* LID_BEHAVIOR_NONE */)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LID_BEHAVIOR,
                Integer.valueOf((String) newValue));
        return true;
    }
}
