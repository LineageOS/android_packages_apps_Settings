/*
   Copyright (c) 2014, The Linux Foundation. All Rights Reserved.
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

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;

public class WifiApSwitch implements CompoundButton.OnCheckedChangeListener {
    private final Context mContext;
    private final HotspotSettings mParent;
    private Switch mSwitch;
    private static final int WIFI_TETHERING = 0;
    private static final int PROVISION_REQUEST = 0;
    private static final String TAG = "WifiApSwitch";

    private boolean mStateMachineEvent;
    private WifiManager mWifiManager;
    private final IntentFilter mIntentFilter;

    ConnectivityManager mCm;
    private String[] mWifiRegexs;
    /* Indicates if we have to wait for WIFI_STATE_CHANGED intent */
    private boolean mWaitForWifiStateChange;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiApStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED));
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                if (mWaitForWifiStateChange == true) {
                    handleWifiStateChanged(intent.getIntExtra(
                            WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
                }
            } else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED.equals(action)) {
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateTetherState(available.toArray(), active.toArray(), errored.toArray());
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                enableWifiCheckBox();
            }
        }
    };

    public WifiApSwitch(Context context, HotspotSettings parent, Switch switch_) {
        mContext = context;
        mParent = parent;
        mSwitch = switch_;
        mWaitForWifiStateChange = true;

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = mCm.getTetherableWifiRegexs();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(this);
        enableWifiCheckBox();
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    private void enableWifiCheckBox() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (!isAirplaneMode) {
            mSwitch.setEnabled(true);
        } else {
            mSwitch.setEnabled(false);
        }
    }

    public void setSoftapEnabled(boolean enable) {
        final ContentResolver cr = mContext.getContentResolver();
        int wifiSavedState = 0;
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = mWifiManager.getWifiState();
        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            mWifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
        }
        /**
         * Check if we have to wait for the WIFI_STATE_CHANGED intent before we
         * re-enable the Checkbox.
         */
        if (!enable) {
            mWaitForWifiStateChange = false;
            try {
                wifiSavedState = Settings.Global.getInt(cr, Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                ;
            }

            if (wifiSavedState == 1) {
                mWaitForWifiStateChange = true;
            }
        }

        if (mWifiManager.setWifiApEnabled(null, enable)) {
            /* Disable here, enabled on receiving success broadcast */
            mSwitch.setEnabled(false);
        }

        /**
         * If needed, restore Wifi on tether disable
         */
        if (!enable) {
            if (wifiSavedState == 1) {
                mWifiManager.setWifiEnabled(true);
                Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 0);
            }
        }
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);
    }

    private void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        for (Object o : tethered) {
            String s = (String) o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex))
                    wifiTethered = true;
            }
        }
        for (Object o : errored) {
            String s = (String) o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex))
                    wifiErrored = true;
            }
        }

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
        }
    }

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                /**
                 * Summary on enable is handled by tether broadcast notice
                 */
                setSwitchChecked(true);
                /* Doesnt need the airplane check */
                mSwitch.setEnabled(true);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                setSwitchChecked(false);
                if (mWaitForWifiStateChange == false) {
                    enableWifiCheckBox();
                }
                break;
            default:
                setSwitchChecked(false);
                enableWifiCheckBox();
        }
    }

    private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_UNKNOWN:
                enableWifiCheckBox();
                break;
            default:
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }

        if (isChecked) {
            mParent.startProvisioningIfNecessary(WIFI_TETHERING);
        } else {
            setSoftapEnabled(false);
        }
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }

}
