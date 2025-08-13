/*
 * Copyright (C) 2025 The LineageOS Project
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

package com.android.settings.fuelgauge;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.ArrayUtils;
import com.android.settings.core.BasePreferenceController;

import lineageos.health.HealthInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller to change and update the fast charging settings
 */
public class ChargingSpeedPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private HealthInterface mHealthInterface;
    private ListPreference mListPreference;

    public ChargingSpeedPreferenceController(Context context, String key) {
        super(context, key);

        try {
            mHealthInterface = HealthInterface.getInstance(context);
        } catch (RuntimeException e) {
            // Not supported.
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mListPreference = screen.findPreference(getPreferenceKey());

        if (getAvailabilityStatus() == UNSUPPORTED_ON_DEVICE) {
            return;
        }

        List<CharSequence> entries = new ArrayList<>();
        entries.addAll(Arrays.asList(mListPreference.getEntries()));

        List<CharSequence> entryValues = new ArrayList<>();
        entryValues.addAll(Arrays.asList(mListPreference.getEntryValues()));

        int[] supportedFastChargeModes = mHealthInterface.getSupportedFastChargeModes();

        for (int i = entryValues.size() - 1; i >= 0; i--) {
            int fastChargeMode = Integer.parseInt(entryValues.get(i).toString());
            if (!ArrayUtils.contains(supportedFastChargeModes, fastChargeMode)) {
                entries.remove(i);
                entryValues.remove(i);
            }
        }

        mListPreference.setEntries(entries.stream().toArray(CharSequence[]::new));
        mListPreference.setEntryValues(entryValues.stream().toArray(CharSequence[]::new));
    }

    @Override
    public int getAvailabilityStatus() {
        return mHealthInterface != null && mHealthInterface.isFastChargeSupported()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        String fastChargeMode = String.valueOf(mHealthInterface.getFastChargeMode());
        int index = mListPreference.findIndexOfValue(fastChargeMode);
        if (index < 0) index = 0;
        mListPreference.setValueIndex(index);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mHealthInterface.setFastChargeMode(Integer.parseInt((String) newValue))) {
            updateState(preference);
            return true;
        }

        return false;
    }
}
