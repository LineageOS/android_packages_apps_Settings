/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cyanogenmod.app.profiles.ProfilePluginManager;

/**
 * Receives Broadcasts from frameworks when a new profile has activated and Custom Actions have
 * fired.
 */
public class ActionFiredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ProfilePluginManager manager = ProfilePluginManager.getInstance(context);
        String[] actionStrings = intent.getStringArrayExtra("customActions");
        for (String actionString : actionStrings) {
            String[] split = actionString.split("/");
            String id = split[0];
            String state = split[1];
            manager.fireAction(id, state);
        }
    }
}
