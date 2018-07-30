/**
 * Copyright (C) 2018 The LineageOS Project
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

package com.android.settings.notification;

import android.content.Context;
import android.os.RemoteException;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.NoSuchElementException;

import vendor.lineage.vibrator.V1_0.IVibrator;

public class VibratorIntensityController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_VIBRATOR_INTENSITY = "vibrator_intensity";

    public VibratorIntensityController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        try {
            IVibrator server = IVibrator.getService("default");
            return server != null;
        } catch (NoSuchElementException | RemoteException ignored) {
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIBRATOR_INTENSITY;
    }
}
