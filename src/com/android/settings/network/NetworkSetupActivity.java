/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2021 The LineageOS Project
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

import android.content.Intent;
import android.content.res.Resources;

import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.ButtonBarHandler;
import com.android.settings.network.NetworkProviderSettings;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsWifiSettings2;

public class NetworkSetupActivity extends SettingsActivity implements ButtonBarHandler {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        if (!modIntent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getNetworkProviderSettingsClass().getName());
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                    R.string.provider_internet_settings);
        }
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        final boolean isSavedAccessPointsWifiSettings =
                SavedAccessPointsWifiSettings2.class.getName().equals(fragmentName);

        if (NetworkProviderSettings.class.getName().equals(fragmentName)
                || WifiP2pSettings.class.getName().equals(fragmentName)
                || isSavedAccessPointsWifiSettings) {
            return true;
        }
        return false;
    }

    /* package */ Class<? extends PreferenceFragmentCompat> getNetworkProviderSettingsClass() {
        return NetworkProviderSettings.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        final int new_resid = SetupWizardUtils.getTheme(this, getIntent());
        super.onApplyThemeResource(theme, new_resid, first);
    }
}
