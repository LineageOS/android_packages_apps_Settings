/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

// LINT.IfChange
public class ColorModePreferenceController extends BasePreferenceController
        implements LifecycleObserver {

    private Preference mPreference;

    private final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            if (mPreference != null) {
                updateState(mPreference);
            }
        }
    };

    public ColorModePreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        final int[] availableColorModes = mContext.getResources().getIntArray(
                com.android.internal.R.array.config_availableColorModes);
        return mContext.getSystemService(ColorDisplayManager.class)
                .isDeviceColorManaged()
                && availableColorModes.length > 0
                && !ColorDisplayManager.areAccessibilityTransformsEnabled(mContext) ?
                AVAILABLE : DISABLED_FOR_USER;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.DISPLAY_COLOR_MODE),
                /* notifyForDescendants= */ false,
                mContentObserver);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public CharSequence getSummary() {
        return getColorModeName();
    }

    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        super.updateState(preference);
        preference.setSummary(getSummary());
    }

    @NonNull
    private String getColorModeName() {
        return ColorModeUtils.getActiveColorModeName(mContext);
    }
}
// LINT.ThenChange(ColorModeScreen.kt, ColorModeApiScreen.kt)
