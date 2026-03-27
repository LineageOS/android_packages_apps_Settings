/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.ManagedSubscriptionsPolicy;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
        ShadowUserManager.class,
        ShadowApplicationPackageManager.class
})
public class ConfirmDeviceCredentialActivityTest {

    private static final String ANDROID_SETTINGS_PKG = "com.android.settings";
    private static final String CONFIRM_REMOTE_DEVICE_CREDENTIAL_ACTIVITY_ALIAS =
            "com.android.settings.ConfirmRemoteDeviceCredentialActivity";

    @Before
    public void setUp() {
        final ShadowDevicePolicyManager shadowDpm = ShadowDevicePolicyManager.getShadow();
        shadowDpm.setManagedSubscriptionsPolicy(
                new ManagedSubscriptionsPolicy(
                        ManagedSubscriptionsPolicy.TYPE_ALL_PERSONAL_SUBSCRIPTIONS));

        ShadowLockPatternUtils.setIsSecure(UserHandle.myUserId(), true);
        ShadowLockPatternUtils.setKeyguardStoredPasswordQuality(
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
    }

    @Test
    public void onCreate_remoteValidationWithAlias_doesNotFinish() {
        Intent intent = new Intent(KeyguardManager.ACTION_CONFIRM_REMOTE_DEVICE_CREDENTIAL);
        intent.setComponent(new ComponentName(ANDROID_SETTINGS_PKG,
                CONFIRM_REMOTE_DEVICE_CREDENTIAL_ACTIVITY_ALIAS));

        try (ActivityScenario<ConfirmDeviceCredentialActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isNotEqualTo(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void onCreate_remoteValidationWithoutAlias_finishes() {
        Intent intent = new Intent(KeyguardManager.ACTION_CONFIRM_REMOTE_DEVICE_CREDENTIAL);
        intent.setComponent(new ComponentName(ANDROID_SETTINGS_PKG,
                ConfirmDeviceCredentialActivity.class.getName()));

        try (ActivityScenario<ConfirmDeviceCredentialActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }
    }
}
