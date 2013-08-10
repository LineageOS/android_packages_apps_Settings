/*
 * Copyright (C) 2012 CyanogenMod
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

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PowerMenu extends SettingsPreferenceFragment {
    private static final String TAG = "PowerMenu";

    private static final String KEY_REBOOT = "power_menu_reboot";
    private static final String KEY_SCREENSHOT = "power_menu_screenshot";
    private static final String KEY_EXPANDED_DESKTOP = "power_menu_expanded_desktop";
    private static final String KEY_PROFILES = "power_menu_profiles";
    private static final String KEY_AIRPLANE = "power_menu_airplane";
    private static final String KEY_USER = "power_menu_user";
    private static final String KEY_SOUND = "power_menu_sound";

    private static final class OptionDesc {
        public OptionDesc(String pref, String setting, int defaultValue) {
            this.pref = pref;
            this.setting = setting;
            this.defValue = defaultValue;
        }
        String pref;
        String setting;
        int defValue;
    }

    private static final OptionDesc OPTION_DESCS[] = new OptionDesc[] {
        new OptionDesc(KEY_REBOOT, Settings.System.POWER_MENU_REBOOT_ENABLED, 1),
        new OptionDesc(KEY_SCREENSHOT, Settings.System.POWER_MENU_SCREENSHOT_ENABLED, 0),
        new OptionDesc(KEY_EXPANDED_DESKTOP,
                Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0),
        new OptionDesc(KEY_PROFILES, Settings.System.POWER_MENU_PROFILES_ENABLED, 1),
        new OptionDesc(KEY_AIRPLANE, Settings.System.POWER_MENU_AIRPLANE_ENABLED, 1),
        new OptionDesc(KEY_USER, Settings.System.POWER_MENU_USER_ENABLED, 0),
        new OptionDesc(KEY_SOUND, Settings.System.POWER_MENU_SOUND_ENABLED, 1)
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);

        final ContentResolver resolver = getContentResolver();
        for (OptionDesc desc : OPTION_DESCS) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(desc.pref);
            pref.setChecked(Settings.System.getInt(resolver, desc.setting, desc.defValue) != 0);
        }

        // Only enable expanded desktop item if expanded desktop support is also enabled
        findPreference(KEY_EXPANDED_DESKTOP).setEnabled(Settings.System.getInt(resolver,
                Settings.System.EXPANDED_DESKTOP_STYLE, 0) != 0);

        // Only enable profiles item if System Profiles are also enabled
        findPreference(KEY_PROFILES).setEnabled(Settings.System.getInt(resolver,
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) != 0);

        if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
            getPreferenceScreen().removePreference(findPreference(KEY_USER));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        for (OptionDesc desc : OPTION_DESCS) {
            if (desc.pref.equals(pref.getKey())) {
                boolean checked = ((CheckBoxPreference) pref).isChecked();
                Settings.System.putInt(getContentResolver(), desc.setting, checked ? 1 : 0);
                return true;
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, pref);
    }
}
