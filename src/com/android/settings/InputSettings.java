/*
 * Copyright (C) 2012 CyanogenMod Project
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

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class InputSettings extends SettingsPreferenceFragment {

    private static final String VOLUME_WAKE_PREF = "pref_volume_wake";
    private CheckBoxPreference mVolumeWakePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.input_settings);

        initInputSettings();
    }

    private void initInputSettings() {
        ContentResolver resolver = getContentResolver();

        mVolumeWakePref = (CheckBoxPreference) findPreference(VOLUME_WAKE_PREF);
        if (mVolumeWakePref != null) {
            mVolumeWakePref.setChecked(Settings.System.getInt(resolver,
                    Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);

        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVolumeWakePref) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_WAKE_SCREEN,
                    mVolumeWakePref.isChecked() ? 1 : 0);
        }

        return true;
    }
}
