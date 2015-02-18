/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import org.cyanogenmod.hardware.AdaptiveBacklight;
import org.cyanogenmod.hardware.ColorEnhancement;
import org.cyanogenmod.hardware.SunlightEnhancement;

import java.util.ArrayList;
import java.util.List;

public class LiveDisplay extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "LiveDisplay";

    private static final String KEY_CATEGORY_LIVE_DISPLAY = "live_display_options";
    private static final String KEY_CATEGORY_CALIBRATION = "calibration";

    private static final String KEY_LIVE_DISPLAY = "live_display";
    private static final String KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE = "live_display_outdoor_mode";
    private static final String KEY_LIVE_DISPLAY_LOW_POWER = "live_display_low_power";
    private static final String KEY_LIVE_DISPLAY_COLOR_ENHANCE = "live_display_color_enhance";
    private static final String KEY_LIVE_DISPLAY_DAY_TEMP = "live_display_day_temperature";
    private static final String KEY_LIVE_DISPLAY_NIGHT_TEMP = "live_display_night_temperature";

    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";
    private static final String KEY_SCREEN_COLOR_SETTINGS = "screencolor_settings";

    private ListPreference mLiveDisplay;

    private SwitchPreference mColorEnhancement;
    private SwitchPreference mLowPower;
    private SwitchPreference mOutdoorMode;

    private PreferenceScreen mScreenColorSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.livedisplay);

        PreferenceCategory liveDisplayPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_LIVE_DISPLAY);
        PreferenceCategory calibrationPrefs = (PreferenceCategory)
                findPreference(KEY_CATEGORY_CALIBRATION);

        mLiveDisplay = (ListPreference) findPreference(KEY_LIVE_DISPLAY);
        int displayMode = Settings.System.getInt(resolver,
                Settings.System.DISPLAY_TEMPERATURE_MODE, 0);
        mLiveDisplay.setValue(String.valueOf(displayMode));
        mLiveDisplay.setEntries(getResources().getStringArray(
                com.android.internal.R.array.live_display_entries));
        mLiveDisplay.setEntryValues(getResources().getStringArray(
                com.android.internal.R.array.live_display_values));
        mLiveDisplay.setSummary(getResources().getStringArray(
                com.android.internal.R.array.live_display_summaries)[displayMode]);
        mLiveDisplay.setOnPreferenceChangeListener(this);

        mLowPower = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_LOW_POWER);
        if (liveDisplayPrefs != null && !isAdaptiveBacklightSupported()) {
            liveDisplayPrefs.removePreference(mLowPower);
            mLowPower = null;
        }

        mOutdoorMode = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
        if (liveDisplayPrefs != null && mOutdoorMode != null
                && !isSunlightEnhancementSupported()) {
            liveDisplayPrefs.removePreference(mOutdoorMode);
            mOutdoorMode = null;
        }

        mColorEnhancement = (SwitchPreference) findPreference(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
        if (liveDisplayPrefs != null && mColorEnhancement != null
                && !isColorEnhancementSupported()) {
            liveDisplayPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        if (calibrationPrefs != null && !DisplayGamma.isSupported()) {
            Preference gammaPref = findPreference(KEY_DISPLAY_GAMMA);
            if (gammaPref != null) {
                calibrationPrefs.removePreference(gammaPref);
            }
        }

        mScreenColorSettings = (PreferenceScreen) findPreference(KEY_SCREEN_COLOR_SETTINGS);
        if (calibrationPrefs != null) {
            if (!isPostProcessingSupported(getActivity()) && mScreenColorSettings != null) {
                calibrationPrefs.removePreference(mScreenColorSettings);
            } else if ("user".equals(Build.TYPE)) {
                // Remove simple RGB controls if HSIC controls are available
                Preference displayColor = findPreference(KEY_DISPLAY_COLOR);
                if (displayColor != null) {
                    calibrationPrefs.removePreference(displayColor);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mLiveDisplay) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.DISPLAY_TEMPERATURE_MODE, value);
            mLiveDisplay.setSummary(getResources().getStringArray(
                    com.android.internal.R.array.live_display_summaries)[value]);
        }
        return true;
    }

    private static boolean isAdaptiveBacklightSupported() {
        try {
            return AdaptiveBacklight.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isSunlightEnhancementSupported() {
        try {
            return SunlightEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isColorEnhancementSupported() {
        try {
            return ColorEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isPostProcessingSupported(Context context) {
        return Utils.isPackageInstalled(context, "com.qualcomm.display");
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        private boolean mHasSunlightEnhancement, mHasColorEnhancement, mHasLowPower;
        private boolean mHasDisplayGamma;

        @Override
        public void prepare() {
            mHasSunlightEnhancement = isSunlightEnhancementSupported();
            mHasColorEnhancement = isColorEnhancementSupported();
            mHasLowPower = isAdaptiveBacklightSupported();
            mHasDisplayGamma = DisplayGamma.isSupported();
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                boolean enabled) {
            ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.livedisplay;
            result.add(sir);

            return result;
        }
        
        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<String>();
            if (!mHasSunlightEnhancement) {
                result.add(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
            }
            if (!mHasColorEnhancement) {
                result.add(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
            }
            if (!mHasLowPower) {
                result.add(KEY_LIVE_DISPLAY_LOW_POWER);
            }
            if (!isPostProcessingSupported(context)) {
                result.add(KEY_SCREEN_COLOR_SETTINGS);
            }
            if (!mHasDisplayGamma) {
                result.add(KEY_DISPLAY_GAMMA);
            }
            return result;
        }
    };
}
