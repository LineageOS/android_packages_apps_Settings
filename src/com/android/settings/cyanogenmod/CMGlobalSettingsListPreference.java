/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import cyanogenmod.providers.CMSettings;

public class CMGlobalSettingsListPreference extends ListPreference {
    public CMGlobalSettingsListPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CMGlobalSettingsListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CMGlobalSettingsListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CMGlobalSettingsListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        int index = findIndexOfValue(getValue());
        if (index  >= 0) {
            return getEntries()[index];
        } else {
            return "";
        }
    }

    @Override
    protected boolean persistString(String value) {
        if (!shouldPersist()) return false;

        CMSettings.Global.putString(getContext().getContentResolver(), getKey(), value);
        return true;
    }

    @Override
    protected String getPersistedString(String defaultValue) {
        if (!shouldPersist()) return defaultValue;

        return CMSettings.Global.getString(getContext().getContentResolver(),
                getKey());
    }

    @Override
    protected boolean isPersisted() {
        // Using getString instead of getInt so we can simply check for null
        // instead of catching an exception. (All values are stored as strings.)
        return CMSettings.Global.getString(getContext().getContentResolver(), getKey()) != null;
    }
}
