/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.*;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.internal.util.wifi.ClientsList;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;

public class WifiApSettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener,
        WifiApEnabler.OnStateChangeListener {

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String CONNECTED_CLIENTS = "connected_clients";

    private static final int DIALOG_AP_SETTINGS = 1;

    private WifiApEnabler mWifiApEnabler;

    private String[] mSecurityType;

    private Preference mCreateNetwork;
    private WifiApClientsProgressCategory mClientsCategory;
    private ArrayList<ClientsList.ClientScanResult> mLastClientList;
    private boolean mApEnabled;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;

    private Handler mHandler = new ClientUpdateHandler();
    private Handler mScanHandler;
    private HandlerThread mScanThread;

    private boolean mIsRestarting = false;

    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_ap_prefs);

        final Activity activity = getActivity();

        mClientsCategory = (WifiApClientsProgressCategory) findPreference(CONNECTED_CLIENTS);

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        String[] wifiRegexs = cm.getTetherableWifiRegexs();

        final boolean wifiAvailable = wifiRegexs.length != 0;

        if (wifiAvailable && !Utils.isMonkeyRunning()) {

            Switch actionBarSwitch = new Switch(activity);

            if (activity instanceof PreferenceActivity) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch,
                        new ActionBar.LayoutParams(
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                ActionBar.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER_VERTICAL | Gravity.END));
            }

            mWifiApEnabler = new WifiApEnabler(activity, actionBarSwitch, this, mClientsCategory);
            actionBarSwitch.setOnCheckedChangeListener(this);
            setHasOptionsMenu(true);
            initWifiTethering();
        }

        mProvisionApp = getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
    }

    private void initWifiTethering() {
        final Activity activity = getActivity();
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);

        if (mWifiConfig == null) {
            final String s = activity.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
            final String summary = String.format(getString(R.string.wifi_tether_configure_subtext),
                    s, mSecurityType[WifiApDialog.OPEN_INDEX]);
            mCreateNetwork.setSummary(summary);
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            final String summary = String.format(getString(R.string.wifi_tether_configure_subtext),
                    mWifiConfig.SSID, mSecurityType[index]);
            mCreateNetwork.setSummary(summary);
        }
    }

    private void updateClientPreferences() {
        final Activity activity = getActivity();

        mClientsCategory.removeAll();

        if (mApEnabled) {
            mClientsCategory.setProgress(mLastClientList == null);
            mClientsCategory.setEmptyTextRes(R.string.wifi_ap_client_none_connected);
            if (mLastClientList != null) {
                for (ClientsList.ClientScanResult client : mLastClientList) {
                    Preference preference = new Preference(activity);
                    preference.setTitle(client.hwAddr);
                    preference.setSummary(client.ipAddr + "   " + client.vendor);
                    mClientsCategory.addPreference(preference);
                }
            }
        } else {
            mClientsCategory.setProgress(false);
            mClientsCategory.setEmptyTextRes(R.string.wifi_ap_client_ap_disabled);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
        }

        return null;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mWifiApEnabler != null) {
            mWifiApEnabler.resume();
        }

        mScanThread = new HandlerThread("WifiApClientScan");
        mScanThread.start();
        mScanHandler = new ClientScanHandler(mScanThread.getLooper());
        if (mApEnabled) {
            mScanHandler.sendEmptyMessage(0);
        }
        updateClientPreferences();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mWifiApEnabler != null) {
            mWifiApEnabler.pause();
        }

        mHandler.removeCallbacksAndMessages(null);
        mScanHandler.removeCallbacksAndMessages(null);
        mScanThread.quit();
        mScanHandler = null;
        mScanThread = null;
    }

    @Override
    public void onStateChanged(boolean enabled) {
        mApEnabled = enabled;
        mLastClientList = null;
        updateClientPreferences();
        if (enabled) {
            mScanHandler.sendEmptyMessage(0);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            startProvisioningIfNecessary();
        } else {
            if (!mIsRestarting) {
                mWifiApEnabler.setSoftapEnabled(false);
            } else {
                mIsRestarting = false;
            }
            mScanHandler.removeCallbacksAndMessages(null);
        }
    }

    boolean isProvisioningNeeded() {
        return !SystemProperties.getBoolean("net.tethering.noprovisioning", false) && mProvisionApp.length == 2;
    }

    private void startProvisioningIfNecessary() {
        if (isProvisioningNeeded()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
            startActivityForResult(intent, PROVISION_REQUEST);
        } else {
            startTethering();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PROVISION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                startTethering();
            }
        }
    }

    private void startTethering() {
        mWifiApEnabler.setSoftapEnabled(true);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        }

        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up
                 * else restart with new config
                 * TODO: update config on a running access point when framework support is added
                 */
                mWifiManager.setWifiApConfiguration(mWifiConfig);
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    mIsRestarting = true;
                    mWifiApEnabler.setSoftapEnabled(false);
                    mWifiApEnabler.setSoftapEnabled(true);
                }
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                String summary = String.format(getString(R.string.wifi_tether_configure_subtext),
                        mWifiConfig.SSID, mSecurityType[index]);
                mCreateNetwork.setSummary(summary);
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private final class ClientUpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            ArrayList<ClientsList.ClientScanResult> clients
                    = (ArrayList<ClientsList.ClientScanResult>) msg.obj;
            if (!clients.equals(mLastClientList)) {
                mLastClientList = clients;
                updateClientPreferences();
            }
            if (mScanHandler != null) {
                mScanHandler.sendEmptyMessageDelayed(0, 2000);
            }
        }
    }

    private final class ClientScanHandler extends Handler {
        public ClientScanHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ArrayList<ClientsList.ClientScanResult> clients
                    = ClientsList.get(true, WifiApSettings.this.getActivity());
            mHandler.obtainMessage(0, clients).sendToTarget();
        }
    }
}
