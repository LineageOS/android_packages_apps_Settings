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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controller that switch the screen resolution. */
public class ScreenResolutionController extends BasePreferenceController {

    private Display mDisplay;

    private int[] mScreenWidthOptions;

    private String[] mScreenResolutionSummaries;

    public ScreenResolutionController(Context context, String key) {
        super(context, key);

        mDisplay =
                mContext.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);

        mScreenResolutionSummaries =
                context.getResources().getStringArray(
                        R.array.config_screen_resolution_summaries_strings);

        mScreenWidthOptions = context.getResources().getIntArray(
                R.array.config_screen_resolution_widths);
    }

    /** Check if the width is supported by the display. */
    private boolean isSupportedMode(int width) {
        for (Display.Mode mode : getSupportedModes()) {
            if (mode.getPhysicalWidth() == width) return true;
        }
        return false;
    }

    /** Return true if the device contains two (or more) resolutions. */
    protected boolean checkSupportedResolutions() {
        boolean resolution_supported =
                mScreenWidthOptions != null && mScreenWidthOptions.length > 1;

        if (!resolution_supported) {
            return false;
        }

        // All modes defined in config_screen_resolution_widths should be supported by system
        for (int screenWidthOption : mScreenWidthOptions) {
            resolution_supported = resolution_supported && isSupportedMode(screenWidthOption);
        }

        return resolution_supported;
    }

    @Override
    public int getAvailabilityStatus() {
        return (checkSupportedResolutions()) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        String summary = null;
        int disp_width = getDisplayWidth();

        for (int i = 0; i < mScreenWidthOptions.length; i++) {
            if (disp_width == mScreenWidthOptions[i]) {
                summary = mScreenResolutionSummaries[i];
            }
        }

        return summary;
    }

    @VisibleForTesting
    public int getDisplayWidth() {
        return mDisplay.getMode().getPhysicalWidth();
    }

    @VisibleForTesting
    public Display.Mode[] getSupportedModes() {
        return mDisplay.getSupportedModes();
    }
}
