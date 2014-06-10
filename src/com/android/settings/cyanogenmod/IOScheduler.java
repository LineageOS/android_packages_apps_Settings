/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

//
// I/O Scheduler Related Settings
//
public class IOScheduler extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String IOSCHED_PREF = "pref_io_sched";
    public static final String IOSCHED_LIST_FILE = "/sys/block/mmcblk0/queue/scheduler";
    public static final String IOSCHED_MTD_LIST_FILE = "/sys/block/mtdblock0/queue/scheduler";

    public static final String SOB_PREF = "pref_io_sched_set_on_boot";

    private static final String TAG = "IOScheduler";

    private String mIOSchedulerFormat;

    private ListPreference mIOSchedulerPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIOSchedulerFormat = getString(R.string.io_sched_summary);

        String[] availableIOSchedulers = new String[0];
        String availableIOSchedulersLine;
        int bropen, brclose;
        String currentIOScheduler = null;

        addPreferencesFromResource(R.xml.ioscheduler_settings);

        PreferenceScreen prefScreen = getPreferenceScreen();

        mIOSchedulerPref = (ListPreference) prefScreen.findPreference(IOSCHED_PREF);

        /* I/O scheduler
        Some systems might not use I/O schedulers */
        if (!Utils.fileExists(IOSCHED_LIST_FILE) ||
            (availableIOSchedulersLine = Utils.fileReadOneLine(IOSCHED_LIST_FILE)) == null) {
            prefScreen.removePreference(mIOSchedulerPref);

        } else {
            availableIOSchedulers = availableIOSchedulersLine.replace("[", "").replace("]", "").split(" ");
            bropen = availableIOSchedulersLine.indexOf("[");
            brclose = availableIOSchedulersLine.lastIndexOf("]");
            if (bropen >= 0 && brclose >= 0)
                currentIOScheduler = availableIOSchedulersLine.substring(bropen + 1, brclose);

            mIOSchedulerPref.setEntryValues(availableIOSchedulers);
            mIOSchedulerPref.setEntries(availableIOSchedulers);
            if (currentIOScheduler != null)
                mIOSchedulerPref.setValue(currentIOScheduler);
            mIOSchedulerPref.setSummary(String.format(mIOSchedulerFormat, currentIOScheduler));
            mIOSchedulerPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        String availableIOSchedulersLine;
        int bropen, brclose;
        String currentIOScheduler;

        super.onResume();

        if (Utils.fileExists(IOSCHED_LIST_FILE) &&
            (availableIOSchedulersLine = Utils.fileReadOneLine(IOSCHED_LIST_FILE)) != null) {
            bropen = availableIOSchedulersLine.indexOf("[");
            brclose = availableIOSchedulersLine.lastIndexOf("]");
            if (bropen >= 0 && brclose >= 0) {
                currentIOScheduler = availableIOSchedulersLine.substring(bropen + 1, brclose);
                mIOSchedulerPref.setSummary(String.format(mIOSchedulerFormat, currentIOScheduler));
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        final String newValue = (String) value;
        String fname = "";
        String mtdfname = "";
        String ioschedfile = "";

        if (newValue != null) {
            if (preference == mIOSchedulerPref) {
                fname = IOSCHED_LIST_FILE;
                mtdfname = IOSCHED_MTD_LIST_FILE;
            }

            if (Utils.fileExists(fname) && Utils.fileWriteOneLine(fname, newValue)) {
                final String file = fname;
                ioschedfile = file.replace("mmcblk0", "mmcblk1");
                if (Utils.fileExists(ioschedfile)) {
                    if (Utils.fileIsWritable(ioschedfile)) {
                        Utils.fileWriteOneLine(ioschedfile, newValue);
                    } else {
                        Log.e(TAG, ioschedfile +
                        " not writable, did you set ueventd rules?");
                    }
                }

                if (Utils.fileExists(mtdfname) && Utils.fileIsWritable(mtdfname)) {
                    Utils.fileWriteOneLine(mtdfname, newValue);
                    final String mtdfile = mtdfname;
                    for (int i = 1; i < 10; i++) {
                        ioschedfile = mtdfile.replace("mtdblock0", "mtdblock" + i);
                        if (Utils.fileExists(ioschedfile)) {
                            if (Utils.fileIsWritable(ioschedfile)) {
                                Utils.fileWriteOneLine(ioschedfile, newValue);
                            } else {
                                Log.e(TAG, ioschedfile +
                                " not writable, did you set ueventd rules?");
                            }
                        }
                    }
                }

                if (preference == mIOSchedulerPref) {
                    mIOSchedulerPref.setSummary(String.format(mIOSchedulerFormat, newValue));
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
