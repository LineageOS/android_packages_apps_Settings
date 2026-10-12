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
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Preference fragment used for switch screen resolution */
// LINT.IfChange
@SearchIndexable
public class ScreenResolutionFragment extends RadioButtonPickerFragment {
    private static final String TAG = "ScreenResolution";

    private Resources mResources;
    private static final String SCREEN_RESOLUTION = "user_selected_resolution";
    private static final String SCREEN_RESOLUTION_KEY = "screen_resolution";
    private Display mDefaultDisplay;
    private String[] mScreenResolutionOptions;
    private Set<Point> mResolutions;
    private SpannableString[] mScreenResolutionSummaries;

    private IllustrationPreference mImagePreference;
    private DisplayObserver mDisplayObserver;

    private int mHighWidth;
    private int mFullWidth;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mDefaultDisplay =
                context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        mResources = context.getResources();
        mScreenResolutionOptions =
                mResources.getStringArray(R.array.config_screen_resolution_options_strings);
        mImagePreference = new IllustrationPreference(context);
        mDisplayObserver = new DisplayObserver(context);
        ScreenResolutionController controller =
                new ScreenResolutionController(context, SCREEN_RESOLUTION_KEY);
        mResolutions = controller.getAllSupportedResolutions();
        mHighWidth = ScreenResolutionExtensionsKt.getHighResolutionDisplayWidth(context);
        mFullWidth = ScreenResolutionExtensionsKt.getFullResolutionDisplayWidth(context);
        Log.i(TAG, "mHighWidth:" + mHighWidth + "mFullWidth:" + mFullWidth);
        mScreenResolutionSummaries =
                new SpannableString[] {
                    getResolutionSpannable(mHighWidth,
                        ScreenResolutionExtensionsKt.getHighResolutionDisplayHeight(context)),
                    getResolutionSpannable(mFullWidth,
                        ScreenResolutionExtensionsKt.getFullResolutionDisplayHeight(context))
                };
    }


    private SpannableString getResolutionSpannable(int width, int height) {
        String resolutionString = width + " x " + height;
        String accessibleText = mResources.getString(
                R.string.screen_resolution_delimiter_a11y, width, height);
        return Utils.createAccessibleSequence(resolutionString, accessibleText);
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
        footerPreference.setLayoutResource(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer);
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

    /** Get current display mode. */
    @VisibleForTesting
    public Display.Mode getDisplayMode() {
        return mDefaultDisplay.getMode();
    }

    /** Using display manager to set the display mode. */
    @VisibleForTesting
    public void setDisplayMode(final int width) {
        ScreenResolutionExtensionsKt.setDisplayModeByWidth(getContext(), width);
    }

    /** Get the key corresponding to the resolution. */
    @VisibleForTesting
    String getKeyForResolution(int width) {
        return width == mHighWidth
                ? mScreenResolutionOptions[ScreenResolutionController.HIGHRESOLUTION_IDX]
                : width == mFullWidth
                        ? mScreenResolutionOptions[ScreenResolutionController.FULLRESOLUTION_IDX]
                        : null;
    }

    /** Get the width corresponding to the resolution key. */
    int getWidthForResoluitonKey(String key) {
        return mScreenResolutionOptions[ScreenResolutionController.HIGHRESOLUTION_IDX].equals(key)
                ? mHighWidth
                : mScreenResolutionOptions[ScreenResolutionController.FULLRESOLUTION_IDX].equals(
                    key)
                ? mFullWidth : -1;
    }

    @Override
    protected String getDefaultKey() {
        int physicalWidth = getDisplayMode().getPhysicalWidth();

        return getKeyForResolution(physicalWidth);
    }

    @Override
    protected boolean setDefaultKey(final String key) {
        int width = getWidthForResoluitonKey(key);
        if (width < 0) {
            return false;
        }

        setDisplayMode(width);
        updateIllustrationImage(mImagePreference);

        return true;
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference selected) {
        String selectedKey = selected.getKey();
        int selectedWidth = getWidthForResoluitonKey(selectedKey);
        if (!mDisplayObserver.setPendingResolutionChange(selectedWidth)) {
            return;
        }

        super.onRadioButtonClicked(selected);
    }

    /** Update the resolution image according display mode. */
    private void updateIllustrationImage(IllustrationPreference preference) {
        String key = getDefaultKey();
        if (TextUtils.equals(
                mScreenResolutionOptions[ScreenResolutionController.HIGHRESOLUTION_IDX], key)) {
            preference.setLottieAnimationResId(R.raw.lottie_screen_resolution_high);
            preference.applyDynamicColor();
        } else if (TextUtils.equals(
                mScreenResolutionOptions[ScreenResolutionController.FULLRESOLUTION_IDX], key)) {
            preference.setLottieAnimationResId(R.raw.lottie_screen_resolution_full);
            preference.applyDynamicColor();
        }
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
                    return ScreenResolutionExtensionsKt.checkSupportedResolutions(context);
                }
            };

    static final class DisplayObserver implements DisplayManager.DisplayListener {
        private static DisplayObserver sPendingObserver;
        private final Runnable mTimeout = this::stopObserve;
        private int mRequestedWidth;
        private final @Nullable Context mContext;
        private float mDensityScale;
        private AtomicInteger mPreviousWidth = new AtomicInteger(-1);

        DisplayObserver(Context context) {
            mContext = context;
        }

        public boolean startObserve(int requestedWidth) {
            if (sPendingObserver != null || mContext == null) {
                return false;
            }

            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            final int currentIndex = density.getCurrentIndex();
            final int defaultDensity = density.getDefaultDensity();

            if (currentIndex < 0 || defaultDensity <= 0 || density.getValues() == null
                    || currentIndex >= density.getValues().length) {
                return false;
            }

            mDensityScale = density.getValues()[currentIndex] / (float) defaultDensity;
            mRequestedWidth = requestedWidth;
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            dm.registerDisplayListener(this, ThreadUtils.getUiThreadHandler());
            sPendingObserver = this;
            ThreadUtils.getUiThreadHandler().postDelayed(mTimeout, 5000);
            return true;
        }

        public void stopObserve() {
            ThreadUtils.getUiThreadHandler().removeCallbacks(mTimeout);
            if (sPendingObserver == this) {
                sPendingObserver = null;
            }
            if (mContext == null) {
                return;
            }

            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            dm.unregisterDisplayListener(this);
        }

        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (sPendingObserver != this || displayId != Display.DEFAULT_DISPLAY
                    || getCurrentWidth() != mRequestedWidth) {
                return;
            }

            try {
                restoreDensity();
            } finally {
                stopObserve();
            }
        }

        private void restoreDensity() {
            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            final int[] values = density.getValues();
            if (values != null && values.length > 0) {
                final float targetDensity = density.getDefaultDensity() * mDensityScale;
                int closestIndex = 0;
                for (int i = 1; i < values.length; i++) {
                    if (Math.abs(values[i] - targetDensity)
                            < Math.abs(values[closestIndex] - targetDensity)) {
                        closestIndex = i;
                    }
                }
                if (mDensityScale == 1f
                        || values[closestIndex] == density.getDefaultDensity()) {
                    density.clearForcedDisplayDensity();
                } else {
                    density.setForcedDisplayDensity(closestIndex);
                }
            }
        }

        private int getCurrentWidth() {
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            return dm.getDisplay(Display.DEFAULT_DISPLAY).getMode().getPhysicalWidth();
        }

        private boolean setPendingResolutionChange(int selectedWidth) {
            int currentWidth = getCurrentWidth();

            if (selectedWidth == currentWidth) {
                return false;
            }
            if (mPreviousWidth.get() != -1 && !isResolutionChangeApplied()) {
                return false;
            }

            mPreviousWidth.set(currentWidth);

            return true;
        }

        private boolean isResolutionChangeApplied() {
            if (mPreviousWidth.get() == getCurrentWidth()) {
                return false;
            }

            Log.i(TAG,
                    "resolution changed from " + mPreviousWidth.get() + " to " + getCurrentWidth());
            return true;
        }
    }
}
// LINT.ThenChange(ScreenResolutionApiScreen.kt, ScreenResolutionExtensions.kt)
