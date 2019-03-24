/*
 * Copyright (C) 2019 The LineageOS Project
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

package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class LocationModePreferenceController extends LocationBasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_LOCATION_MODE = "location_battery_saving_mode";

    private Preference mPreference;
    private boolean mAvailable;
    private int mLocationMode = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;

    public LocationModePreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        mAvailable = context.getResources().getBoolean(R.bool.config_location_mode_available);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCATION_MODE;
    }

    @Override
    public boolean isAvailable() {
        return mAvailable;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(
                mLocationMode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING);

        // Restricted user can't change the location mode, so disable the master switch.
        // However, in some corner cases, the location might still be enabled and therefore,
        // the master switch should be disabled, but checked at the same time.
        preference.setEnabled(mLocationEnabler.isEnabled(mLocationMode));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        mLocationMode = mode;
        updateState(mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mLocationEnabler.setLocationMode((Boolean) newValue ?
                Settings.Secure.LOCATION_MODE_BATTERY_SAVING :
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        return true;
    }

}
