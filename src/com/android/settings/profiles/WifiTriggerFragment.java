/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Settings Preference to configure triggers to switch profiles base on Wi-Fi events
 */
public class WifiTriggerFragment extends SettingsPreferenceFragment {
    private Profile mProfile;
    private WifiTriggerAPPreference mSelectedAccessPoint;
    private ProfileManager mProfileManager;
    private WifiManager mWifiManager;

    private static final int WIFI_TRIGGER = 1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProfileManager = (ProfileManager) getSystemService(Context.PROFILE_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        addPreferencesFromResource(R.xml.wifi_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWifiConfiguration();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mProfile = args.getParcelable("profile");
        }
    }

    private void loadWifiConfiguration() {
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        final Resources res = getResources();
        final List<WifiTriggerAPPreference> aps = new ArrayList<WifiTriggerAPPreference>();

        getPreferenceScreen().removeAll();

        if (configs != null) {
            for (WifiConfiguration config : configs) {
                WifiTriggerAPPreference accessPoint = new WifiTriggerAPPreference(getActivity(), config);
                int state = mProfile.getWifiTrigger(accessPoint.getSSID());
                String summary = res.getStringArray(R.array.profile_trigger_wifi_options)[state];

                accessPoint.setSummary(summary);
                accessPoint.setTriggerType(state);
                aps.add(accessPoint);
            }
        }

        Collections.sort(aps, new Comparator<WifiTriggerAPPreference>() {
            @Override
            public int compare(WifiTriggerAPPreference o1, WifiTriggerAPPreference o2) {
                if (o1.getTriggerType() == o2.getTriggerType()) {
                    return o1.getSSID().compareTo(o2.getSSID());
                }
                return o1.getTriggerType() < o2.getTriggerType() ? -1 : 1;
            }
        });
        for (WifiTriggerAPPreference ap: aps) {
            getPreferenceScreen().addPreference(ap);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof WifiTriggerAPPreference) {
            mSelectedAccessPoint = (WifiTriggerAPPreference) preference;
            showDialog(WIFI_TRIGGER);
            return true;
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WIFI_TRIGGER:
                final String ssid = mSelectedAccessPoint.getSSID();
                int currentTriggerType = mProfile.getWifiTrigger(ssid);

                return new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.profile_trigger_configure)
                        .setSingleChoiceItems(R.array.profile_trigger_wifi_options, currentTriggerType,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mProfile.setWifiTrigger(ssid, which);
                                mProfileManager.updateProfile(mProfile);
                                loadWifiConfiguration();
                                dialog.dismiss();
                            }
                        })
                        .create();
        }
        return super.onCreateDialog(dialogId);
    }
}
