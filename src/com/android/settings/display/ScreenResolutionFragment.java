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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.text.TextUtils;
import android.view.Display;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Preference fragment used for switch screen resolution */
@SearchIndexable
public class ScreenResolutionFragment extends RadioButtonPickerFragment {

    private static final String TAG = "ScreenResolution";

    private Resources mResources;
    private Display mDefaultDisplay;
    private String[] mScreenResolutionOptions;
    private Set<Point> mResolutions;
    private String[] mScreenResolutionSummaries;
    private int[] mScreenWidthOptions;

    private IllustrationPreference mImagePreference;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mDefaultDisplay =
                context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        mResources = context.getResources();
        mScreenResolutionOptions =
                mResources.getStringArray(R.array.config_screen_resolution_options_strings);
        mScreenWidthOptions =
                mResources.getIntArray(R.array.config_screen_resolution_widths);
        mScreenResolutionSummaries =
                mResources.getStringArray(R.array.config_screen_resolution_summaries_strings);
        mResolutions = getAllSupportedResolution();
        mImagePreference = new IllustrationPreference(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_resolution_settings;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        updateIllustrationImage(mImagePreference);
        screen.addPreference(mImagePreference);

        final FooterPreference footerPreference = new FooterPreference(screen.getContext());
        footerPreference.setTitle(R.string.screen_resolution_footer);
        footerPreference.setSelectable(false);
        footerPreference.setLayoutResource(R.layout.preference_footer);
        screen.addPreference(footerPreference);
    }

    @Override
    public void bindPreferenceExtra(
            SelectorWithWidgetPreference pref,
            String key,
            CandidateInfo info,
            String defaultKey,
            String systemDefaultKey) {
        final ScreenResolutionCandidateInfo candidateInfo = (ScreenResolutionCandidateInfo) info;
        final CharSequence summary = candidateInfo.loadSummary();
        if (summary != null) pref.setSummary(summary);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<ScreenResolutionCandidateInfo> candidates = new ArrayList<>();

        for (int i = 0; i < mScreenResolutionOptions.length; i++) {
            candidates.add(
                    new ScreenResolutionCandidateInfo(
                            mScreenResolutionOptions[i],
                            mScreenResolutionSummaries[i],
                            mScreenResolutionOptions[i],
                            true /* enabled */));
        }

        return candidates;
    }

    /** Get all supported resolutions on the device. */
    private Set<Point> getAllSupportedResolution() {
        Set<Point> resolutions = new HashSet<>();
        for (Display.Mode mode : mDefaultDisplay.getSupportedModes()) {
            resolutions.add(new Point(mode.getPhysicalWidth(), mode.getPhysicalHeight()));
        }

        return resolutions;
    }

    /** Get prefer display mode. */
    private Display.Mode getPreferMode(int width) {
        for (Point resolution : mResolutions) {
            if (resolution.x == width) {
                return new Display.Mode(
                        resolution.x, resolution.y, getDisplayMode().getRefreshRate());
            }
        }

        return getDisplayMode();
    }

    /** Get current display mode. */
    @VisibleForTesting
    public Display.Mode getDisplayMode() {
        return mDefaultDisplay.getMode();
    }

    /** Using display manager to set the display mode. */
    @VisibleForTesting
    public void setDisplayMode(int width) {
        mDefaultDisplay.setUserPreferredDisplayMode(getPreferMode(width));
    }

    /** Get the key corresponding to the resolution. */
    @VisibleForTesting
    String getKeyForResolution(int width) {
        for (int i = 0; i < mScreenWidthOptions.length; i++) {
            if (width == mScreenWidthOptions[i]) {
                return mScreenResolutionOptions[i];
            }
        }
        return null;
    }

    @Override
    protected String getDefaultKey() {
        int physicalWidth = getDisplayMode().getPhysicalWidth();

        return getKeyForResolution(physicalWidth);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        for (int i = 0; i < mScreenResolutionOptions.length; i++) {
            if (mScreenResolutionOptions[i].equals(key)) {
                setDisplayMode(mScreenWidthOptions[i]);
            }
        }

        updateIllustrationImage(mImagePreference);
        return true;
    }

    /** Update the resolution image according display mode. */
    private void updateIllustrationImage(IllustrationPreference preference) {
        String key = getDefaultKey();

        // FIXME: Do we really need this illustration image? Different devices will have different
        //        number of resolution options, we can't have them all
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_RESOLUTION;
    }

    /** This is an extension of the CandidateInfo class, which adds summary information. */
    public static class ScreenResolutionCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final CharSequence mSummary;
        private final String mKey;

        ScreenResolutionCandidateInfo(
                CharSequence label, CharSequence summary, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mSummary = summary;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        /** It is the summary for radio options. */
        public CharSequence loadSummary() {
            return mSummary;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_resolution_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    ScreenResolutionController mController =
                            new ScreenResolutionController(context, "fragment");
                    return mController.checkSupportedResolutions();
                }
            };
}
