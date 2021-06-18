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

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;

public class FaceSettingsLockscreenUnlockMethodPreferenceController
        extends BasePreferenceController {

    protected FaceManager mFaceManager;
    private UserManager mUserManager;

    public FaceSettingsLockscreenUnlockMethodPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            mFaceManager = context.getSystemService(FaceManager.class);
        }

        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mUserManager.isManagedProfile(UserHandle.myUserId())) {
            return UNSUPPORTED_ON_DEVICE;
        }

        boolean faceAuthOnlyOnSecurityView  = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthOnlyOnSecurityView);

        if (mFaceManager != null && mFaceManager.isHardwareDetected() &&
                !faceAuthOnlyOnSecurityView) {
            return mFaceManager.hasEnrolledTemplates(UserHandle.myUserId())
                    ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }
}