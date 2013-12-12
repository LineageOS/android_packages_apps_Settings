/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.flags.Flags;
import com.android.settingslib.widget.theme.R.style;

import com.google.android.material.button.MaterialButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Setup Wizard's version of ChooseLockPattern screen. It inherits the logic and basic structure
 * from ChooseLockPattern class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPattern class instead and let this class inherit
 * those changes.
 */
public class SetupChooseLockPattern extends ChooseLockPattern {

    public static Intent modifyIntentForSetup(Context context, Intent chooseLockPatternIntent) {
        chooseLockPatternIntent.setClass(context, ChooseLockPatternSize.class);
        chooseLockPatternIntent.putExtra("className", SetupChooseLockPattern.class.getName());
        chooseLockPatternIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_USE_EXPRESSIVE_STYLE,
                ThemeHelper.shouldApplyGlifExpressiveStyle(context));
        return chooseLockPatternIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return SetupChooseLockPatternFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show generic pattern title when pattern lock screen launch in Setup wizard flow before
        // fingerprint and face setup.
        setTitle(R.string.lockpassword_choose_your_pattern_header);
    }

    public static class SetupChooseLockPatternFragment extends ChooseLockPatternFragment
            implements ChooseLockTypeDialogFragment.OnLockTypeSelectedListener {

        private static final String TAG_SKIP_SCREEN_LOCK_DIALOG = "skip_screen_lock_dialog";

        @Nullable
        private Button mOptionsButton;
        private boolean mLeftButtonIsSkip;

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            final boolean isExpressiveStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(
                    getContext());
            if (!getResources().getBoolean(R.bool.config_lock_pattern_minimal_ui)) {
                if (isExpressiveStyle) {
                    mOptionsButton = new MaterialButton(new ContextThemeWrapper(getActivity(),
                            style.SettingslibTextButtonStyle_Expressive));
                } else {
                    mOptionsButton = new Button(new ContextThemeWrapper(getActivity(),
                            com.google.android.setupdesign.R.style.SudGlifButton_Tertiary));
                }
                mOptionsButton.setId(R.id.screen_lock_options);
                PasswordUtils.setupScreenLockOptionsButton(getActivity(), view, mOptionsButton,
                        isExpressiveStyle);
                mOptionsButton.setOnClickListener((btn) ->
                        ChooseLockTypeDialogFragment.newInstance(mUserId)
                                .show(getChildFragmentManager(), TAG_SKIP_SCREEN_LOCK_DIALOG));
            }
            // Show the skip button during SUW but not during Settings > Biometric Enrollment
            mSkipOrClearButton.setOnClickListener(this::onSkipOrClearButtonClick);
            return view;
        }

        @Override
        protected void onSkipOrClearButtonClick(View view) {
            if (mLeftButtonIsSkip) {
                if (shouldHideSkipButton()) {
                    throw new IllegalStateException("skip button pressed despite being hidden");
                }
                final Intent intent = getActivity().getIntent();
                final boolean frpSupported = intent
                        .getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false);
                final boolean forFingerprint = intent
                        .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
                final boolean forFace = intent
                        .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, false);
                final boolean forBiometrics = intent
                        .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, false);
                final boolean isExpressiveStyle = intent
                        .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_USE_EXPRESSIVE_STYLE,
                                false);
                final SetupSkipDialog dialog = SetupSkipDialog.newInstance(
                        CREDENTIAL_TYPE_PATTERN,
                        frpSupported,
                        forFingerprint,
                        forFace,
                        forBiometrics,
                        WizardManagerHelper.isAnySetupWizard(intent),
                        isExpressiveStyle);
                dialog.show(getFragmentManager());
                return;
            }
            super.onSkipOrClearButtonClick(view);
        }

        @Override
        public void onLockTypeSelected(ScreenLockType lock) {
            if (ScreenLockType.PATTERN == lock) {
                return;
            }
            startChooseLockActivity(lock, getActivity());
        }

        private boolean showMinimalUi() {
            return getResources().getBoolean(R.bool.config_lock_pattern_minimal_ui);
        }

        @Override
        protected void updateStage(Stage stage) {
            super.updateStage(stage);
            if (!showMinimalUi() && mOptionsButton != null) {
                mOptionsButton.setVisibility(
                        (stage == Stage.Introduction || stage == Stage.FirstInputInProgress
                                || stage == Stage.HelpScreen || stage == Stage.ChoiceTooShort
                                || stage == Stage.FirstChoiceValid)
                                ? View.VISIBLE : View.INVISIBLE);
            }

            if (stage.leftMode == LeftButtonMode.Gone && stage == Stage.Introduction) {
                if (!shouldHideSkipButton()) {
                    // Only show the skip button if LSKF enforcement is disabled
                    mSkipOrClearButton.setVisibility(View.VISIBLE);
                }
                mSkipOrClearButton.setText(getActivity(), R.string.skip_label);
                mLeftButtonIsSkip = true;
            } else {
                mLeftButtonIsSkip = false;
            }
        }

        private boolean shouldHideSkipButton() {
            return Flags.hideLskfSkipDuringSuw()
                    && getResources().getBoolean(R.bool.config_hide_skip_security_options_in_suw);
        }

        @Override
        protected boolean shouldShowGenericTitle() {
            return true;
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            // Setup wizard's redaction interstitial is deferred to optional step. Enable that
            // optional step if the lock screen was set up.
            SetupRedactionInterstitial.setEnabled(context, true);
            return null;
        }
    }
}
