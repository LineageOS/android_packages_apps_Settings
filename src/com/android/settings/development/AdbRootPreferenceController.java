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

package com.android.settings.development;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import lineageos.app.LineageContextConstants;
import lineageos.providers.LineageSettings;
import lineageos.system.AdbRootManager;

public class AdbRootPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "AdbRootPreferenceController";
    private static final String PREF_KEY = "enable_adb_root";

    private final AdbRootManager mManager;

    public AdbRootPreferenceController(Context context,
            DevelopmentSettingsDashboardFragment fragment) {
        super(context);

        mManager = new AdbRootManager();
    }

    @Override
    public boolean isAvailable() {
        // User builds don't get root, and eng always gets root
        return Build.IS_DEBUGGABLE || "eng".equals(Build.TYPE);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        ((SwitchPreference) mPreference).setChecked(mManager.getEnabled());

        if (!isAdminUser()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean rootEnabled = (Boolean) newValue;
        mManager.setEnabled(rootEnabled);
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isAdminUser()) {
            mPreference.setEnabled(true);
        }
    }

    private boolean isAdminUser() {
        return ((UserManager) mContext.getSystemService(Context.USER_SERVICE)).isAdminUser();
    }
}
