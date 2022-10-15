/*
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

import static java.lang.Thread.sleep;

import android.util.Log;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.display.DisplayDensityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Controller that switch the screen resolution. */
public class ScreenResolutionController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_RESOLUTION_SWITCH = "screen_resolution";

    private static final String TAG = "screenResolution";

    private ListPreference mListPreference;

    private final List<String> mEntries = new ArrayList<>();
    private final List<String> mValues = new ArrayList<>();

    private final Display mDisplay;

    private final DisplayDensityUtils mDisplayDensityUtils;

    public ScreenResolutionController(Context context) {
        super(context, KEY_RESOLUTION_SWITCH);

        mDisplay = Objects.requireNonNull(
                context.getSystemService(DisplayManager.class)).getDisplay(
                Display.DEFAULT_DISPLAY);
        mDisplayDensityUtils = new DisplayDensityUtils(context);

        // Find related display resolutions
        Display.Mode mode = mDisplay.getMode();
        Display.Mode[] avail_modes = mDisplay.getSupportedModes();

        for (Display.Mode m : avail_modes) {
            if (m.getRefreshRate() == mode.getRefreshRate()) {
                mEntries.add(String.format("%d x %d", m.getPhysicalWidth(), m.getPhysicalHeight()));
                mValues.add(String.format(Locale.US, "%d %d", m.getPhysicalWidth(),
                        m.getPhysicalHeight()));
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

        int index = mListPreference.findIndexOfValue(
                String.format(Locale.US, "%d %d", mode.getPhysicalWidth(),
                        mode.getPhysicalHeight()));
        if (index < 0) {
            index = 0;
        }
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String[] valueString = newValue.toString().split(" ");
        assert valueString.length == 2;
        int resolutionWidth = Integer.parseInt(valueString[0]);
        int resolutionHeight = Integer.parseInt(valueString[1]);
        int originalWidth = mDisplay.getMode().getPhysicalWidth();
        int originalDpi = mDisplayDensityUtils.getValues()[mDisplayDensityUtils.getCurrentIndex()];
        Display.Mode switchMode = new Display.Mode(resolutionWidth, resolutionHeight,
                mDisplay.getMode().getRefreshRate());
        mDisplay.setUserPreferredDisplayMode(switchMode);

        // FIXME: Dirty hack, wait some time for the new width to be updated
        try {
            sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Scale the DPI based on the following formula:
        //   px = C1 * (dpi / C2) ==> px_new / px_old = dpi_new / dpi_old
        // So we have:
        //   dpi_new = dpi_old * px_new / px_old
        int[] densityValues = mDisplayDensityUtils.getValues();
        int newWidth = mDisplay.getMode().getPhysicalWidth();
        int newDensity = (int) ((double) originalDpi * newWidth / originalWidth);

        // Find the closet DPI setting
        int minDistance = Math.abs(densityValues[0] - newDensity);
        int idx = 0;
        for (int i = 1; i < densityValues.length; i++) {
            int dist = Math.abs(densityValues[i] - newDensity);
            if (dist < minDistance) {
                minDistance = dist;
                idx = i;
            }
        }

        Log.d(TAG, "Current width " + newWidth + ", old width " + originalWidth);
        Log.d(TAG,
                "Original dpi: " + originalDpi + ", would like to change to " + newDensity
                        + " actually set to " + densityValues[idx]);
        DisplayDensityUtils.setForcedDisplayDensity(Display.DEFAULT_DISPLAY,
                densityValues[idx]);

        return true;
    }

}
