/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoSettings extends PreferenceActivity {
    private static final String TAG = "DeviceInfoSettings";

    private static final String KEY_CONTAINER = "container";
    private static final String KEY_TEAM = "team";
    private static final String KEY_CONTRIBUTORS = "contributors";
    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";

    long[] mHits = new long[3];

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_settings);

        // If we don't have an IME tutorial, remove that option
        String currentIme = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName component = ComponentName.unflattenFromString(currentIme);
        Intent imeIntent = new Intent(component.getPackageName() + ".tutorial");
        PackageManager pm = getPackageManager();
        List<ResolveInfo> tutorials = pm.queryIntentActivities(imeIntent, 0);
        if(tutorials == null || tutorials.isEmpty()) {
            getPreferenceScreen().removePreference(findPreference("system_tutorial"));
        }

        setStringSummary("device_cpu", getCPUInfo());
        setStringSummary("device_memory", getMemAvail().toString()+" MB / "+getMemTotal().toString()+" MB");
        setStringSummary("firmware_version", Build.VERSION.RELEASE);
        findPreference("firmware_version").setEnabled(true);
        setValueSummary("baseband_version", "gsm.version.baseband");
        setStringSummary("device_model", Build.MODEL);
        setStringSummary("build_number", Build.DISPLAY);
        findPreference("kernel_version").setSummary(getFormattedKernelVersion());
        setValueSummary("mod_version", "ro.modversion");
        findPreference("mod_version").setEnabled(true);
        setValueSummary("build_date", "ro.build.date");

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), "safetylegal",
                PROPERTY_URL_SAFETYLEGAL);

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */

        // These are contained in the "container" preference group
        PreferenceGroup parentPreference = (PreferenceGroup) findPreference(KEY_CONTAINER);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_TERMS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_COPYRIGHT,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_TEAM,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

        // These are contained by the root preference screen
        parentPreference = getPreferenceScreen();
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference,
                KEY_SYSTEM_UPDATE_SETTINGS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parentPreference, KEY_CONTRIBUTORS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

        // Read platform settings for additional system update setting
        boolean mUpdateSettingAvailable =
                getResources().getBoolean(R.bool.config_additional_system_update_setting_enable);

        if(mUpdateSettingAvailable == false) {
            getPreferenceScreen().removePreference(findPreference(KEY_UPDATE_SETTING));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("firmware_version")
                || preference.getKey().equals("mod_version")) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("android",
                        com.android.internal.app.PlatLogoActivity.class.getName());
                if (preference.getKey().equals("mod_version")) {
                    intent.putExtra("special", true);
                }
                try {
                    startActivity(intent);
                } catch (Exception e) {
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals(""))
        {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {

        }
    }

    private Long getMemTotal() {
      Long total = null;
      BufferedReader reader = null;

      try {
         // Grab a reader to /proc/meminfo
         reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")), 1000);

         // Grab the first line which contains mem total
         String line = reader.readLine();

         // Split line on the colon, we need info to the right of the colon
         String[] info = line.split(":");

         // We have to remove the kb on the end
         String[] memTotal = info[1].trim().split(" ");

         // Convert kb into mb
         total = Long.parseLong(memTotal[0]);
         total = total / 1024;
      }
      catch(Exception e) {
         e.printStackTrace();
         // We don't want to return null so default to 0
         total = Long.parseLong("0");
      }
      finally {
         // Make sure the reader is closed no matter what
         try { reader.close(); }
         catch(Exception e) {}
         reader = null;
      }

      return total;
    }

    private Long getMemAvail() {
      Long avail = null;
      BufferedReader reader = null;

      try {
         // Grab a reader to /proc/meminfo
         reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")), 1000);

         // This is memTotal which we don't need
         String line = reader.readLine();

         // This is memFree which we need
         line = reader.readLine();
         String[] free = line.split(":");
         // Have to remove the kb on the end
         String [] memFree = free[1].trim().split(" ");

         // This is Buffers which we don't need
         line = reader.readLine();

         // This is Cached which we need
         line = reader.readLine();
         String[] cached = line.split(":");
         // Have to remove the kb on the end
         String[] memCached = cached[1].trim().split(" ");

         avail = Long.parseLong(memFree[0]) + Long.parseLong(memCached[0]);
         avail = avail / 1024;
      }
      catch(Exception e) {
         e.printStackTrace();
         // We don't want to return null so default to 0
         avail = Long.parseLong("0");
      }
      finally {
         // Make sure the reader is closed no matter what
         try { reader.close(); }
         catch(Exception e) {}
         reader = null;
      }

      return avail;
    }

   private String getCPUInfo() {
      String[] info = null;
      BufferedReader reader = null;

      try {
         // Grab a reader to /proc/cpuinfo
         reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/cpuinfo")), 1000);

         // Grab a single line from cpuinfo
         String line = reader.readLine();

         // Split on the colon, we need info to the right of colon
         info = line.split(":");
      }
      catch(IOException io) {
         io.printStackTrace();
         info = new String[1];
         info[1] = "error";
      }
      finally {
         // Make sure the reader is closed no matter what
         try { reader.close(); }
         catch(Exception e) {}
         reader = null;
      }

      return info[1];
    }

    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "Unavailable";
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {
            Log.e(TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

}
