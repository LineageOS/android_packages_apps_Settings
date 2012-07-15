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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

public class Calendar extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KEY_SHOW_CALENDAR = "lockscreen_calendar";
    private static final String KEY_CALENDARS = "lockscreen_calendars";
    private static final String KEY_REMINDERS_ONLY = "lockscreen_calendar_reminders_only";
    private static final String KEY_LOOKAHEAD = "lockscreen_calendar_lookahead";
    private static final String KEY_SHOW_LOCATION = "lockscreen_calendar_show_location";
    private static final String KEY_SHOW_DESCRIPTION = "lockscreen_calendar_show_description";

    private CheckBoxPreference mCalendarPref;
    private CheckBoxPreference mCalendarRemindersOnlyPref;
    private MultiSelectListPreference mCalendarsPref;
    private ListPreference mCalendarLookaheadPref;
    private ListPreference mCalendarShowLocationPref;
    private ListPreference mCalendarShowDescriptionPref;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResolver = getContentResolver();

        addPreferencesFromResource(R.xml.calendar_prefs);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Show next calendar event on lock screen
        mCalendarPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_CALENDAR);
        mCalendarPref.setChecked(Settings.System.getInt(mResolver,
                Settings.System.LOCKSCREEN_CALENDAR, 0) == 1);

        mCalendarsPref = (MultiSelectListPreference) prefSet.findPreference(KEY_CALENDARS);
        mCalendarsPref.setDefaultValue(Settings.System.getString(mResolver,
                Settings.System.LOCKSCREEN_CALENDARS));
        mCalendarsPref.setOnPreferenceChangeListener(this);
        CalendarEntries calEntries = CalendarEntries.findCalendars(getActivity());
        mCalendarsPref.setEntries(calEntries.getEntries());
        mCalendarsPref.setEntryValues(calEntries.getEntryValues());

        mCalendarRemindersOnlyPref = (CheckBoxPreference) prefSet
                .findPreference(KEY_REMINDERS_ONLY);
        mCalendarRemindersOnlyPref.setChecked(Settings.System.getInt(mResolver,
                KEY_REMINDERS_ONLY, 0) == 1);

        mCalendarLookaheadPref = (ListPreference) prefSet
                .findPreference(KEY_LOOKAHEAD);
        long calendarLookahead = Settings.System.getLong(mResolver,
                Settings.System.LOCKSCREEN_CALENDAR_LOOKAHEAD, 10800000);
        mCalendarLookaheadPref.setValue(String.valueOf(calendarLookahead));
        mCalendarLookaheadPref.setSummary(mapLookaheadValue(calendarLookahead));
        mCalendarLookaheadPref.setOnPreferenceChangeListener(this);

        mCalendarShowLocationPref = (ListPreference) prefSet
                .findPreference(KEY_SHOW_LOCATION);
        int calendarShowLocation = Settings.System.getInt(mResolver,
                Settings.System.LOCKSCREEN_CALENDAR_SHOW_LOCATION, 0);
        mCalendarShowLocationPref.setValue(String.valueOf(calendarShowLocation));
        mCalendarShowLocationPref.setSummary(mapMetadataValue(calendarShowLocation));
        mCalendarShowLocationPref.setOnPreferenceChangeListener(this);

        mCalendarShowDescriptionPref = (ListPreference) prefSet
                .findPreference(KEY_SHOW_DESCRIPTION);
        int calendarShowDescription = Settings.System.getInt(mResolver,
                Settings.System.LOCKSCREEN_CALENDAR_SHOW_DESCRIPTION, 0);
        mCalendarShowDescriptionPref.setValue(String.valueOf(calendarShowDescription));
        mCalendarShowDescriptionPref.setSummary(mapMetadataValue(calendarShowDescription));
        mCalendarShowDescriptionPref.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mCalendarPref) {
            value = mCalendarPref.isChecked();
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_CALENDAR, value ? 1 : 0);
            return true;
        } else if (preference == mCalendarRemindersOnlyPref) {
            value = mCalendarRemindersOnlyPref.isChecked();
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_CALENDAR_REMINDERS_ONLY,
                    value ? 1 : 0);
            return true;
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCalendarShowLocationPref) {
            int calendarShowLocation = Integer.valueOf((String) newValue);
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_CALENDAR_SHOW_LOCATION,
                    calendarShowLocation);
            mCalendarShowLocationPref.setSummary(mapMetadataValue(calendarShowLocation));
            return true;
        } else if (preference == mCalendarShowDescriptionPref) {
            int calendarShowDescription = Integer.valueOf((String) newValue);
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_CALENDAR_SHOW_DESCRIPTION,
                    calendarShowDescription);
            mCalendarShowDescriptionPref.setSummary(mapMetadataValue(calendarShowDescription));
            return true;
        } else if (preference == mCalendarLookaheadPref) {
            long calendarLookahead = Long.valueOf((String) newValue);
            Settings.System.putLong(mResolver, Settings.System.LOCKSCREEN_CALENDAR_LOOKAHEAD,
                    calendarLookahead);
            mCalendarLookaheadPref.setSummary(mapLookaheadValue(calendarLookahead));
            return true;
        } else if (preference == mCalendarsPref) {
            String calendars = newValue.toString();
            Settings.System.putString(mResolver, Settings.System.LOCKSCREEN_CALENDARS,
                    calendars);
            return true;
        }

        return false;
    }

    private String mapMetadataValue(Integer value) {
        Resources resources = getActivity().getResources();

        String[] names = resources.getStringArray(R.array.calendar_show_event_metadata_entries);
        String[] values = resources.getStringArray(R.array.calendar_show_event_metadata_values);

        for (int i = 0; i < values.length; i++) {
            if (Integer.decode(values[i]).equals(value)) {
                return names[i];
            }
        }

        return getActivity().getString(R.string.unknown);
    }

    private String mapLookaheadValue(Long value) {
        Resources resources = getActivity().getResources();

        String[] names = resources.getStringArray(R.array.calendar_lookahead_entries);
        String[] values = resources.getStringArray(R.array.calendar_lookahead_values);

        for (int i = 0; i < values.length; i++) {
            if (Long.decode(values[i]).equals(value)) {
                return names[i];
            }
        }

        return getActivity().getString(R.string.unknown);
    }

    private static class CalendarEntries {
        private final CharSequence[] mEntries;
        private final CharSequence[] mEntryValues;
        private static Uri uri = CalendarContract.Calendars.CONTENT_URI;

        // Calendar projection array
        private static String[] projection = new String[] {
               CalendarContract.Calendars._ID,
               CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        };

        // The indices for the projection array
        private static final int CALENDAR_ID_INDEX = 0;
        private static final int DISPLAY_NAME_INDEX = 1;

        static CalendarEntries findCalendars(Activity activity) {
            List<CharSequence> entries = new ArrayList<CharSequence>();
            List<CharSequence> entryValues = new ArrayList<CharSequence>();

            Cursor calendarCursor = activity.managedQuery(uri, projection, null, null, null);
            if (calendarCursor.moveToFirst()) {
                do {
                    entryValues.add(calendarCursor.getString(CALENDAR_ID_INDEX));
                    entries.add(calendarCursor.getString(DISPLAY_NAME_INDEX));
                } while (calendarCursor.moveToNext());
            }

            return new CalendarEntries(entries, entryValues);
        }

        private CalendarEntries(List<CharSequence> mEntries, List<CharSequence> mEntryValues) {
            this.mEntries = mEntries.toArray(new CharSequence[mEntries.size()]);
            this.mEntryValues = mEntryValues.toArray(new CharSequence[mEntryValues.size()]);
        }

        CharSequence[] getEntries() {
            return mEntries;
        }

        CharSequence[] getEntryValues() {
            return mEntryValues;
        }

    }
}