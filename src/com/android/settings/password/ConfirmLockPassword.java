/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PASSWORD_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PIN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LAST_PASSWORD_ATTEMPT_BEFORE_WIPE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LAST_PIN_ATTEMPT_BEFORE_WIPE;
import static android.app.admin.DevicePolicyResources.UNDEFINED;

import static com.android.settings.flags.Flags.showFooterButtonsOnPasswordConfirm;
import static com.android.settings.biometrics.GatekeeperPasswordProvider.containsGatekeeperPasswordHandle;
import static com.android.settings.biometrics.GatekeeperPasswordProvider.getGatekeeperPasswordHandle;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationResult;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImeAwareEditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.Nullable;
import androidx.core.view.insets.ProtectionLayout;
import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.Utils;
import com.android.settings.accessibility.shared.utils.SetupWizardUtilKt;
import com.android.settings.flags.Flags;
import com.android.settings.widget.FocusIndicatorDrawable;
import com.android.settings.widget.ImeAwareTextInputEditText;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;

public class ConfirmLockPassword extends ConfirmDeviceCredentialBaseActivity {

    // The index of the array is isStrongAuth << 1 + isAlpha.
    private static final int[] DETAIL_TEXTS = new int[] {
        R.string.lockpassword_confirm_your_pin_generic,
        R.string.lockpassword_confirm_your_password_generic,
        R.string.lockpassword_strong_auth_required_device_pin,
        R.string.lockpassword_strong_auth_required_device_password,
    };

    public static class InternalActivity extends ConfirmLockPassword {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPasswordFragment.class.getName());
        modIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_USE_EXPRESSIVE_STYLE,
                ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext()));
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && fragment instanceof ConfirmLockPasswordFragment) {
            ((ConfirmLockPasswordFragment) fragment).onWindowFocusChanged(hasFocus);
        }
    }

    public static class ConfirmLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment
            implements OnClickListener, OnEditorActionListener,
            CredentialCheckResultTracker.Listener, SaveAndFinishWorker.Listener,
            RemoteLockscreenValidationFragment.Listener {
        private static final String FRAGMENT_TAG_CHECK_LOCK_RESULT = "check_lock_result";
        private EditText mPasswordEntry;
        private TextInputLayout mPasswordEntryLayout;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private boolean mDisappearing = false;
        private CountDownTimer mCountdownTimer;
        private boolean mIsAlpha;
        private InputMethodManager mImm;
        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private boolean mIsManagedProfile;
        private CharSequence mCheckBoxLabel;
        private boolean mIsExpressiveStyle;

        // Focus ring indicator dimensions
        private static final int FOCUS_RING_HORIZONTAL_PADDING_ADJUSTMENT_DP = -12;
        private static final int FOCUS_RING_VERTICAL_PADDING_ADJUSTMENT_DP = -4;
        // Use "999" to specify a circular corner radius
        private static final int FOCUS_RING_CORNER_RADIUS_DP = 999;

        // required constructor for fragments
        public ConfirmLockPasswordFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(
                    mEffectiveUserId);
            final ConfirmLockPassword activity = (ConfirmLockPassword) getActivity();
            mIsExpressiveStyle = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_USE_EXPRESSIVE_STYLE, false) || (
                    activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.EXPRESSIVE);
            int layoutId = switch (activity.getConfirmCredentialTheme()) {
                case ConfirmCredentialTheme.NORMAL -> R.layout.confirm_lock_password_normal;
                case ConfirmCredentialTheme.EXPRESSIVE -> R.layout.confirm_lock_password_expressive;
                default -> R.layout.confirm_lock_password;
            };
            View view = inflater.inflate(layoutId, container, false);
            mGlifLayout = view.findViewById(R.id.setup_wizard_layout);

            // TODO(b/440023111):This can be removed once SetupDesignLib and SettingsLib have
            //  integrated the solution.
            if (Flags.removeProtectionLayout() && ThemeHelper.shouldApplyGlifExpressiveStyle(
                    getContext())) {
                final ProtectionLayout protect = mGlifLayout.findViewById(
                        com.google.android.setupdesign.R.id.sud_layout_protection);
                if (protect != null) {
                    protect.setProtections(Collections.emptyList());
                }
            }
            mPasswordEntry = (EditText) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);

            // Support a11y actions to submit the input
            mPasswordEntry.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host,
                        AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLICK,
                            getContext().getString(R.string.lockpassword_confirm_label)));
                }

                @Override
                public boolean performAccessibilityAction(View host, int action, Bundle args) {
                    if (action == AccessibilityNodeInfo.ACTION_CLICK
                            && !TextUtils.isEmpty(mPasswordEntry.getText())) {
                        handleNext();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });

            // EditText inside ScrollView doesn't automatically get focus.
            mPasswordEntry.requestFocus();
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);
            mPasswordEntryLayout = view.findViewById(R.id.password_entry_layout);
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);

            if (mRemoteValidation) {
                mIsAlpha = mRemoteLockscreenValidationSession.getLockType()
                        == KeyguardManager.PASSWORD;
                // ProgressBar visibility is set to GONE until interacted with.
                // Set progress bar to INVISIBLE, so the EditText does not get bumped down later.
                mGlifLayout.setProgressBarShown(false);
            } else {
                mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                        || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                        || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality
                        || DevicePolicyManager.PASSWORD_QUALITY_MANAGED == storedQuality;
            }
            if (mIsExpressiveStyle && mPasswordEntryLayout != null) {
                final int textEntryLayoutHintId = mIsAlpha
                        ? R.string.unlock_set_unlock_mode_password
                        : R.string.unlock_set_unlock_mode_pin;
                mPasswordEntryLayout.setHint(textEntryLayoutHintId);
                mPasswordEntryLayout.setHintEnabled(true);
            }

            mImm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);

            mIsManagedProfile = UserManager.get(getActivity()).isManagedProfile(mEffectiveUserId);
            boolean isSupervisingProfile = Utils.isSupervisingProfile(mUserId, getActivity());

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                CharSequence detailsMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
                if (TextUtils.isEmpty(headerMessage) && mIsManagedProfile) {
                    headerMessage = mDevicePolicyManager.getOrganizationNameForUser(mUserId);
                }
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getDefaultHeader();
                }
                if (TextUtils.isEmpty(detailsMessage)) {
                    detailsMessage = getDefaultDetails();
                }
                mGlifLayout.setHeaderText(headerMessage);

                if (mIsManagedProfile || isSupervisingProfile) {
                    mGlifLayout.getDescriptionTextView().setVisibility(View.GONE);
                } else {
                    mGlifLayout.setDescriptionText(detailsMessage);
                }
                mCheckBoxLabel = intent.getCharSequenceExtra(KeyguardManager.EXTRA_CHECKBOX_LABEL);

                if (isSupervisingProfile) {
                    Drawable iconDrawable = getActivity().getDrawable(
                            R.drawable.ic_account_child_invert_48);
                    iconDrawable.mutate();
                    iconDrawable.setTintList(mGlifLayout.getPrimaryColor());
                    mGlifLayout.setIcon(iconDrawable);
                }
            }
            int currentType = mPasswordEntry.getInputType();
            if (mIsAlpha) {
                mPasswordEntry.setInputType(currentType);
                mPasswordEntry.setContentDescription(
                        getContext().getString(R.string.unlock_accessibility_password));
            } else {
                mPasswordEntry.setInputType(
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                mPasswordEntry.setContentDescription(
                        getContext().getString(R.string.unlock_accessibility_pin_area));
            }
            // Can't set via XML since setInputType resets the fontFamily to null
            mPasswordEntry.setTypeface(Typeface.create(
                    getContext().getString(com.android.internal.R.string.config_headlineFontFamily),
                    Typeface.NORMAL));
            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    220, 2f /* translationScale */, 1f /* delayScale*/,
                    AnimationUtils.loadInterpolator(getContext(),
                            android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    110, 1f /* translationScale */,
                    0.5f /* delayScale */, AnimationUtils.loadInterpolator(
                            getContext(), android.R.interpolator.fast_out_linear_in));
            setAccessibilityTitle(mGlifLayout.getHeaderText());

            mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAG_CHECK_LOCK_RESULT);
            if (mCredentialCheckResultTracker == null) {
                mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(mCredentialCheckResultTracker,
                        FRAGMENT_TAG_CHECK_LOCK_RESULT).commit();
            }

            return view;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (mIsExpressiveStyle && showFooterButtonsOnPasswordConfirm()
                    && getResources().getBoolean(
                        R.bool.config_show_footer_buttons_on_password_confirmation_screen)) {
                mFooterBarMixin = mGlifLayout.getMixin(FooterBarMixin.class);
                mFooterBarMixin.setSecondaryButton(
                    new FooterButton.Builder(getActivity())
                        .setText(R.string.lockpassword_clear_label)
                        .setListener(this::onClearButtonClick)
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());
                mFooterBarMixin.setPrimaryButton(
                    new FooterButton.Builder(getActivity())
                        .setText(R.string.lockpassword_confirm_label)
                        .setListener(this::onConfirmButtonClick)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
            }

            if (mRemoteValidation) {
                if (mCheckBox != null) {
                    mCheckBox.setText(TextUtils.isEmpty(mCheckBoxLabel)
                            ? getDefaultCheckboxLabel()
                            : mCheckBoxLabel);
                }
                if (TextUtils.isEmpty(mAlternateButtonText)) {
                    int forgotLockPasswordResId =
                            mIsAlpha
                                    ? R.string.lockpassword_forgot_password
                                    : R.string.lockpassword_forgot_pin;
                    if (mExpressiveTheme
                            && mFooterBarMixin != null
                            && mFooterBarMixin.getSecondaryButton() != null) {
                        mFooterBarMixin
                                .getSecondaryButton()
                                .setText(getActivity(), forgotLockPasswordResId);

                    } else if (mCancelButton != null) {
                        mCancelButton.setText(forgotLockPasswordResId);
                    }
                }
                updateRemoteLockscreenValidationViews();
            }

            final boolean shouldShowFocusRingsInSuw =
                    SetupWizardUtilKt.shouldShowFocusRingsInSuw(getActivity())
                    ||
                    (com.android.settings.accessibility.Flags.enableInsetFocusRingsInSuwReadOnly()
                    && getResources().getBoolean(
                        com.android.internal.R.bool.config_enableInsetFocusRingsInSuw)
                    && !WizardManagerHelper.isUserSetupComplete(getContext()));
            if (mCheckBox != null && shouldShowFocusRingsInSuw) {
                mCheckBox.setForeground(
                        new FocusIndicatorDrawable.Builder(getContext())
                                .withHorizontalPaddingAdjustment(
                                    FOCUS_RING_HORIZONTAL_PADDING_ADJUSTMENT_DP)
                                .withVerticalPaddingAdjustment(
                                    FOCUS_RING_VERTICAL_PADDING_ADJUSTMENT_DP)
                                .withCornerRadius(FOCUS_RING_CORNER_RADIUS_DP)
                                .build());
            }

            if (mForgotButton != null) {
                mForgotButton.setText(mIsAlpha
                        ? R.string.lockpassword_forgot_password
                        : R.string.lockpassword_forgot_pin);
            }
        }

        private void onConfirmButtonClick(View view) {
            handleNext();
        }

        private void onClearButtonClick(View view) {
            mPasswordEntry.setText(null);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mPasswordEntry != null) {
                mPasswordEntry.setText(null);
            }
            // Force a garbage collection to remove remnant of user password shards from memory.
            // Execute this with a slight delay to allow the activity lifecycle to complete and
            // the instance to become gc-able.
            new Handler(Looper.myLooper()).postDelayed(() -> {
                System.gc();
                System.runFinalization();
                System.gc();
            }, 5000);
        }

        private String getDefaultHeader() {
            if (mFrp) {
                return mIsAlpha ? getString(R.string.lockpassword_confirm_your_password_header_frp)
                        : getString(R.string.lockpassword_confirm_your_pin_header_frp);
            }
            if (mRepairMode) {
                return mIsAlpha
                        ? getString(R.string.lockpassword_confirm_repair_mode_password_header)
                        : getString(R.string.lockpassword_confirm_repair_mode_pin_header);
            }
            if (mRemoteValidation) {
                return getString(R.string.lockpassword_remote_validation_header);
            }
            if (Utils.isSupervisingProfile(mUserId, getActivity()) && !mIsAlpha) {
                return getString(R.string.supervision_full_screen_pin_verification_title);
            }
            if (mIsManagedProfile) {
                if (mIsAlpha) {
                    return mDevicePolicyManager.getResources().getString(
                            CONFIRM_WORK_PROFILE_PASSWORD_HEADER,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_work_password_header));
                }
                return mDevicePolicyManager.getResources().getString(
                        CONFIRM_WORK_PROFILE_PIN_HEADER,
                        () -> getString(R.string.lockpassword_confirm_your_work_pin_header));
            }
            if (Utils.isPrivateProfile(mEffectiveUserId, getActivity())
                    && !UserManager.get(getActivity())
                    .isQuietModeEnabled(UserHandle.of(mEffectiveUserId))) {
                return mIsAlpha ? getString(R.string.private_space_confirm_your_password_header)
                        : getString(R.string.private_space_confirm_your_pin_header);
            }

            return mIsAlpha ? getString(R.string.lockpassword_confirm_your_password_header)
                    : getString(R.string.lockpassword_confirm_your_pin_header);
        }

        private String getDefaultDetails() {
            if (mFrp) {
                return mIsAlpha ? getString(R.string.lockpassword_confirm_your_password_details_frp)
                        : getString(R.string.lockpassword_confirm_your_pin_details_frp);
            }
            if (mRepairMode) {
                return mIsAlpha
                        ? getString(R.string.lockpassword_confirm_repair_mode_password_details)
                        : getString(R.string.lockpassword_confirm_repair_mode_pin_details);
            }
            if (mRemoteValidation) {
                return getContext().getString(mIsAlpha
                        ? R.string.lockpassword_remote_validation_password_details
                        : R.string.lockpassword_remote_validation_pin_details);
            }
            boolean isStrongAuthRequired = isStrongAuthRequired();
            // Map boolean flags to an index by isStrongAuth << 1 + isAlpha.
            int index = ((isStrongAuthRequired ? 1 : 0) << 1) + (mIsAlpha ? 1 : 0);
            return getString(DETAIL_TEXTS[index]);
        }

        private String getDefaultCheckboxLabel() {
            if (mRemoteValidation) {
                return getString(mIsAlpha
                        ? R.string.lockpassword_remote_validation_set_password_as_screenlock
                        : R.string.lockpassword_remote_validation_set_pin_as_screenlock);
            }
            throw new IllegalStateException(
                    "Trying to get default checkbox label for illegal flow");
        }

        private int getErrorMessage() {
            return mIsAlpha ? R.string.lockpassword_invalid_password
                    : R.string.lockpassword_invalid_pin;
        }

        @Override
        protected String getLastTryOverrideErrorMessageId(int userType) {
            if (userType == USER_TYPE_MANAGED_PROFILE) {
                return mIsAlpha ?  WORK_PROFILE_LAST_PASSWORD_ATTEMPT_BEFORE_WIPE
                        : WORK_PROFILE_LAST_PIN_ATTEMPT_BEFORE_WIPE;
            }

            return UNDEFINED;
        }

        @Override
        protected int getLastTryDefaultErrorMessage(int userType) {
            switch (userType) {
                case USER_TYPE_PRIMARY:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_device
                            : R.string.lock_last_pin_attempt_before_wipe_device;
                case USER_TYPE_MANAGED_PROFILE:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_profile
                            : R.string.lock_last_pin_attempt_before_wipe_profile;
                case USER_TYPE_SECONDARY:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_user
                            : R.string.lock_last_pin_attempt_before_wipe_user;
                default:
                    throw new IllegalArgumentException("Unrecognized user type:" + userType);
            }
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mGlifLayout.getHeaderTextView().setAlpha(0f);
            mGlifLayout.getDescriptionTextView().setAlpha(0f);
            mCancelButton.setAlpha(0f);
            if (mForgotButton != null) {
                mForgotButton.setAlpha(0f);
            }
            mPasswordEntry.setAlpha(0f);
            mErrorTextView.setAlpha(0f);
        }

        private View[] getActiveViews() {
            ArrayList<View> result = new ArrayList<>();
            result.add(mGlifLayout.getHeaderTextView());
            result.add(mGlifLayout.getDescriptionTextView());
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(mCancelButton);
            }
            if (mForgotButton != null) {
                result.add(mForgotButton);
            }
            result.add(mPasswordEntry);
            result.add(mErrorTextView);
            return result.toArray(new View[] {});
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mAppearAnimationUtils.startAnimation(getActiveViews(), this::updatePasswordEntry);
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
                mCountdownTimer = null;
            }
            mCredentialCheckResultTracker.setListener(null);
            if (mRemoteLockscreenValidationFragment != null) {
                mRemoteLockscreenValidationFragment.setListener(null, /* handler= */ null);
            }
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(null);
            }
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CONFIRM_LOCK_PASSWORD;
        }

        @Override
        public void onResume() {
            super.onResume();
            long deadline = mLockPatternUtils.getLockoutEndTime(mEffectiveUserId).toMillis();
            if (deadline != 0) {
                mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else {
                updatePasswordEntry();
                mErrorTextView.setText("");
                updateErrorMessage(
                        mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));
            }
            mCredentialCheckResultTracker.setListener(this);

            if (mRemoteLockscreenValidationFragment != null) {
                mRemoteLockscreenValidationFragment.setListener(this, mHandler);
            }
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(this);
            }
        }

        @Override
        protected void authenticationSucceeded() {
            mCredentialCheckResultTracker.setResult(true, new Intent(), Duration.ZERO,
                    mEffectiveUserId);
        }

        private void updatePasswordEntry() {
            final boolean isLockedOut =
                    !mLockPatternUtils.getLockoutEndTime(mEffectiveUserId).isZero();
            final boolean isRemoteLockscreenValidationInProgress =
                    mRemoteLockscreenValidationFragment != null
                    && mRemoteLockscreenValidationFragment.isRemoteValidationInProgress();
            boolean shouldEnableInput = !isLockedOut && !isRemoteLockscreenValidationInProgress;
            mPasswordEntry.setEnabled(shouldEnableInput);
            mPasswordEntryInputDisabler.setInputEnabled(shouldEnableInput);
            if (shouldEnableInput) {
                if (mPasswordEntry instanceof ImeAwareEditText) {
                    ((ImeAwareEditText) mPasswordEntry).scheduleShowSoftInput();
                } else if (mPasswordEntry instanceof ImeAwareTextInputEditText) {
                    ((ImeAwareTextInputEditText) mPasswordEntry).scheduleShowSoftInput();
                } else {
                    Log.w(TAG,
                            "mPasswordEntry neither ImeAwareEditText nor "
                                    + "ImeAwareTextInputEditText");
                }
                mPasswordEntry.requestFocus();
            } else {
                mImm.hideSoftInputFromWindow(mPasswordEntry.getWindowToken(), /* flags= */0);
            }
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (!hasFocus) {
                return;
            }
            // Post to let window focus logic to finish to allow soft input show/hide properly.
            mPasswordEntry.post(this::updatePasswordEntry);
        }

        private void handleNext() {
            if (mPendingLockCheck != null || mDisappearing) {
                return;
            }

            // TODO(b/120484642): This is a point of entry for passwords from the UI
            final Editable passwordText = mPasswordEntry.getText();
            if (TextUtils.isEmpty(passwordText)) {
                return;
            }
            final LockscreenCredential credential = mIsAlpha
                    ? LockscreenCredential.createPassword(passwordText)
                    : LockscreenCredential.createPin(passwordText);

            mPasswordEntryInputDisabler.setInputEnabled(false);

            if (mRemoteValidation) {
                validateGuess(credential);
                updateRemoteLockscreenValidationViews();
                updatePasswordEntry();
                return;
            }

            Intent intent = new Intent();
            // TODO(b/161956762): Sanitize this
            if (mReturnGatekeeperPassword) {
                if (isInternalActivity()) {
                    startVerifyPassword(credential, intent,
                            LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE);
                    return;
                }
            } else if (mForceVerifyPath)  {
                if (isInternalActivity()) {
                    final int flags = mRequestWriteRepairModePassword
                            ? LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW : 0;
                    startVerifyPassword(credential, intent, flags);
                    return;
                }
            } else {
                startCheckPassword(credential, intent);
                return;
            }

            mCredentialCheckResultTracker.setResult(false, intent, Duration.ZERO,
                    mEffectiveUserId);
        }

        private boolean isInternalActivity() {
            return getActivity() instanceof ConfirmLockPassword.InternalActivity;
        }

        private void startVerifyPassword(LockscreenCredential credential, final Intent intent,
                @LockPatternUtils.VerifyFlag int flags) {
            final int localEffectiveUserId = mEffectiveUserId;
            final int localUserId = mUserId;
            final LockPatternChecker.OnVerifyCallback onVerifyCallback = response -> {
                final Duration timeout = response.getTimeout();
                mPendingLockCheck = null;
                final boolean matched = response.isMatched();
                if (matched && mReturnCredentials) {
                    if ((flags & LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE) != 0) {
                        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                                response.getGatekeeperPasswordHandle());
                    } else {
                        intent.putExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                                response.getGatekeeperHAT());
                    }
                }
                mCredentialCheckResultTracker.setResult(matched, intent, timeout,
                        localEffectiveUserId);
            };
            mPendingLockCheck = (localEffectiveUserId == localUserId)
                    ? LockPatternChecker.verifyCredential(mLockPatternUtils, credential,
                            localUserId, flags, onVerifyCallback)
                    : LockPatternChecker.verifyTiedProfileChallenge(mLockPatternUtils, credential,
                            localUserId, flags, onVerifyCallback);
        }

        private void startCheckPassword(final LockscreenCredential credential,
                final Intent intent) {
            final int localEffectiveUserId = mEffectiveUserId;
            mPendingLockCheck = LockPatternChecker.checkCredential(
                    mLockPatternUtils,
                    credential,
                    localEffectiveUserId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onChecked(VerifyCredentialResponse response) {
                            final boolean matched = response.isMatched();
                            final Duration timeout = response.getTimeout();
                            mPendingLockCheck = null;
                            if (matched && isInternalActivity() && mReturnCredentials) {
                                intent.putExtra(
                                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, credential);
                            }
                            mCredentialCheckResultTracker.setResult(matched, intent, timeout,
                                    localEffectiveUserId);
                        }
                    });
        }

        private void startDisappearAnimation(final Intent intent) {
            ConfirmDeviceCredentialUtils.hideImeImmediately(
                    getActivity().getWindow().getDecorView());

            if (mDisappearing) {
                return;
            }
            mDisappearing = true;

            final ConfirmLockPassword activity = (ConfirmLockPassword) getActivity();
            // Bail if there is no active activity.
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.DARK) {
                mDisappearAnimationUtils.startAnimation(getActiveViews(), () -> {
                    activity.setResult(RESULT_OK, intent);
                    activity.finish();
                    activity.overridePendingTransition(
                            R.anim.confirm_credential_close_enter,
                            R.anim.confirm_credential_close_exit);
                });
            } else {
                activity.setResult(RESULT_OK, intent);
                activity.finish();
            }
        }

        private void onPasswordChecked(boolean matched, Intent intent, Duration timeout,
                int effectiveUserId, boolean newResult) {
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (matched) {
                if (newResult) {
                    ConfirmDeviceCredentialUtils.reportSuccessfulAttempt(mLockPatternUtils,
                            mUserManager, mDevicePolicyManager, mEffectiveUserId,
                            /* isStrongAuth */ true);
                }
                startDisappearAnimation(intent);
                ConfirmDeviceCredentialUtils.checkForPendingIntent(getActivity());
            } else {
                if (timeout.isPositive()) {
                    refreshLockScreen();
                    long deadline = mLockPatternUtils.getLockoutEndTime(
                            effectiveUserId).toMillis();
                    handleAttemptLockout(deadline);
                } else {
                    showError(getErrorMessage(), CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);
                }
                if (newResult) {
                    reportFailedAttempt();
                }
            }
        }

        @Override
        public void onRemoteLockscreenValidationResult(
                RemoteLockscreenValidationResult result) {
            switch (result.getResultCode()) {
                case RemoteLockscreenValidationResult.RESULT_GUESS_VALID:
                    if (mCheckBox.isChecked() && mRemoteLockscreenValidationFragment
                            .getLockscreenCredential() != null) {
                        Log.i(TAG, "Setting device screen lock to the other device's screen lock.");
                        if (mSaveAndFinishWorker == null) {
                            mSaveAndFinishWorker = new SaveAndFinishWorker();
                            getFragmentManager().beginTransaction().add(mSaveAndFinishWorker,
                                            FRAGMENT_TAG_SAVE_AND_FINISH)
                                    .commit();
                            getFragmentManager().executePendingTransactions();
                            mSaveAndFinishWorker
                                    .setListener(this)
                                    .setRequestGatekeeperPasswordHandle(true);
                        }
                        mSaveAndFinishWorker.start(
                                mLockPatternUtils,
                                mRemoteLockscreenValidationFragment.getLockscreenCredential(),
                                /* currentCredential= */ null,
                                mEffectiveUserId,
                                mLockPatternUtils.getLockPatternSize(mEffectiveUserId));
                    } else {
                        mCredentialCheckResultTracker.setResult(/* matched= */ true, new Intent(),
                                /* timeout= */ Duration.ZERO, mEffectiveUserId);
                    }
                    return;
                case RemoteLockscreenValidationResult.RESULT_GUESS_INVALID:
                    mCredentialCheckResultTracker.setResult(/* matched= */ false, new Intent(),
                            /* timeout= */ Duration.ZERO, mEffectiveUserId);
                    break;
                case RemoteLockscreenValidationResult.RESULT_LOCKOUT:
                    mCredentialCheckResultTracker.setResult(/* matched= */ false, new Intent(),
                            Duration.ofMillis(result.getTimeoutMillis()), mEffectiveUserId);
                    break;
                case RemoteLockscreenValidationResult.RESULT_NO_REMAINING_ATTEMPTS:
                case RemoteLockscreenValidationResult.RESULT_SESSION_EXPIRED:
                    onRemoteLockscreenValidationFailure(String.format(
                            "Cannot continue remote lockscreen validation. ResultCode=%d",
                            result.getResultCode()));
                    break;
            }
            updateRemoteLockscreenValidationViews();
            updatePasswordEntry();
            mRemoteLockscreenValidationFragment.clearLockscreenCredential();
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, Duration timeout,
                int effectiveUserId, boolean newResult) {
            onPasswordChecked(matched, intent, timeout, effectiveUserId, newResult);
        }

        @Override
        protected void onShowError() {
            mPasswordEntry.setText(null);
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            clearResetErrorRunnable();
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - SystemClock.elapsedRealtime(),
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    showError(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts,
                            secondsCountdown), 0);
                }

                @Override
                public void onFinish() {
                    updatePasswordEntry();
                    mErrorTextView.setText("");
                    updateErrorMessage(
                            mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));
                }
            }.start();
            updatePasswordEntry();
        }

        public void onClick(View v) {
            if (v.getId() == R.id.next_button) {
                handleNext();
            } else if (v.getId() == R.id.cancel_button) {
                getActivity().setResult(RESULT_CANCELED);
                getActivity().finish();
            }
        }

        // {@link OnEditorActionListener} methods.
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }

        /**
         * Callback for when the current device's lockscreen was set to the guess used for
         * remote lockscreen validation.
         */
        @Override
        public void onChosenLockSaveFinished(boolean wasSecureBefore, Intent resultData) {
            Log.i(TAG, "Device lockscreen has been set to remote device's lockscreen.");
            mRemoteLockscreenValidationFragment.clearLockscreenCredential();

            Intent result = new Intent();
            if (mRemoteValidation && containsGatekeeperPasswordHandle(resultData)) {
                result.putExtra(EXTRA_KEY_GK_PW_HANDLE, getGatekeeperPasswordHandle(resultData));
                SetupRedactionInterstitial.setEnabled(getContext(), true);
            }
            mCredentialCheckResultTracker.setResult(/* matched= */ true, result,
                    /* timeout= */ Duration.ZERO, mEffectiveUserId);
        }
    }
}
