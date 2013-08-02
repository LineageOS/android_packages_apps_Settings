/*
 * Copyright (C) 2012 The CyanogenMod Project
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
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notificationlight.ColorPickerView;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final int REQUEST_CODE_BG_WALLPAPER = 1024;

    private static final int LOCKSCREEN_BACKGROUND_COLOR_FILL = 0;
    private static final int LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE = 1;
    private static final int LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER = 2;

    private static final String KEY_ALWAYS_BATTERY = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_LOCKSCREEN_MAXIMIZE_WIDGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private static final String KEY_BACKGROUND = "lockscreen_background";
    private static final String KEY_SCREEN_SECURITY = "screen_security";

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_LOCKSCREEN_ENABLE_WIDGETS = "lockscreen_enable_widgets";
    private static final String KEY_LOCKSCREEN_ENABLE_CAMERA = "lockscreen_enable_camera";

    private ListPreference mCustomBackground;
    private ListPreference mBatteryStatus;
    private CheckBoxPreference mMaximizeWidgets;
    private CheckBoxPreference mMusicControls;
    private CheckBoxPreference mEnableWidgets;
    private CheckBoxPreference mEnableCamera;

    private File mWallpaperImage;
    private File mWallpaperTemporary;
    private DevicePolicyManager mDPM;

    private boolean mIsPrimary;

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        PreferenceCategory generalCategory = (PreferenceCategory) findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory widgetsCategory = (PreferenceCategory) findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Determine which user is logged in
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mIsPrimary) {
            // Its the primary user, show all the settings
            mBatteryStatus = (ListPreference) findPreference(KEY_ALWAYS_BATTERY);
            if (mBatteryStatus != null) {
                mBatteryStatus.setOnPreferenceChangeListener(this);
            }

            mMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MAXIMIZE_WIDGETS);
            if (!Utils.isPhone(getActivity())) {
                widgetsCategory.removePreference(mMaximizeWidgets);
                mMaximizeWidgets = null;
            } else {
                mMaximizeWidgets.setOnPreferenceChangeListener(this);
            }

            mMusicControls = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MUSIC_CONTROLS);
            mMusicControls.setOnPreferenceChangeListener(this);

            PreferenceScreen lockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
            if (!hasButtons()) {
                generalCategory.removePreference(lockscreenButtons);
            }
        } else {
            // Secondary user is logged in, remove all primary user specific preferences
            generalCategory.removePreference(findPreference(KEY_SCREEN_SECURITY));
            widgetsCategory.removePreference(findPreference(KEY_LOCKSCREEN_MAXIMIZE_WIDGETS));
            generalCategory.removePreference(findPreference(KEY_ALWAYS_BATTERY));
            generalCategory.removePreference(findPreference(KEY_LOCKSCREEN_BUTTONS));
        }

        // This applies to all users
        mCustomBackground = (ListPreference) findPreference(KEY_BACKGROUND);
        mCustomBackground.setOnPreferenceChangeListener(this);
        updateCustomBackgroundSummary();

        mEnableWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ENABLE_WIDGETS);
        mEnableWidgets.setOnPreferenceChangeListener(this);
        mEnableCamera = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ENABLE_CAMERA);
        mEnableCamera.setOnPreferenceChangeListener(this);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        int disabledFeatures = mDPM.getKeyguardDisabledFeatures(null);
        mEnableWidgets.setChecked((disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL) == 0);
        mEnableCamera.setChecked((disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA) == 0);

        // Remove the camera widget preference if the device doesn't have one
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            widgetsCategory.removePreference(mEnableCamera);
        }

        mWallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper");
        mWallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");

        // Don't display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK), widgetsCategory);
    }

    private void updateCustomBackgroundSummary() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND);
        if (value == null) {
            resId = R.string.lockscreen_background_default_wallpaper;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER);
        } else if (value.isEmpty()) {
            resId = R.string.lockscreen_background_custom_image;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE);
        } else {
            resId = R.string.lockscreen_background_color_fill;
            mCustomBackground.setValueIndex(LOCKSCREEN_BACKGROUND_COLOR_FILL);
        }
        mCustomBackground.setSummary(getResources().getString(resId));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsPrimary) {
            ContentResolver cr = getActivity().getContentResolver();
            if (mBatteryStatus != null) {
                int batteryStatus = Settings.System.getInt(cr,
                        Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, 0);
                mBatteryStatus.setValueIndex(batteryStatus);
                mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
            }

            if (mMaximizeWidgets != null) {
                mMaximizeWidgets.setChecked(Settings.System.getInt(cr,
                        Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, 0) == 1);
            }
            if (mMusicControls != null) {
                mMusicControls.setChecked(Settings.System.getInt(cr,
                        Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 1) == 1);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BG_WALLPAPER) {
            int hintId;

            if (resultCode == Activity.RESULT_OK) {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.renameTo(mWallpaperImage);
                }
                mWallpaperImage.setReadOnly();
                hintId = R.string.lockscreen_background_result_successful;
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, "");
                updateCustomBackgroundSummary();
            } else {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.delete();
                }
                hintId = R.string.lockscreen_background_result_not_successful;
            }
            Toast.makeText(getActivity(),
                    getResources().getString(hintId), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();

        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mMaximizeWidgets) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, value ? 1 : 0);
            return true;
        } else if (preference == mMusicControls) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MUSIC_CONTROLS, value ? 1 : 0);
            return true;
        } else if (preference == mCustomBackground) {
            int selection = mCustomBackground.findIndexOfValue(objValue.toString());
            return handleBackgroundSelection(selection);
        } else if (preference == mEnableCamera) {
            updateKeyguardState((Boolean) objValue, mEnableWidgets.isChecked());
            return true;
        } else if (preference == mEnableWidgets) {
            updateKeyguardState(mEnableCamera.isChecked(), (Boolean) objValue);
            return true;
        }

        return false;
    }

    private void updateKeyguardState(boolean enableCamera, boolean enableWidgets) {
        ComponentName dpmAdminName = new ComponentName(getActivity(),
                DeviceAdminLockscreenReceiver.class);
        mDPM.setActiveAdmin(dpmAdminName, true);
        int disabledFeatures = enableWidgets
                ? DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
                : DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL;
        if (!enableCamera) {
            disabledFeatures |= DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA;
        }
        mDPM.setKeyguardDisabledFeatures(dpmAdminName, disabledFeatures);
    }

    private boolean handleBackgroundSelection(int selection) {
        if (selection == LOCKSCREEN_BACKGROUND_COLOR_FILL) {
            final ColorPickerView colorView = new ColorPickerView(getActivity());
            int currentColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND, -1);

            if (currentColor != -1) {
                colorView.setColor(currentColor);
            }
            colorView.setAlphaSliderVisible(true);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lockscreen_custom_background_dialog_title)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getContentResolver(),
                                    Settings.System.LOCKSCREEN_BACKGROUND, colorView.getColor());
                            updateCustomBackgroundSummary();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setView(colorView)
                    .show();
        } else if (selection == LOCKSCREEN_BACKGROUND_CUSTOM_IMAGE) {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            final Rect rect = new Rect();
            final Window window = getActivity().getWindow();

            window.getDecorView().getWindowVisibleDisplayFrame(rect);

            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            int width = display.getWidth();
            int height = display.getHeight() - titleBarHeight;

            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                mWallpaperTemporary.createNewFile();
                mWallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mWallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_CODE_BG_WALLPAPER);
            } catch (IOException e) {
                // Do nothing here
            } catch (ActivityNotFoundException e) {
                // Do nothing here
            }
        } else if (selection == LOCKSCREEN_BACKGROUND_DEFAULT_WALLPAPER) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND, null);
            updateCustomBackgroundSummary();
            return true;
        }

        return false;
    }

    public static class DeviceAdminLockscreenReceiver extends DeviceAdminReceiver {}
}
