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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.AmbientDisplayConfiguration;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class ScreenOffUdfpsSettings extends DashboardFragment {

    private static final String TAG = "ScreenOffUdfps";

    public static final String PREF_KEY_SUGGESTION_COMPLETE =
            "pref_screen_off_udfps_suggestion_complete";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SuggestionFeatureProvider suggestionFeatureProvider =
                FeatureFactory.Companion.getFeatureFactory().getSuggestionFeatureProvider();
        SharedPreferences prefs = suggestionFeatureProvider.getSharedPrefs(context);
        prefs.edit().putBoolean(PREF_KEY_SUGGESTION_COMPLETE, true).apply();

        use(ScreenOffUdfpsPreferenceController.class)
                .setConfig(new AmbientDisplayConfiguration(context));
    }


    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_off_udfps_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_off_udfps_settings);
}
