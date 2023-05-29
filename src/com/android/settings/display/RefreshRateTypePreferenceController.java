/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.hardware.display.DisplayManagerGlobal;
import android.provider.Settings;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;


public class RefreshRateTypePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    /**
     * DisplayManager.SWITCHING_TYPE_NONE value is 0
     * No mode switching will happen.
     * <p>
     * DisplayManager.SWITCHING_TYPE_WITHIN_GROUPS value is 1
     * Allow only refresh rate switching between modes in the same configuration group. This way
     * only switches without visual interruptions for the user will be allowed.
     * <p>
     * SWITCHING_TYPE_ACROSS_AND_WITHIN_GROUPS value is 2
     * Allow refresh rate switching between all refresh rates even if the switch with have visual
     * interruptions for the user.
     */

    private static final String KEY_REFRESH_RATE_TYPE = "refresh_rate_type";

    private ListPreference mListPreference;
    private DisplayManagerGlobal mDisplayManager;

    public RefreshRateTypePreferenceController(Context context) {
        super(context, KEY_REFRESH_RATE_TYPE);
	mDisplayManager = mContext.getSystemService(DisplayManagerGlobal.class);
    }

    @Override
    public int getAvailabilityStatus() {
        Display.Mode[] modes = mContext.getDisplay().getSupportedModes();
        boolean configRefreshRateType = mContext.getResources().getBoolean(
                R.bool.config_refresh_rate_type_list);
        return (modes.length > 1 && configRefreshRateType) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REFRESH_RATE_TYPE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mListPreference = screen.findPreference(getPreferenceKey());
        mListPreference.setEntries(R.array.refresh_rate_switching_types);
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        int index = mListPreference.findIndexOfValue(
                String.valueOf(mDisplayManager.getRefreshRateSwitchingType()));
        if (index < 0) index = 0;
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int index = mListPreference.findIndexOfValue((String) newValue);
        mDisplayManager.setRefreshRateSwitchingType(index);
        updateState(preference);
        return true;
    }

}

