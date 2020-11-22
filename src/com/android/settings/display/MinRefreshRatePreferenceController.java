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

package com.android.settings.display;

import static android.provider.Settings.System.MIN_REFRESH_RATE;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.R;

public class MinRefreshRatePreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "MinRefreshRatePrefContr";

    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";

    public MinRefreshRatePreferenceController(Context context) {
        super(context, KEY_MIN_REFRESH_RATE);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getStringArray(R.array.min_refresh_rate_entries).length > 0
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MIN_REFRESH_RATE;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final CharSequence[] entries = listPreference.getEntries();
        final int currentValue = Settings.System.getInt(mContext.getContentResolver(),
                MIN_REFRESH_RATE, 60);
        int index = listPreference.findIndexOfValue(String.valueOf(currentValue));
        if (index < 0) index = 0;
        listPreference.setValueIndex(index);
        listPreference.setSummary(entries[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            Settings.System.putInt(mContext.getContentResolver(), MIN_REFRESH_RATE,
                    Integer.parseInt((String) newValue));
            updateState(preference);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist min refresh rate setting", e);
        }
        return true;
    }

}
