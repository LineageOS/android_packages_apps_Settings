/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.security;

import static android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_V2;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.security.ContentProtectionTogglePreferenceController.KEY_CONTENT_PROTECTION_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.UnknownAuthority;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class,
                   ShadowUserManager.class,
                   ShadowDevicePolicyManager.class})
public class ContentProtectionTogglePreferenceControllerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock private PreferenceScreen mMockPreferenceScreen;

    @Mock private SettingsMainSwitchPreference mMockSwitchPreference;

    @Nullable private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    @Nullable private EnforcingAdmin mEnforcingAdmin =
            new EnforcingAdmin(
                    "test.pkg",
                    UnknownAuthority.UNKNOWN_AUTHORITY,
                    UserHandle.of(UserHandle.myUserId()),
                    new ComponentName("", ""));
    @Nullable private UserHandle mManagedProfile;

    @DevicePolicyManager.ContentProtectionPolicy
    private int mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;

    private TestContentProtectionTogglePreferenceController mController;

    private int mSettingBackupValue;
    private ShadowUserManager mShadowUserManager;

    private ShadowDevicePolicyManager mShadowDevicePolicyManager;

    @Before
    public void setUp() {
        mManagedProfile = UserHandle.of(10);
        mShadowUserManager = ShadowUserManager.getShadow();
        mShadowDevicePolicyManager = ShadowDevicePolicyManager.getShadow();
        mShadowUserManager.setIsAdminUser(true);
        mController = new TestContentProtectionTogglePreferenceController();
        SettingsMainSwitchPreference switchPreference = new SettingsMainSwitchPreference(mContext);
        when(mMockPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(switchPreference);
        mSettingBackupValue = getContentProtectionGlobalSetting();
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                KEY_CONTENT_PROTECTION_PREFERENCE,
                mSettingBackupValue);
        ShadowUtils.reset();
    }

    @Test
    public void constructor_fetchesData() {
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.mCounterGetManagedProfile).isEqualTo(1);
        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        assertThat(mController.mCounterGetContentProtectionPolicy).isEqualTo(1);
    }

    @Test
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_noEnforcedAdmin_readsSettingsTrue() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_noEnforcingAdmin_readsSettingsTrue() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_noEnforcedAdmin_readsSettingsFalse() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_noEnforcingAdmin_readsSettingsFalse() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_noEnforcedAdmin_readsSettingsDefaultTrue() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_noEnforcingAdmin_readsSettingsDefaultTrue() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcedAdmin_policyDisabled_false() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcingAdmin_policyDisabled_false() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        setUpEnforcingAdmin();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcedAdmin_policyEnabled_true() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcingAdmin_policyEnabled_true() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);
        setUpEnforcingAdmin();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcedAdmin_policyNotControlled_readsSettingsTrue() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcingAdmin_policyNotControlled_readsSettingsTrue() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        Settings.Global.putInt(mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        setUpEnforcingAdmin();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcedAdmin_policyNotControlled_readsSettingsFalse() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcingAdmin_policyNotControlled_readsSettingsFalse() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, -1);
        setUpEnforcingAdmin();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_enforcedAdmin_policyNotControlled_readsSettingsDefaultTrue() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        mController = new TestContentProtectionTogglePreferenceController();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enforcingAdmin_policyNotControlled_readsSettingsDefaultTrue() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setUpEnforcingAdmin();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void displayPreference() {
        setupForDisplayPreference();

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockSwitchPreference).addOnSwitchChangeListener(mController);
    }

    @Test
    public void updateState_noEnforcedAdmin_policyDisabled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcingAdmin_policyDisabled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        setupForUpdateStateWithEnforcingAdmin(false);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(EnforcingAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcedAdmin_policyEnabled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcingAdmin_policyEnabled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateStateWithEnforcingAdmin(false);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(EnforcingAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcedAdmin_nonAdminUser_switchBarDisabled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mShadowUserManager.setIsAdminUser(false);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcingAdmin_nonAdminUser_switchBarDisabled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mShadowUserManager.setIsAdminUser(false);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateStateWithEnforcingAdmin(false);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcedAdmin_adminUser_switchBarEnabled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mShadowUserManager.setIsAdminUser(true);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        // Verify that the switch bar is *not* set to disabled.
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcingAdmin_adminUser_switchBarEnabled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mShadowUserManager.setIsAdminUser(true);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateStateWithEnforcingAdmin(false);

        mController.updateState(mMockSwitchPreference);

        // Verify that the switch bar is *not* set to disabled.
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcedAdmin_policyNotControlled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_noEnforcingAdmin_policyNotControlled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setupForUpdateStateWithEnforcingAdmin(false);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_enforcedAdmin_policyDisabled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_enforcingAdmin_policyDisabled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;
        setupForUpdateStateWithEnforcingAdmin(true);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcingAdmin);
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_enforcedAdmin_policyEnabled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcedAdmin);
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_enforcingAdmin_policyEnabled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
        setupForUpdateStateWithEnforcingAdmin(true);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference).setDisabledByAdmin(mEnforcingAdmin);
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_enforcedAdmin_policyNotControlled() {
        mSetFlagsRule.disableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin();
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setupForUpdateState();

        mController.updateState(mMockSwitchPreference);

        assertThat(mController.mCounterGetEnforcedAdmin).isEqualTo(1);
        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void updateState_enforcingAdmin_policyNotControlled() {
        mSetFlagsRule.enableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_V2);
        mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY;
        setupForUpdateStateWithEnforcingAdmin(true);

        mController.updateState(mMockSwitchPreference);

        verify(mMockSwitchPreference, never()).setDisabledByAdmin(
                any(EnforcingAdmin.class));
        verify(mMockSwitchPreference, never()).setEnabled(false);
    }

    @Test
    public void onSwitchChanged_switchChecked_manuallyEnabled() {
        mController.setChecked(false);

        mController.onCheckedChanged(/* switchView= */ null, /* isChecked= */ true);

        assertThat(getContentProtectionGlobalSetting()).isEqualTo(1);
    }

    @Test
    public void onSwitchChanged_switchUnchecked_manuallyDisabled() {
        mController.onCheckedChanged(/* switchView= */ null, /* isChecked= */ false);

        assertThat(getContentProtectionGlobalSetting()).isEqualTo(-1);
    }

    @Test
    public void onSwitchChanged_switchChecked_nonAdminUser_doesNotChangeSetting() {
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
        mShadowUserManager.setIsAdminUser(false);
        int initialSetting = getContentProtectionGlobalSetting();

        boolean result = mController.setChecked(true);

        assertThat(result).isFalse();
        assertThat(getContentProtectionGlobalSetting()).isEqualTo(initialSetting);
    }

    @Test
    public void onSwitchChanged_switchUnchecked_nonAdminUser_doesNotChangeSetting() {
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 1);
        mShadowUserManager.setIsAdminUser(false);
        int initialSetting = getContentProtectionGlobalSetting();

        boolean result = mController.setChecked(false);

        assertThat(result).isFalse();
        assertThat(getContentProtectionGlobalSetting()).isEqualTo(initialSetting);
    }

    private void setUpEnforcingAdmin() {
        mShadowDevicePolicyManager.setPolicyEnforcementInfoForPolicy(
                DevicePolicyIdentifiers.CONTENT_PROTECTION_POLICY,
                new PolicyEnforcementInfo(List.of(mEnforcingAdmin)));
        mController = new TestContentProtectionTogglePreferenceController();
    }

    private int getContentProtectionGlobalSetting() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), KEY_CONTENT_PROTECTION_PREFERENCE, 0);
    }

    private void setupForDisplayPreference() {
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mMockSwitchPreference);
        when(mMockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController = new TestContentProtectionTogglePreferenceController();
    }

    private void setupForDisplayPreferenceWithEnforcingAdmin(boolean isSetUpByEnforcingAdmin) {
        when(mMockPreferenceScreen.findPreference(any())).thenReturn(mMockSwitchPreference);
        when(mMockSwitchPreference.getKey()).thenReturn(mController.getPreferenceKey());
        if (isSetUpByEnforcingAdmin) {
            setUpEnforcingAdmin();
        }
    }

    private void setupForUpdateState() {
        setupForDisplayPreference();
        mController.displayPreference(mMockPreferenceScreen);
    }

    private void setupForUpdateStateWithEnforcingAdmin(boolean isSetUpByEnforcingAdmin) {
        setupForDisplayPreferenceWithEnforcingAdmin(isSetUpByEnforcingAdmin);
        mController.displayPreference(mMockPreferenceScreen);
    }

    private class TestContentProtectionTogglePreferenceController
            extends ContentProtectionTogglePreferenceController {

        public int mCounterGetManagedProfile;

        public int mCounterGetEnforcedAdmin;

        public int mCounterGetContentProtectionPolicy;

        TestContentProtectionTogglePreferenceController() {
            super(ContentProtectionTogglePreferenceControllerTest.this.mContext, "key");
        }

        @Override
        @Nullable
        protected UserHandle getManagedProfile() {
            mCounterGetManagedProfile++;
            return mManagedProfile;
        }

        @Override
        @Nullable
        protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin() {
            mCounterGetEnforcedAdmin++;
            return mEnforcedAdmin;
        }

        @Override
        @DevicePolicyManager.ContentProtectionPolicy
        protected int getContentProtectionPolicy(@Nullable UserHandle userHandle) {
            mCounterGetContentProtectionPolicy++;
            return mContentProtectionPolicy;
        }
    }
}
