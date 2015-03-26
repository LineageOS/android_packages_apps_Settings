/*
 * Copyright (C) 2013 The CyanogenMod project
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

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP = "hardware_keys_home_double_tap";
    private static final String KEY_HOME_ANSWER_CALL = "hardware_keys_home_answer_call";
    private static final String KEY_MENU_PRESS = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String KEY_VOLUME_WAKE_DEVICE = "volume_key_wake_device";
    private static final String KEY_POWER_END_CALL = "hardware_keys_power_key_end_call";
    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String CATEGORY_NAVBAR = "navigation_bar";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;
    private static final int ACTION_LAUNCH_CAMERA = 6;
    private static final int ACTION_LAST_APP = 7;
    private static final int ACTION_SLEEP = 8;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;

    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private CheckBoxPreference mHomeAnswerCallAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private CheckBoxPreference mVolumeKeyWakeControl;
    private CheckBoxPreference mPowerKeyEndCallAction;

    private PreferenceCategory mNavigationPreferencesCat;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;

        boolean hasAnyBindableKey = false;
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);

        mHandler = new Handler();

        if (hasPowerKey) {
            int defaultPowerAction = 0;
            int powerEndCall = Settings.Secure.getInt(resolver,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            mPowerKeyEndCallAction = initCheckBox(KEY_POWER_END_CALL, (powerEndCall == Settings
                    .Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP));
        }

        Intent intent = ((SearchManager) getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(getActivity(), true, UserHandle.USER_CURRENT);
        List<ResolveInfo> list = null;
        PackageManager pm = getPackageManager();
        if (intent != null) {
            list = pm.queryIntentActivities(intent, 0);
        }
        boolean assistAvailable = list != null && !list.isEmpty();
        intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        list = pm.queryIntentActivities(intent, 0);
        boolean voiceAssistAvailable = !list.isEmpty();

        if (hasHomeKey) {
            int defaultLongPressAction = res.getInteger(
                    com.android.internal.R.integer.config_longPressOnHomeBehavior);
            if (defaultLongPressAction < ACTION_NOTHING ||
                    defaultLongPressAction > ACTION_IN_APP_SEARCH) {
                defaultLongPressAction = ACTION_NOTHING;
            }

            int defaultDoubleTapAction = res.getInteger(
                    com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
            if (defaultDoubleTapAction < ACTION_NOTHING ||
                    defaultDoubleTapAction > ACTION_IN_APP_SEARCH) {
                defaultDoubleTapAction = ACTION_NOTHING;
            }

            int longPressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                    defaultLongPressAction);
            mHomeLongPressAction = initActionList(KEY_HOME_LONG_PRESS, longPressAction);
            if (!assistAvailable) {
                filterEntry(mHomeLongPressAction,
                        getString(R.string.hardware_keys_action_search),
                        ACTION_SEARCH);
            }
            if (!voiceAssistAvailable) {
                filterEntry(mHomeLongPressAction,
                        getString(R.string.hardware_keys_action_voice_search),
                        ACTION_VOICE_SEARCH);
            }

            int doubleTapAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                    defaultDoubleTapAction);
            mHomeDoubleTapAction = initActionList(KEY_HOME_DOUBLE_TAP, doubleTapAction);
            if (!assistAvailable) {
                filterEntry(mHomeDoubleTapAction,
                        getString(R.string.hardware_keys_action_search),
                        ACTION_SEARCH);
            }
            if (!voiceAssistAvailable) {
                filterEntry(mHomeDoubleTapAction,
                        getString(R.string.hardware_keys_action_voice_search),
                        ACTION_VOICE_SEARCH);
            }

            int defaultAnswerAction = 0;
            int answerCallAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_HOME_ANSWER_RINGING_CALL,
                    defaultAnswerAction);
            mHomeAnswerCallAction = initCheckBox(KEY_HOME_ANSWER_CALL, (answerCallAction == 1));

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(homeCategory);
        }

        if (hasMenuKey) {
            int pressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_MENU_ACTION, ACTION_MENU);
            mMenuPressAction = initActionList(KEY_MENU_PRESS, pressAction);
            if (!assistAvailable) {
                filterEntry(mMenuPressAction,
                        getString(R.string.hardware_keys_action_search),
                        ACTION_SEARCH);
            }
            if (!voiceAssistAvailable) {
                filterEntry(mMenuPressAction,
                        getString(R.string.hardware_keys_action_voice_search),
                        ACTION_VOICE_SEARCH);
            }

            int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                        hasAssistKey ? ACTION_NOTHING : ACTION_SEARCH);
            mMenuLongPressAction = initActionList(KEY_MENU_LONG_PRESS, longPressAction);
            if (!assistAvailable) {
                filterEntry(mMenuLongPressAction,
                        getString(R.string.hardware_keys_action_search),
                        ACTION_SEARCH);
            }
            if (!voiceAssistAvailable) {
                filterEntry(mMenuLongPressAction,
                        getString(R.string.hardware_keys_action_voice_search),
                        ACTION_VOICE_SEARCH);
            }

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(menuCategory);
        }

        int wakeControlAction = Settings.System.getInt(resolver,
                Settings.System.VOLUME_WAKE_SCREEN, 0);
        mVolumeKeyWakeControl = initCheckBox(KEY_VOLUME_WAKE_DEVICE, (wakeControlAction == 1));
    }

    private CheckBoxPreference initCheckBox(String key, boolean checked) {
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) getPreferenceManager()
                .findPreference(key);
        if (checkBoxPreference != null) {
            checkBoxPreference.setChecked(checked);
            checkBoxPreference.setOnPreferenceChangeListener(this);
        }
        return checkBoxPreference;
    }

    private static void filterEntry(ListPreference listPreference, String entry,
        int entryValue) {
        CharSequence[] entries = listPreference.getEntries();
        int size = entries.length;
        CharSequence[] filteredEntries = new CharSequence[size - 1];

        int pos = 0; //new field in
        for (int i = 0; i < size; i++) {
            if (!entry.equals(entries[i])) {
                filteredEntries[pos] = entries[i];
                pos++;
            }
        }
        listPreference.setEntries(filteredEntries);

        CharSequence[] entryValues = listPreference.getEntryValues();
        size = entryValues.length;
        CharSequence[] filteredEntryValues = new CharSequence[size - 1];

        pos = 0;
        for (int i = 0; i < size; i++) {
            if (entryValue != Integer.parseInt((String) entryValues[i])) {
                filteredEntryValues[pos] = entryValues[i];
                pos++;
            }
        }

        listPreference.setEntryValues(filteredEntryValues);
    }

    private void handleCheckBoxChange(CheckBoxPreference pref, Object newValue, String setting) {
        Boolean value = (Boolean) newValue;
        int intValue = (value) ? 1 : 0;
        Settings.System.putInt(getContentResolver(), setting, intValue);
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHomeLongPressAction) {
            handleActionListChange(mHomeLongPressAction, newValue,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mHomeDoubleTapAction) {
            handleActionListChange(mHomeDoubleTapAction, newValue,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            return true;
        } else if (preference == mMenuPressAction) {
            handleActionListChange(mMenuPressAction, newValue,
                    Settings.System.KEY_MENU_ACTION);
            return true;
        } else if (preference == mMenuLongPressAction) {
            handleActionListChange(mMenuLongPressAction, newValue,
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);
            return true;
       } else if (preference == mVolumeKeyWakeControl) {
            handleCheckBoxChange(mVolumeKeyWakeControl, newValue,
                    Settings.System.VOLUME_WAKE_SCREEN);
            return true;
        } else if (preference == mHomeAnswerCallAction) {
            handleCheckBoxChange(mHomeAnswerCallAction, newValue,
                    Settings.System.KEY_HOME_ANSWER_RINGING_CALL);
            return true;
        } else if (preference == mPowerKeyEndCallAction) {
            Boolean value = (Boolean) newValue;
            int intValue = (value) ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP :
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF;
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, intValue);
            return true;
        }
        return false;
    }

}
