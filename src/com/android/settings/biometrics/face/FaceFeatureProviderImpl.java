/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceAuthenticatorsRegisteredCallback;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

public class FaceFeatureProviderImpl implements FaceFeatureProvider {
    private static final String TAG = "FaceFeatureProvider";

    private int mMaxEnrollableCount = -1;

    public FaceFeatureProviderImpl(@NonNull Context context) {
        final FaceManager faceManager = Utils.getFaceManagerOrNull(context);
        if (faceManager != null) {
            faceManager.addAuthenticatorsRegisteredCallback(
                    new IFaceAuthenticatorsRegisteredCallback.Stub() {
                        @Override
                        public void onAllAuthenticatorsRegistered(
                                @NonNull List<FaceSensorPropertiesInternal> sensors) {
                            Log.d(TAG, "onAllAuthenticatorsRegistered sensors=" + sensors);
                            if (sensors.isEmpty()) {
                                return;
                            }
                            mMaxEnrollableCount = sensors.get(0).maxEnrollmentsPerUser;
                        }
                    });
        }
    }

    /**
     * Returns the guidance page intent if device support {@link FoldingFeature}, and we want to
     * guide user enrolling faces with specific device posture.
     *
     * @param context the application context
     * @return the posture guidance intent, otherwise null if device not support
     */
    @Nullable
    @Override
    public Intent getPostureGuidanceIntent(Context context) {
        final String flattenedString = context.getString(R.string.config_face_enroll_guidance_page);
        final Intent intent;
        if (!TextUtils.isEmpty(flattenedString)) {
            ComponentName componentName = ComponentName.unflattenFromString(flattenedString);
            if (componentName != null) {
                intent = new Intent();
                intent.setComponent(componentName);
                return intent;
            }
        }
        return null;
    }

    @Override
    public boolean isAttentionSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_face_attention_supported);
    }

    @Override
    public boolean isSetupWizardSupported(@NonNull Context context) {
        return true;
    }

    @Override
    public int getMaxEnrollableCount(@NonNull Context context) {
        if (mMaxEnrollableCount == -1) {
            Log.e(TAG, "The max enrollable count is not yet initialized.");
            return 0;
        }
        return mMaxEnrollableCount;
    }

    @NonNull
    @Override
    public Class<? extends FaceEnrollParentalConsent> getParentalConsentPage() {
        return FaceEnrollParentalConsent.class;
    }

    @NonNull
    @Override
    public int[] getParentalConsentStringRes() {
        return  FaceEnrollParentalConsent.CONSENT_STRING_RESOURCES;
    }
}
