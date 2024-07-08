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

package com.android.settings.biometrics.fingerprint.feature;

import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_CENTER;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_FINGERTIP;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_LEFT_EDGE;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_NO_ANIMATION;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_RIGHT_EDGE;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.STAGE_UNKNOWN;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;
import com.google.hardware.biometrics.sidefps.IFingerprintExt;

import java.util.function.Function;
import java.util.function.Supplier;

public class SfpsEnrollmentFeatureImpl implements SfpsEnrollmentFeature {
    static final String TAG = SfpsEnrollmentFeatureImpl.class.getSimpleName();

    @VisibleForTesting
    public static final int HELP_ANIMATOR_DURATION = 550;

    @Nullable
    private FingerprintManager mFingerprintManager = null;

    @Override
    public int getCurrentSfpsEnrollStage(int progressSteps, Function<Integer, Integer> mapper) {
        if (mapper == null) {
            return STAGE_UNKNOWN;
        }
        if (progressSteps < mapper.apply(0)) {
            return SFPS_STAGE_NO_ANIMATION;
        } else if (progressSteps < mapper.apply(1)) {
            return SFPS_STAGE_CENTER;
        } else if (progressSteps < mapper.apply(2)) {
            return SFPS_STAGE_FINGERTIP;
        } else if (progressSteps < mapper.apply(3)) {
            return SFPS_STAGE_LEFT_EDGE;
        } else {
            return SFPS_STAGE_RIGHT_EDGE;
        }
    }

    @Override
    public int getFeaturedStageHeaderResource(int stage) {
        return switch (stage) {
            case SFPS_STAGE_NO_ANIMATION
                    -> R.string.security_settings_fingerprint_enroll_repeat_title;
            case SFPS_STAGE_CENTER -> R.string.security_settings_sfps_enroll_finger_center_title;
            case SFPS_STAGE_FINGERTIP -> R.string.security_settings_sfps_enroll_fingertip_title;
            case SFPS_STAGE_LEFT_EDGE -> R.string.security_settings_sfps_enroll_left_edge_title;
            case SFPS_STAGE_RIGHT_EDGE -> R.string.security_settings_sfps_enroll_right_edge_title;
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        };
    }

    @Override
    public int getSfpsEnrollLottiePerStage(int stage) {
        return switch (stage) {
            case SFPS_STAGE_NO_ANIMATION -> R.raw.sfps_lottie_no_animation;
            case SFPS_STAGE_CENTER -> R.raw.sfps_lottie_pad_center;
            case SFPS_STAGE_FINGERTIP -> R.raw.sfps_lottie_tip;
            case SFPS_STAGE_LEFT_EDGE -> R.raw.sfps_lottie_left_edge;
            case SFPS_STAGE_RIGHT_EDGE -> R.raw.sfps_lottie_right_edge;
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        };
    }

    private final boolean isGoogleDevice = "google".equals(Build.BRAND);
    private float[] mEnrollStageThresholds;

    @Override
    public float getEnrollStageThreshold(@NonNull Context context, int index) {
        if (isGoogleDevice) {
            Log.d(TAG, "getEnrollStageThreshold " + index);
            if (mEnrollStageThresholds == null) {
                // TODO: extract these values automatically from SettingsGoogle resource:
                //  com.google.android.settings.R.array.config_sfps_enroll_stage_thresholds
                mEnrollStageThresholds = new float[] {
                        0f, 0.04f, 0.48f, 0.52f
                };
            }

            // this logic was copied from FingerprintManager.getEnrollStageThreshold()

            if (index < 0 || index > mEnrollStageThresholds.length) {
                Log.w(TAG, "Unsupported enroll stage index: " + index);
                return index < 0 ? 0f : 1f;
            }

            return index == mEnrollStageThresholds.length ? 1f : mEnrollStageThresholds[index];
        }

        if (mFingerprintManager == null) {
            mFingerprintManager = context.getSystemService(FingerprintManager.class);
        }
        return mFingerprintManager.getEnrollStageThreshold(index);
    }

    @Override
    public Animator getHelpAnimator(@NonNull View target) {
        final float translationX = 40;
        final ObjectAnimator help = ObjectAnimator.ofFloat(target,
                "translationX" /* propertyName */,
                0, translationX, -1 * translationX, translationX, 0f);
        help.setInterpolator(new AccelerateInterpolator());
        help.setDuration(HELP_ANIMATOR_DURATION);
        help.setAutoCancel(false);
        return help;
    }

    @Override
    public boolean shouldAdjustHeaderText(@NonNull Configuration conf, boolean isFolded) {
        return conf.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void handleOnEnrollmentHelp(int helpMsgId, CharSequence helpString, Supplier<FingerprintEnrollEnrolling> enrollingSupplier) {
        if (isGoogleDevice) {
            Log.d(TAG, "handleOnEnrollmentHelp, helpMsgId: " + helpMsgId + ", helpString: " + helpString, new Throwable());
            if (helpMsgId == BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_VENDOR_BASE ||
                    helpMsgId == BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_IMMOBILE) {
                Context ctx = enrollingSupplier.get();
                Log.d(TAG, "getVendorString: " + getVendorString(ctx, 0));

                if (getGoogleFingerprintExt() == null) {
                    return;
                }

                if (mHelpDialog != null) {
                    mHelpDialog.dismiss();
                    mHelpDialog = null;
                }

                var d = new AlertDialog.Builder(ctx);
                d.setMessage(getVendorString(ctx, VENDOR_STRING_FINGERPRINT_ACQUIRED_IMMOBILE));
                d.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    resumeEnroll();
                });
                mHelpDialog = d.show();
            }
        }
    }

    private AlertDialog mHelpDialog;

    @Override
    public CharSequence getFeaturedVendorString(Context context, int id, CharSequence msg) {
        if (!isGoogleDevice) {
            return msg;
        }

        Log.d(TAG, "getFeaturedVendorString, id: " + id + ", msg: " + msg, new Throwable());

        if (id == BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_VENDOR_BASE ||
                id == BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_IMMOBILE) {
            return getVendorString(context, VENDOR_STRING_FINGERPRINT_ACQUIRED_IMMOBILE);
        }

       return msg;
    }

    private static final int VENDOR_STRING_FINGERPRINT_ACQUIRED_IMMOBILE = 0;

    private static String getVendorString(Context ctx, int index) {
        String[] strings = ctx.getResources().getStringArray(R.array.fingerprint_acquired_vendor);
        Preconditions.checkArgumentInRange(index, 0, strings.length - 1, "vendor string index");
        return strings[index];
    }

    @Nullable
    private static IFingerprintExt getGoogleFingerprintExt() {
        String fpServiceName = android.hardware.biometrics.fingerprint.IFingerprint.class.getName() + "/default";
        IBinder fpService = ServiceManager.getService(fpServiceName);
        if (fpService == null) {
            throw new IllegalStateException(fpServiceName + " is null");
        }

        IBinder fpServiceExt;
        try {
            fpServiceExt = fpService.getExtension();
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }

        if (fpServiceExt == null) {
            Log.e(TAG, "no IFingerprintExt");
            return null;
        }

        return IFingerprintExt.Stub.asInterface(fpServiceExt);
    }

    private static void resumeEnroll() {
        IFingerprintExt fpExt = getGoogleFingerprintExt();
        if (fpExt == null) {
            return;
        }
        try {
            fpExt.resumeEnroll();
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }
}
