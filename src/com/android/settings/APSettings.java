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

package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class APSettings extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = "APSettings";

    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";

    private static final int DIALOG_AP_SETTINGS = 1;

    private WifiApEnabler mWifiApEnabler;
    private CheckBoxPreference mEnableWifiAp;

    private String[] mWifiRegexs;

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private String[] mSecurityType;
    private Preference mCreateNetwork;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;

    private static final int WIFI_TETHERING = 0;

    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;

    private WifiApManager wifiApManager;

    private Activity activity;
    private PreferenceScreen root;
    private PreferenceCategory clientsCategory;
    private ArrayList<APSettings.ClientScanResult> clients_old;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_ap_prefs);
        root = this.getPreferenceScreen();

        activity = getActivity();

        clientsCategory = new PreferenceCategory(activity);
        clientsCategory.setTitle(R.string.clients);
        root.addPreference(clientsCategory);

        mEnableWifiAp =
                (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = cm.getTetherableWifiRegexs();

        final boolean wifiAvailable = mWifiRegexs.length != 0;

        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            mWifiApEnabler = new WifiApEnabler(activity, mEnableWifiAp);
            initWifiTethering();
        }

        mProvisionApp = getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);

        wifiApManager = new WifiApManager(activity);

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                            scan();
                        } else {
                            clientsCategory.removeAll();
                        }
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    private void scan() {
        ArrayList<APSettings.ClientScanResult> clients = wifiApManager.getClientList(false);
        if (clients != clients_old) {
            clients_old = clients;
            clientsCategory.removeAll();
            for (ClientScanResult clientScanResult : clients) {
                Preference preference = new Preference(activity);
                if (clientScanResult.isReachable()) {
                    preference.setSummary(clientScanResult.getHWAddr() + " - " + getString(R.string.reachable));
                } else {
                    preference.setSummary(clientScanResult.getHWAddr() + " - " + getString(R.string.unreachable));
                }
                preference.setTitle(clientScanResult.getIpAddr());
                clientsCategory.addPreference(preference);
            }
        }
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
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    s, mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    mWifiConfig.SSID,
                    mSecurityType[index]));
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

        activity = getActivity();

        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(this);
            mWifiApEnabler.resume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(null);
            mWifiApEnabler.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = (Boolean) value;

        if (enable) {
            startProvisioningIfNecessary(WIFI_TETHERING);
            scan();
        } else {
            mWifiApEnabler.setSoftapEnabled(false);
        }
        return false;
    }

    boolean isProvisioningNeeded() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            return false;
        }
        return mProvisionApp.length == 2;
    }

    private void startProvisioningIfNecessary(int choice) {
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

    private static String findIface(String[] ifaces, String[] regexes) {
        for (String iface : ifaces) {
            for (String regex : regexes) {
                if (iface.matches(regex)) {
                    return iface;
                }
            }
        }
        return null;
    }

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
                mCreateNetwork.setSummary(String.format(getActivity().getString(CONFIG_SUBTEXT),
                        mWifiConfig.SSID,
                        mSecurityType[index]));
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    /**
     * Checks whether this screen will have anything to show on this device. This is called by
     * the shortcut picker for Settings shortcuts (home screen widget).
     *
     * @param context a context object for getting a system service.
     * @return whether Tether & portable hotspot should be shown in the shortcuts picker.
     */
    public static boolean showInShortcuts(Context context) {
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final boolean isSecondaryUser = UserHandle.myUserId() != UserHandle.USER_OWNER;
        return !isSecondaryUser && cm.isTetheringSupported();
    }

    class ClientScanResult {

        private String IpAddr;

        private String HWAddr;

        private String Device;

        private boolean isReachable;

        public ClientScanResult(String ipAddr, String hWAddr, String device, boolean isReachable) {
            super();
            IpAddr = ipAddr;
            HWAddr = hWAddr;
            Device = device;
            this.setReachable(isReachable);
        }

        public String getIpAddr() {
            return IpAddr;
        }

        public void setIpAddr(String ipAddr) {
            IpAddr = ipAddr;
        }

        public String getHWAddr() {
            return HWAddr;
        }

        public void setHWAddr(String hWAddr) {
            HWAddr = hWAddr;
        }

        public String getDevice() {
            return Device;
        }

        public void setDevice(String device) {
            Device = device;
        }

        public boolean isReachable() {
            return isReachable;
        }

        public void setReachable(boolean isReachable) {
            this.isReachable = isReachable;
        }
    }

    class WifiApManager {
        private final WifiManager mWifiManager;

        public WifiApManager(Context context) {
            mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }


        /**
         * Gets a list of the clients connected to the Hotspot, reachable timeout is 300
         *
         * @param onlyReachables {@code false} if the list should contain unreachable (probably disconnected) clients, {@code true} otherwise
         * @return ArrayList of {@link APSettings.ClientScanResult}
         */
        public ArrayList<APSettings.ClientScanResult> getClientList(boolean onlyReachables) {
            getClientList getClientList1 = new getClientList(onlyReachables, 300);
            try {
                return getClientList1.execute().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }

        class getClientList extends AsyncTask<String, Integer, ArrayList<ClientScanResult>> {

            BufferedReader br = null;
            ArrayList<APSettings.ClientScanResult> result = null;
            boolean onlyReachables;
            int reachableTimeout;

            public getClientList(boolean onlyReachablesO, int reachableTimeoutO) {
                onlyReachables = onlyReachablesO;
                reachableTimeout = reachableTimeoutO;
            }

            protected ArrayList<APSettings.ClientScanResult> doInBackground(String... sUrl) {
                try {
                    result = new ArrayList<APSettings.ClientScanResult>();
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

                                if (!onlyReachables || isReachable) {
                                    result.add(new APSettings.ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

                return result;
            }
        }
    }
}
