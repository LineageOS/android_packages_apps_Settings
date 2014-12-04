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

package com.android.settings.profiles;

import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Switch;
import com.android.settings.widget.SwitchBar;
import com.android.settings.cyanogenmod.BaseSystemSettingEnabler;

public class ProfileEnabler extends BaseSystemSettingEnabler {
    private Context mContext;

    public ProfileEnabler(Context context, SwitchBar switchBar) {
        super(context, switchBar);
        mContext = context;
    }

    @Override
    public String getSettingName() {
        return Settings.System.PHONE_BLACKLIST_ENABLED;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        super.onSwitchChanged(switchView, isChecked);

        Intent intent=new Intent(ProfileManager.PROFILES_STATE_CHANGED_ACTION);
        intent.putExtra(
                ProfileManager.EXTRA_PROFILES_STATE,
                isChecked ?
                        ProfileManager.PROFILES_STATE_ENABLED :
                        ProfileManager.PROFILES_STATE_DISABLED);
        mContext.sendBroadcast(intent);
    }
}
