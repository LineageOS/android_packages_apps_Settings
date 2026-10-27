/*
 * Copyright (C) 2026 The LineageOS Project
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

import static android.provider.Settings.Secure.DESKTOP_MODE_APP_HANDLE_STYLE;
import static android.provider.Settings.Secure.DESKTOP_MODE_APP_HANDLE_STYLE_PILL;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.wm.shell.shared.desktopmode.DesktopState;

/** Controller for the desktop mode app handle style preference. */
public class DesktopModeAppHandleStylePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final DesktopState mDesktopState;

    public DesktopModeAppHandleStylePreferenceController(Context context, String key) {
        super(context, key);
        mDesktopState = DesktopState.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return mDesktopState.canEnterDesktopModeOrShowAppHandle()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((ListPreference) preference).setValue(String.valueOf(Settings.Secure.getInt(
                mContext.getContentResolver(), DESKTOP_MODE_APP_HANDLE_STYLE,
                DESKTOP_MODE_APP_HANDLE_STYLE_PILL)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                DESKTOP_MODE_APP_HANDLE_STYLE, Integer.parseInt((String) newValue));
    }
}
