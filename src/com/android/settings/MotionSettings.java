/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2016 The CyanogenMod Project
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

import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserManager;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SeekBarVolumizer;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import cyanogenmod.hardware.CMHardwareManager;
import cyanogenmod.providers.CMSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.cyanogenmod.internal.logging.CMMetricsLogger;

public class MotionSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = MotionSettings.class.getSimpleName();

    private static final String KEY_SOUND = "sounds";

    private Context mContext;
    private SwitchPreference mFlipToMuteIncomingCallPref;

    private static final String KEY_FLIP_TO_MUTE_INCOMING_CALL = "flip_to_mute_incoming_call";

    @Override
    protected int getMetricsCategory() {
        return CMMetricsLogger.MOTION_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.motions);

        mFlipToMuteIncomingCallPref = (SwitchPreference)
                    findPreference(KEY_FLIP_TO_MUTE_INCOMING_CALL);

        int flipBehavior = CMSettings.Secure.getInt(
                mContext.getContentResolver(), CMSettings.Secure.MOTION_BEHAVIOR,
                CMSettings.Secure.MOTION_BEHAVIOR_DEFAULT);

        mFlipToMuteIncomingCallPref.setChecked(flipBehavior == CMSettings.Secure.MOTION_BEHAVIOR_FLIP_TO_MUTE_INCOMING_CALL);
        mFlipToMuteIncomingCallPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object isFlipOnObject) {
                boolean isFlipOn = (Boolean) isFlipOnObject;
                int value;
                if (isFlipOn) {
                    value = CMSettings.Secure.MOTION_BEHAVIOR_FLIP_TO_MUTE_INCOMING_CALL;
                } else {
                    value = CMSettings.Secure.MOTION_BEHAVIOR_NOTHING;
                }
                CMSettings.Secure.putInt(mContext.getContentResolver(),
                    CMSettings.Secure.MOTION_BEHAVIOR, value);
                return true;
            }
            });
    }

}
