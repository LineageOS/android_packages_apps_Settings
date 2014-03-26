/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.settings.voicewakeup;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.widget.SwitchBar;

public class VoiceWakeupEnabler implements SwitchBar.OnSwitchChangeListener {
    private Context mContext;
    private SwitchBar mSwitchBar;
    private SettingsObserver mSettingsObserver;
    private boolean mListeningToOnSwitchChange = false;

    private boolean mStateMachineEvent;

    public VoiceWakeupEnabler(Context context, SwitchBar switchBar) {
        mContext = context;
        mSwitchBar = switchBar;
        mSettingsObserver = new SettingsObserver(new Handler());
        setupSwitchBar(); 
    }

    public void setupSwitchBar() {
        setSwitchState();
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = true;
        }
        mSwitchBar.show();
    }   
    
    public void teardownSwitchBar() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mListeningToOnSwitchChange = false;
        }
        mSwitchBar.hide();
    }

    public void resume(Context context) {
        mContext = context;
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mSettingsObserver.observe();
            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mSettingsObserver.unobserve();

            mListeningToOnSwitchChange = false;
        }
    }

    private void setSwitchBarChecked(boolean checked) {
        mStateMachineEvent = true;
        mSwitchBar.setChecked(checked);
        mStateMachineEvent = false;
    }

    private void setSwitchState() {
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.VOICE_WAKEUP, 0) == 1;
        mStateMachineEvent = true;
        setSwitchBarChecked(enabled);
        mStateMachineEvent = false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (mStateMachineEvent) {
            return;
        }
        // Handle a switch change
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.VOICE_WAKEUP, isChecked ? 1 : 0);

        // service should shut down on its own after switch off
        if (isChecked) {
            Intent i = new Intent("com.cyanogenmod.voicewakeup.action.START_ENGINE");
            i.setComponent(new ComponentName("com.cyanogenmod.voicewakeup",
                        "com.cyanogenmod.voicewakeup.VoiceWakeupEngine"));
            mContext.startService(i);
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.VOICE_WAKEUP), false, this);
            update();
        }

        void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        public void update() {
            setSwitchState();
        }
    }
}
