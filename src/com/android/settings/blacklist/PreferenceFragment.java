/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.blacklist;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.telephony.util.BlacklistUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.HashSet;
import java.util.Set;

public class PreferenceFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String BUTTON_NOTIFY               = "button_notify";
    private static final String BUTTON_USE_REGEX            = "button_blacklist_regex";
    private static final String BUTTON_BLACKLIST_PRIVATE    = "button_blacklist_private_numbers";
    private static final String BUTTON_BLACKLIST_UNKNOWN    = "button_blacklist_unknown_numbers";

    private CheckBoxPreference mNotify;
    private CheckBoxPreference mUseRegex;
    private MultiSelectListPreference mBlacklistPrivate;
    private MultiSelectListPreference mBlacklistUnknown;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.blacklist_prefs);

        PreferenceScreen prefSet = getPreferenceScreen();
        mNotify = (CheckBoxPreference) prefSet.findPreference(BUTTON_NOTIFY);
        mNotify.setOnPreferenceChangeListener(this);
        mUseRegex = (CheckBoxPreference) prefSet.findPreference(BUTTON_USE_REGEX);
        mUseRegex.setOnPreferenceChangeListener(this);
        mBlacklistPrivate =
                (MultiSelectListPreference) prefSet.findPreference(BUTTON_BLACKLIST_PRIVATE);
        mBlacklistPrivate.setOnPreferenceChangeListener(this);
        mBlacklistUnknown =
                (MultiSelectListPreference) prefSet.findPreference(BUTTON_BLACKLIST_UNKNOWN);
        mBlacklistUnknown.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();
        mNotify.setChecked(BlacklistUtils.isBlacklistNotifyEnabled(context));
        mUseRegex.setChecked(BlacklistUtils.isBlacklistRegexEnabled(context));
        updateSelectListFromPolicy(mBlacklistPrivate,
                Settings.System.PHONE_BLACKLIST_PRIVATE_NUMBER_MODE);
        updateSelectListSummary(mBlacklistPrivate, mBlacklistPrivate.getValues(),
                R.string.blacklist_private_numbers_summary);
        updateSelectListFromPolicy(mBlacklistUnknown,
                Settings.System.PHONE_BLACKLIST_UNKNOWN_NUMBER_MODE);
        updateSelectListSummary(mBlacklistUnknown, mBlacklistUnknown.getValues(),
                R.string.blacklist_unknown_numbers_summary);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNotify) {
            boolean checked = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PHONE_BLACKLIST_NOTIFY_ENABLED, checked ? 1 : 0);
        } else if (preference == mUseRegex) {
            boolean checked = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PHONE_BLACKLIST_REGEX_ENABLED, checked ? 1 : 0);
        } else if (preference == mBlacklistUnknown) {
            Set<String> newValues = (Set<String>) objValue;
            updatePolicyFromSelectList(newValues,
                    Settings.System.PHONE_BLACKLIST_UNKNOWN_NUMBER_MODE);
            updateSelectListSummary(mBlacklistUnknown, newValues,
                    R.string.blacklist_unknown_numbers_summary);
        } else if (preference == mBlacklistPrivate) {
            Set<String> newValues = (Set<String>) objValue;
            updatePolicyFromSelectList(newValues,
                    Settings.System.PHONE_BLACKLIST_PRIVATE_NUMBER_MODE);
            updateSelectListSummary(mBlacklistPrivate, newValues,
                    R.string.blacklist_private_numbers_summary);
        }

        return true;
    }

    private void updateSelectListFromPolicy(MultiSelectListPreference pref, String setting) {
        int mode = Settings.System.getInt(getContentResolver(), setting, 0);
        Set<String> values = new HashSet<String>();

        if ((mode & BlacklistUtils.BLOCK_CALLS) != 0) {
            values.add(Integer.toString(BlacklistUtils.BLOCK_CALLS));
        }
        if ((mode & BlacklistUtils.BLOCK_MESSAGES) != 0) {
            values.add(Integer.toString(BlacklistUtils.BLOCK_MESSAGES));
        }
        pref.setValues(values);
    }

    private int getPolicyFromSelectList(Set<String> values) {
        int mode = 0;

        for (String value : values) {
            mode |= Integer.parseInt(value);
        }

        return mode;
    }

    private void updatePolicyFromSelectList(Set<String> values, String setting) {
        int mode = getPolicyFromSelectList(values);
        Settings.System.putInt(getContentResolver(), setting, mode);
    }

    private void updateSelectListSummary(MultiSelectListPreference pref,
            Set<String> values, int summaryResId) {
        int mode = getPolicyFromSelectList(values);
        int typeResId;

        if (mode == 0) {
            typeResId = R.string.blacklist_summary_type_nothing;
        } else if (mode == BlacklistUtils.BLOCK_CALLS) {
            typeResId = R.string.blacklist_summary_type_calls_only;
        } else if (mode == BlacklistUtils.BLOCK_MESSAGES) {
            typeResId = R.string.blacklist_summary_type_messages_only;
        } else {
            typeResId = R.string.blacklist_summary_type_calls_and_messages;
        }

        pref.setSummary(getString(summaryResId, getString(typeResId)));
    }
}
