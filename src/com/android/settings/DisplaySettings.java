/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import java.util.ArrayList;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.cyanogenmod.DisplayRotation;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_AUTOMATIC_BACKLIGHT = "backlight_widget";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_LOCKSCREEN_ROTATION = "lockscreen_rotation";
    private static final String KEY_ELECTRON_BEAM_ANIMATION_ON = "electron_beam_animation_on";
    private static final String KEY_ELECTRON_BEAM_ANIMATION_OFF = "electron_beam_animation_off";
    private static final String KEY_ELECTRON_BEAM_CATEGORY_ANIMATION = "category_animation_options";
    private static final String KEY_WAKEUP_CATEGORY = "category_wakeup_options";
    private static final String KEY_VOLUME_WAKE = "pref_volume_wake";

    private static final String LOCKSCREEN_ROTATION_MODE = "Lockscreen";
    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";
    private static final String ROTATION_ANGLE_DELIM = ", ";
    private static final String ROTATION_ANGLE_DELIM_FINAL = " & ";

    private CheckBoxPreference mLockscreenRotation;
    private CheckBoxPreference mVolumeWake;
    private CheckBoxPreference mElectronBeamAnimationOn;
    private CheckBoxPreference mElectronBeamAnimationOff;
    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private PreferenceScreen mDisplayRotationPreference;
    private PreferenceScreen mAutomaticBacklightPreference;

    private ContentObserver mAccelerometerRotationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        mLockscreenRotation = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ROTATION);
        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);
        updateDisplayRotationPreferenceDescription();

        mAutomaticBacklightPreference = (PreferenceScreen) findPreference(KEY_AUTOMATIC_BACKLIGHT);
        if (mAutomaticBacklightPreference != null
                && !getResources().getBoolean(
                        com.android.internal.R.bool.config_automatic_brightness_available)) {
            getPreferenceScreen().removePreference(mAutomaticBacklightPreference);
        }

        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                getPreferenceScreen().removePreference(mNotificationPulse);
            } else {
                updateLightPulseDescription();
            }
        }

        mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
        if (mBatteryPulse != null) {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveBatteryLed) == false) {
                getPreferenceScreen().removePreference(mBatteryPulse);
            } else {
                updateBatteryPulseDescription();
            }
        }

        mElectronBeamAnimationOn = (CheckBoxPreference) findPreference(KEY_ELECTRON_BEAM_ANIMATION_ON);
        mElectronBeamAnimationOff = (CheckBoxPreference) findPreference(KEY_ELECTRON_BEAM_ANIMATION_OFF);
        mElectronBeamAnimationOn.setChecked(Settings.System.getInt(resolver,
                Settings.System.ELECTRON_BEAM_ANIMATION_ON, 0) == 1);
        mElectronBeamAnimationOff.setChecked(Settings.System.getInt(resolver,
                Settings.System.ELECTRON_BEAM_ANIMATION_OFF, 1) == 1);

        mElectronBeamAnimationOn = (CheckBoxPreference) findPreference(KEY_ELECTRON_BEAM_ANIMATION_ON);
        if(getResources().getInteger(com.android.internal.R.integer.config_screenOnAnimation) >= 0) {
            mElectronBeamAnimationOn.setChecked(Settings.System.getInt(resolver,
                    Settings.System.ELECTRON_BEAM_ANIMATION_ON, 0) == 1);
        } else {
            getPreferenceScreen().removePreference(mElectronBeamAnimationOn);
        }
        mElectronBeamAnimationOff = (CheckBoxPreference) findPreference(KEY_ELECTRON_BEAM_ANIMATION_OFF);
        if(getResources().getBoolean(com.android.internal.R.bool.config_screenOffAnimation)) {
            mElectronBeamAnimationOff.setChecked(Settings.System.getInt(resolver,
                    Settings.System.ELECTRON_BEAM_ANIMATION_OFF, 1) == 1);
        } else {
            getPreferenceScreen().removePreference(mElectronBeamAnimationOff);
        }
        if(getResources().getInteger(com.android.internal.R.integer.config_screenOnAnimation) < 0 &&
              !getResources().getBoolean(com.android.internal.R.bool.config_screenOffAnimation)) {
            getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_ELECTRON_BEAM_CATEGORY_ANIMATION));
        }

        mVolumeWake = (CheckBoxPreference) findPreference(KEY_VOLUME_WAKE);
        if (mVolumeWake != null) {
            if (!getResources().getBoolean(R.bool.config_show_volumeRockerWake)) {
                getPreferenceScreen().removePreference(mVolumeWake);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_WAKEUP_CATEGORY));
            } else {
                mVolumeWake.setChecked(Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);
            }

        }

    }

    private void updateDisplayRotationPreferenceDescription() {
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        Boolean lockscreenRotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE|DisplayRotation.ROTATION_270_MODE);

        if (mLockscreenRotation != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation)) {
                getPreferenceScreen().removePreference(mLockscreenRotation);
            } else {
                mLockscreenRotation.setChecked(Settings.System.getInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_ROTATION, 0) != 1);
            }
        }

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
            summary.append(getString(R.string.display_rotation_enabled) + " ");
            if (lockscreenRotationEnabled) {
                rotationList.add(LOCKSCREEN_ROTATION_MODE);
            }
            if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_0);
            }
            if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_90);
            }
            if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_180);
            }
            if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_270);
            }
            for(int i=0;i<rotationList.size();i++) {
                summary.append(delim).append(rotationList.get(i));
                if (rotationList.size() >= 2 && (rotationList.size() - 2) == i) {
                    delim = " " + ROTATION_ANGLE_DELIM_FINAL + " ";
                } else {
                    delim = ROTATION_ANGLE_DELIM + " ";
                }
            }
            summary.append(" " + getString(R.string.display_rotation_unit));
        }
        preference.setSummary(summary);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.parseLong(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }
            summary = preference.getContext().getString(R.string.screen_timeout_summary,
                    entries[best]);
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    private void updateBatteryPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisplayRotationPreferenceDescription();
        updateLightPulseDescription();
        updateBatteryPulseDescription();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mElectronBeamAnimationOn) {
            Settings.System.putInt(getContentResolver(), Settings.System.ELECTRON_BEAM_ANIMATION_ON,
                    mElectronBeamAnimationOn.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mElectronBeamAnimationOff) {
            Settings.System.putInt(getContentResolver(), Settings.System.ELECTRON_BEAM_ANIMATION_OFF,
                    mElectronBeamAnimationOff.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mVolumeWake) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_WAKE_SCREEN,
                    mVolumeWake.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mLockscreenRotation) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_ROTATION,
                    mLockscreenRotation.isChecked() ? 1 : 0);
            updateDisplayRotationPreferenceDescription();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        return true;
    }
}
