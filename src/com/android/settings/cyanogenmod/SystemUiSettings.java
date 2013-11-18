/*
 * Copyright (C) 2012 The CyanogenMod project
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

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemUiSettings extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";

    private PreferenceScreen mPieControl;
    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();
//
//        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);
//
//        // Expanded desktop
//        mExpandedDesktopPref = (ListPreference) findPreference(KEY_EXPANDED_DESKTOP);
//        mExpandedDesktopNoNavbarPref =
//                (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP_NO_NAVBAR);
//
//        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
//                getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);
//
//        int expandedDesktopValue = Settings.System.getInt(getContentResolver(),
//                Settings.System.EXPANDED_DESKTOP_STYLE, 0);
//
        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
//
//            // Hide no-op "Status bar visible" mode on devices without navigation bar
//            if (hasNavBar) {
//                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
//                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
//                updateExpandedDesktop(expandedDesktopValue);
//                prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
//            } else {
//                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
//                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
//                prefScreen.removePreference(mExpandedDesktopPref);
//            }
//
            // Hide navigation bar category on devices without navigation bar
            if (!hasNavBar) {
                prefScreen.removePreference(findPreference(CATEGORY_NAVBAR));
//                mPieControl = null;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //updatePieControlSummary();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
//        if (preference == mExpandedDesktopPref) {
//            int expandedDesktopValue = Integer.valueOf((String) objValue);
//            updateExpandedDesktop(expandedDesktopValue);
//            return true;
//        } else if (preference == mExpandedDesktopNoNavbarPref) {
//            boolean value = (Boolean) objValue;
//            updateExpandedDesktop(value ? 2 : 0);
//            return true;
//        }
//
        return false;
    }
//
//    private void updatePieControlSummary() {
//        if (mPieControl != null) {
//            boolean enabled = Settings.System.getInt(getContentResolver(),
//                Settings.System.PIE_CONTROLS, 0) != 0;
//
//            if (enabled) {
//                mPieControl.setSummary(R.string.pie_control_enabled);
//            } else {
//                mPieControl.setSummary(R.string.pie_control_disabled);
//            }
//        }
//    }
//
//    private void updateExpandedDesktop(int value) {
//        ContentResolver cr = getContentResolver();
//        Resources res = getResources();
//        int summary = -1;
//
//        Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STYLE, value);
//
//        if (value == 0) {
//            // Expanded desktop deactivated
//            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 0);
//            Settings.System.putInt(cr, Settings.System.EXPANDED_DESKTOP_STATE, 0);
//            summary = R.string.expanded_desktop_disabled;
//        } else if (value == 1) {
//            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
//            summary = R.string.expanded_desktop_status_bar;
//        } else if (value == 2) {
//            Settings.System.putInt(cr, Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
//            summary = R.string.expanded_desktop_no_status_bar;
//        }
//
//        if (mExpandedDesktopPref != null && summary != -1) {
//            mExpandedDesktopPref.setSummary(res.getString(summary));
//        }
//    }
}
