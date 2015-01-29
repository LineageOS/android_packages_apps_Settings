/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.settings;

import android.os.Bundle;
import android.content.ContentResolver;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import java.util.List;

public class BlurEffectSettings extends SettingsPreferenceFragment {
    private static final String TAG = "BlurEffectSettings";

	private static final String KEY_LOCKSCREEN = "lockscreen";
	private static final String KEY_GLOBALACTION = "globalaction";
	private static final String KEY_VOLUMECONTROL = "volumecontrol";

	private CheckBoxPreference mLockscreen;
	private CheckBoxPreference mGlobalaction;
	private CheckBoxPreference mVolumecontrol;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.blur_effect_settings);

        ContentResolver resolver = getContentResolver();

        mLockscreen = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN);
        if (mLockscreen != null) {
            boolean lockScreen = Settings.System.getInt(resolver,
                    Settings.System.BLUR_EFFECT_LOCKSCREEN, 0) > 0 ? true : false;
            mLockscreen.setChecked(lockScreen);
        }
        mGlobalaction = (CheckBoxPreference) findPreference(KEY_GLOBALACTION);
        if (mGlobalaction != null) {
            boolean globalAction = Settings.System.getInt(resolver,
                    Settings.System.BLUR_EFFECT_GLOBALACTION, 0) > 0 ? true : false;
            mGlobalaction.setChecked(globalAction);
        }
        mVolumecontrol = (CheckBoxPreference) findPreference(KEY_VOLUMECONTROL);
        if (mVolumecontrol != null) {
            boolean volumeControl = Settings.System.getInt(resolver,
                    Settings.System.BLUR_EFFECT_VOLUMECONTROL, 0) > 0 ? true : false;
            mVolumecontrol.setChecked(volumeControl);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLockscreen) {
            boolean value = mLockscreen.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.BLUR_EFFECT_LOCKSCREEN, value ? 1 : 0);
            return true;
        } else if (preference == mGlobalaction) {
            boolean value = mGlobalaction.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.BLUR_EFFECT_GLOBALACTION, value ? 1 : 0);
            return true;
        } else if (preference == mVolumecontrol) {
            boolean value = mVolumecontrol.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.BLUR_EFFECT_VOLUMECONTROL, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
