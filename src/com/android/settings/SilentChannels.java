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

import android.content.ContentResolver;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

public class SilentChannels extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SilentChannels";

    private Preference mResetToDefault;
    private Preference mMuteAll;

    private CheckBoxPreference mStreamVoiceCall; // STREAM_VOICE_CALL
    private CheckBoxPreference mStreamSystem; // STREAM_SYSTEM
    private CheckBoxPreference mStreamRing; // STREAM_RING
    private CheckBoxPreference mStreamMusic; // STREAM_MUSIC
    private CheckBoxPreference mStreamAlarm; // STREAM_ALARM
    private CheckBoxPreference mStreamNotification; // STREAM_NOTIFICATION
    private CheckBoxPreference mStreamBluetooth; // STREAM_BLUETOOTH_SCO
    private CheckBoxPreference mStreamSystemEnforced; // STREAM_SYSTEM_ENFORCED
    private CheckBoxPreference mStreamDTMF; // STREAM_DTMF
    private CheckBoxPreference mStreamTTS; // STREAM_TTS

    private CheckBoxPreference[] mAllStreamPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.silent_mode_settings);

        mResetToDefault = findPreference("reset_to_default");
        mMuteAll = findPreference("mute_all");

        /* Note that some of these may be null, if the preference is disabled. */
        mStreamVoiceCall = (CheckBoxPreference) findPreference("stream_voice_call");
        mStreamSystem = (CheckBoxPreference) findPreference("stream_system");
        mStreamRing = (CheckBoxPreference) findPreference("stream_ring");
        mStreamMusic = (CheckBoxPreference) findPreference("stream_music");
        mStreamAlarm = (CheckBoxPreference) findPreference("stream_alarm");
        mStreamNotification = (CheckBoxPreference) findPreference("stream_notification");
        mStreamBluetooth = (CheckBoxPreference) findPreference("stream_bluetooth");
        mStreamSystemEnforced = (CheckBoxPreference) findPreference("stream_system_enforced");
        mStreamDTMF = (CheckBoxPreference) findPreference("stream_dtmf");
        mStreamTTS = (CheckBoxPreference) findPreference("stream_tts");

        CheckBoxPreference allPreferences[] = {
            mStreamVoiceCall, mStreamSystem, mStreamRing, mStreamMusic, mStreamAlarm,
            mStreamNotification, mStreamBluetooth, mStreamSystemEnforced, mStreamDTMF, mStreamTTS
        };
        mAllStreamPrefs = allPreferences;

        updateState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private int getPreferenceMask(Preference preference) {
        if (preference == mStreamVoiceCall)      return 1 << AudioManager.STREAM_VOICE_CALL;
        if (preference == mStreamSystem)         return 1 << AudioManager.STREAM_SYSTEM;
        if (preference == mStreamRing)           return 1 << AudioManager.STREAM_RING;
        if (preference == mStreamMusic)          return 1 << AudioManager.STREAM_MUSIC;
        if (preference == mStreamAlarm)          return 1 << AudioManager.STREAM_ALARM;
        if (preference == mStreamNotification)   return 1 << AudioManager.STREAM_NOTIFICATION;
        if (preference == mStreamBluetooth)      return 1 << AudioManager.STREAM_BLUETOOTH_SCO;
        if (preference == mStreamSystemEnforced) return 1 << AudioManager.STREAM_SYSTEM_ENFORCED;
        if (preference == mStreamDTMF)           return 1 << AudioManager.STREAM_DTMF;
        if (preference == mStreamTTS)            return 1 << AudioManager.STREAM_TTS;
        Log.w(TAG, "Unexpected preference given to getPreferenceMask");
        return 0;
    }

    private void setAllOn() {
        /* Set all streams which are displayed in the menu to silenced.  Don't change
         * any streams which we're not displaying in the menu. */
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        for (int i = 0; i < mAllStreamPrefs.length; i++) {
            CheckBoxPreference pref = mAllStreamPrefs[i];
            if (pref == null)
                continue;

            silentModeStreams |= getPreferenceMask(pref);
        }

        Settings.System.putInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, silentModeStreams);
        updateState();
    }

    private void resetToDefault() {
        /*
         * This matches the default set in
         *   frameworks/base/packages/SettingsProvider/src/com/android/providers/settings/DatabaseHelper.java
         *
         * There's no way to query the default properly.
         *
         * Don't change any streams which we're not displaying in the menu.
         */
        final int defaultStreams =
              (1 << AudioManager.STREAM_RING)
            | (1 << AudioManager.STREAM_NOTIFICATION)
            | (1 << AudioManager.STREAM_SYSTEM)
            | (1 << AudioManager.STREAM_SYSTEM_ENFORCED);

        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        for (int i = 0; i < mAllStreamPrefs.length; i++) {
            CheckBoxPreference pref = mAllStreamPrefs[i];
            if (pref == null)
                continue;

            int bit = getPreferenceMask(pref);
            if ((defaultStreams & bit) != 0)
                silentModeStreams |= bit;
            else
                silentModeStreams &= ~bit;
        }

        Settings.System.putInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, silentModeStreams);
        updateState();
    }

    private void updateState() {
        final int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        for (int i = 0; i < mAllStreamPrefs.length; i++) {
            CheckBoxPreference pref = mAllStreamPrefs[i];
            if (pref == null)
                continue;

            boolean silenced = (silentModeStreams & getPreferenceMask(pref)) != 0;
            pref.setChecked(silenced);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mResetToDefault)
        {
            resetToDefault();
            return true;
        }

        if (preference == mMuteAll)
        {
            setAllOn();
            return true;
        }

        for (int i = 0; i < mAllStreamPrefs.length; i++) {
            CheckBoxPreference pref = mAllStreamPrefs[i];
            if (pref == null || pref != preference)
                continue;

            final int mask = getPreferenceMask(pref);
            int silentModeStreams = Settings.System.getInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
            if (pref.isChecked())
                silentModeStreams |= mask;
            else
                silentModeStreams &= ~mask;

            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, silentModeStreams);
            return true;
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }
}
