/*
 * Copyright (C) 2020 The LineageOS Project
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

package com.android.settings.development;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class TreatMockLocationRealPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TREAT_MOCK_LOCATION_REAL_KEY = "treat_mock_location_real";

    static final int SETTING_VALUE_ON = 1;
    static final int SETTING_VALUE_OFF = 0;

    public TreatMockLocationRealPreferenceController(Context context) {
        super(context);
    }

    /**
     * Returns the key for this preference.
     */
    @Override
    public String getPreferenceKey() {
        return TREAT_MOCK_LOCATION_REAL_KEY;
    }

    /**
     * Called when the preference has been changed by the user
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.TREAT_MOCK_LOCATION_REAL, isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    /**
     * Called when developer options is disabled and the preference is available
     */
    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.TREAT_MOCK_LOCATION_REAL,
                SETTING_VALUE_OFF);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
