/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_action_label_pause;
import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_action_label_resume;
import static com.android.settings.biometrics.BiometricUtils.isPostureAllowEnrollment;
import static com.android.settings.biometrics.BiometricUtils.isPostureGuidanceShowing;

import android.animation.Animator;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.BiometricsOnboardingProto;
import com.android.settings.biometrics.metrics.BiometricsLogger;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.SetupSkipDialog;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.DelightHelper;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.LottieAnimationHelper;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.android.setupdesign.view.IllustrationVideoView;

import java.util.Arrays;
import java.util.List;

/**
 * Provides animated education for users to know how to enroll a face with appropriate posture.
 */
public class FaceEnrollEducation extends BiometricEnrollBase {
    private static final String TAG = "FaceEducation";

    private FaceManager mFaceManager;
    private FaceEnrollAccessibilityToggle mSwitchDiversity;
    private boolean mIsUsingLottie;
    private IllustrationVideoView mIllustrationDefault;
    private LottieAnimationView mIllustrationLottie;
    private View mIllustrationAccessibility;
    private Intent mResultIntent;
    private boolean mAccessibilityEnabled;
    protected Intent mExtraInfoIntent;

    private boolean mIsUsingExpressiveStyle;

    private final CompoundButton.OnCheckedChangeListener mSwitchDiversityListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    final int descriptionRes = isChecked
                            ? R.string.security_settings_face_enroll_education_message_accessibility
                            : R.string.security_settings_face_enroll_education_message;
                    setDescriptionText(descriptionRes);

                    if (isChecked) {
                        hideDefaultIllustration();
                        mIllustrationAccessibility.setVisibility(View.VISIBLE);
                    } else {
                        showDefaultIllustration();
                        mIllustrationAccessibility.setVisibility(View.INVISIBLE);
                        adjustIllustrationLottiePosition();
                    }
                }
            };

    final View.OnLayoutChangeListener mSwitchDiversityOnLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (oldBottom == 0 && bottom != 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        final ScrollView scrollView =
                                findViewById(com.google.android.setupdesign.R.id.sud_scroll_view);
                        if (scrollView != null) {
                            scrollView.fullScroll(View.FOCUS_DOWN); // scroll down
                        }
                        if (mSwitchDiversity != null) {
                            mSwitchDiversity.removeOnLayoutChangeListener(
                                    this.mSwitchDiversityOnLayoutChangeListener);
                        }
                    });
                }
            };

    private final Animator.AnimatorListener mA11yUpdater = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(@NonNull Animator animation) {}

        @Override
        public void onAnimationEnd(@NonNull Animator animation) {
            forceConfigureA11yDelegate(false);
        }

        @Override
        public void onAnimationCancel(@NonNull Animator animation) {}

        @Override
        public void onAnimationRepeat(@NonNull Animator animation) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.face_enroll_education);

        setTitle(R.string.security_settings_face_enroll_education_title);
        setDescriptionText(R.string.security_settings_face_enroll_education_message);

        mFaceManager = Utils.getFaceManagerOrNull(this);

        mIllustrationDefault = findViewById(R.id.illustration_default);
        mIllustrationLottie = findViewById(R.id.illustration_lottie);
        mIllustrationAccessibility = findViewById(R.id.illustration_accessibility);
        mIsUsingExpressiveStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(
                getApplicationContext());
        if (mIsUsingExpressiveStyle && mIllustrationAccessibility instanceof ImageView) {
            ((ImageView) mIllustrationAccessibility).setImageResource(
                    R.drawable.face_enroll_icon_large_expressive);
        }

        mIsUsingLottie = getResources().getBoolean(R.bool.config_face_education_use_lottie);
        if (mIsUsingLottie) {
            mIllustrationDefault.stop();
            mIllustrationDefault.setVisibility(View.INVISIBLE);
            mIllustrationLottie.setAnimation(mIsUsingExpressiveStyle
                    ? R.raw.face_education_lottie_expressive : R.raw.face_education_lottie);
            if (mIsUsingExpressiveStyle) {
                setupllIllustrationAnim(mIllustrationLottie);
            }
            mIllustrationLottie.setVisibility(View.VISIBLE);

            mIllustrationLottie.addAnimatorListener(mA11yUpdater);
            configureA11yDelegate(true);
            playWithDelightAnimationIcon(mIllustrationLottie);

            mIllustrationLottie.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIllustrationLottie.isAnimating()) {
                        mIllustrationLottie.pauseAnimation();
                    } else {
                        mIllustrationLottie.resumeAnimation();
                    }
                }
            });
        }

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);

        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            mFooterBarMixin.setSecondaryButton(
                    new FooterButton.Builder(this)
                            .setText(R.string.skip_label)
                            .setListener(this::onSkipButtonClick)
                            .setButtonType(FooterButton.ButtonType.SKIP)
                            .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                            .build()
            );
        } else {
            mFooterBarMixin.setSecondaryButton(
                    new FooterButton.Builder(this)
                            .setText(R.string.security_settings_face_enroll_introduction_cancel)
                            .setListener(this::onSkipButtonClick)
                            .setButtonType(FooterButton.ButtonType.CANCEL)
                            .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                            .build()
            );
        }

        final FooterButton footerButton = new FooterButton.Builder(this)
                .setText(R.string.security_settings_face_enroll_education_start)
                .setListener(this::onNextButtonClick)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build();

        final AccessibilityManager accessibilityManager = getApplicationContext().getSystemService(
                AccessibilityManager.class);
        if (accessibilityManager != null) {
            // Add additional check for touch exploration. This prevents other accessibility
            // features such as Live Transcribe from defaulting to the accessibility setup.
            mAccessibilityEnabled = accessibilityManager.isEnabled()
                    && accessibilityManager.isTouchExplorationEnabled();
        }
        mFooterBarMixin.setPrimaryButton(footerButton);

        final Button accessibilityButton = getAccessibilityButton();
        accessibilityButton.setOnClickListener(this::onAccessibilityButtonClicked);

        mSwitchDiversity = findViewById(R.id.toggle_diversity);
        mSwitchDiversity.setListener(mSwitchDiversityListener);
        mSwitchDiversity.setOnClickListener(v -> {
            updateOnboardingScreenInfoActions(
                    mSwitchDiversity.isChecked()
                            ? BiometricsOnboardingProto.OnboardingAction.ACTION_FACE_A11Y_ON_VALUE
                            : BiometricsOnboardingProto.OnboardingAction.ACTION_FACE_A11Y_OFF_VALUE
            );
            mSwitchDiversity.getSwitch().toggle();
        });
        if (mIsUsingExpressiveStyle) {
            final MaterialSwitch switchButton = (MaterialSwitch) mSwitchDiversity.getSwitch();
            switchButton.setThumbIconDrawable(switchButton.getContext().getDrawable(
                    com.android.settingslib.widget.theme.R.drawable
                            .settingslib_expressive_switch_thumb_icon));
        }

        if (mAccessibilityEnabled) {
            accessibilityButton.callOnClick();
        }
    }

    private void configureA11yDelegate(boolean isAnimating) {
        mIllustrationLottie.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                    @NonNull AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);

                // Not speak "Image" for [LottieAnimationView] in a11y mode
                info.setClassName(null);

                AccessibilityNodeInfo.AccessibilityAction clickAction =
                        new AccessibilityNodeInfo.AccessibilityAction(
                                AccessibilityNodeInfo.ACTION_CLICK,
                                getString(isAnimating
                                        ? settingslib_action_label_pause
                                        : settingslib_action_label_resume)
                        );
                info.addAction(clickAction);
            }
        });
    }

    private void forceConfigureA11yDelegate(boolean isAnimating) {
        // Update delegate to read correct text based on latest animating state
        configureA11yDelegate(isAnimating);
        // Trigger the accessibility service to re-create AccessibilityNode
        mIllustrationLottie.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getPostureGuidanceIntent() == null) {
            Log.d(TAG, "Device do not support posture guidance");
            return;
        }

        BiometricUtils.setDevicePosturesAllowEnroll(
                getResources().getInteger(R.integer.config_face_enroll_supported_posture));

        if (getPostureCallback() == null) {
            mFoldCallback = isFolded -> {
                mDevicePostureState = isFolded ? BiometricUtils.DEVICE_POSTURE_CLOSED
                        : BiometricUtils.DEVICE_POSTURE_OPENED;
                if (BiometricUtils.shouldShowPostureGuidance(mDevicePostureState,
                        mLaunchedPostureGuidance) && !mNextLaunched) {
                    launchPostureGuidance();
                }
            };
        }

        if (mScreenSizeFoldProvider == null) {
            mScreenSizeFoldProvider = new ScreenSizeFoldProvider(getApplicationContext());
            mScreenSizeFoldProvider.registerCallback(mFoldCallback, getMainExecutor());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchDiversityListener.onCheckedChanged(mSwitchDiversity.getSwitch(),
                mSwitchDiversity.isChecked());

        // If the user goes back after enrollment, we should send them back to the intro page
        // if they've met the max limit.
        final int max = getResources().getInteger(
                com.android.internal.R.integer.config_faceMaxTemplatesPerUser);
        final int numEnrolledFaces = mFaceManager.getEnrolledFaces(mUserId).size();
        if (numEnrolledFaces >= max) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mIllustrationLottie != null && mIsUsingLottie) {
            mIllustrationLottie.removeAnimatorListener(mA11yUpdater);
            mIllustrationLottie.setAccessibilityDelegate(null);
            mIllustrationLottie.setOnClickListener(null);
        }
        super.onDestroy();
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        return super.shouldFinishWhenBackgrounded() && !mNextLaunched
                && !isPostureGuidanceShowing(mDevicePostureState, mLaunchedPostureGuidance);
    }

    @Override
    protected void onNextButtonClick(View view) {
        final Intent intent = new Intent();
        if (mToken != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        }
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        intent.putExtra(EXTRA_KEY_CHALLENGE, mChallenge);
        intent.putExtra(EXTRA_KEY_SENSOR_ID, mSensorId);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, mFromSettingsSummary);
        BiometricUtils.copyMultiBiometricExtras(getIntent(), intent);
        final String flattenedString = getString(R.string.config_face_enroll);
        if (!TextUtils.isEmpty(flattenedString)) {
            ComponentName componentName = ComponentName.unflattenFromString(flattenedString);
            intent.setComponent(componentName);
        } else {
            intent.setClass(this, FaceEnrollEnrolling.class);
        }
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        if (mResultIntent != null) {
            intent.putExtras(mResultIntent);
        }
        if (mExtraInfoIntent != null) {
            intent.putExtras(mExtraInfoIntent);
        }

        intent.putExtra(EXTRA_KEY_REQUIRE_DIVERSITY, !mSwitchDiversity.isChecked());
        intent.putExtra(BiometricUtils.EXTRA_ENROLL_REASON,
                getIntent().getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1));

        updateOnboardingScreenInfoActions(
                BiometricsOnboardingProto.OnboardingAction.ACTION_NEXT_VALUE);
        if (mOnboardingEvent != null && mBiometricsLogger != null) {
            addScreenInfoToEvent();
            intent.putExtra(
                    BiometricsLogger.EXTRA_BIOMETRICS_ONBOARDING_EVENT_BYTES,
                    mBiometricsLogger.eventToMessageByteArray(mOnboardingEvent)
            );
        }

        if (!mSwitchDiversity.isChecked() && mAccessibilityEnabled) {
            FaceEnrollAccessibilityDialog dialog = FaceEnrollAccessibilityDialog.newInstance();
            dialog.setPositiveButtonListener((dialog1, which) -> {
                startActivityForResult(intent, BIOMETRIC_FIND_SENSOR_REQUEST);
                mNextLaunched = true;
            });
            dialog.show(getSupportFragmentManager(), FaceEnrollAccessibilityDialog.class.getName());
        } else {
            startActivityForResult(intent, BIOMETRIC_FIND_SENSOR_REQUEST);
            mNextLaunched = true;
        }

    }

    public boolean isAccessibilityEnabled() {
        return mAccessibilityEnabled;
    }

    private Button getAccessibilityButton() {
        final Button a11yButton = findViewById(R.id.accessibility_button);
        final Button a11yButtonExpressive = findViewById(R.id.accessibility_button_expressive);
        a11yButton.setVisibility(mIsUsingExpressiveStyle ? View.GONE : View.VISIBLE);
        a11yButtonExpressive.setVisibility(mIsUsingExpressiveStyle ? View.VISIBLE : View.GONE);

        return mIsUsingExpressiveStyle ? a11yButtonExpressive : a11yButton;
    }
    private void setupllIllustrationAnim(LottieAnimationView illustrationLottie) {
        String[] colorArray = getResources().getStringArray(R.array.face_education_illustration);
        List<String> colorMappings = Arrays.asList(colorArray);
        LottieAnimationHelper.get().applyColor(getApplicationContext(), illustrationLottie,
                colorMappings);
    }

    @VisibleForTesting
    public void adjustIllustrationLottiePosition() {
        final GlifLayout glifLayout = findViewById(R.id.setup_wizard_layout);
        final TextView descView =  glifLayout.getDescriptionTextView();
        Rect rectDescView = new Rect();
        descView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        descView.getGlobalVisibleRect(rectDescView);
                        final int descBottomPos = getOnScreenPositionTop(descView)
                                + descView.getHeight();
                        final int illustrationLottieTop = getOnScreenPositionTop(
                                mIllustrationLottie);
                        if (illustrationLottieTop < descBottomPos) {
                            final int posDiff = descBottomPos - illustrationLottieTop;
                            FrameLayout.LayoutParams layoutParams =
                                    (FrameLayout.LayoutParams)
                                            mIllustrationLottie.getLayoutParams();
                            layoutParams.topMargin += posDiff;
                            mIllustrationLottie.setLayoutParams(layoutParams);
                            mIllustrationLottie.requestLayout();
                        }

                        descView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    @VisibleForTesting
    public int getOnScreenPositionTop(@Nullable View view) {
        if (view == null) {
            return 0;
        }
        int [] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1];
    }

    protected void onAccessibilityButtonClicked(View view) {
        mSwitchDiversity.setChecked(true);
        view.setVisibility(View.GONE);
        mSwitchDiversity.setVisibility(View.VISIBLE);
        mSwitchDiversity.addOnLayoutChangeListener(mSwitchDiversityOnLayoutChangeListener);
        adjustIllustrationLottiePosition();
    }

    protected void onSkipButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST,
                "edu_skip")) {
            updateOnboardingScreenInfoActions(
                    BiometricsOnboardingProto.OnboardingAction.ACTION_SKIP_VALUE);
            setResult(RESULT_SKIP, newResultIntent());
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mScreenSizeFoldProvider != null && getPostureCallback() != null) {
            mScreenSizeFoldProvider.onConfigurationChange(newConfig);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_POSTURE_GUIDANCE) {
            mLaunchedPostureGuidance = false;
            if (resultCode == RESULT_CANCELED || resultCode == RESULT_SKIP) {
                onSkipButtonClick(getCurrentFocus());
            }
            return;
        }
        mResultIntent = data;
        boolean hasEnrolledFace = false;
        if (data != null) {
            hasEnrolledFace = data.getBooleanExtra(EXTRA_FINISHED_ENROLL_FACE, false);
        }
        if (resultCode == RESULT_TIMEOUT || !isPostureAllowEnrollment(mDevicePostureState)) {
            setResult(resultCode, data);
            finish();
        } else if (requestCode == BIOMETRIC_FIND_SENSOR_REQUEST
                || requestCode == ENROLL_NEXT_BIOMETRIC_REQUEST) {
            // If the user finished or skipped enrollment, finish this activity
            if (resultCode == RESULT_SKIP || resultCode == RESULT_FINISHED
                    || resultCode == SetupSkipDialog.RESULT_SKIP || hasEnrolledFace) {
                setResult(resultCode, data);
                finish();
            }
        }
        mNextLaunched = false;
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getOnboardingScreen() {
        return BiometricsOnboardingProto.OnboardingScreen.SCREEN_EDUCATION_VALUE;
    }

    @VisibleForTesting
    @Nullable
    protected Intent getPostureGuidanceIntent() {
        return mPostureGuidanceIntent;
    }

    @VisibleForTesting
    @Nullable
    protected FoldProvider.FoldCallback getPostureCallback() {
        return mFoldCallback;
    }

    @VisibleForTesting
    @BiometricUtils.DevicePostureInt
    protected int getDevicePostureState() {
        return mDevicePostureState;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_INTRO;
    }


    @VisibleForTesting
    void playWithDelightAnimationIcon(@Nullable LottieAnimationView lottieView) {
        if (lottieView == null) {
            return;
        }

        if (DelightHelper.shouldApplyAnimatedIcon(this)) {
            lottieView.cancelAnimation();
            lottieView.postDelayed(lottieView::playAnimation, getResources().getInteger(
                    com.google.android.setupdesign.R.integer.sud_lottie_animation_delay_ms)
            );
        } else {
            lottieView.playAnimation();
        }
    }

    private void hideDefaultIllustration() {
        if (mIsUsingLottie) {
            forceConfigureA11yDelegate(false);
            mIllustrationLottie.cancelAnimation();
            mIllustrationLottie.setVisibility(View.INVISIBLE);
        } else {
            mIllustrationDefault.stop();
            mIllustrationDefault.setVisibility(View.INVISIBLE);
        }
    }

    private void showDefaultIllustration() {
        if (mIsUsingLottie) {
            mIllustrationLottie.setAnimation(mIsUsingExpressiveStyle
                    ? R.raw.face_education_lottie_expressive : R.raw.face_education_lottie);
            if (mIsUsingExpressiveStyle) {
                setupllIllustrationAnim(mIllustrationLottie);
            }
            mIllustrationLottie.setVisibility(View.VISIBLE);
            forceConfigureA11yDelegate(true);
            mIllustrationLottie.setProgress(0f);
            playWithDelightAnimationIcon(mIllustrationLottie);
        } else {
            mIllustrationDefault.setVisibility(View.VISIBLE);
            mIllustrationDefault.start();
        }
    }

    @VisibleForTesting
    boolean isUsingExpressiveStyle() {
        return mIsUsingExpressiveStyle;
    }
}
