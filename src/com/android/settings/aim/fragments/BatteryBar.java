/*
 * Copyright (C) 2017 AIM ROM
 *
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

package com.android.settings.aim.fragments;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.app.Fragment;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.preference.ColorPickerPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.MetricsProto.MetricsEvent;


/**
 * Created by cedwards on 6/3/2016.
 */
public class BatteryBar extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

        private static final String PREF_BATT_BAR = "battery_bar_list";
        private static final String PREF_BATT_BAR_NO_NAVBAR = "battery_bar_no_navbar_list";
        private static final String PREF_BATT_BAR_STYLE = "battery_bar_style";
        private static final String PREF_BATT_BAR_COLOR = "battery_bar_color";
        private static final String PREF_BATT_BAR_CHARGING_COLOR = "battery_bar_charging_color";
        private static final String PREF_BATT_BAR_BATTERY_LOW_COLOR = "battery_bar_battery_low_color";
        private static final String PREF_BATT_BAR_WIDTH = "battery_bar_thickness";
        private static final String PREF_BATT_ANIMATE = "battery_bar_animate";
        private static final String PREF_BATT_USE_CHARGING_COLOR = "battery_bar_enable_charging_color";
        private static final String PREF_BATT_BLEND_COLORS = "battery_bar_blend_color";
        private static final String PREF_BATT_BLEND_COLORS_REVERSE = "battery_bar_blend_color_reverse";

        private ListPreference mBatteryBar;
        private ListPreference mBatteryBarNoNavbar;
        private ListPreference mBatteryBarStyle;
        private ListPreference mBatteryBarThickness;
        private SwitchPreference mBatteryBarChargingAnimation;
        private SwitchPreference mBatteryBarUseChargingColor;
        private SwitchPreference mBatteryBarBlendColors;
        private SwitchPreference mBatteryBarBlendColorsReverse;
        private ColorPickerPreference mBatteryBarColor;
        private ColorPickerPreference mBatteryBarChargingColor;
        private ColorPickerPreference mBatteryBarBatteryLowColor;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.battery_bar);

            PreferenceScreen prefSet = getPreferenceScreen();
            ContentResolver resolver = getContentResolver();


            int barVal = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR, 0);
            mBatteryBar = (ListPreference) findPreference(PREF_BATT_BAR);
            mBatteryBar.setValue(String.valueOf(barVal));
            mBatteryBar.setOnPreferenceChangeListener(this);

            int barValNoNav = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR, 0);
            mBatteryBarNoNavbar = (ListPreference) findPreference(PREF_BATT_BAR_NO_NAVBAR);
            mBatteryBarNoNavbar.setValue(String.valueOf(barValNoNav));
            mBatteryBarNoNavbar.setOnPreferenceChangeListener(this);

            int barStyle = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_STYLE, 0);
            mBatteryBarStyle = (ListPreference) findPreference(PREF_BATT_BAR_STYLE);
            mBatteryBarStyle.setValue(String.valueOf(barStyle));
            mBatteryBarStyle.setOnPreferenceChangeListener(this);

            mBatteryBarColor = (ColorPickerPreference) findPreference(PREF_BATT_BAR_COLOR);
            int battcolor = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_COLOR, Color.WHITE);
            mBatteryBarColor.setNewPreviewColor(battcolor);
            mBatteryBarColor.setOnPreferenceChangeListener(this);

            mBatteryBarChargingColor = (ColorPickerPreference) findPreference(PREF_BATT_BAR_CHARGING_COLOR);
            mBatteryBarChargingColor.setOnPreferenceChangeListener(this);
            int battchcolor = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_CHARGING_COLOR, Color.WHITE);
            mBatteryBarChargingColor.setNewPreviewColor(battchcolor);
            mBatteryBarChargingColor.setOnPreferenceChangeListener(this);

            mBatteryBarBatteryLowColor = (ColorPickerPreference) findPreference(PREF_BATT_BAR_BATTERY_LOW_COLOR);
            mBatteryBarBatteryLowColor.setOnPreferenceChangeListener(this);
            int battlowcolor = Settings.System.getInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_BATTERY_LOW_COLOR, Color.WHITE);
            mBatteryBarBatteryLowColor.setNewPreviewColor(battlowcolor);
            mBatteryBarBatteryLowColor.setOnPreferenceChangeListener(this);

            mBatteryBarChargingAnimation = (SwitchPreference) findPreference(PREF_BATT_ANIMATE);
            mBatteryBarChargingAnimation.setChecked(Settings.System.getInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE, 0) == 1);

            mBatteryBarThickness = (ListPreference) findPreference(PREF_BATT_BAR_WIDTH);
            mBatteryBarThickness.setOnPreferenceChangeListener(this);
            mBatteryBarThickness.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS,
                0)));
            mBatteryBarThickness.setSummary(mBatteryBarThickness.getEntry());

            mBatteryBarUseChargingColor = (SwitchPreference) findPreference(PREF_BATT_USE_CHARGING_COLOR);
            mBatteryBarBlendColors = (SwitchPreference) findPreference(PREF_BATT_BLEND_COLORS);
            mBatteryBarBlendColorsReverse = (SwitchPreference) findPreference(PREF_BATT_BLEND_COLORS_REVERSE);

            boolean hasNavBarByDefault = getResources().getBoolean(
                    com.android.internal.R.bool.config_showNavigationBar);
            boolean enableNavigationBar = Settings.Secure.getInt(resolver,
                    Settings.Secure.NAVIGATION_BAR_VISIBLE, hasNavBarByDefault ? 1 : 0) == 1;
            boolean batteryBarSupported = Settings.Secure.getInt(resolver,
                    Settings.Secure.NAVIGATION_BAR_MODE, 0) == 0;

            if (!enableNavigationBar || !batteryBarSupported) {
                prefSet.removePreference(mBatteryBar);
            } else {
                prefSet.removePreference(mBatteryBarNoNavbar);
            }

            updateBatteryBarOptions();

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mBatteryBarColor) {
                int color = ((Integer) newValue).intValue();
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR_COLOR, color);
                return true;
            } else if (preference == mBatteryBarChargingColor) {
                int color = ((Integer) newValue).intValue();
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR_CHARGING_COLOR, color);
                return true;
            } else if (preference == mBatteryBarBatteryLowColor) {
                int color = ((Integer) newValue).intValue();
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR_BATTERY_LOW_COLOR, color);
                return true;
            } else if (preference == mBatteryBar) {
                int val = Integer.parseInt((String) newValue);
                int index = mBatteryBar.findIndexOfValue((String) newValue);
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR, val);
                //mBatteryBar.setSummary(mBatteryBar.getEntries()[index]);
                updateBatteryBarOptions();
            } else if (preference == mBatteryBarNoNavbar) {
                int val = Integer.parseInt((String) newValue);
                int index = mBatteryBarNoNavbar.findIndexOfValue((String) newValue);
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR, val);
                //mBatteryBarNoNavbar.setSummary(mBatteryBarNoNavbar.getEntries()[index]);
                updateBatteryBarOptions();
                return true;
            } else if (preference == mBatteryBarStyle) {
                int val = Integer.parseInt((String) newValue);
                int index = mBatteryBarStyle.findIndexOfValue((String) newValue);
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR_STYLE, val);
                //mBatteryBarStyle.setSummary(mBatteryBarStyle.getEntries()[index]);
                return true;
            } else if (preference == mBatteryBarThickness) {
                int val = Integer.parseInt((String) newValue);
                int index = mBatteryBarThickness.findIndexOfValue((String) newValue);
                Settings.System.putInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, val);
                //mBatteryBarThickness.setSummary(mBatteryBarThickness.getEntries()[index]);
                return true;
            }
            return false;
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            ContentResolver resolver = getActivity().getContentResolver();
            boolean value;
            if (preference == mBatteryBarChargingAnimation) {
                value = mBatteryBarChargingAnimation.isChecked();
                Settings.System.putInt(resolver, Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE, value ? 1 : 0);
                return true;
            }
            return false;
        }

        @Override
        protected int getMetricsCategory() {
             return MetricsEvent.AIM;
        }

        private void updateBatteryBarOptions() {
            if (Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR, 0) == 0) {
                mBatteryBarStyle.setEnabled(false);
                mBatteryBarThickness.setEnabled(false);
                mBatteryBarChargingAnimation.setEnabled(false);
                mBatteryBarColor.setEnabled(false);
                mBatteryBarChargingColor.setEnabled(false);
                mBatteryBarBatteryLowColor.setEnabled(false);
                mBatteryBarUseChargingColor.setEnabled(false);
                mBatteryBarBlendColors.setEnabled(false);
                mBatteryBarBlendColorsReverse.setEnabled(false);
            } else {
                mBatteryBarStyle.setEnabled(true);
                mBatteryBarThickness.setEnabled(true);
                mBatteryBarChargingAnimation.setEnabled(true);
                mBatteryBarColor.setEnabled(true);
                mBatteryBarChargingColor.setEnabled(true);
                mBatteryBarBatteryLowColor.setEnabled(true);
                mBatteryBarUseChargingColor.setEnabled(true);
                mBatteryBarBlendColors.setEnabled(true);
                mBatteryBarBlendColorsReverse.setEnabled(true);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }
}
