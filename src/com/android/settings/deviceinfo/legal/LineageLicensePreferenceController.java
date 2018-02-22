/*
 * Copyright (C) 2021 The LineageOS Project
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

package com.android.settings.deviceinfo.legal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class LineageLicensePreferenceController extends LegalPreferenceController {

    private static final String PROPERTY_LINEAGELICENSE_URL = "ro.lineagelegal.url";

    public LineageLicensePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setOnPreferenceClickListener(pref -> {
                mContext.startActivity(getIntent());
                return true;
            });
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (getIntent().resolveActivity(mContext.getPackageManager()) != null) {
            return AVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    protected Intent getIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse(SystemProperties.get(PROPERTY_LINEAGELICENSE_URL)));
    }
}
