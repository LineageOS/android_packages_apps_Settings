/*
 * Copyright (C) 2017 AIM-ROM
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

package com.android.settings.aim.tabs;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.Utils;

import com.android.settings.aim.Preferences.SystemSettingSwitchPreference;

public class Buttons extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String FINGERPRINT_VIB = "fingerprint_success_vib";
    private static final String CATEGORY_FP = "category_fp";
    private static final String FP_UNLOCK_KEYSTORE = "fp_unlock_keystore";

    private FingerprintManager mFingerprintManager;

    private SwitchPreference mFpKeystore;
    private SystemSettingSwitchPreference mFingerprintVib;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aim_buttons_tab);
        PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        mFpKeystore = (SwitchPreference) findPreference(FP_UNLOCK_KEYSTORE);
        final PreferenceCategory fpCat = (PreferenceCategory) prefScreen.findPreference(CATEGORY_FP);
        if (!mFingerprintManager.isHardwareDetected()){
            prefScreen.removePreference(fpCat);
       } else {
       mFpKeystore.setChecked((Settings.System.getInt(getContentResolver(),
              Settings.System.FP_UNLOCK_KEYSTORE, 0) == 1));
       mFpKeystore.setOnPreferenceChangeListener(this);
     }
   }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
      if (preference == mFpKeystore) {
          boolean value = (Boolean) objValue;
          Settings.System.putInt(getActivity().getContentResolver(),
                   Settings.System.FP_UNLOCK_KEYSTORE, value ? 1 : 0);
        return true;
    }
         return false;
    }

}
