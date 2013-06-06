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

import android.app.Profile;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.preference.Preference;

import com.android.settings.R;

public class WifiTriggerAPPreference extends Preference {

    private String mSsid;
    private int mTriggerType = Profile.TriggerState.DISABLED;
    private WifiConfiguration mConfig;

    WifiTriggerAPPreference(Context context, WifiConfiguration config) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_wifi_signal);
        loadConfig(config);
        setTitle(mSsid);
    }

    public void setTriggerType(int trigger) {
        mTriggerType = trigger;
    }

    public int getTriggerType() {
        return mTriggerType;
    }

     private void loadConfig(WifiConfiguration config) {
        mSsid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
        mConfig = config;
    }

    public WifiConfiguration getConfig() {
        return mConfig;
    }

    public String getSSID() {
        return mSsid;
    }

    public static String removeDoubleQuotes(String string) {
        final int length = string.length();
        if (length >= 2) {
            if (string.startsWith("\"") && string.endsWith("\"")) {
                return string.substring(1, length - 1);
            }
        }
        return string;
    }
}
