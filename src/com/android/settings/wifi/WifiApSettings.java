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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class WifiApSettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener,
        WifiApEnabler.OnStateChangeListener {
    private static final String TAG = "WifiApSettings";

    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String CONNECTED_CLIENTS = "connected_clients";

    private static final int DIALOG_AP_SETTINGS = 1;

    private WifiApEnabler mWifiApEnabler;

    private String[] mWifiRegexs;
    private String[] mSecurityType;

    private Preference mCreateNetwork;
    private CheckBoxPreference mEnableWifiAp;
    private WifiApClientsProgressCategory mClientsCategory;
    private ArrayList<ClientScanResult> mLastClientList;
    private boolean mApEnabled;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;

    private Handler mHandler = new ClientUpdateHandler();
    private Handler mScanHandler;
    private HandlerThread mScanThread;

    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;

    private static class ClientScanResult {
        String ipAddr;
        String hwAddr;
        String device;
        boolean isReachable;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_ap_prefs);

        final Activity activity = getActivity();

        mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);
        mClientsCategory = (WifiApClientsProgressCategory) findPreference(CONNECTED_CLIENTS);

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = cm.getTetherableWifiRegexs();

        final boolean wifiAvailable = mWifiRegexs.length != 0;

        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            mWifiApEnabler = new WifiApEnabler(activity, mEnableWifiAp, this);
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
                for (ClientScanResult client : mLastClientList) {
                    Preference preference = new Preference(activity);
                    int summaryResId = client.isReachable
                            ? R.string.wifi_ap_client_reachable
                            : R.string.wifi_ap_client_unreachable;
                    preference.setSummary(getString(summaryResId, client.hwAddr));
                    preference.setTitle(client.ipAddr);
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

        final Activity activity = getActivity();

        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(this);
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
            mEnableWifiAp.setOnPreferenceChangeListener(null);
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
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = (Boolean) value;

        if (enable) {
            startProvisioningIfNecessary();
        } else {
            mWifiApEnabler.setSoftapEnabled(false);
            mScanHandler.removeCallbacksAndMessages(null);
        }
        return false;
    }

    boolean isProvisioningNeeded() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            return false;
        }
        return mProvisionApp.length == 2;
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
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                } else {
                    mWifiManager.setWifiApConfiguration(mWifiConfig);
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
            final Activity activity = getActivity();
            ArrayList<ClientScanResult> clients = (ArrayList<ClientScanResult>) msg.obj;
            if (!clients.equals(mLastClientList)) {
                mLastClientList = clients;
                updateClientPreferences();
            }
            if (mScanHandler != null && clients != null) {
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
            ArrayList<ClientScanResult> clients = getClientList(false);
            mHandler.obtainMessage(0, clients).sendToTarget();
        }

        /**
         * Gets a list of the clients connected to the Hotspot, reachable timeout is 1000
         *
         * @param onlyReachables {@code false} if the list should contain unreachable
         *                       (probably disconnected) clients, {@code true} otherwise
         * @return ArrayList of {@link ClientScanResult}
         */
        private ArrayList<ClientScanResult> getClientList(boolean onlyReachables) {
            BufferedReader br = null;
            ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();

            try {
                br = new BufferedReader(new FileReader("/proc/net/arp"));
                String line;

                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");

                    if (splitted.length >= 6) {
                        // Basic sanity check
                        String mac = splitted[3];

                        if (mac.matches("..:..:..:..:..:..")) {
                            InetAddress address = InetAddress.getByName(splitted[0]);
                            boolean isReachable = address.isReachable(1000);

                            if (!onlyReachables || isReachable) {
                                ClientScanResult client = new ClientScanResult();
                                client.ipAddr = splitted[0];
                                client.hwAddr = mac;
                                client.device = splitted[5];
                                client.isReachable = isReachable;
                                result.add(client);
                            }
                        }
                    }
                }
            } catch (UnknownHostException e) {
                Log.d(TAG, "catch UnknownHostException hit in run", e);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "catch FileNotFoundException hit in run", e);
            } catch (IOException e) {
                Log.d(TAG, "catch IOException hit in run", e);
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            return result;
        }
    }
}
