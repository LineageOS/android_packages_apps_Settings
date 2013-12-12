/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PATTERN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LAST_PATTERN_ATTEMPT_BEFORE_WIPE;
import static android.app.admin.DevicePolicyResources.UNDEFINED;

import static com.android.settings.biometrics.GatekeeperPasswordProvider.containsGatekeeperPasswordHandle;
import static com.android.settings.biometrics.GatekeeperPasswordProvider.getGatekeeperPasswordHandle;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationResult;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.insets.ProtectionLayout;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.InputMode;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.Utils;
import com.android.settings.flags.Flags;
import com.android.settings.msds.MSDLPlayerWrapper;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import com.google.android.msdl.data.model.MSDLToken;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.util.ThemeHelper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Launch this when you want the user to confirm their lock pattern.
 *
 * Sets an activity result of {@link Activity#RESULT_OK} when the user
 * successfully confirmed their pattern.
 */
public class ConfirmLockPattern extends ConfirmDeviceCredentialBaseActivity {

    public static class InternalActivity extends ConfirmLockPattern {
    }

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPatternFragment.class.getName());
        modIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_USE_EXPRESSIVE_STYLE,
                ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext()));
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPatternFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    public static class ConfirmLockPatternFragment extends ConfirmDeviceCredentialBaseFragment
            implements AppearAnimationCreator<Object>, CredentialCheckResultTracker.Listener,
            SaveAndFinishWorker.Listener, RemoteLockscreenValidationFragment.Listener {

        private static final String FRAGMENT_TAG_CHECK_LOCK_RESULT = "check_lock_result";

        private static final String KEY_INPUT_MODE = "input_mode";
        private static final String KEY_INPUT_PATTERN = "input_pattern";

        private FooterButton mClearButton;
        private FooterButton mNextButton;
        private LockPatternView mLockPatternView;
        @Nullable private LockPatternView.InputMode mInputMode;
        @Nullable private List<LockPatternView.Cell> mInputPattern;
        @Nullable
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private boolean mDisappearing = false;
        private CountDownTimer mCountdownTimer;

        private View mSudContent;
        private Stage mUiStage;

        // caller-supplied text for various prompts
        private CharSequence mHeaderText;
        private CharSequence mDetailsText;
        private CharSequence mCheckBoxLabel;

        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;

        private boolean mIsManagedProfile;
        private byte mPatternSize;

        @Nullable private static Boolean sIsPatternInputClickSupportedForTesting;

        private final LockPatternView.ExternalHapticsPlayer mExternalHapticsPlayer = () -> {
            MSDLPlayerWrapper.INSTANCE.playToken(MSDLToken.DRAG_INDICATOR_DISCRETE);
        };

        // required constructor for fragments
        public ConfirmLockPatternFragment() {

        }

        @VisibleForTesting
        public static void setPatternInputClickSupportedForTesting(boolean enabled) {
            sIsPatternInputClickSupportedForTesting = enabled;
        }

        private boolean isPatternInputClickSupported() {
            if (sIsPatternInputClickSupportedForTesting != null) {
                return sIsPatternInputClickSupportedForTesting;
            }
            return Flags.patternInputClickSupport()
                    && getResources().getBoolean(R.bool.config_enable_pattern_input_click_support);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ConfirmLockPattern activity = (ConfirmLockPattern) getActivity();
            int layoutId = switch (activity.getConfirmCredentialTheme()) {
                case ConfirmCredentialTheme.NORMAL, ConfirmCredentialTheme.EXPRESSIVE ->
                        R.layout.confirm_lock_pattern_normal;
                default -> R.layout.confirm_lock_pattern;
            };
            View view = inflater.inflate(layoutId, container, false);
            mGlifLayout = view.findViewById(R.id.setup_wizard_layout);

            // TODO(b/440023111):This can be removed once SetupDesignLib and SettingsLib have
            //  integrated the solution.
            if (Flags.removeProtectionLayout()
                    && ThemeHelper.shouldApplyGlifExpressiveStyle(getContext())) {
                final ProtectionLayout protect = mGlifLayout.findViewById(
                        com.google.android.setupdesign.R.id.sud_layout_protection);
                if (protect != null) {
                    protect.setProtections(Collections.emptyList());
                }
            }
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            if (mLockPatternView != null && mExternalHapticsPlayer != null) {
                mLockPatternView.setExternalHapticsPlayer(mExternalHapticsPlayer);
            }
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);

            if (isPatternInputClickSupported()) {
                final FooterBarMixin mixin = mGlifLayout.getMixin(FooterBarMixin.class);
                mixin.setSecondaryButton(
                        new FooterButton.Builder(getActivity())
                                .setText(R.string.lockpattern_retry_button_text)
                                .setListener(this::onClearButtonClick)
                                .setButtonType(FooterButton.ButtonType.OTHER)
                                .setTheme(
                                        com.google.android.setupdesign.R.style
                                                .SudGlifButton_Secondary)
                                .build()
                );
                mixin.setPrimaryButton(
                        new FooterButton.Builder(getActivity())
                                .setText(R.string.next_label)
                                .setListener(this::onNextButtonClick)
                                .setButtonType(FooterButton.ButtonType.NEXT)
                                .setTheme(
                                        com.google.android.setupdesign.R.style
                                                .SudGlifButton_Primary)
                                .build()
                );
                mClearButton = mixin.getSecondaryButton();
                mNextButton = mixin.getPrimaryButton();
            }

            // TODO(b/243008023) Workaround for Glif layout on 2 panel choose lock settings.
            mSudContent = mGlifLayout.findViewById(
                    com.google.android.setupdesign.R.id.sud_layout_content);
            mSudContent.setPadding(mSudContent.getPaddingLeft(), 0, mSudContent.getPaddingRight(),
                    0);
            mIsManagedProfile = UserManager.get(getActivity()).isManagedProfile(mEffectiveUserId);
            mPatternSize = mLockPatternUtils.getLockPatternSize(mEffectiveUserId);

            // make it so unhandled touch events within the unlock screen go to the
            // lock pattern view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockPatternView);

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                mHeaderText = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                mDetailsText = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
                mCheckBoxLabel = intent.getCharSequenceExtra(KeyguardManager.EXTRA_CHECKBOX_LABEL);
                mPatternSize = intent.getByteExtra("pattern_size", mPatternSize);
            }
            if (TextUtils.isEmpty(mHeaderText) && mIsManagedProfile) {
                mHeaderText = mDevicePolicyManager.getOrganizationNameForUser(mUserId);
            }

            mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(
                    mEffectiveUserId));
            mLockPatternView.setLockPatternSize(mPatternSize);
            mLockPatternView.setOnPatternListener(mConfirmExistingLockPatternListener);
            mLockPatternView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            });
            mLockPatternView.setClickable(false);
            mLockPatternView.setClickInputSupported(isPatternInputClickSupported());
            updateStage(Stage.NeedToUnlock);

            if (savedInstanceState == null) {
                // on first launch, if no lock pattern is set, then finish with
                // success (don't want user to get stuck confirming something that
                // doesn't exist).
                // Don't do this check for FRP though, because the pattern is not stored
                // in a way that isLockPatternEnabled is aware of for that case.
                // TODO(roosa): This block should no longer be needed since we removed the
                //              ability to disable the pattern in L. Remove this block after
                //              ensuring it's safe to do so. (Note that ConfirmLockPassword
                //              doesn't have this).
                if (!mFrp && !mRemoteValidation && !mRepairMode
                        && !mLockPatternUtils.isLockPatternEnabled(mEffectiveUserId)) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            } else {
                if (savedInstanceState.containsKey(KEY_INPUT_MODE)) {
                    mInputMode = LockPatternView.InputMode.valueOf(
                            savedInstanceState.getString(KEY_INPUT_MODE));
                }
                if (savedInstanceState.containsKey(KEY_INPUT_PATTERN)) {
                    mInputPattern = LockPatternUtils.byteArrayToPattern(
                            savedInstanceState.getString(KEY_INPUT_PATTERN).getBytes(),
                            mPatternSize);
                }
            }
            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 2f /* translationScale */,
                    1.3f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    125, 4f /* translationScale */,
                    0.3f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.fast_out_linear_in),
                    new AppearAnimationUtils.RowTranslationScaler() {
                        @Override
                        public float getRowTranslationScale(int row, int numRows) {
                            return (float)(numRows - row) / numRows;
                        }
                    });
            setAccessibilityTitle(mGlifLayout.getHeaderText());

            mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAG_CHECK_LOCK_RESULT);
            if (mCredentialCheckResultTracker == null) {
                mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(mCredentialCheckResultTracker,
                        FRAGMENT_TAG_CHECK_LOCK_RESULT).commit();
            }

            if (mRemoteValidation) {
                // ProgressBar visibility is set to GONE until interacted with.
                // Set progress bar to INVISIBLE, so the pattern does not get bumped down later.
                mGlifLayout.setProgressBarShown(false);
                // Lock pattern is generally not visible until the user has set a lockscreen for the
                // first time. For a new user, this means that the pattern will always be hidden.
                // Despite this prerequisite, we want to show the pattern anyway for this flow.
                mLockPatternView.setInStealthMode(false);
            }

            return view;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (mCheckBox != null && getResources().getBoolean(
                    R.bool.config_hide_pattern_security_option)) {
                mCheckBox.setVisibility(View.GONE);
                mCheckBox.setChecked(false);
            }
            if (mRemoteValidation) {
                if (mCheckBox != null) {
                    mCheckBox.setText(TextUtils.isEmpty(mCheckBoxLabel)
                            ? getDefaultCheckboxLabel()
                            : mCheckBoxLabel);
                }
                if (TextUtils.isEmpty(mAlternateButtonText)) {
                    int forgotLockPasswordResId = R.string.lockpassword_forgot_pattern;
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

            if (mForgotButton != null) {
                mForgotButton.setText(R.string.lockpassword_forgot_pattern);
            }
        }

        @Override
        @SuppressLint("MissingSuperCall")
        public void onSaveInstanceState(Bundle outState) {
            // deliberately not calling super since we are managing this in full

            if (mInputMode != null) {
                outState.putString(KEY_INPUT_MODE, mInputMode.name());
            }
            if (mInputPattern != null && !mInputPattern.isEmpty()) {
                byte[] patternBytes = LockPatternUtils.patternToByteArray(mInputPattern,
                        mPatternSize);
                if (patternBytes != null) {
                    outState.putString(KEY_INPUT_PATTERN, new String(patternBytes));
                }
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
            }
            mCredentialCheckResultTracker.setListener(null);
            if (mRemoteLockscreenValidationFragment != null) {
                mRemoteLockscreenValidationFragment.setListener(null, /* handler= */ null);
            }
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(null);
            }
            if (mLockPatternView != null) {
                mLockPatternView.setExternalHapticsPlayer(null);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mInputPattern = null;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CONFIRM_LOCK_PATTERN;
        }

        @Override
        public void onResume() {
            super.onResume();

            // if the user is currently locked out, enforce it.
            long deadline = mLockPatternUtils.getLockoutEndTime(mEffectiveUserId).toMillis();
            if (deadline != 0) {
                mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else if (!mLockPatternView.isEnabled()) {
                // The deadline has passed, but the timer was cancelled. Or the pending lock
                // check was cancelled. Need to clean up.
                updateStage(Stage.NeedToUnlock);
            }
            mCredentialCheckResultTracker.setListener(this);

            if (mRemoteLockscreenValidationFragment != null) {
                mRemoteLockscreenValidationFragment.setListener(this, mHandler);
                if (mRemoteLockscreenValidationFragment.isRemoteValidationInProgress()) {
                    mLockPatternView.setEnabled(false);
                }
            }
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(this);
            }
            if (mLockPatternView != null && mExternalHapticsPlayer != null) {
                mLockPatternView.setExternalHapticsPlayer(mExternalHapticsPlayer);
            }
        }

        @Override
        protected void onShowError() {
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mGlifLayout.getHeaderTextView().setAlpha(0f);
            mCancelButton.setAlpha(0f);
            if (mForgotButton != null) {
                mForgotButton.setAlpha(0f);
            }
            mLockPatternView.setAlpha(0f);
            mGlifLayout.getDescriptionTextView().setAlpha(0f);
        }

        private String getDefaultDetails() {
            if (mFrp) {
                return getString(R.string.lockpassword_confirm_your_pattern_details_frp);
            }
            if (mRepairMode) {
                return getString(R.string.lockpassword_confirm_repair_mode_pattern_details);
            }
            if (mRemoteValidation) {
                return getString(
                        R.string.lockpassword_remote_validation_pattern_details);
            }
            final boolean isStrongAuthRequired = isStrongAuthRequired();
            return isStrongAuthRequired
                    ? getString(R.string.lockpassword_strong_auth_required_device_pattern)
                    : getString(R.string.lockpassword_confirm_your_pattern_generic);
        }

        private Object[][] getActiveViews() {
            ArrayList<ArrayList<Object>> result = new ArrayList<>();
            result.add(new ArrayList<>(Collections.singletonList(mGlifLayout.getHeaderTextView())));
            result.add(new ArrayList<>(
                    Collections.singletonList(mGlifLayout.getDescriptionTextView())));
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(new ArrayList<>(Collections.singletonList(mCancelButton)));
            }
            if (mForgotButton != null) {
                result.add(new ArrayList<>(Collections.singletonList(mForgotButton)));
            }
            LockPatternView.CellState[][] cellStates = mLockPatternView.getCellStates();
            for (int i = 0; i < cellStates.length; i++) {
                ArrayList<Object> row = new ArrayList<>();
                for (int j = 0; j < cellStates[i].length; j++) {
                    row.add(cellStates[i][j]);
                }
                result.add(row);
            }
            Object[][] resultArr = new Object[result.size()][cellStates[0].length];
            for (int i = 0; i < result.size(); i++) {
                ArrayList<Object> row = result.get(i);
                for (int j = 0; j < row.size(); j++) {
                    resultArr[i][j] = row.get(j);
                }
            }
            return resultArr;
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mLockPatternView.setAlpha(1f);
            mAppearAnimationUtils.startAnimation2d(getActiveViews(), null, this);
        }

        private void updateStage(Stage stage) {
            mUiStage = stage;
            switch (stage) {
                case NeedToUnlock:
                    if (mHeaderText != null) {
                        mGlifLayout.setHeaderText(mHeaderText);
                    } else {
                        mGlifLayout.setHeaderText(getDefaultHeader());
                    }

                    CharSequence detailsText =
                            mDetailsText == null ? getDefaultDetails() : mDetailsText;

                    if (mIsManagedProfile) {
                        mGlifLayout.getDescriptionTextView().setVisibility(View.GONE);
                    } else {
                        mGlifLayout.setDescriptionText(detailsText);
                    }

                    mErrorTextView.setText("");
                    updateErrorMessage(
                            mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));

                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    mLockPatternView.clearPattern();
                    if (isPatternInputClickSupported()) {
                        mClearButton.setEnabled(true);
                        mNextButton.setEnabled(true);
                    }
                    break;
                case NeedToUnlockWrong:
                    showError(R.string.lockpattern_need_to_unlock_wrong,
                            CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    if (isPatternInputClickSupported()) {
                        mClearButton.setEnabled(true);
                        mNextButton.setEnabled(true);
                    }
                    break;
                case LockedOut:
                    mLockPatternView.clearPattern();
                    // enabled = false means: disable input, and have the
                    // appearance of being disabled.
                    mLockPatternView.setEnabled(false); // appearance of being disabled
                    if (isPatternInputClickSupported()) {
                        mClearButton.setEnabled(false);
                        mNextButton.setEnabled(false);
                    }
                    break;
            }

            // Always announce the header for accessibility. This is a no-op
            // when accessibility is disabled.
            mGlifLayout.getHeaderTextView().announceForAccessibility(mGlifLayout.getHeaderText());
        }

        private String getDefaultHeader() {
            if (mFrp) {
                return getString(R.string.lockpassword_confirm_your_pattern_header_frp);
            }
            if (mRepairMode) {
                return getString(R.string.lockpassword_confirm_repair_mode_pattern_header);
            }
            if (mRemoteValidation) {
                return getString(R.string.lockpassword_remote_validation_header);
            }
            if (mIsManagedProfile) {
                return mDevicePolicyManager.getResources().getString(
                        CONFIRM_WORK_PROFILE_PATTERN_HEADER,
                        () -> getString(R.string.lockpassword_confirm_your_work_pattern_header));
            }
            if (Utils.isPrivateProfile(mEffectiveUserId, getActivity())
                    && !UserManager.get(getActivity())
                    .isQuietModeEnabled(UserHandle.of(mEffectiveUserId))) {
                return getString(R.string.private_space_confirm_your_pattern_header);
            }

            return getString(R.string.lockpassword_confirm_your_pattern_header);
        }

        private String getDefaultCheckboxLabel() {
            if (mRemoteValidation) {
                return getString(R.string.lockpassword_remote_validation_set_pattern_as_screenlock);
            }
            throw new IllegalStateException(
                    "Trying to get default checkbox label for illegal flow");
        }

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);
        }

        @Override
        protected void authenticationSucceeded() {
            mCredentialCheckResultTracker.setResult(true, new Intent(),
                    Duration.ZERO, mEffectiveUserId);
        }

        private void startDisappearAnimation(final Intent intent) {
            if (mDisappearing) {
                return;
            }
            mDisappearing = true;

            final ConfirmLockPattern activity = (ConfirmLockPattern) getActivity();
            // Bail if there is no active activity.
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.DARK) {
                mLockPatternView.clearPattern();
                mDisappearAnimationUtils.startAnimation2d(getActiveViews(),
                        () -> {
                            activity.setResult(RESULT_OK, intent);
                            activity.finish();
                            activity.overridePendingTransition(
                                    R.anim.confirm_credential_close_enter,
                                    R.anim.confirm_credential_close_exit);
                        }, this);
            } else {
                activity.setResult(RESULT_OK, intent);
                activity.finish();
            }
        }

        private void onClearButtonClick(View view) {
            if (mUiStage != Stage.NeedToUnlock && mUiStage != Stage.NeedToUnlockWrong) {
                throw new IllegalStateException("expected ui stage "
                        + Stage.NeedToUnlock + " or " + Stage.NeedToUnlockWrong
                        + " for clear button");
            }
            // clearPattern() is called within the updateStage itself.
            updateStage(Stage.NeedToUnlock);
        }

        private void onNextButtonClick(View view) {
            if (mUiStage != Stage.NeedToUnlock && mUiStage != Stage.NeedToUnlockWrong) {
                throw new IllegalStateException("expected ui stage "
                        + Stage.NeedToUnlock + " or " + Stage.NeedToUnlockWrong
                        + " for next button");
            }
            if (mInputMode == InputMode.Click && mInputPattern != null) {
                verifyPattern(mInputPattern, mPatternSize);
            }
        }

        /**
         * The pattern listener that responds according to a user confirming
         * an existing lock pattern.
         */
        private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener
                = new LockPatternView.OnPatternListener() {
                    public void onPatternStart(InputMode inputMode) {
                        mInputMode = inputMode;
                        mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    }

                    public void onPatternCleared() {
                        mLockPatternView.removeCallbacks(mClearPatternRunnable);
                        mInputPattern = new ArrayList<LockPatternView.Cell>();
                    }

                    public void onPatternCellAdded(List<LockPatternView.Cell> pattern,
                                                   InputMode inputMode) {
                        mInputMode = inputMode;
                        mInputPattern = pattern;
                    }

                    public void onPatternDetected(List<LockPatternView.Cell> pattern,
                                                  InputMode inputMode, byte patternSize) {
                        mInputMode = inputMode;
                        mInputPattern = pattern;
                        if (inputMode != InputMode.Click) {
                            verifyPattern(pattern, patternSize);
                        }
                    }
                };

        private void verifyPattern(List<LockPatternView.Cell> pattern, byte patternSize) {
            if (mPendingLockCheck != null || mDisappearing) {
                return;
            }

            mLockPatternView.setEnabled(false);

            final LockscreenCredential credential = LockscreenCredential.createPattern(pattern,
                    patternSize);

            if (mRemoteValidation) {
                validateGuess(credential);
                updateRemoteLockscreenValidationViews();
                return;
            }

            // TODO(b/161956762): Sanitize this
            Intent intent = new Intent();
            if (mReturnGatekeeperPassword) {
                if (isInternalActivity()) {
                    startVerifyPattern(credential, intent,
                            LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE);
                    return;
                }
            } else if (mForceVerifyPath) {
                if (isInternalActivity()) {
                    final int flags = mRequestWriteRepairModePassword
                            ? LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW : 0;
                    startVerifyPattern(credential, intent, flags);
                    return;
                }
            } else {
                startCheckPattern(credential, intent);
                return;
            }

            mCredentialCheckResultTracker.setResult(false, intent, Duration.ZERO,
                    mEffectiveUserId);
        }

        private boolean isInternalActivity() {
            return getActivity() instanceof ConfirmLockPattern.InternalActivity;
        }

        private void startVerifyPattern(final LockscreenCredential pattern,
                final Intent intent, @LockPatternUtils.VerifyFlag int flags) {
            final int localEffectiveUserId = mEffectiveUserId;
            final int localUserId = mUserId;
            final LockPatternChecker.OnVerifyCallback onVerifyCallback =
                    response -> {
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
                    ? LockPatternChecker.verifyCredential(
                    mLockPatternUtils, pattern, localUserId, flags,
                    onVerifyCallback)
                    : LockPatternChecker.verifyTiedProfileChallenge(
                            mLockPatternUtils, pattern, localUserId, flags,
                            onVerifyCallback);
        }

        private void startCheckPattern(final LockscreenCredential pattern,
                final Intent intent) {
            if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                // Pattern size is less than the minimum, do not count it as an fail attempt.
                onPatternChecked(false, intent, Duration.ZERO, mEffectiveUserId,
                        false /* newResult */);
                return;
            }

            final int localEffectiveUserId = mEffectiveUserId;
            mPendingLockCheck = LockPatternChecker.checkCredential(
                    mLockPatternUtils,
                    pattern,
                    localEffectiveUserId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onChecked(VerifyCredentialResponse response) {
                            final boolean matched = response.isMatched();
                            final Duration timeout = response.getTimeout();
                            mPendingLockCheck = null;
                            if (matched && isInternalActivity() && mReturnCredentials) {
                                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                                        pattern);
                            }
                            mCredentialCheckResultTracker.setResult(matched, intent, timeout,
                                    localEffectiveUserId);
                        }
                    });
        }

        private void onPatternChecked(boolean matched, Intent intent, Duration timeout,
                int effectiveUserId, boolean newResult) {
            mLockPatternView.setEnabled(true);
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
                    updateStage(Stage.NeedToUnlockWrong);
                    postClearPatternRunnable();
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
            mRemoteLockscreenValidationFragment.clearLockscreenCredential();
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, Duration timeout,
                int effectiveUserId, boolean newResult) {
            onPatternChecked(matched, intent, timeout, effectiveUserId, newResult);
        }

        @Override
        protected String getLastTryOverrideErrorMessageId(int userType) {
            if (userType == USER_TYPE_MANAGED_PROFILE) {
                return WORK_PROFILE_LAST_PATTERN_ATTEMPT_BEFORE_WIPE;
            }

            return UNDEFINED;
        }

        @Override
        protected int getLastTryDefaultErrorMessage(int userType) {
            switch (userType) {
                case USER_TYPE_PRIMARY:
                    return R.string.lock_last_pattern_attempt_before_wipe_device;
                case USER_TYPE_MANAGED_PROFILE:
                    return R.string.lock_last_pattern_attempt_before_wipe_profile;
                case USER_TYPE_SECONDARY:
                    return R.string.lock_last_pattern_attempt_before_wipe_user;
                default:
                    throw new IllegalArgumentException("Unrecognized user type:" + userType);
            }
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            clearResetErrorRunnable();
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    mErrorTextView.setText(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }

        @Override
        public void createAnimation(Object obj, long delay,
                long duration, float translationY, final boolean appearing,
                Interpolator interpolator,
                final Runnable finishListener) {
            if (obj instanceof LockPatternView.CellState) {
                final LockPatternView.CellState animatedCell = (LockPatternView.CellState) obj;
                mLockPatternView.startCellStateAnimation(animatedCell,
                        1f, appearing ? 1f : 0f, /* alpha */
                        appearing ? translationY : 0f, /* startTranslation */
                        appearing ? 0f : translationY, /* endTranslation */
                        appearing ? 0f : 1f, 1f /* scale */,
                        delay, duration, interpolator, finishListener);
            } else {
                mAppearAnimationUtils.createAnimation((View) obj, delay, duration, translationY,
                        appearing, interpolator, finishListener);
            }
        }

        /**
         * Callback for when the current device's lockscreen to the guess used for
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
