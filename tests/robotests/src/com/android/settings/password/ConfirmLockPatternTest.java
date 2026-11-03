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

package com.android.settings.password;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

import static com.android.settings.password.ConfirmLockPattern.ConfirmLockPatternFragment;
import static com.android.settings.password.TestUtils.GUESS_INVALID_RESULT;
import static com.android.settings.password.TestUtils.GUESS_VALID_RESULT;
import static com.android.settings.password.TestUtils.LOCKOUT_RESULT;
import static com.android.settings.password.TestUtils.NO_REMAINING_ATTEMPTS_RESULT;
import static com.android.settings.password.TestUtils.PACKAGE_NAME;
import static com.android.settings.password.TestUtils.SERVICE_NAME;
import static com.android.settings.password.TestUtils.TIMEOUT;
import static com.android.settings.password.TestUtils.VALID_REMAINING_ATTEMPTS;
import static com.android.settings.password.TestUtils.buildConfirmDeviceCredentialBaseActivity;
import static com.android.settings.password.TestUtils.createPackageInfoWithService;
import static com.android.settings.password.TestUtils.createRemoteLockscreenValidationIntent;
import static com.android.settings.password.TestUtils.getConfirmDeviceCredentialBaseFragment;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.admin.ManagedSubscriptionsPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.service.remotelockscreenvalidation.IRemoteLockscreenValidationCallback;
import android.service.remotelockscreenvalidation.RemoteLockscreenValidationClient;
import android.util.FeatureFlagUtils;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;
import org.robolectric.util.ReflectionHelpers;

import java.time.Duration;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
        ShadowUserManager.class,
        ShadowApplicationPackageManager.class,
        SettingsShadowResources.class
})
public class ConfirmLockPatternTest {

    @Mock
    CredentialCheckResultTracker mCredentialCheckResultTracker;
    @Mock
    RemoteLockscreenValidationClient mRemoteLockscreenValidationClient;
    @Captor
    ArgumentCaptor<IRemoteLockscreenValidationCallback> mCallbackCaptor;

    protected Context mContext;
    protected LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mLockPatternUtils = new LockPatternUtils(mContext);

        ShadowApplicationPackageManager shadowApplicationPackageManager =
                (ShadowApplicationPackageManager) Shadows.shadowOf(mContext.getPackageManager());
        shadowApplicationPackageManager.addPackageNoDefaults(
                createPackageInfoWithService(
                        PACKAGE_NAME,
                        SERVICE_NAME,
                        Manifest.permission.BIND_REMOTE_LOCKSCREEN_VALIDATION_SERVICE));

        final ShadowDevicePolicyManager shadowDpm = ShadowDevicePolicyManager.getShadow();
        shadowDpm.setManagedSubscriptionsPolicy(
                new ManagedSubscriptionsPolicy(
                        ManagedSubscriptionsPolicy.TYPE_ALL_PERSONAL_SUBSCRIPTIONS));

        // Set false by default so we can check if lock was set when remote validation succeeds.
        ShadowLockPatternUtils.setIsSecure(UserHandle.myUserId(), false);

        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_REMOTE_DEVICE_CREDENTIAL_VALIDATION, true);

        ConfirmLockPatternFragment.setPatternInputClickSupportedForTesting(true);
    }

    @After
    public void tearDown() {
        ShadowLockPatternUtils.reset();
        SettingsShadowResources.reset();
    }

    @Test
    public void onCreate_remoteValidation_successfullyStart() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);

        assertThat(activity.isFinishing()).isFalse();
        assertThat(fragment.mRemoteValidation).isTrue();
        LockPatternView lockPatternView = (LockPatternView) activity.findViewById(R.id.lockPattern);
        assertThat(lockPatternView.isInStealthMode()).isFalse();
    }

    @Test
    public void onPatternDetected_normalFlow_doesNotAttemptRemoteLockscreenValidation() {
        ConfirmLockPattern activity = Robolectric.buildActivity(
                ConfirmLockPattern.class, new Intent()).setup().get();
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);

        verifyNoInteractions(mRemoteLockscreenValidationClient);
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_guessValid_checkboxChecked() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_VALID_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(true), any(), eq(Duration.ZERO), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isTrue();
        assertThat(fragment.mRemoteLockscreenValidationFragment.getLockscreenCredential()).isNull();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_guessValid_checkboxUnchecked() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mCheckBox.setChecked(false);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_VALID_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(true), any(), eq(Duration.ZERO), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
        assertThat(fragment.mRemoteLockscreenValidationFragment.getLockscreenCredential()).isNull();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_guessInvalid() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_INVALID_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(false), any(), eq(Duration.ZERO), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_lockout() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(LOCKOUT_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(false), any(), eq(TIMEOUT), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_noRemainingAttempts_finishActivity() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(NO_REMAINING_ATTEMPTS_RESULT);

        assertThat(activity.isFinishing()).isTrue();
        verifyNoInteractions(mCredentialCheckResultTracker);
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
    }

    @Test
    public void onChosenLockSaveFinished_setsRedactionInterstitial() throws Exception {
        // This test verifies that the RedactionInterstitial is available. This is the screen
        // responsible for allowing the user to show sensitive lockscreen content. This
        // also allows the Settings tile for RedactionInterstitial to appear in the "anything else"
        // page during SUW.
        final ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        final ConfirmLockPatternFragment fragment = (ConfirmLockPatternFragment)
                getConfirmDeviceCredentialBaseFragment(activity);
        final Intent intent = new Intent();
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 1L);

        fragment.onChosenLockSaveFinished(true, intent);

        final ComponentName componentName =
                new ComponentName(mContext, SetupRedactionInterstitial.class);
        final int isEnabled = mContext.getPackageManager()
                .getComponentEnabledSetting(componentName);

        assertThat(isEnabled).isEqualTo(COMPONENT_ENABLED_STATE_ENABLED);
    }

    @Test
    public void nextButton_shouldBeVisibleAndProceed() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        mouseClick(lockPatternView, 0, 0);
        triggerOnPatternDetected(lockPatternView);

        final PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        final Button nextButton =
                layout.getMixin(FooterBarMixin.class).getPrimaryButtonView();
        assertThat(nextButton).isNotNull();
        assertThat(nextButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(nextButton.getText().toString()).isEqualTo(
                mContext.getString(R.string.next_label));

        nextButton.performClick();

        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_VALID_RESULT);
    }

    @Test
    public void clearButton_shouldBeVisibleAndClearPattern() throws Exception {
        ConfirmLockPattern activity = Robolectric.buildActivity(
                ConfirmLockPattern.class, new Intent()).setup().get();
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        final PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        final Button clearButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertThat(clearButton).isNotNull();
        assertThat(clearButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(clearButton.getText().toString()).isEqualTo(
                mContext.getString(R.string.lockpattern_retry_button_text));

        List<LockPatternView.Cell> pattern =
                List.of(LockPatternView.Cell.of(0, 0, LockPatternUtils.PATTERN_SIZE_DEFAULT));
        lockPatternView.setPattern(LockPatternView.DisplayMode.Correct, pattern);
        clearButton.performClick();
        assertThat(lockPatternView.isEmpty()).isTrue();
    }
    @Test
    public void checkPatternTransfer_bothHideConfigsFalse_checkboxIsVisible() throws Exception {
        SettingsShadowResources.overrideResource(
                R.bool.config_disable_remote_lockscreen_transfer_checkbox, false);
        SettingsShadowResources.overrideResource(R.bool.config_hide_pattern_security_option, false);
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmLockPatternFragment fragment =
                (ConfirmLockPatternFragment) getConfirmDeviceCredentialBaseFragment(activity);
        assertThat(fragment.mCheckBox.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(fragment.mCheckBox.isChecked()).isTrue();
    }

    @Test
    public void checkPatternTransfer_hideCheckboxConfigTrue_checkboxIsGone() throws Exception {
        SettingsShadowResources.overrideResource(
                R.bool.config_disable_remote_lockscreen_transfer_checkbox, true);
        SettingsShadowResources.overrideResource(R.bool.config_hide_pattern_security_option, false);
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmLockPatternFragment fragment =
                (ConfirmLockPatternFragment) getConfirmDeviceCredentialBaseFragment(activity);
        assertThat(fragment.mCheckBox.getVisibility()).isEqualTo(View.GONE);
        assertThat(fragment.mCheckBox.isChecked()).isFalse();
    }

    @Test
    public void checkPatternTransfer_hidePatternConfigTrue_checkboxIsGone() throws Exception {
        SettingsShadowResources.overrideResource(
                R.bool.config_disable_remote_lockscreen_transfer_checkbox, false);
        SettingsShadowResources.overrideResource(R.bool.config_hide_pattern_security_option, true);
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmLockPatternFragment fragment =
                (ConfirmLockPatternFragment) getConfirmDeviceCredentialBaseFragment(activity);
        assertThat(fragment.mCheckBox.getVisibility()).isEqualTo(View.GONE);
        assertThat(fragment.mCheckBox.isChecked()).isFalse();
    }

    @Test
    public void checkPatternTransfer_bothHideConfigsTrue_checkboxIsGone() throws Exception {
        SettingsShadowResources.overrideResource(
                R.bool.config_disable_remote_lockscreen_transfer_checkbox, true);
        SettingsShadowResources.overrideResource(R.bool.config_hide_pattern_security_option, true);
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmLockPatternFragment fragment =
                (ConfirmLockPatternFragment) getConfirmDeviceCredentialBaseFragment(activity);
        assertThat(fragment.mCheckBox.getVisibility()).isEqualTo(View.GONE);
        assertThat(fragment.mCheckBox.isChecked()).isFalse();
    }

    private void triggerOnPatternDetected(LockPatternView lockPatternView) {
        List<LockPatternView.Cell> pattern =
                List.of(LockPatternView.Cell.of(0, 0, LockPatternUtils.PATTERN_SIZE_DEFAULT));
        lockPatternView.setPattern(LockPatternView.DisplayMode.Correct, pattern);
        ReflectionHelpers.callInstanceMethod(lockPatternView, "notifyPatternDetected");
    }

    private void mouseClick(View view, float x, float y) {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 1);
        event.setSource(InputDevice.SOURCE_MOUSE);
        view.onTouchEvent(event);
        event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x, y, 1);
        event.setSource(InputDevice.SOURCE_MOUSE);
        view.onTouchEvent(event);
    }
}
