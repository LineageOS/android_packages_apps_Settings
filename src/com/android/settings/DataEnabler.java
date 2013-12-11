/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above
       copyright notice, this list of conditions and the following
       disclaimer in the documentation and/or other materials provided
       with the distribution.
     * Neither the name of The Linux Foundation, Inc. nor the names of its
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

package com.android.settings;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.internal.telephony.TelephonyIntents;

import com.android.settings.R;
import com.android.settings.WirelessSettings;

import java.util.concurrent.atomic.AtomicBoolean;

public class DataEnabler {
    private static final String TAG = "DataEnabler";
    private final Context mContext;
    private Switch mSwitch;
    private final IntentFilter mIntentFilter;
    private ConnectivityManager mConnService;
    private final NetworkStatusChangeIntentReceiver mReceiver;
    private Boolean mMobileDataEnabled;

    public DataEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;

        mConnService = (ConnectivityManager) mContext.
                getSystemService(Context.CONNECTIVITY_SERVICE);

        mMobileDataEnabled = mConnService.getMobileDataEnabled();
        mSwitch.setChecked(mMobileDataEnabled);

        // Register a broadcast receiver to listen the mobile connectivity
        // changed.
        mReceiver = new NetworkStatusChangeIntentReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    }

    public void resume() {
        // Adjust the switch component's availability
        // according to the "AirPlane" mode.
        mSwitch.setEnabled(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 0);
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(mDataEnabledListener);
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;

        // Adjust the switch component's availability
        // according to the "AirPlane" mode.
        mSwitch.setEnabled(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 0);

        mSwitch.setOnCheckedChangeListener(mDataEnabledListener);
        mMobileDataEnabled = mConnService.getMobileDataEnabled();
        mSwitch.setChecked(mMobileDataEnabled);
    }

    private OnCheckedChangeListener mDataEnabledListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(mMobileDataEnabled != mSwitch.isChecked()){
                mConnService.setMobileDataEnabled(isChecked);
            }
        }
    };

    /**
     * Receives notifications when enable/disable mobile data.
     */
    private class NetworkStatusChangeIntentReceiver extends BroadcastReceiver {
         @Override
         public void onReceive(Context context, Intent intent) {
             String actionStr = intent.getAction();
             if (TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED.equals(actionStr)) {
                 mMobileDataEnabled = mConnService.getMobileDataEnabled();
                 mSwitch.setChecked(mMobileDataEnabled);
             } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(actionStr)) {
                 mSwitch.setEnabled(!intent.getBooleanExtra("state", false));
             }
         }
    }
}
