/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IHardwareService;
import android.os.IMountService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.IWindowManager;

public class SoundAndDisplaySettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundAndDisplaysSettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;
    private static final int FALLBACK_EMERGENCY_TONE_VALUE = 0;

    private static final String KEY_SILENT = "silent";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_SPEAKER_NOTIFICATIONS = "speaker_notifications";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_DTMF_TONE = "dtmf_tone";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_ANIMATIONS = "animations";
    private static final String KEY_PLAY_MEDIA_NOTIFICATION_SOUNDS =
            "play_media_notification_sounds";
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String KEY_SOUND_DISPLAY_SETTINGS = "sound_display_settings";
    private static final String KEY_TRACKBALL_SETTINGS = "trackball_settings";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_NOTIFICATION_PULSE_BLEND = "notification_pulse_blend";
    private static final String KEY_NOTIFICATION_SCREEN_ON = "notification_screen_on";
    private static final String KEY_BREATHING_LIGHT_COLOR = "breathing_light_color";
    private static final String KEY_TRACKBALL_WAKE_SCREEN = "trackball_wake_screen";
    private static final String KEY_TRACKBALL_UNLOCK_SCREEN = "trackball_unlock_screen";
    private static final String KEY_ACCELEROMETER_MODE = "accelerometer_mode";
    
    private CheckBoxPreference mSilent;

    private CheckBoxPreference mPlayMediaNotificationSounds;

    private IMountService mMountService = null;

    /*
     * If we are currently in one of the silent modes (the ringer mode is set to either
     * "silent mode" or "vibrate mode"), then toggling the "Phone vibrate"
     * preference will switch between "silent mode" and "vibrate mode".
     * Otherwise, it will adjust the normal ringer mode's ring or ring+vibrate
     * setting.
     */
    private CheckBoxPreference mVibrate;
    private CheckBoxPreference mSpeakerNotifications;
    private CheckBoxPreference mDtmfTone;
    private CheckBoxPreference mSoundEffects;
    private CheckBoxPreference mHapticFeedback;
    private ListPreference mAnimations;
    private ListPreference mAccelerometerMode;
    private CheckBoxPreference mNotificationPulse;
    private CheckBoxPreference mNotificationPulseBlend;
    private CheckBoxPreference mNotificationScreenOn;
    private ListPreference mBreathingLightColor;
    private CheckBoxPreference mTrackballWakeScreen;
    private CheckBoxPreference mTrackballUnlockScreen;
    private CheckBoxPreference mMenuUnlockScreen;
    private float[] mAnimationScales;

    private AudioManager mAudioManager;

    private IWindowManager mWindowManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                updateState(false);
            }
        }
    };

    private PreferenceGroup mSoundDisplaySettings;
    private PreferenceGroup mTrackballSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        int activePhoneType = TelephonyManager.getDefault().getPhoneType();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        mMountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));

        addPreferencesFromResource(R.xml.sound_and_display_settings);

        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType) {
            // device is not CDMA, do not display CDMA emergency_tone
            getPreferenceScreen().removePreference(findPreference(KEY_EMERGENCY_TONE));
        }

        mSilent = (CheckBoxPreference) findPreference(KEY_SILENT);
        mPlayMediaNotificationSounds = (CheckBoxPreference) findPreference(KEY_PLAY_MEDIA_NOTIFICATION_SOUNDS);

        mVibrate = (CheckBoxPreference) findPreference(KEY_VIBRATE);
        mSpeakerNotifications = (CheckBoxPreference) findPreference(KEY_SPEAKER_NOTIFICATIONS);
        mDtmfTone = (CheckBoxPreference) findPreference(KEY_DTMF_TONE);
        mDtmfTone.setPersistent(false);
        mDtmfTone.setChecked(Settings.System.getInt(resolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);
        mSoundEffects.setPersistent(false);
        mSoundEffects.setChecked(Settings.System.getInt(resolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 0) != 0);
        mHapticFeedback = (CheckBoxPreference) findPreference(KEY_HAPTIC_FEEDBACK);
        mHapticFeedback.setPersistent(false);
        mHapticFeedback.setChecked(Settings.System.getInt(resolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0);
        mAnimations = (ListPreference) findPreference(KEY_ANIMATIONS);
        mAnimations.setOnPreferenceChangeListener(this);
        mAccelerometerMode = (ListPreference) findPreference(KEY_ACCELEROMETER_MODE);
        mAccelerometerMode.setOnPreferenceChangeListener(this);

        ListPreference screenTimeoutPreference =
            (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        screenTimeoutPreference.setValue(String.valueOf(Settings.System.getInt(
                resolver, SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE)));
        screenTimeoutPreference.setOnPreferenceChangeListener(this);

        if (TelephonyManager.PHONE_TYPE_CDMA == activePhoneType) {
            ListPreference emergencyTonePreference =
                (ListPreference) findPreference(KEY_EMERGENCY_TONE);
            emergencyTonePreference.setValue(String.valueOf(Settings.System.getInt(
                resolver, Settings.System.EMERGENCY_TONE, FALLBACK_EMERGENCY_TONE_VALUE)));
            emergencyTonePreference.setOnPreferenceChangeListener(this);
        }

        mSoundDisplaySettings = (PreferenceGroup) findPreference(KEY_SOUND_DISPLAY_SETTINGS);
        mTrackballSettings = (PreferenceGroup) mSoundDisplaySettings.findPreference(KEY_TRACKBALL_SETTINGS);
        mNotificationPulse = (CheckBoxPreference)
                mTrackballSettings.findPreference(KEY_NOTIFICATION_PULSE);
        mNotificationPulseBlend = (CheckBoxPreference)
                mTrackballSettings.findPreference(KEY_NOTIFICATION_PULSE_BLEND);
        mNotificationScreenOn = (CheckBoxPreference)
                mTrackballSettings.findPreference(KEY_NOTIFICATION_SCREEN_ON);
        mBreathingLightColor = (ListPreference)
                mTrackballSettings.findPreference(KEY_BREATHING_LIGHT_COLOR);
        mBreathingLightColor.setOnPreferenceChangeListener(this);
        mTrackballWakeScreen = (CheckBoxPreference)
                mTrackballSettings.findPreference(KEY_TRACKBALL_WAKE_SCREEN);
        mTrackballUnlockScreen = (CheckBoxPreference)
                mTrackballSettings.findPreference(KEY_TRACKBALL_UNLOCK_SCREEN);

        if (mNotificationPulse != null &&
                getResources().getBoolean(R.bool.has_intrusive_led) == false) {
            mSoundDisplaySettings.removePreference(mTrackballSettings);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
            try {
                mNotificationPulseBlend.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_PULSE_BLEND) == 1);
                mNotificationPulseBlend.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_PULSE_BLEND + " not found");
            }
            try {
                mNotificationScreenOn.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_SCREEN_ON) == 1);
                mNotificationScreenOn.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_SCREEN_ON + " not found");
            }
            try {
                mTrackballUnlockScreen.setChecked(Settings.System.getInt(resolver,
                        Settings.System.TRACKBALL_UNLOCK_SCREEN) == 1);
                mNotificationScreenOn.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.TRACKBALL_UNLOCK_SCREEN + " not found");
            }
        }
        
        mTrackballWakeScreen.setChecked(Settings.System.getInt(resolver, 
                Settings.System.TRACKBALL_WAKE_SCREEN, 0) == 1);
        mTrackballWakeScreen.setOnPreferenceChangeListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();

        updateState(true);

        IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    private void updateState(boolean force) {
        final int ringerMode = mAudioManager.getRingerMode();
        final boolean silentOrVibrateMode =
                ringerMode != AudioManager.RINGER_MODE_NORMAL;

        if (silentOrVibrateMode != mSilent.isChecked() || force) {
            mSilent.setChecked(silentOrVibrateMode);
        }

        try {
            mPlayMediaNotificationSounds.setChecked(mMountService.getPlayNotificationSounds());
        } catch (RemoteException e) {
        }

        boolean vibrateSetting;
        if (silentOrVibrateMode) {
            vibrateSetting = ringerMode == AudioManager.RINGER_MODE_VIBRATE;
        } else {
            vibrateSetting = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER)
                    == AudioManager.VIBRATE_SETTING_ON;
        }
        if (vibrateSetting != mVibrate.isChecked() || force) {
            mVibrate.setChecked(vibrateSetting);
        }

	boolean speakerNotificationSetting = Settings.Secure.getInt(getContentResolver(),
                Settings.System.NOTIFICATIONS_TO_SPEAKER, 1) != 0;
        if (speakerNotificationSetting != mSpeakerNotifications.isChecked() || force) {
            mSpeakerNotifications.setChecked(speakerNotificationSetting);
        }

        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        boolean isAlarmInclSilentMode = (silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0;
        mSilent.setSummary(isAlarmInclSilentMode ?
                R.string.silent_mode_incl_alarm_summary :
                R.string.silent_mode_summary);

        int animations = 0;
        try {
            mAnimationScales = mWindowManager.getAnimationScales();
        } catch (RemoteException e) {
        }
        if (mAnimationScales != null) {
            if (mAnimationScales.length >= 1) {
                animations = ((int)(mAnimationScales[0]+.5f)) % 10;
            }
            if (mAnimationScales.length >= 2) {
                animations += (((int)(mAnimationScales[1]+.5f)) & 0x7) * 10;
            }
        }
        int idx = 0;
        int best = 0;
        CharSequence[] aents = mAnimations.getEntryValues();
        for (int i=0; i<aents.length; i++) {
            int val = Integer.parseInt(aents[i].toString());
            if (val <= animations && val > best) {
                best = val;
                idx = i;
            }
        }
        mAnimations.setValueIndex(idx);
        updateAnimationsSummary(mAnimations.getValue());

        int accelerometerEnabled = Settings.System.getInt(
                getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 1);
        int accelerometerMode = 0;
        if (accelerometerEnabled > 0) {
            accelerometerMode = 1 + Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_MODE, 0);
        }
        mAccelerometerMode.setValueIndex(accelerometerMode);
        updateOrientationSummary(accelerometerMode);
    }

    private void updateOrientationSummary(int index) {
        mAccelerometerMode.setSummary(getResources().getTextArray(R.array.accelerometer_mode_summaries)[index]);
    }
    
    private void updateAnimationsSummary(Object value) {
        CharSequence[] summaries = getResources().getTextArray(R.array.animations_summaries);
        CharSequence[] values = mAnimations.getEntryValues();
        for (int i=0; i<values.length; i++) {
            //Log.i("foo", "Comparing entry "+ values[i] + " to current "
            //        + mAnimations.getValue());
            if (values[i].equals(value)) {
                mAnimations.setSummary(summaries[i]);
                break;
            }
        }
    }

    private void setRingerMode(boolean silent, boolean vibrate) {
        if (silent) {
            mAudioManager.setRingerMode(vibrate ? AudioManager.RINGER_MODE_VIBRATE :
                AudioManager.RINGER_MODE_SILENT);
        } else {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
        mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
                vibrate ? AudioManager.VIBRATE_SETTING_ON
                        : AudioManager.VIBRATE_SETTING_OFF);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mSilent || preference == mVibrate) {
            setRingerMode(mSilent.isChecked(), mVibrate.isChecked());
            if (preference == mSilent) updateState(false);
        } else if (preference == mPlayMediaNotificationSounds) {
            try {
                mMountService.setPlayNotificationSounds(mPlayMediaNotificationSounds.isChecked());
            } catch (RemoteException e) {
            }
        } else if (preference == mDtmfTone) {
            Settings.System.putInt(getContentResolver(), Settings.System.DTMF_TONE_WHEN_DIALING,
                    mDtmfTone.isChecked() ? 1 : 0);

        } else if (preference == mSoundEffects) {
            if (mSoundEffects.isChecked()) {
                mAudioManager.loadSoundEffects();
            } else {
                mAudioManager.unloadSoundEffects();
            }
            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED,
                    mSoundEffects.isChecked() ? 1 : 0);

        } else if (preference == mHapticFeedback) {
            Settings.System.putInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    mHapticFeedback.isChecked() ? 1 : 0);

        } else if (preference == mSpeakerNotifications) {
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATIONS_TO_SPEAKER,
                    mSpeakerNotifications.isChecked() ? 1 : 0);

        } else if (preference == mNotificationPulse) {
            value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_LIGHT_PULSE, value ? 1 : 0);
        } else if (preference == mNotificationPulseBlend) {
            value = mNotificationPulseBlend.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_PULSE_BLEND, value ? 1 : 0);
        } else if (preference == mNotificationScreenOn) {
            value = mNotificationScreenOn.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_SCREEN_ON, value ? 1 : 0);
        } else if (preference == mTrackballWakeScreen) {
            value = mTrackballWakeScreen.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.TRACKBALL_WAKE_SCREEN, value ? 1 : 0);
        } else if (preference == mTrackballUnlockScreen) {
            value = mTrackballUnlockScreen.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.TRACKBALL_UNLOCK_SCREEN, value ? 1 : 0);
        }
        else
        {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        IHardwareService hardware = IHardwareService.Stub.asInterface(
                ServiceManager.getService("hardware"));

        final String key = preference.getKey();
        if (KEY_ANIMATIONS.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                if (mAnimationScales.length >= 1) {
                    mAnimationScales[0] = value%10;
                }
                if (mAnimationScales.length >= 2) {
                    mAnimationScales[1] = (value/10)%10;
                }
                try {
                    mWindowManager.setAnimationScales(mAnimationScales);
                } catch (RemoteException e) {
                }
                updateAnimationsSummary(objValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist animation setting", e);
            }

        }
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(),
                        SCREEN_OFF_TIMEOUT, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        } else if (KEY_EMERGENCY_TONE.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.EMERGENCY_TONE, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist emergency tone setting", e);
            }
        } else if (KEY_ACCELEROMETER_MODE.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                int enabled = value == 0 ? 0 : 1;
                int mode = value > 0 ? value - 1 : 0;
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, enabled);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_MODE, mode);
                updateOrientationSummary(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist accelerometer mode setting", e);
            }
            
        } else if (KEY_BREATHING_LIGHT_COLOR.equals(key)) {
            int value = Color.parseColor((String) objValue);
            try {
                hardware.pulseBreathingLightColor(value);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.BREATHING_LIGHT_COLOR, value);
                Log.d(TAG, "BREATHING_LIGHT_COLOR set to " + value);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "could not persist breathing light color settings", nfe);
            } catch (RemoteException re) {
                Log.e(TAG, "could not preview breathing light color", re);
            }
        }
        return true;
    }
}
