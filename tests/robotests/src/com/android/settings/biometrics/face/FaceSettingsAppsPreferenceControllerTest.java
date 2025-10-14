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

import static android.provider.Settings.Secure.BIOMETRIC_APP_ENABLED;
import static android.provider.Settings.Secure.FACE_APP_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorProperties;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
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

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSecureSettings.class})
public class FaceSettingsAppsPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private FaceManager mFaceManager;
    private FaceSettingsAppsPreferenceController mController;

    private FaceSensorPropertiesInternal mConvenienceSensorProperty =
            new FaceSensorPropertiesInternal(
                    0 /* sensorId */,
                    SensorProperties.STRENGTH_CONVENIENCE,
                    1 /* maxEnrollmentsPerUser */,
                    new ArrayList<ComponentInfoInternal>(),
                    FaceSensorProperties.TYPE_UNKNOWN,
                    true /* supportsFaceDetection */,
                    true /* supportsSelfIllumination */,
                    true /* resetLockoutRequiresChallenge */);
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        final ArrayList<FaceSensorPropertiesInternal> list = new ArrayList<>();
        list.add(mConvenienceSensorProperty);
        when(mFaceManager.getSensorPropertiesInternal()).thenReturn(list);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void isChecked_BiometricAppEnableOff_FaceAppEnabledNotSet_returnFalse() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_APP_ENABLED, 0, mContext.getUserId());
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FACE_APP_ENABLED, -1, mContext.getUserId());

        mController = new FaceSettingsAppsPreferenceController(
                mContext, "biometric_settings_face_app");

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_BiometricAppEnableOff_FaceAppEnabledOn_returnTrue() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                BIOMETRIC_APP_ENABLED, 0, mContext.getUserId());
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FACE_APP_ENABLED, 1, mContext.getUserId());

        mController = new FaceSettingsAppsPreferenceController(
                mContext, "biometric_settings_face_app");

        assertThat(mController.isChecked()).isTrue();
    }
}
