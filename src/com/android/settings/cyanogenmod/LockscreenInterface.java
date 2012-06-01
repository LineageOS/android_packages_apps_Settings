/*
 * Copyright (C) 2012 CyanogenMod
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

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notificationlight.ColorPickerView;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";
    private static final int LOCKSCREEN_BACKGROUND = 1024;
    public static final String KEY_WEATHER_PREF = "lockscreen_weather";
    public static final String KEY_CALENDAR_PREF = "lockscreen_calendar";
    public static final String KEY_BACKGROUND_PREF = "lockscreen_background";
    private ListPreference mCustomBackground;
    private Preference mWeatherPref;
    private Preference mCalendarPref;
    private Activity mActivity;
    ContentResolver mResolver;

    private File wallpaperImage;
    private File wallpaperTemporary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        mWeatherPref = (Preference) findPreference(KEY_WEATHER_PREF);
        mCalendarPref = (Preference) findPreference(KEY_CALENDAR_PREF);
        mCustomBackground = (ListPreference) findPreference(KEY_BACKGROUND_PREF);
        mCustomBackground.setOnPreferenceChangeListener(this);
        wallpaperImage = new File(mActivity.getFilesDir()+"/lockwallpaper");
        wallpaperTemporary = new File(mActivity.getCacheDir()+"/lockwallpaper.tmp");
        updateCustomBackgroundSummary();
    }

    private void updateCustomBackgroundSummary() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND);
        if (value == null) {
            resId = R.string.lockscreen_background_default_wallpaper;
            mCustomBackground.setValueIndex(2);
        } else if (value.isEmpty()) {
            resId = R.string.lockscreen_background_custom_image;
            mCustomBackground.setValueIndex(1);
        } else {
            resId = R.string.lockscreen_background_color_fill;
            mCustomBackground.setValueIndex(0);
        }
        mCustomBackground.setSummary(getResources().getString(resId));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
        // Set the weather description text
        if (mWeatherPref != null) {
            boolean weatherEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_WEATHER, 0) == 1;
            if (weatherEnabled) {
                mWeatherPref.setSummary(R.string.lockscreen_weather_enabled);
            } else {
                mWeatherPref.setSummary(R.string.lockscreen_weather_summary);
            }
        }

        // Set the calendar description text
        if (mCalendarPref != null) {
            boolean weatherEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CALENDAR, 0) == 1;
            if (weatherEnabled) {
                mCalendarPref.setSummary(R.string.lockscreen_calendar_enabled);
            } else {
                mCalendarPref.setSummary(R.string.lockscreen_calendar_summary);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOCKSCREEN_BACKGROUND) {
            if (resultCode == Activity.RESULT_OK) {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.renameTo(wallpaperImage);
                }
                wallpaperImage.setReadOnly();
                Toast.makeText(mActivity, getResources().getString(R.string.
                        lockscreen_background_result_successful), Toast.LENGTH_LONG).show();
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND,"");
                updateCustomBackgroundSummary();
            } else {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.delete();
                }
                Toast.makeText(mActivity, getResources().getString(R.string.
                        lockscreen_background_result_not_successful), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mCustomBackground) {
            int indexOf = mCustomBackground.findIndexOfValue(objValue.toString());
            switch (indexOf) {
            //Displays color dialog when user has chosen color fill
            case 0:
                final ColorPickerView colorView = new ColorPickerView(mActivity);
                int currentColor = Settings.System.getInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, -1);
                if (currentColor != -1) {
                    colorView.setColor(currentColor);
                }
                colorView.setAlphaSliderVisible(true);
                new AlertDialog.Builder(mActivity)
                .setTitle(R.string.lockscreen_custom_background_dialog_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BACKGROUND, colorView.getColor());
                        updateCustomBackgroundSummary();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setView(colorView).show();
                return false;
            //Launches intent for user to select an image/crop it to set as background
            case 1:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("scale", true);
                intent.putExtra("scaleUpIfNeeded", false);
                intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
                int width = mActivity.getWindowManager().getDefaultDisplay().getWidth();
                int height = mActivity.getWindowManager().getDefaultDisplay().getHeight();
                Rect rect = new Rect();
                Window window = mActivity.getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(rect);
                int statusBarHeight = rect.top;
                int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
                int titleBarHeight = contentViewTop - statusBarHeight;
                boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;
                intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
                intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
                try {
                    wallpaperTemporary.createNewFile();
                    wallpaperTemporary.setWritable(true, false);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(wallpaperTemporary));
                    intent.putExtra("return-data", false);
                    mActivity.startActivityFromFragment(this, intent, LOCKSCREEN_BACKGROUND);
                } catch (IOException e) {
                } catch (ActivityNotFoundException e) {
                }
                return false;
            //Sets background color to default
            case 2:
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, null);
                updateCustomBackgroundSummary();
                break;
            }
            return true;
        }
        return false;
    }
}
