/*
 * Copyright (C) 2021 The LineageOS Project
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
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

import vendor.lineage.fastcharge.V1_1.ISuperFastCharge;

import java.util.NoSuchElementException;

public class SuperFastChargingPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_SUPER_FAST_CHARGING = "super_fast_charging";
    private static final String TAG = "SuperFastChargingPreferenceController";

    private ISuperFastCharge mSuperFastCharge = null;

    public SuperFastChargingPreferenceController(Context context) {
        super(context, KEY_SUPER_FAST_CHARGING);
        try {
            mSuperFastCharge = ISuperFastCharge.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.e(TAG, "Failed to get ISuperFastCharge interface", e);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mSuperFastCharge != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        boolean superFastChargingEnabled = false;

        try {
            superFastChargingEnabled = mSuperFastCharge.isEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "isEnabled failed", e);
        }

        ((SwitchPreference) preference).setChecked(superFastChargingEnabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean shouldEnableSuperFastCharging = (Boolean) newValue;

        try {
            mSuperFastCharge.setEnabled(shouldEnableSuperFastCharging);
            updateState(preference);
        } catch (RemoteException e) {
            Log.e(TAG, "setEnabled failed", e);
        }

        return false;
    }
}
