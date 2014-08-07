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

package com.android.settings.wifi;

import com.android.settings.R;
import com.android.settings.WirelessSettings;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.widget.Switch;

public class WifiApEnabler {
    public interface OnStateChangeListener {
        void onStateChanged(boolean enabled);
    }

    private final Context mContext;
    private final Activity mActivity;
    private Switch mSwitch;
    private final OnStateChangeListener mListener;

    private WifiManager mWifiManager;
    private final IntentFilter mIntentFilter;

    ConnectivityManager mCm;
    private String[] mWifiRegexs;
    /* Indicates if we have to wait for WIFI_STATE_CHANGED intent */
    private boolean mWaitForWifiStateChange;

    private WifiApClientsProgressCategory mClientsCategory;

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
                enableWifiSwitch();
            }

        }
    };

    public WifiApEnabler(Activity activity, Switch switch_,
            OnStateChangeListener listener, WifiApClientsProgressCategory clientsCategory) {
        mContext = activity;
        mActivity = activity;
        mSwitch = switch_;
        mListener = listener;
        mClientsCategory = clientsCategory;
        mWaitForWifiStateChange = false;

        mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = mCm.getTetherableWifiRegexs();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        enableWifiSwitch();
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void enableWifiSwitch() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if(!isAirplaneMode) {
            mSwitch.setEnabled(true);
        } else {
            mClientsCategory.setEmptyTextRes(R.string.wifi_ap_client_ap_disabled);
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
         * Check if we have to wait for the WIFI_STATE_CHANGED intent
         * before we re-enable the Checkbox.
         */
        if (!enable) {
            try {
                wifiSavedState = Settings.Global.getInt(cr, Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException ignored) {
            }

            if (wifiSavedState == 1) {
                 mWaitForWifiStateChange = true;
            }
        }

        if (mWifiManager.setWifiApEnabled(null, enable)) {
            // Disable here, enabled on receiving success broadcast
            mSwitch.setEnabled(false);
        } else {
            mClientsCategory.setEmptyTextRes(R.string.wifi_error);
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

    private void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        for (Object o : tethered) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiTethered = true;
            }
        }

        for (Object o: errored) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiErrored = true;
            }
        }

        if (wifiErrored && !wifiTethered) {
            mClientsCategory.setEmptyTextRes(R.string.wifi_error);
        }
    }

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                mClientsCategory.setEmptyTextRes(R.string.wifi_tether_starting);
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                updateState(true);
                /* Doesnt need the airplane check */
                mClientsCategory.setEmptyTextRes(R.string.wifi_ap_client_none_connected);
                mSwitch.setEnabled(true);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                mClientsCategory.setEmptyTextRes(R.string.wifi_tether_stopping);
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                updateState(false);
                mClientsCategory.setEmptyTextRes(R.string.wifi_ap_client_ap_disabled);
                if (mWaitForWifiStateChange == false) {
                    enableWifiSwitch();
                }
                break;
            default:
                updateState(false);
                mClientsCategory.setEmptyTextRes(R.string.wifi_ap_client_ap_disabled);
                enableWifiSwitch();
        }
    }

    private void updateState(boolean enabled) {
        mSwitch.setChecked(enabled);
        if (mListener != null) {
            mListener.onStateChanged(enabled);
        }
    }

    private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_UNKNOWN:
                enableWifiSwitch();
                mWaitForWifiStateChange = false;
                break;
            default:
        }
    }
}
