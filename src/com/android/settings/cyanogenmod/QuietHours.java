/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import java.util.Calendar;
import java.util.Date;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class QuietHours extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "QuietHours";

    private static final int DIALOG_QUIET_HOURS_START = 1;

    private static final int DIALOG_QUIET_HOURS_END = 2;

    private static final String KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";

    private static final String KEY_QUIET_HOURS_START = "quiet_hours_start";

    private static final String KEY_QUIET_HOURS_END = "quiet_hours_end";

    private static final String KEY_QUIET_HOURS_MUTE = "quiet_hours_mute";

    private static final String KEY_QUIET_HOURS_STILL = "quiet_hours_still";

    private static final String KEY_QUIET_HOURS_DIM = "quiet_hours_dim";

    private static final String KEY_QUIET_HOURS_HAPTIC = "quiet_hours_haptic";

    private static final String KEY_QUIET_HOURS_NOTE = "quiet_hours_note";

    private CheckBoxPreference mQuietHoursEnabled;

    private Preference mQuietHoursStart;

    private Preference mQuietHoursEnd;

    private Preference mQuietHoursNote;

    private CheckBoxPreference mQuietHoursMute;

    private CheckBoxPreference mQuietHoursStill;

    private CheckBoxPreference mQuietHoursDim;

    private CheckBoxPreference mQuietHoursHaptic;

    private String returnTime(String t) {
        if (t == null || t.equals("")) {
            return "";
        }
        int hr = Integer.parseInt(t.trim());
        int mn = hr;

        hr = hr / 60;
        mn = mn % 60;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, mn);
        Date date = cal.getTime();
        return DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(date);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_settings);

            ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();

            PreferenceScreen prefSet = getPreferenceScreen();

            // Load the preferences
            mQuietHoursNote = prefSet.findPreference(KEY_QUIET_HOURS_NOTE);
            mQuietHoursEnabled = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_ENABLED);
            mQuietHoursStart = prefSet.findPreference(KEY_QUIET_HOURS_START);
            mQuietHoursEnd = prefSet.findPreference(KEY_QUIET_HOURS_END);
            mQuietHoursMute = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_MUTE);
            mQuietHoursStill = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_STILL);
            mQuietHoursHaptic = (CheckBoxPreference) prefSet.findPreference(KEY_QUIET_HOURS_HAPTIC);
            mQuietHoursDim = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_DIM);

            // Remove the "Incoming calls behaviour" note if the device does not support phone calls
            if (mQuietHoursNote != null && getResources().getBoolean(com.android.internal.R.bool.config_voice_capable) == false) {
                getPreferenceScreen().removePreference(mQuietHoursNote);
            }

            // Set the preference state and listeners where applicable
            mQuietHoursEnabled.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_ENABLED, 0) == 1);
            mQuietHoursStart.setSummary(returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_START)));
            mQuietHoursStart.setOnPreferenceChangeListener(this);
            mQuietHoursEnd.setSummary(returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_END)));
            mQuietHoursEnd.setOnPreferenceChangeListener(this);
            mQuietHoursMute.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_MUTE, 0) == 1);
            mQuietHoursStill.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_STILL, 0) == 1);
            mQuietHoursHaptic.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_HAPTIC, 0) == 1);

            // Remove the notification light setting if the device does not support it 
            if (mQuietHoursDim != null && getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
                getPreferenceScreen().removePreference(mQuietHoursDim);
            } else {
                mQuietHoursDim.setChecked(Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_DIM, 0) == 1);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();

        if (preference == mQuietHoursEnabled) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_ENABLED,
                    mQuietHoursEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursMute) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_MUTE,
                    mQuietHoursMute.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStill) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_STILL,
                    mQuietHoursStill.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursDim) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_DIM,
                    mQuietHoursDim.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStart) {
            showDialog(DIALOG_QUIET_HOURS_START);
            mQuietHoursStart.setSummary(returnTime(Settings.System.getString(resolver,
                    Settings.System.QUIET_HOURS_START)));
            return true;
        } else if (preference == mQuietHoursEnd) {
            showDialog(DIALOG_QUIET_HOURS_END);
            mQuietHoursEnd.setSummary(returnTime(Settings.System.getString(resolver,
                    Settings.System.QUIET_HOURS_END)));
            return true;
        } else if (preference == mQuietHoursHaptic) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_HAPTIC,
                    mQuietHoursHaptic.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();

        if (key.equals(KEY_QUIET_HOURS_START)) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START,
                    getBoolean(newValue) ? 1 : 0);
            mQuietHoursStart.setSummary(returnTime(Settings.System.getString(resolver,
                    Settings.System.QUIET_HOURS_START)));
            return true;
        } else if (key.equals(KEY_QUIET_HOURS_END)) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END,
                    getBoolean(newValue) ? 1 : 0);
            mQuietHoursEnd.setSummary(returnTime(Settings.System.getString(resolver,
                    Settings.System.QUIET_HOURS_END)));
            return true;
        }
        return false;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_QUIET_HOURS_START:
                return createTimePicker(Settings.System.QUIET_HOURS_START);
            case DIALOG_QUIET_HOURS_END:
                return createTimePicker(Settings.System.QUIET_HOURS_END);
        }
        return super.onCreateDialog(id);
    }

    private TimePickerDialog createTimePicker(final String key) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        int value = Settings.System.getInt(resolver, key, -1);
        int hour;
        int minutes;

        if (value < 0) {
            Calendar calendar = Calendar.getInstance();
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minutes = calendar.get(Calendar.MINUTE);
        } else {
            hour = value / 60;
            minutes = value % 60;
        }

        TimePickerDialog dlg = new TimePickerDialog(getActivity(),
        new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker v, int hours, int minutes) {
                ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
                // Set the appropriate time setting
                Settings.System.putInt(resolver, key, hours * 60 + minutes);
                // Set the summary to reflect the set time
                if (key.equals(KEY_QUIET_HOURS_START)) {
                    mQuietHoursStart.setSummary(returnTime(Settings.System.getString(
                            resolver, Settings.System.QUIET_HOURS_START)));
                } else {
                    mQuietHoursEnd.setSummary(returnTime(Settings.System.getString(
                            resolver, Settings.System.QUIET_HOURS_END)));
                }
            };
        }, hour, minutes, DateFormat.is24HourFormat(getActivity()));

        return dlg;
    }

    private boolean getBoolean(Object o) {
        return Boolean.valueOf(o.toString());
    }
}