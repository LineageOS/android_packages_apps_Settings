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

package com.android.settings.security.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import lineageos.providers.LineageSettings;

public class StatusBarPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    static final String KEY_STATUS_BAR_SHOWN_ON_SECURE_KEYGUARD =
            "status_bar_shown_on_secure_keyguard";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public StatusBarPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean isAvailable() {
        if (!mLockPatternUtils.isSecure(mUserId)) {
            return false;
        }
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_STATUS_BAR_SHOWN_ON_SECURE_KEYGUARD;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(LineageSettings.Secure.getInt(
                mContext.getContentResolver(),
                LineageSettings.Secure.QS_TILES_TOGGLEABLE_ON_LOCK_SCREEN,
                1) == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        LineageSettings.Secure.putInt(
                mContext.getContentResolver(),
                LineageSettings.Secure.QS_TILES_TOGGLEABLE_ON_LOCK_SCREEN,
                (Boolean) newValue ? 1 : 0);
        return true;
    }
}
