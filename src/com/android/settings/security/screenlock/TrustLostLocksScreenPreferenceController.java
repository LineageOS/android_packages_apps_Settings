/*
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

package com.android.settings.security.screenlock;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class TrustLostLocksScreenPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_TRUST_LOST_LOCKS_SCREEN =
        "security_setting_trust_lost_locks_screen";

    private final int mUserId;

    public TrustLostLocksScreenPreferenceController(Context context, int userId) {
        super(context);
        mUserId = userId;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TRUST_LOST_LOCKS_SCREEN;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_WHEN_TRUST_LOST,
                (Boolean) newValue ? 1 : 0,
                mUserId);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(Settings.System.getIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_WHEN_TRUST_LOST,
                0,
                mUserId) == 1);
    }
}
