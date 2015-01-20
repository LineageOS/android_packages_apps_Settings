/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod.qs;

import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.android.internal.util.cm.QSConstants;
import com.android.settings.R;

public class QSTileSettingsFragment extends PreferenceFragment {

    private final static String TILE_TYPE_KEY = "tile_type";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String tileType = getArguments().getString(TILE_TYPE_KEY);
        int layout = -1;

        switch (tileType) {
            case QSConstants.TILE_LOCATION:
                layout = R.xml.qs_settings_location;
                break;
            case QSConstants.TILE_WIFI:
                layout = R.xml.qs_settings_wifi;
                break;
        }

        // Load the preferences
        if (layout != -1) {
            addPreferencesFromResource(layout);
        }
    }
}
