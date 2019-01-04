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
package com.android.settings.wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;

public class WifiCountryCodePreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_WIFI_COUNTRY_CODE = "wifi_countrycode";
    private WifiManager mWifiManager;

    public WifiCountryCodePreferenceController(Context context) {
        super(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey() != getPreferenceKey()) {
            return false;
        }

        try {
            mWifiManager.setCountryCode((String) newValue, true);
        } catch (ClassCastException | IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isAvailable() {
         return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WIFI_COUNTRY_CODE;
    }
}
