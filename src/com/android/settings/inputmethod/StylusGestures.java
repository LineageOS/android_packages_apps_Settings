/*   Copyright (C) 2012 The CyanogenMod Project
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.android.settings.inputmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class StylusGestures extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    public static final String TAG = "Stylus Gestures";
    public static final String KEY_SPEN_ENABLE = "enable_spen";
    public static final String KEY_SPEN_LEFT = "gestures_left";
    public static final String KEY_SPEN_RIGHT = "gestures_right";
    public static final String KEY_SPEN_UP = "gestures_up";
    public static final String KEY_SPEN_DOWN = "gestures_down";
    public static final String KEY_SPEN_LONG = "gestures_long";
    public static final String KEY_SPEN_DOUBLE = "gestures_double";
    public static final int KEY_NO_ACTION = 1000;
    public static final String TEXT_NO_ACTION = "No Action";

    private CheckBoxPreference mEnableGestures;
    private ListPreference mSwipeLeft;
    private ListPreference mSwipeRight;
    private ListPreference mSwipeUp;
    private ListPreference mSwipeDown;
    private ListPreference mSwipeLong;
    private ListPreference mSwipeDouble;

    private Context mContext;
    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gestures_prefs);
        mContext = getActivity();
        mResolver = getContentResolver();

        // Setup the preferences
        mEnableGestures = (CheckBoxPreference) findPreference(KEY_SPEN_ENABLE);
        mEnableGestures.setChecked(Settings.System.getInt(mResolver,
                Settings.System.ENABLE_STYLUS_GESTURES, 0) == 1);

        mSwipeLeft = (ListPreference) findPreference(KEY_SPEN_LEFT);
        String packageName = Settings.System.getString(mResolver,
                Settings.System.GESTURES_LEFT_SWIPE);
        addApplicationEntries(mSwipeLeft, packageName);
        mSwipeLeft.setOnPreferenceChangeListener(this);

        mSwipeRight = (ListPreference) findPreference(KEY_SPEN_RIGHT);
        packageName = Settings.System.getString(mResolver,
                Settings.System.GESTURES_RIGHT_SWIPE);
        addApplicationEntries(mSwipeRight, packageName);
        mSwipeRight.setOnPreferenceChangeListener(this);

        mSwipeUp = (ListPreference) findPreference(KEY_SPEN_UP);
        packageName = Settings.System.getString(mResolver,
                Settings.System.GESTURES_UP_SWIPE);
        addApplicationEntries(mSwipeUp, packageName);
        mSwipeUp.setOnPreferenceChangeListener(this);

        mSwipeDown = (ListPreference) findPreference(KEY_SPEN_DOWN);
        packageName = Settings.System.getString(mResolver,
                Settings.System.GESTURES_DOWN_SWIPE);
        addApplicationEntries(mSwipeDown, packageName);
        mSwipeDown.setOnPreferenceChangeListener(this);

        mSwipeLong = (ListPreference) findPreference(KEY_SPEN_LONG);
        packageName = Settings.System.getString(mResolver,
                Settings.System.GESTURES_LONG_PRESS);
        addApplicationEntries(mSwipeLong, packageName);
        mSwipeLong.setOnPreferenceChangeListener(this);

        mSwipeDouble = (ListPreference) findPreference(KEY_SPEN_DOUBLE);
        packageName = Settings.System.getString(mResolver,
                Settings.System.GESTURES_DOUBLE_TAP);
        addApplicationEntries(mSwipeDouble, packageName);
        mSwipeDouble.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mEnableGestures) {
            Settings.System.putInt(mResolver,
                    Settings.System.ENABLE_STYLUS_GESTURES,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;

        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String packageName = newValue.toString();
        if (preference == mSwipeLeft) {
            Settings.System.putString(mResolver,
                    Settings.System.GESTURES_LEFT_SWIPE, packageName);
            setPrefValue(mSwipeLeft, packageName);
        } else if (preference == mSwipeRight) {
            Settings.System.putString(mResolver,
                    Settings.System.GESTURES_RIGHT_SWIPE, packageName);
            setPrefValue(mSwipeRight, packageName);
        } else if (preference == mSwipeUp) {
            Settings.System.putString(mResolver,
                    Settings.System.GESTURES_UP_SWIPE, packageName);
            setPrefValue(mSwipeUp, packageName);
        } else if (preference == mSwipeDown) {
            Settings.System.putString(mResolver,
                    Settings.System.GESTURES_DOWN_SWIPE, packageName);
            setPrefValue(mSwipeDown, packageName);
        } else if (preference == mSwipeLong) {
            Settings.System.putString(mResolver,
                    Settings.System.GESTURES_LONG_PRESS, packageName);
            setPrefValue(mSwipeLong, packageName);
        } else if (preference == mSwipeDouble) {
            Settings.System.putString(mResolver,
                    Settings.System.GESTURES_DOUBLE_TAP, packageName);
            setPrefValue(mSwipeDouble, packageName);
        }

        return false;
    }

    private String mapUpdateValue(String time) {
        Resources resources = mContext.getResources();

        String[] actionNames = resources
                .getStringArray(R.array.gestures_entries);
        String[] actionValues = resources
                .getStringArray(R.array.gestures_values);
        for (int i = 0; i < actionValues.length; i++) {
            if (actionValues[i].equalsIgnoreCase(time)) {
                return actionNames[i];
            }
        }
        return null;
    }

    private void setPrefValue(ListPreference pref, String packageName) {
        if (packageName == null)
            return;
        String text = mapUpdateValue(packageName);
        if (text != null) {
            pref.setValue(packageName);
            pref.setSummary(text);
        } else {
            String appName = getAppName(packageName);
            if (appName != null) {
                pref.setValue(packageName);
                pref.setSummary(appName);
            } else {
                pref.setSummary(packageName + " not installed");
            }
        }

    }

    private void addApplicationEntries(ListPreference pref, String packageName) {
        PackageManager pm = this.getPackageManager();
        Resources resources = mContext.getResources();

        String[] actionNames = resources
                .getStringArray(R.array.gestures_entries);
        String[] actionValues = resources
                .getStringArray(R.array.gestures_values);

        List<String> entryList = new ArrayList<String> (Arrays.asList(actionNames));
        List<String> entryListValue = new ArrayList<String> (Arrays.asList(actionValues));
        Map<String, String> prefMap = new TreeMap<String, String>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(intent,
                PackageManager.PERMISSION_GRANTED);

        for (ResolveInfo rInfo : list) {
            String pkgName = rInfo.activityInfo.applicationInfo.packageName;
            String appName = rInfo.activityInfo.applicationInfo.loadLabel(pm)
                    .toString();
            prefMap.put(appName, pkgName);
        }
        getSortedListsUsingMap(prefMap, entryListValue, entryList);
        pref.setEntries((String[]) entryList.toArray(new String[entryList
                .size()]));
        pref.setEntryValues((String[]) entryListValue
                .toArray(new String[entryListValue.size()]));
        setPrefValue(pref, packageName);
    }

    private String getAppName(String packageName) {
        final PackageManager pm = mContext.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }
        String applicationName = (String) (ai != null ? pm
                .getApplicationLabel(ai) : null);
        return applicationName;
    }

    private void getSortedListsUsingMap(Map map, List list1, List list2) {
        Set<Map.Entry> set = map.entrySet();
        Iterator<Map.Entry> iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = iterator.next();
            list2.add(entry.getKey().toString());
            list1.add(entry.getValue().toString());
        }
    }
}
