package com.android.settings.aim.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.util.cm.PowerMenuConstants;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import cyanogenmod.providers.CMSettings;

import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_AIRPLANE;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_ASSIST;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_LOCKDOWN;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_RESTART;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_SCREENSHOT;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_SETTINGS;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_SILENT;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_USERS;
import static com.android.internal.util.cm.PowerMenuConstants.GLOBAL_ACTION_KEY_VOICEASSIST;

public class PowerMenuSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    final static String TAG = "PowerMenuSettings";

    private CheckBoxPreference mRebootPref;
    private CheckBoxPreference mScreenshotPref;
    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mUsersPref;
    private CheckBoxPreference mSettingsPref;
    private CheckBoxPreference mLockdownPref;
    private CheckBoxPreference mSilentPref;
    private CheckBoxPreference mVoiceAssistPref;
    private CheckBoxPreference mAssistPref;

    Context mContext;
    private ArrayList<String> mLocalUserConfig = new ArrayList<String>();
    private String[] mAvailableActions;
    private String[] mAllActions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aim_power_menu);
        mContext = getActivity().getApplicationContext();

        final PreferenceScreen prefScreen = getPreferenceScreen();
        mAvailableActions = getActivity().getResources().getStringArray(
                R.array.power_menu_actions_array);
        mAllActions = PowerMenuConstants.getAllActions();

        for (String action : mAllActions) {
            // Remove preferences not present in the overlay
            if (!isActionAllowed(action)) {
                if (prefScreen.findPreference(action) != null) {
                    prefScreen.removePreference(prefScreen.findPreference(action));
                }
                continue;
            }

            if (action.equals(GLOBAL_ACTION_KEY_RESTART)) {
                mRebootPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_RESTART);
                mRebootPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_SCREENSHOT)) {
                mScreenshotPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_SCREENSHOT);
                mScreenshotPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_AIRPLANE)) {
                mAirplanePref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
                mAirplanePref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_USERS)) {
                mUsersPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_USERS);
                mUsersPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_SETTINGS)) {
                mSettingsPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_SETTINGS);
                mSettingsPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_LOCKDOWN)) {
                mLockdownPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_LOCKDOWN);
                mLockdownPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_SILENT)) {
                mSilentPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_SILENT);
                mSilentPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_VOICEASSIST)) {
                mVoiceAssistPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_VOICEASSIST);
                mVoiceAssistPref.setOnPreferenceChangeListener(this);
            } else if (action.equals(GLOBAL_ACTION_KEY_ASSIST)) {
                mAssistPref = (CheckBoxPreference) prefScreen.findPreference(GLOBAL_ACTION_KEY_ASSIST);
                mAssistPref.setOnPreferenceChangeListener(this);
            }
        }

        getUserConfig();
    }

    @Override
    public void onStart() {
        super.onStart();

        final PreferenceScreen prefScreen = getPreferenceScreen();

        if (mRebootPref != null) {
            mRebootPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_RESTART));
        }

        if (mScreenshotPref != null) {
            mScreenshotPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SCREENSHOT));
        }

        if (mAirplanePref != null) {
            mAirplanePref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_AIRPLANE));
        }

        if (mUsersPref != null) {
            if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
                if (prefScreen.findPreference(GLOBAL_ACTION_KEY_USERS) != null) {
                    prefScreen.removePreference(prefScreen.findPreference(GLOBAL_ACTION_KEY_USERS));
                }
                mUsersPref = null;
            } else {
                List<UserInfo> users = ((UserManager) mContext.getSystemService(
                        Context.USER_SERVICE)).getUsers();
                boolean enabled = (users.size() > 1);
                mUsersPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_USERS) && enabled);
                mUsersPref.setEnabled(enabled);
            }
        }

        if (mSettingsPref != null) {
            mSettingsPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SETTINGS));
        }

        if (mLockdownPref != null) {
            mLockdownPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_LOCKDOWN));
        }

        if (mSilentPref != null) {
            mSilentPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_SILENT));
        }

        if (mVoiceAssistPref != null) {
            mVoiceAssistPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_VOICEASSIST));
        }

        if (mAssistPref != null) {
            mAssistPref.setChecked(settingsArrayContains(GLOBAL_ACTION_KEY_ASSIST));
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = newValue == null ? false : (boolean) newValue;

        if (preference == mRebootPref) {
            mRebootPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_RESTART);

        } else if (preference == mScreenshotPref) {
            mScreenshotPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_SCREENSHOT);

        } else if (preference == mAirplanePref) {
            mAirplanePref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_AIRPLANE);

        } else if (preference == mUsersPref) {
            mUsersPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_USERS);

        } else if (preference == mSettingsPref) {
            mSettingsPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_SETTINGS);

        } else if (preference == mLockdownPref) {
            mLockdownPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_LOCKDOWN);

        } else if (preference == mSilentPref) {
            mSilentPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_SILENT);

        } else if (preference == mVoiceAssistPref) {
            mVoiceAssistPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_VOICEASSIST);

        } else if (preference == mAssistPref) {
            mAssistPref.setChecked(value);
            updateUserConfig(value, GLOBAL_ACTION_KEY_ASSIST);

        } else {
            return false;
        }
        return true;
    }

    private boolean settingsArrayContains(String preference) {
        return mLocalUserConfig.contains(preference);
    }

    private boolean isActionAllowed(String action) {
        if (Arrays.asList(mAvailableActions).contains(action)) {
            return true;
        }
        return false;
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

    private void getUserConfig() {
        mLocalUserConfig.clear();
        String[] defaultActions;
        String savedActions = CMSettings.Secure.getStringForUser(mContext.getContentResolver(),
                CMSettings.Secure.POWER_MENU_ACTIONS, UserHandle.USER_CURRENT);

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

        // TODO: Use DragSortListView
        ArrayList<String> setactions = new ArrayList<String>();
        for (String action : mAllActions) {
            if (settingsArrayContains(action) && isActionAllowed(action)) {
                setactions.add(action);
            } else {
                continue;
            }
        }

        for (int i = 0; i < setactions.size(); i++) {
            s.append(setactions.get(i).toString());
            if (i != setactions.size() - 1) {
                s.append("|");
            }
        }

        CMSettings.Secure.putStringForUser(getContentResolver(),
                CMSettings.Secure.POWER_MENU_ACTIONS, s.toString(), UserHandle.USER_CURRENT);
        updatePowerMenuDialog();
    }

    private void updatePowerMenuDialog() {
        Intent u = new Intent();
        u.setAction(Intent.UPDATE_POWER_MENU);
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }
}
