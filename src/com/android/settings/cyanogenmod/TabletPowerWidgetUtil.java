/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import com.android.settings.R;

import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TabletPowerWidgetUtil {

    protected static final String NO_TOGGLES = "no_toggles";

   protected static final String[] KEY_TOGGLES = new String[]{"pref_airplane_toggle", "pref_rotate_toggle", "pref_bluetooth_toggle", "pref_gps_toggle", "pref_wifi_toggle", "pref_flashlight_toggle", "pref_mobile_data_toggle", "pref_network_mode_toggle", "pref_sound_toggle"};

    public static final HashMap<String, ButtonInfo> BUTTONS = new HashMap<String, ButtonInfo>();
    static {
        BUTTONS.put(KEY_TOGGLES[0], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[0], R.string.title_toggle_airplane,
                "com.android.systemui:drawable/stat_airplane_on"));
        BUTTONS.put(KEY_TOGGLES[1], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[1], R.string.title_toggle_autorotate,
                "com.android.systemui:drawable/stat_orientation_on"));
        BUTTONS.put(KEY_TOGGLES[2], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[2], R.string.title_toggle_bluetooth,
                "com.android.systemui:drawable/stat_bluetooth_on"));
        BUTTONS.put(KEY_TOGGLES[3], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[3], R.string.title_toggle_gps, "com.android.systemui:drawable/stat_gps_on"));
        BUTTONS.put(KEY_TOGGLES[4], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[4], R.string.title_toggle_wifi,
                "com.android.systemui:drawable/stat_wifi_on"));
        BUTTONS.put(KEY_TOGGLES[5], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[5], R.string.title_toggle_flashlight,
                "com.android.systemui:drawable/stat_flashlight_on"));
        BUTTONS.put(KEY_TOGGLES[6], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[6], R.string.title_toggle_mobiledata,
                "com.android.systemui:drawable/stat_data_on"));
        BUTTONS.put(KEY_TOGGLES[7], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[7], R.string.title_toggle_networkmode,
                "com.android.systemui:drawable/stat_2g3g_on"));
        BUTTONS.put(KEY_TOGGLES[8], new TabletPowerWidgetUtil.ButtonInfo(
                KEY_TOGGLES[8], R.string.title_toggle_sound,
                "com.android.systemui:drawable/stat_ring_on"));
    }

    protected static final String BUTTON_DELIMITER = "\\|";
    protected static final String BUTTONS_DEFAULT = KEY_TOGGLES[0]
        + BUTTON_DELIMITER + KEY_TOGGLES[4]
        + BUTTON_DELIMITER + KEY_TOGGLES[2]
        + BUTTON_DELIMITER + KEY_TOGGLES[1];

    public static String getCurrentButtons(Context context) {
        String buttons = Settings.System.getString(context.getContentResolver(),
                Settings.System.WIDGET_BUTTONS_TABLET);
        if (buttons == null)
            buttons = BUTTONS_DEFAULT;
        return buttons;
    }

    public static void saveCurrentButtons(Context context, String buttons) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.WIDGET_BUTTONS_TABLET, buttons);
    }

    public static String mergeInNewButtonString(String oldString, String newString) {
        ArrayList<String> oldList = getButtonListFromString(oldString);
        ArrayList<String> newList = getButtonListFromString(newString);
        ArrayList<String> mergedList = new ArrayList<String>();

        // add any items from oldlist that are in new list
        for (String button : oldList) {
            if (newList.contains(button)) {
                mergedList.add(button);
            }
        }

        // append anything in newlist that isn't already in the merged list to
        // the end of the list
        for (String button : newList) {
            if (!mergedList.contains(button)) {
                mergedList.add(button);
            }
        }

        // return merged list
        return getButtonStringFromList(mergedList);
    }

    public static ArrayList<String> getButtonListFromString(String buttons) {
        String[] buttonArray = buttons.split("\\|");
        ArrayList<String> mButtonContainer = new ArrayList();
        for(int i=0; i<buttonArray.length; i++){
            mButtonContainer.add(buttonArray[i].replace("\\", ""));
        }
        return mButtonContainer;
    }

    public static String getButtonStringFromList(ArrayList<String> buttons) {
        if (buttons == null || buttons.size() <= 0) {
            return "";
        } else {
            String s = buttons.get(0);
            for (int i = 1; i < buttons.size(); i++) {
                s += BUTTON_DELIMITER + buttons.get(i);
            }
            return s;
        }
    }

    public static class ButtonInfo {
        private String mId;
        private int mTitleResId;
        private String mIcon;

        public ButtonInfo(String id, int titleResId, String icon) {
            mId = id;
            mTitleResId = titleResId;
            mIcon = icon;
        }

        public String getId() {
            return mId;
        }

        public int getTitleResId() {
            return mTitleResId;
        }

        public String getIcon() {
            return mIcon;
        }
    }
}
