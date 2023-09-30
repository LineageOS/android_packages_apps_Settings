/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.gestures;

import android.annotation.UserIdInt;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

public class ScreenOffUdfpsPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "gesture_screen_off_udfps_video";

    private static final String SECURE_KEY = "screen_off_udfps_enabled";

    private AmbientDisplayConfiguration mAmbientConfig;
    @UserIdInt
    private final int mUserId;

    public ScreenOffUdfpsPreferenceController(Context context, String key) {
        super(context, key);
        mUserId = UserHandle.myUserId();
    }

    public ScreenOffUdfpsPreferenceController setConfig(AmbientDisplayConfiguration config) {
        mAmbientConfig = config;
        return this;
    }

    private static boolean screenOffUdfpsAvailable(AmbientDisplayConfiguration config) {
        return !TextUtils.isEmpty(config.udfpsLongPressSensorType());
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return isSuggestionComplete(new AmbientDisplayConfiguration(context), prefs);
    }

    @VisibleForTesting
    static boolean isSuggestionComplete(AmbientDisplayConfiguration config,
            SharedPreferences prefs) {
        return !screenOffUdfpsAvailable(config)
                || prefs.getBoolean(ScreenOffUdfpsSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    @Override
    public int getAvailabilityStatus() {
        // No hardware support for Screen-Off UDFPS
        if (!screenOffUdfpsAvailable(getAmbientConfig())) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_screen_off_udfps");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().screenOffUdfpsEnabled(mUserId);
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mAmbientConfig == null) {
            mAmbientConfig = new AmbientDisplayConfiguration(mContext);
        }
        return mAmbientConfig;
    }
}
