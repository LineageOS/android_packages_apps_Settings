/*
 * Copyright (C) 2022 The Android Open Source Project
 * Copyright (C) 2022 The LineageOS Project
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

import android.annotation.Nullable;
import android.util.Log;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.display.DisplayDensityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.android.settings.core.BasePreferenceController;

/** Controller that switch the screen resolution. */
public class CustomScreenResolutionController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {
    private static final String KEY_RESOLUTION_SWITCH = "custom_screen_resolution";
    private static final String TAG = "customScreenResolution";
    private static final String RESOLUTION_METRIC_SETTING_KEY = "user_selected_custom_resolution";

    private ListPreference mListPreference;

    private final List<String> mEntries = new ArrayList<>();
    private final List<String> mValues = new ArrayList<>();

    private final Display mDisplay;
    private final DisplayObserver mDisplayObserver;

    public CustomScreenResolutionController(Context context) {
        super(context, KEY_RESOLUTION_SWITCH);
        mDisplay = Objects.requireNonNull(
                context.getSystemService(DisplayManager.class)).getDisplay(Display.DEFAULT_DISPLAY);

        mDisplayObserver = new DisplayObserver(context);

        // Find related display resolutions
        Display.Mode mode = mDisplay.getMode();
        Display.Mode[] avail_modes = mDisplay.getSupportedModes();

        if (context.getResources().getBoolean(R.bool.config_show_custom_screen_resolution_switch)) {
            for (Display.Mode m : avail_modes) {
                if (m.getRefreshRate() == mode.getRefreshRate()) {
                    mEntries.add(
                            String.format("%d x %d", m.getPhysicalWidth(), m.getPhysicalHeight()));
                    mValues.add(String.format("%d %d", m.getPhysicalWidth(),
                            m.getPhysicalHeight()));
                }
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mEntries.size() > 1 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RESOLUTION_SWITCH;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mListPreference = screen.findPreference(getPreferenceKey());
        assert mListPreference != null;
        mListPreference.setEntries(mEntries.toArray(new String[0]));
        mListPreference.setEntryValues(mValues.toArray(new String[0]));

        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        Display.Mode mode = mDisplay.getMode();

        int index = Math.max(0, mListPreference.findIndexOfValue(
                String.format("%d %d", mode.getPhysicalWidth(), mode.getPhysicalHeight())));
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mDisplayObserver.startObserve();

        // Set display resolution here
        String[] valueString = newValue.toString().split(" ");
        assert valueString.length == 2;
        int resolutionWidth = Integer.parseInt(valueString[0]);
        int resolutionHeight = Integer.parseInt(valueString[1]);
        Display.Mode switchMode = new Display.Mode(resolutionWidth, resolutionHeight,
                mDisplay.getMode().getRefreshRate());

        /** For store settings globally. */
        /** TODO(b/238061217): Moving to an atom with the same string */
        Settings.System.putString(
                mContext.getContentResolver(),
                RESOLUTION_METRIC_SETTING_KEY,
                switchMode.getPhysicalWidth()
                        + "x"
                        + switchMode.getPhysicalHeight());

        mDisplay.setUserPreferredDisplayMode(switchMode);
        return true;
    }

    private static final class DisplayObserver implements DisplayManager.DisplayListener {
        @Nullable
        private final Context mContext;
        private final AtomicInteger mOldDensity = new AtomicInteger(-1);
        private final AtomicInteger mPreviousWidth = new AtomicInteger(-1);

        DisplayObserver(Context context) {
            mContext = context;
        }

        public void startObserve() {
            // Here, we record the initial screen density and width for future use
            if (mContext == null) {
                return;
            }
            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            mOldDensity.set(density.getValues()[density.getCurrentIndex()]);
            mPreviousWidth.set(getCurrentWidth());

            // Register itself as a display listener
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            assert dm != null;
            dm.registerDisplayListener(this, null);
        }

        public void stopObserve() {
            if (mContext == null) {
                return;
            }

            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            assert dm != null;
            dm.unregisterDisplayListener(this);
        }

        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                return;
            }
            if (!isResolutionChangeApplied(displayId)) {
                return;
            }
            restoreDensity();

            stopObserve();
        }

        public void restoreDensity() {
            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);

            // Get available density values and current resolution width
            int[] densityValues = density.getValues();
            int newWidth = getCurrentWidth();

            // Scale the DPI based on the following formula:
            //   px = C1 * (dpi / C2) ==> px_new / px_old = dpi_new / dpi_old
            // So we have:
            //   dpi_new = dpi_old * px_new / px_old
            int newDensity = (int) ((double) mOldDensity.get() * newWidth / mPreviousWidth.get());

            // Find the closet dpi available in density values
            int minDistance = Math.abs(densityValues[0] - newDensity);
            int idx = 0;
            for (int i = 1; i < densityValues.length; i++) {
                int dist = Math.abs(densityValues[i] - newDensity);
                if (dist < minDistance) {
                    minDistance = dist;
                    idx = i;
                }
            }
            Log.d(TAG, "Current width " + newWidth + ", old width " + mPreviousWidth.get());
            Log.d(TAG,
                    "Original dpi: " + mOldDensity + ", would like to change to " + newDensity
                            + " actually set to " + densityValues[idx]);
            DisplayDensityUtils.setForcedDisplayDensity(Display.DEFAULT_DISPLAY,
                    densityValues[idx]);

            // Update values
            mPreviousWidth.set(newWidth);
            mOldDensity.set(densityValues[idx]);
        }

        public boolean isResolutionChangeApplied(int displayId) {
            return mPreviousWidth.get() != getCurrentWidth();
        }

        // Get the width of current resolution
        private int getCurrentWidth() {
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            assert dm != null;
            return dm.getDisplay(Display.DEFAULT_DISPLAY).getMode().getPhysicalWidth();
        }
    }
}


