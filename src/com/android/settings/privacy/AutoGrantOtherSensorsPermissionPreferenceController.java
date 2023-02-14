/*
 * Copyright (C) 2023 GrapheneOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.privacy;

import static android.provider.Settings.Secure.AUTO_GRANT_OTHER_SENSORS_PERMISSION;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Toggle for granting the OTHER_SENSORS ("Sensors") permission by default to newly installed apps.
 */
public class AutoGrantOtherSensorsPermissionPreferenceController
        extends TogglePreferenceController {
    public AutoGrantOtherSensorsPermissionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                AUTO_GRANT_OTHER_SENSORS_PERMISSION, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                AUTO_GRANT_OTHER_SENSORS_PERMISSION, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
