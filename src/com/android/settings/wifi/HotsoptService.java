/*
    Copyright (c) 2014, The Linux Foundation. All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:
        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials provided
          with the distribution.
        * Neither the name of The Linux Foundation nor the names of its
          contributors may be used to endorse or promote products derived
          from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
    ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
    BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
    BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
    WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
    OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
    IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings.wifi;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiDevice;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HotsoptService extends Service {
    private static final String HOTSPOT_PREFERENCE_FILE = "wifi_hotsopt_preference";
    private static final String KEY_HOTSOPT_MODE = "wifi_hotsopt_mode";
    private static final int TIMER_SCHEDULE = 0;
    /* Always */
    private static final String HOTSPOT_MODE_ALWAYS = "Always";
    /* Turn off when idle for 5 min */
    private static final String HOTSPOT_MODE_5MIN = "Turn_off_5min";
    /* Turn off when idle for 10 min */
    private static final String HOTSPOT_MODE_10MIN = "Turn_off_10min";
    private long mHotspotLastValidTime = 0L;
    private Timer mTimer;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.TETHER_CONNECT_STATE_CHANGED.equals(action)) {
                setCurrentTimeToLastValidTime();
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case TIMER_SCHEDULE:
                    long turnOffTime = getTurnOffTimebyMode();
                    if (turnOffTime > 0L) {
                        long currentTime = System.currentTimeMillis();
                        long a = currentTime - getHotspotLastValidTime();
                        if (currentTime - getHotspotLastValidTime() > turnOffTime) {
                            disableWifiAp();
                            stopSelf();
                        } else {
                            setCurrentTimeToLastValidTime();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private long getTurnOffTimebyMode() {
        String hotsoptMode = getHotspotMode(this);
        if (hotsoptMode.equals(HOTSPOT_MODE_5MIN)) {
            return 5 * 60 * 1000;
        } else if (hotsoptMode.equals(HOTSPOT_MODE_10MIN)) {
            return 10 * 60 * 1000;
        }
        return 0L;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        long currentTime = System.currentTimeMillis();
        setHotspotLastValidTime(currentTime);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.TETHER_CONNECT_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);
        TimerTask task = new TimerTask() {
            public void run() {
                Message message = mHandler.obtainMessage();
                message.what = TIMER_SCHEDULE;
                mHandler.sendMessage(message);
            }
        };
        mTimer = new Timer(true);
        mTimer.schedule(task, 10 * 1000, 10 * 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        this.unregisterReceiver(mReceiver);
    }

    private void setCurrentTimeToLastValidTime() {
        ConnectivityManager connManager = (ConnectivityManager)getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (connManager != null){
            List<WifiDevice> tetherList = connManager.getTetherConnectedSta();
            if (tetherList != null && tetherList.size() > 0) {
                long currentTime = System.currentTimeMillis();
                setHotspotLastValidTime(currentTime);
            }
        }
    }

    private void disableWifiAp() {
        WifiManager wifiManager = (WifiManager)getSystemService(
                Service.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.setWifiApEnabled(null, false);
        }
    }

    private void setHotspotLastValidTime(Long value) {
        synchronized (this) {
            mHotspotLastValidTime = value;
        }
    }

    private long getHotspotLastValidTime() {
        return mHotspotLastValidTime;
    }

    public static void setHotspotMode(Context context, String value) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(
                    HOTSPOT_PREFERENCE_FILE, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_HOTSOPT_MODE, value);
            editor.commit();
        }
    }

    public static String getHotspotMode(Context context, String defValue) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(
                    HOTSPOT_PREFERENCE_FILE, Activity.MODE_PRIVATE);
            return prefs.getString(KEY_HOTSOPT_MODE, defValue);
        }
        return defValue;
    }

    public static String getHotspotMode(Context context) {
        return getHotspotMode(context, HOTSPOT_MODE_ALWAYS);
    }
}
