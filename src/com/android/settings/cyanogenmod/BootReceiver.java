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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.settings.ButtonSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.contributors.ContributorsCloudFragment;
import com.android.settings.hardware.VibratorIntensity;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.DevelopmentSettings;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final String ONE_TIME_TUNABLE_RESTORE = "hardware_tunable_restored";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!hasRestoredTunable(ctx)) {
            /* Restore the hardware tunable values */
            ButtonSettings.restoreKeyDisabler(ctx);
            VibratorIntensity.restore(ctx);
            InputMethodAndLanguageSettings.restore(ctx);
            DisplaySettings.restore(ctx);
            setRestoredTunable(ctx);
        }

        LocationSettings.restore(ctx);

        // Extract the contributors database
        ContributorsCloudFragment.extractContributorsCloudDatabase(ctx);

        DevelopmentSettings.initializeUpdateRecoveryOption(ctx);
    }

    private boolean hasRestoredTunable(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(ONE_TIME_TUNABLE_RESTORE, false);
    }

    private void setRestoredTunable(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(ONE_TIME_TUNABLE_RESTORE, true).apply();
    }
}
