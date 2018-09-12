/*
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
package com.android.settings.network;

import android.app.Fragment;
import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class CaptivePortalModePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "CaptivePortalModePreferenceController";
    private static final String CAPTIVE_PORTAL_SWITCH_KEY = "captive_portal_switch";

    private SwitchPreference mCaptivePortalMode;
    private final Fragment mFragment;

    public CaptivePortalModePreferenceController(Context context, Fragment hostFragment) {
        super(context);

        mFragment = hostFragment;
    }

    @Override
    public void updateState(Preference preference) {
        boolean value = (Settings.Global.getInt(mContext.getContentResolver(),
                         Settings.Global.CAPTIVE_PORTAL_MODE,
                         Settings.Global.CAPTIVE_PORTAL_MODE_PROMPT) != 0);
        ((SwitchPreference) preference).setChecked(value);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.CAPTIVE_PORTAL_MODE, 1);
        } else {
            CaptivePortalWarningDialog.show(mFragment);
        }
        return true;
    }

    public void onCaptivePortalSwitchOffDialogConfirmed() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CAPTIVE_PORTAL_MODE, 0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return CAPTIVE_PORTAL_SWITCH_KEY;
    }
}
