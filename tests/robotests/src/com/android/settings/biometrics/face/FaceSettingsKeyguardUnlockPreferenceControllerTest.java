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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class})
public class FaceSettingsKeyguardUnlockPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    private FaceSettingsKeyguardUnlockPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    @Test
    public void isChecked_BiometricKeyguardEnabledOff_FaceKeyguardEnabledNotSet_returnFalse() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_KEYGUARD_ENABLED, 0, mContext.getUserId());
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FACE_KEYGUARD_ENABLED, -1, mContext.getUserId());

        mController = new FaceSettingsKeyguardUnlockPreferenceController(
                mContext, "biometric_settings_face_keyguard");

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_BiometricKeyguardEnabledOff_FaceKeyguardEnabledOn_returnTrue() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_KEYGUARD_ENABLED, 0, mContext.getUserId());
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FACE_KEYGUARD_ENABLED, 1, mContext.getUserId());

        mController = new FaceSettingsKeyguardUnlockPreferenceController(
                mContext, "biometric_settings_face_keyguard");

        assertThat(mController.isChecked()).isTrue();
    }
}
