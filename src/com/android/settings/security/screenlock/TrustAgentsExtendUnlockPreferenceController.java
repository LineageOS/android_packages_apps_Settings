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

public class TrustAgentsExtendUnlockPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_TRUST_AGENTS_EXTEND_UNLOCK = "trust_agents_extend_unlock";

    private final int mUserId;

    public TrustAgentsExtendUnlockPreferenceController(Context context, int userId) {
        super(context);
        mUserId = userId;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TRUST_AGENTS_EXTEND_UNLOCK;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.TRUST_AGENTS_EXTEND_UNLOCK,
                (Boolean) newValue ? 1 : 0,
                mUserId);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(Settings.System.getIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.TRUST_AGENTS_EXTEND_UNLOCK,
                1,
                mUserId) == 1);
    }
}
