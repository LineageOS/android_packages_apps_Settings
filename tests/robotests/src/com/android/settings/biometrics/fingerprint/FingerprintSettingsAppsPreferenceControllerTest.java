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

package com.android.settings.biometrics.fingerprint;

import static android.provider.Settings.Secure.BIOMETRIC_APP_ENABLED;
import static android.provider.Settings.Secure.FINGERPRINT_APP_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class})
public class FingerprintSettingsAppsPreferenceControllerTest {
    private Context mContext;
    private FingerprintSettingsAppsPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void isChecked_BiometricAppEnableOff_FingerprintAppEnabledNotSet_returnFalse() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_APP_ENABLED, 0, mContext.getUserId());
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FINGERPRINT_APP_ENABLED, -1, mContext.getUserId());

        mController = new FingerprintSettingsAppsPreferenceController(
                mContext, "biometric_settings_fingerprint_app");

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_BiometricAppEnableOff_FingerprintAppEnabledOn_returnTrue() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_APP_ENABLED, 0, mContext.getUserId());
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FINGERPRINT_APP_ENABLED, 1, mContext.getUserId());

        mController = new FingerprintSettingsAppsPreferenceController(
                mContext, "biometric_settings_fingerprint_app");

        assertThat(mController.isChecked()).isTrue();
    }
}
