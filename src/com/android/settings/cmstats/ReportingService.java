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

package com.android.settings.cmstats;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

import com.android.settings.R;

public class ReportingService extends Service {
    protected static final String TAG = "CMStats";

    protected static final String ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in";

    protected static final String ANONYMOUS_ALARM_SET = "pref_anonymous_alarm_set";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sending anonymous statistics data..");
        Thread thread = new Thread() {
            @Override
            public void run() {
                report();
            }
        };
        thread.start();
        return Service.START_REDELIVER_INTENT;
    }

    private void report() {
        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

        Log.d(TAG, "SERVICE: Device ID=" + deviceId);
        Log.d(TAG, "SERVICE: Device Name=" + deviceName);
        Log.d(TAG, "SERVICE: Device Version=" + deviceVersion);
        Log.d(TAG, "SERVICE: Country=" + deviceCountry);
        Log.d(TAG, "SERVICE: Carrier=" + deviceCarrier);
        Log.d(TAG, "SERVICE: Carrier ID=" + deviceCarrierId);

        // report to google analytics
        GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
        Tracker tracker = ga.getTracker(getString(R.string.ga_trackingId));
        tracker.sendEvent(deviceName, deviceVersion, deviceCountry, null);
        tracker.close();

        // report to the cmstats service
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://stats.cyanogenmod.com/submit");
        try {
            List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
            kv.add(new BasicNameValuePair("device_hash", deviceId));
            kv.add(new BasicNameValuePair("device_name", deviceName));
            kv.add(new BasicNameValuePair("device_version", deviceVersion));
            kv.add(new BasicNameValuePair("device_country", deviceCountry));
            kv.add(new BasicNameValuePair("device_carrier", deviceCarrier));
            kv.add(new BasicNameValuePair("device_carrier_id", deviceCarrierId));
            httppost.setEntity(new UrlEncodedFormEntity(kv));
            httpclient.execute(httppost);
            getSharedPreferences("CMStats", 0).edit().putLong(ANONYMOUS_LAST_CHECKED,
                    System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Got Exception", e);
        }
        ReportingServiceManager.setAlarm(this);
        stopSelf();
    }
}
