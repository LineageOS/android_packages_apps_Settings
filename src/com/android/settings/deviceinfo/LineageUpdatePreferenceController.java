/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2017 The LineageOS Project
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.settings.core.PreferenceController;

public class LineageUpdatePreferenceController extends PreferenceController {

    private static final String KEY_UPDATE_SETTING = "lineage_update_settings";

    public LineageUpdatePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        String packagename = mContext.getResources().getString(
                com.android.settings.R.string.lineage_update_package);
        try {
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_UPDATE_SETTING;
    }
}
