/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.android.settings.wimax;

import com.android.settings.R;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

public class AdvancedSettings extends PreferenceActivity {

    private static final String TAG = "WimaxAdvancedSettings";

    private static final String KEY_MAC_ADDRESS = "mac_address";

    private static final String KEY_HW_VERSION = "hw_version";

    private static final String KEY_SW_VERSION = "sw_version";

    private static final String KEY_IP_ADDRESS = "ip_address";

    private static final String KEY_GATEWAY = "gateway";

    private static final String KEY_SIG_STR_RSSI = "signal_strength_rssi";

    private static final String KEY_SIG_STR_SIMPLE = "signal_strength_simple";

    private WimaxSettingsHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new WimaxSettingsHelper(this);
        addPreferencesFromResource(R.xml.wimax_advanced_settings);
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void refreshAll() {
        refreshDeviceInfo();
        refreshIPInfo();
    }

    private void refreshDeviceInfo() {

        int rssi = mHelper.getSignalStrength();
        int simpleLevel = mHelper.calculateSignalLevel(rssi, 4);
        String simpleLevelStr = "";
        switch (simpleLevel) {
            case 0:
                simpleLevelStr = getString(R.string.wimax_signal_0);
                break;
            case 1:
                simpleLevelStr = getString(R.string.wimax_signal_0);
                break;
            case 2:
                simpleLevelStr = getString(R.string.wimax_signal_1);
                break;
            case 3:
                simpleLevelStr = getString(R.string.wimax_signal_2);
                break;
            case 4:
                simpleLevelStr = getString(R.string.wimax_signal_3);
                break;
            default:
                simpleLevelStr = "Unavailable";
                break;
        }
        Preference wimaxMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        Long macAddress = mHelper.getBsMacAddress();
        wimaxMacAddressPref.setSummary(macAddress == null || macAddress == 0 ? getString(R.string.status_unavailable) : macAddress.toString());

        Preference wimaxSignalStrengthSimplePref = findPreference(KEY_SIG_STR_SIMPLE);
        wimaxSignalStrengthSimplePref.setSummary(simpleLevelStr);

        Preference wimaxSignalStrengthRSSIPref = findPreference(KEY_SIG_STR_RSSI);
        wimaxSignalStrengthRSSIPref.setSummary((rssi != 150 && rssi != 0 ? rssi + "" : "Unknown"));

        /*
         * Preference wimaxHwVersionPref = findPreference(KEY_HW_VERSION);
         * String hwVersion = wimaxInfo == null ? null :
         * wimaxInfo.getHwVersion().getVersion();
         * wimaxHwVersionPref.setSummary(!TextUtils.isEmpty(hwVersion) ?
         * hwVersion : getString(R.string.status_unavailable));
         */

        /*
         * Preference wimaxSwVersionPref = findPreference(KEY_SW_VERSION);
         * String swVersion = wimaxInfo == null ? null :
         * wimaxInfo.getSwVersion().getVersion();
         * wimaxSwVersionPref.setSummary(!TextUtils.isEmpty(swVersion) ?
         * swVersion : getString(R.string.status_unavailable));
         */
    }

    private void refreshIPInfo() {

        Preference wimaxIpAddressPref = findPreference(KEY_IP_ADDRESS);
        Integer ipAddress = mHelper.getIpAddress();
        wimaxIpAddressPref
                .setSummary(ipAddress == null || ipAddress == 0 ? getString(R.string.status_unavailable)
                        : ipAddress.toString());

        Preference wimaxGatewayPref = findPreference(KEY_GATEWAY);
        String gateway = "";
        wimaxGatewayPref.setSummary(!TextUtils.isEmpty(gateway) ? gateway
                : getString(R.string.status_unavailable));
    }

}
