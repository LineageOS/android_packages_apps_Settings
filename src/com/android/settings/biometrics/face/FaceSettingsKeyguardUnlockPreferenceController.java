/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static android.provider.Settings.Secure.BIOMETRIC_KEYGUARD_ENABLED;
import static android.provider.Settings.Secure.FACE_KEYGUARD_ENABLED;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.face.FaceManager;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.Utils;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;

public class FaceSettingsKeyguardUnlockPreferenceController extends
        FaceSettingsPreferenceController {
    private static final int NOT_SET = -1;
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = ON;

    private FaceManager mFaceManager;
    private UserManager mUserManager;

    public FaceSettingsKeyguardUnlockPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
        mFaceManager = Utils.getFaceManagerOrNull(context);
        mUserManager = context.getSystemService(UserManager.class);

        // For OTA case: if FACE_KEYGUARD_ENABLED is not set and BIOMETRIC_KEYGUARD_ENABLED is set,
        // set the default value of the former to that of the latter.
        final int defValue = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                FACE_KEYGUARD_ENABLED, NOT_SET, getUserId());
        final int oldDefValue = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                BIOMETRIC_KEYGUARD_ENABLED, NOT_SET, getUserId());
        if (defValue == NOT_SET && oldDefValue != NOT_SET) {
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    FACE_KEYGUARD_ENABLED, oldDefValue, getUserId());
        }
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                FACE_KEYGUARD_ENABLED, DEFAULT, getUserId()) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_FACE_ENABLED_ON_KEYGUARD_SETTINGS, isChecked);
        return Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FACE_KEYGUARD_ENABLED, isChecked ? ON : OFF, getUserId());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else if (getRestrictingAdmin() != null) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mUserManager.isManagedProfile(getUserId()) || !Utils.hasFaceHardware(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        final ActiveUnlockStatusUtils activeUnlockStatusUtils =
                new ActiveUnlockStatusUtils(mContext);
        if (activeUnlockStatusUtils.isAvailable()) {
            return getAvailabilityFromRestrictingAdmin();
        }
        return getAvailabilityFromRestrictingAdmin();
    }

    private int getAvailabilityFromRestrictingAdmin() {
        return getRestrictingAdmin() != null ? DISABLED_FOR_USER : AVAILABLE;
    }
}
