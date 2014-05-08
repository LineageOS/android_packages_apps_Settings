/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class ThemeSettings extends PreferenceFragment {

    private static final String KEY_LOCKSCREEN_WALLPAPER = "lockscreen_wallpaper_settings";

    private Toast mToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.theme_settings);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_LOCKSCREEN_WALLPAPER.equals(key)) {
            try {
                startActivity(new Intent("android.intent.action.SET_KEYGUARD_WALLPAPER"));
            } catch (ActivityNotFoundException e) {
                if (mToast != null) {
                    mToast.cancel();
                }
                if (getActivity() != null) {
                    mToast = Toast.makeText(getActivity(), R.string.lockscreen_picker_not_found,
                            Toast.LENGTH_SHORT);
                    mToast.show();
                }
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
