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

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseSystemSettingEnabler implements SwitchBar.OnSwitchChangeListener  {
    private Context mContext;
    private SwitchBar mSwitchBar;
    private SettingsObserver mSettingsObserver;
    private boolean mListeningToOnSwitchChange = false;

    private boolean mStateMachineEvent;

    public abstract String getSettingName();

    public BaseSystemSettingEnabler(Context context, SwitchBar switchBar) {
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
        onEnablerChanged(checked);
    }

    private void setSwitchState() {
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                getSettingName(), 1) == 1;
        mStateMachineEvent = true;
        setSwitchBarChecked(enabled);
        mStateMachineEvent = false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }

        // Handle a switch change
        Settings.System.putInt(mContext.getContentResolver(),
                getSettingName(), isChecked ? 1 : 0);

        onEnablerChanged(isChecked);
    }

    public void onEnablerChanged(boolean isEnabled) {
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    getSettingName()), false, this);
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
