/*
 * Copyright (C) 2015 The CyanogenMod project
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

import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.android.settings.R;

public class HiddenSettingsSecretReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SECRET_CODE_ACTION)) {
            System.out.println("Received secret code action");
            ComponentName hiddenSettingsDebugMenuComponent = new ComponentName(
                    "com.android.settings",
                    "com.android.settings.cyanogenmod.HiddenSettingsDebugMenu");

            if (context.getPackageManager().getComponentEnabledSetting(
                    hiddenSettingsDebugMenuComponent) ==
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                context.getPackageManager().setComponentEnabledSetting(
                        hiddenSettingsDebugMenuComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                Toast.makeText(context, R.string.hidden_settings_debug_menu_enabled,
                        Toast.LENGTH_LONG).show();
            } else {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setClass(context, HiddenSettingsDebugMenu.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }
    }
}
