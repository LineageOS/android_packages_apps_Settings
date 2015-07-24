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

package com.android.settings.wifi;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiWakeupService extends IntentService {

    public static final long CANCEL_WIFI_SLEEP = 0;

    private static final String WIFI_WAKEUP_ACTION = "cyanogenmod.intent.action.WIFI_WAKEUP";

    public static final String WIFI_WAKEUP_CHANGED_ACTION
            = "cyanogenmod.intent.action.WIFI_WAKEUP_CHANGED";

    public WifiWakeupService() {
        super("WifiWakeupService");
    }

    public WifiWakeupService(String name) {
        super("WifiWakeupService");
    }

    public static void scheduleSleep(Context ctx, long timeToSleepMs) {
        Intent wifiWakeupIntent = new Intent(ctx, WifiWakeupService.class);
        wifiWakeupIntent.setAction(WIFI_WAKEUP_ACTION);

        PendingIntent pendingWifiWakeup = PendingIntent.getService(ctx, 0, wifiWakeupIntent,
                PendingIntent.FLAG_ONE_SHOT);

        long targetTime = System.currentTimeMillis() + timeToSleepMs;
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // cancel the alarm
        if (timeToSleepMs <= 0 || timeToSleepMs == CANCEL_WIFI_SLEEP) {
            alarmManager.cancel(pendingWifiWakeup);

            // store the shared preference
            setWakeupWifiTime(ctx, 0);
        } else {
            alarmManager.set(AlarmManager.RTC, targetTime, pendingWifiWakeup);

            // turn off wi-fi
            WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(false);

            // store the shared preference
            setWakeupWifiTime(ctx, targetTime);
        }

        // boradcast the change
        Intent wakeupChangedBroadcast = new Intent(WIFI_WAKEUP_CHANGED_ACTION);
        ctx.sendBroadcast(wakeupChangedBroadcast);
    }

    public static void cancel(Context ctx) {
        scheduleSleep(ctx, CANCEL_WIFI_SLEEP);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (WIFI_WAKEUP_ACTION.equals(intent.getAction())) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);
        }
    }

    public static long getWakeupWifiTime(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getLong(WIFI_WAKEUP_ACTION, 0);
    }

    private static void setWakeupWifiTime(Context ctx, long time) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putLong(WIFI_WAKEUP_ACTION, time)
                .commit();
    }
}
