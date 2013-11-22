/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

//
// Performance Profile Related Settings
//
public class PerformanceProfile extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String PERF_PROFILE_PREF = "pref_perf_profile";

    public static final String SOB_PREF = "pref_perf_profile_set_on_boot";

    private String mPerfProfileProp;
    private String mPerfProfileFormat;

    private ListPreference mPerfProfilePref;

    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPerfProfileProp = getString(R.string.config_perf_profile_prop);
        mPerfProfileFormat = getString(R.string.perf_profile_summary);

        mPerfProfileEntries = getResources().getStringArray(R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.perf_profile_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mPerfProfilePref = (ListPreference) prefScreen.findPreference(PERF_PROFILE_PREF);
        mPerfProfilePref.setOnPreferenceChangeListener(this);
        setCurrentPerfProfileSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void setCurrentPerfProfileSummary() {
        String currentValue = SystemProperties.get(mPerfProfileProp);
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].compareTo(currentValue) == 0) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format(mPerfProfileFormat, summary));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                SystemProperties.set(mPerfProfileProp, String.valueOf(newValue));
                setCurrentPerfProfileSummary();
                return true;
            }
        }
        return false;
    }
}
