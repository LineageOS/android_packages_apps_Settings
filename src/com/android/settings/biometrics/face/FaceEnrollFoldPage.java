/*
 * Copyright (C) 2026 The LineageOS Project
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

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;

import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.RuleController;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settingslib.widget.LottieColorUtils;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.HashSet;
import java.util.Set;

/**
 * Guidance page shown before face enrollment on devices that only allow enrollment in a specific
 * posture (for example a foldable that has to be closed). It listens for fold changes and finishes
 * with {@link #RESULT_FINISHED} once the device reaches an allowed posture.
 *
 * <p>This activity is only launched when {@code config_face_enroll_guidance_page} points to it. The
 * allowed posture is read from {@code config_face_enroll_supported_posture}.
 */
public class FaceEnrollFoldPage extends BiometricEnrollBase implements FoldProvider.FoldCallback {

    private static final String EXTRA_POSTURE_STATE = "posture_state";
    private static final long POSTURE_GUIDANCE_TIMEOUT_MS = 60000L;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTimeoutRunnable = () -> onSkipButtonClick(null);

    private LottieAnimationView mIllustrationLottie;
    private int mOrientation;
    private boolean mKeepScreenOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mDevicePostureState = savedInstanceState.getInt(EXTRA_POSTURE_STATE);
        }

        // Always show the guidance fullscreen, even on large screens or in a split.
        final Set<ActivityFilter> activityFilters = new HashSet<>();
        activityFilters.add(new ActivityFilter(
                new ComponentName(this, FaceEnrollFoldPage.class), null /* intentAction */));
        RuleController.getInstance(this).addRule(
                new ActivityRule.Builder(activityFilters).setAlwaysExpand(true).build());

        BiometricUtils.setDevicePosturesAllowEnroll(
                getResources().getInteger(R.integer.config_face_enroll_supported_posture));

        relayout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_POSTURE_STATE, mDevicePostureState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BiometricUtils.isPostureAllowEnrollment(mDevicePostureState)) {
            onFinishPostureGuidance();
        } else {
            setupPostureChangeListener();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setKeepScreenOn(true);
        mHandler.postDelayed(mTimeoutRunnable, POSTURE_GUIDANCE_TIMEOUT_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setKeepScreenOn(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mScreenSizeFoldProvider != null) {
            mScreenSizeFoldProvider.onConfigurationChange(newConfig);
        }
        if (newConfig.orientation != mOrientation) {
            relayout();
        }
    }

    @Override
    public void onFoldUpdated(boolean isFolded) {
        mHandler.removeCallbacks(mTimeoutRunnable);
        mDevicePostureState = isFolded
                ? BiometricUtils.DEVICE_POSTURE_CLOSED : BiometricUtils.DEVICE_POSTURE_OPENED;
        if (BiometricUtils.isPostureAllowEnrollment(mDevicePostureState)) {
            onFinishPostureGuidance();
        }
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        // The page manages its own timeout, so it should not finish when it is backgrounded.
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_INTRO;
    }

    private void setupPostureChangeListener() {
        if (mScreenSizeFoldProvider == null) {
            mScreenSizeFoldProvider = new ScreenSizeFoldProvider(getApplicationContext());
            mFoldCallback = this;
            mScreenSizeFoldProvider.registerCallback(mFoldCallback, getMainExecutor());
        }
    }

    private void onFinishPostureGuidance() {
        if (isFinishing()) {
            return;
        }
        setResult(RESULT_FINISHED);
        onRemoveCallbacksAndFinish();
    }

    private void onRemoveCallbacksAndFinish() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        finish();
        overridePendingTransition(0 /* enterAnim */, 0 /* exitAnim */);
    }

    private void onSkipButtonClick(View view) {
        setResult(RESULT_SKIP, getIntent());
        onRemoveCallbacksAndFinish();
    }

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn == mKeepScreenOn) {
            return;
        }
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        mKeepScreenOn = keepScreenOn;
    }

    private void relayout() {
        mOrientation = getResources().getConfiguration().orientation;
        setContentView(R.layout.face_enroll_fold_page);

        final GlifLayout layout = getLayout();
        setHeaderText(R.string.face_enrolling_close_to_continue);
        setDescriptionText(R.string.face_enrolling_close_to_continue_description);

        mIllustrationLottie = findViewById(R.id.illustration_lottie);
        LottieColorUtils.applyDynamicColors(getApplicationContext(), mIllustrationLottie);
        mIllustrationLottie.setAnimation(R.raw.face_posture_guidance_lottie);
        mIllustrationLottie.setVisibility(View.VISIBLE);
        mIllustrationLottie.playAnimation();

        mFooterBarMixin = layout.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(new FooterButton.Builder(this)
                .setText(R.string.face_enrolling_do_it_later)
                .setListener(this::onSkipButtonClick)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build());
    }
}
