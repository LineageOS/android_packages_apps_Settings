/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

public class PowerMenuActions extends SettingsPreferenceFragment {
    final static String TAG = "PowerMenuActions";

    /* Valid settings for global actions keys.
     * see config.xml config_globalActionList */
    private static final String GLOBAL_ACTION_KEY_POWER = "power";
    private static final String GLOBAL_ACTION_KEY_REBOOT = "reboot";
    private static final String GLOBAL_ACTION_KEY_AIRPLANE = "airplane";
    private static final String GLOBAL_ACTION_KEY_BUGREPORT = "bugreport";
    private static final String GLOBAL_ACTION_KEY_SILENT = "silent";
    private static final String GLOBAL_ACTION_KEY_USERS = "users";
    private static final String GLOBAL_ACTION_KEY_SETTINGS = "settings";
    private static final String GLOBAL_ACTION_KEY_LOCKDOWN = "lockdown";
    private static final String GLOBAL_ACTION_KEY_PROFILE = "profile";

    private CheckBoxPreference mPowerPref;
    private CheckBoxPreference mRebootPref;
    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mBugReportPref;
    private CheckBoxPreference mSilentPref;
    private CheckBoxPreference mUsersPref;
    private CheckBoxPreference mSettingsPref;
    private CheckBoxPreference mLockdownPref;
    private CheckBoxPreference mProfilePref;

    Context mContext;
    private ArrayList<String> mLocalUserConfig = new ArrayList<String>();

    private static String[] AVAILABLE_ACTIONS = {
        GLOBAL_ACTION_KEY_POWER, GLOBAL_ACTION_KEY_REBOOT, GLOBAL_ACTION_KEY_AIRPLANE,
        GLOBAL_ACTION_KEY_BUGREPORT, GLOBAL_ACTION_KEY_SILENT, GLOBAL_ACTION_KEY_USERS,
        GLOBAL_ACTION_KEY_SETTINGS, GLOBAL_ACTION_KEY_LOCKDOWN, GLOBAL_ACTION_KEY_PROFILE
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);
        mContext = getActivity().getApplicationContext();

        getUserConfig();
    }

    @Override
    public void onStart() {
        super.onStart();
        mPowerPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_POWER);
        mPowerPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_POWER));

        mRebootPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_REBOOT);
        mRebootPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_REBOOT));

        mAirplanePref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
        mAirplanePref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_AIRPLANE));

        mBugReportPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_BUGREPORT);
        mBugReportPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_BUGREPORT));

        mSilentPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_SILENT);
        mSilentPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SILENT));

        mUsersPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_USERS);
        mUsersPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_USERS));
        setUsersEnabled();

        mSettingsPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_SETTINGS);
        mSettingsPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SETTINGS));

        mLockdownPref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_LOCKDOWN);
        mLockdownPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_LOCKDOWN));

        mProfilePref = (CheckBoxPreference) findPreference(GLOBAL_ACTION_KEY_PROFILE);
        mProfilePref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_PROFILE));

        updatePreferences();

        if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
            getPreferenceScreen().removePreference(
                    findPreference(GLOBAL_ACTION_KEY_USERS));
        }
    }

    private boolean settingsArrayContains(String preference) {
        return mLocalUserConfig.contains(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mPowerPref) {
            value = mPowerPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_POWER);

        } else if (preference == mRebootPref) {
            value = mRebootPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_REBOOT);

        } else if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_AIRPLANE);

        } else if (preference == mBugReportPref) {
            value = mBugReportPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_BUGREPORT);

        } else if (preference == mSilentPref) {
            value = mSilentPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_SILENT);

        } else if (preference == mUsersPref) {
            value = mUsersPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_USERS);

        } else if (preference == mSettingsPref) {
            value = mSettingsPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_SETTINGS);

        } else if (preference == mLockdownPref) {
            value = mLockdownPref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_LOCKDOWN);

        } else if (preference == mProfilePref) {
            value = mProfilePref.isChecked();
            updateUserConfig(value, GLOBAL_ACTION_KEY_PROFILE);

        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    private void updateUserConfig(boolean enabled, String action) {
        if (enabled) {
            if (!settingsArrayContains(action)) {
                mLocalUserConfig.add(action);
            }
        } else {
            if (settingsArrayContains(action)) {
                mLocalUserConfig.remove(action);
            }
        }
        saveUserConfig();
    }

    private void setUsersEnabled() {
        List<UserInfo> users = ((UserManager) mContext.getSystemService(Context.USER_SERVICE))
                .getUsers();

        if (mUsersPref != null) {
            if (users.size() > 1) {
                mUsersPref.setEnabled(true);
            } else {
                mUsersPref.setEnabled(false);
            }
        }
    }

    private void updatePreferences() {
        boolean bugreport = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.BUGREPORT_IN_POWER_MENU, 0) != 0;
        boolean profiles = Settings.System.getInt(getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) != 0;

        // Only enable profiles item if System Profiles are also enabled
        mProfilePref.setEnabled(profiles);
        if (profiles) {
            mProfilePref.setTitle(R.string.power_menu_profiles_title);
        } else {
            mProfilePref.setTitle(R.string.power_menu_profiles_disabled);
        }

        mBugReportPref.setEnabled(bugreport);
        if (bugreport) {
            mBugReportPref.setTitle(R.string.power_menu_bug_report_title);
        } else {
            mBugReportPref.setTitle(R.string.power_menu_bug_report_disabled);
        }
    }

    private void getUserConfig() {
        mLocalUserConfig.clear();
        String[] defaultActions;
        String savedActions = Settings.Global.getStringForUser(mContext.getContentResolver(),
                Settings.Global.POWER_MENU_ACTIONS, UserHandle.USER_CURRENT);
        if (savedActions == null) {
            defaultActions = mContext.getResources().getStringArray(
                    com.android.internal.R.array.config_globalActionsList);
            for (String action : defaultActions) {
                mLocalUserConfig.add(action);
            }
        } else {
            for (String action : savedActions.split("\\|")) {
                mLocalUserConfig.add(action);
            }
        }
    }

    private void saveUserConfig() {
        StringBuilder s = new StringBuilder();

        /* Enjoy some cheeseburgers until I finish the DragSortListView */
        ArrayList<String> setactions = new ArrayList<String>();
        for (String action : AVAILABLE_ACTIONS) {
            if (settingsArrayContains(action)) {
                setactions.add(action);
            } else {
                continue;
            }
        }
        /* Don't bitch ploxz. kthnx. */

        for (int i = 0; i < setactions.size(); i++) {
            s.append(setactions.get(i).toString());
            if (i != setactions.size() - 1) {
                s.append("|");
            }
        }

        Settings.Global.putStringForUser(getContentResolver(),
                 Settings.Global.POWER_MENU_ACTIONS, s.toString(), UserHandle.USER_CURRENT);
        updateRebootDialog();
    }

    private void updateRebootDialog() {
        Intent u = new Intent();
        u.setAction(Intent.UPDATE_POWER_MENU);
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }
}
