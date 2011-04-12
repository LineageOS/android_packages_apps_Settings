/*
 * Copyright (C) 2010 The CyanogenMod Project
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

package com.android.settings.wimax;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.wimax.WimaxConstants;
import com.android.wimax.WimaxSettingsHelper;

/**
 * Settings screen for Wimax. This will be launched from the main system settings.
 */
public class WimaxSettings extends PreferenceActivity { // implements WimaxLayer.Callback {

    private static final String TAG = "WimaxSettings";

    //============================
    // Preference/activity member variables
    //============================

    private static final int CONTEXT_MENU_ID_CONNECT = Menu.FIRST;
    private static final int CONTEXT_MENU_ID_DISCONNECT = Menu.FIRST + 1;

    private static final String KEY_WIMAX_ENABLED = "wimax_enabled";
    private static final String KEY_WIMAX_NETWORKS = "wimax_networks";

    private static final String KEY_WIMAX_SCAN = "wimax_scan";

    private ProgressCategory mNetworksCategory;
    private CheckBoxPreference mWimaxEnabled;
    private WimaxEnabler mWimaxEnabler;

    private WeakHashMap<String, Preference> mPrefs;
    private Object mWimaxController;
    private WimaxSettingsHelper mHelper;

    //Variables imported from AdvancedSettings

    //private static final String TAG = "WimaxAdvancedSettings";
    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_SW_VERSION = "sw_version";
    private static final String KEY_IP_ADDRESS = "ip_address";
    private static final String KEY_GATEWAY = "gateway";
    private static final String KEY_SIG_STR_SIMPLE = "signal_strength_simple";
    //private static final String KEY_SIG_STR_DB = "signal_strength_db";
    //private WimaxSettingsHelper mHelper;

    //private WimaxLayer mWimaxLayer;

    //============================
    // Activity lifecycle
    //============================

    public WimaxSettings() {
        mPrefs = new WeakHashMap<String, Preference>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new WimaxSettingsHelper(this);
        onCreatePreferences();

        IntentFilter filter = new IntentFilter(WimaxConstants.RSSI_CHANGED_ACTION);
        this.registerReceiver(mIntentReceiver, filter);
        //IntentFilter filter2 = new IntentFilter(WimaxConstants.NETWORK_STATE_CHANGED_ACTION);
        //this.registerReceiver(mIntentReceiver, filter2);

        refreshAll();
        //mWimaxLayer.onCreate();
        //mWimaxLayer.onCreatedCallback();
    }

    private void onCreatePreferences() {
        addPreferencesFromResource(R.xml.wimax_settings);
        mWimaxController = getSystemService(Context.WIMAX_SERVICE);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mNetworksCategory = (ProgressCategory) preferenceScreen.findPreference(KEY_WIMAX_NETWORKS);

        mWimaxEnabled = (CheckBoxPreference) preferenceScreen.findPreference(KEY_WIMAX_ENABLED);
        mWimaxEnabler = new WimaxEnabler(this, mWimaxController, mWimaxEnabled);
        //mWimaxEnabler.setWimaxLayer(//mWimaxLayer);

        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter(WimaxConstants.RSSI_CHANGED_ACTION);
        this.registerReceiver(mIntentReceiver, filter);
        //IntentFilter filter2 = new IntentFilter(WimaxConstants.NETWORK_STATE_CHANGED_ACTION);
        //this.registerReceiver(mIntentReceiver, filter2);

        refreshAll();
        //mWimaxLayer.onResume();
        mWimaxEnabler.resume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(mIntentReceiver);
        //mWimaxLayer.onPause();
        mWimaxEnabler.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //this.unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        //String nspName = getNspNameFromMenuInfo(menuInfo);
        //if (nspName == null) {
        //    return;
        //}

        //menu.setHeaderTitle(nspName);

        //if(mWimaxLayer.getCurrentNspName() != null && mWimaxLayer.getCurrentNspName().equalsIgnoreCase(nspName)) {
        //    menu.add(0, CONTEXT_MENU_ID_DISCONNECT, 1, R.string.wimax_context_menu_disconnect);
        //}else {
        //    menu.add(0, CONTEXT_MENU_ID_CONNECT, 0, R.string.wimax_context_menu_connect);
        //}
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //String nspName = getNspNameFromMenuInfo(item.getMenuInfo());

        //if (nspName == null) {
        //    return false;
        //}

        /*switch (item.getItemId()) {

            case CONTEXT_MENU_ID_CONNECT:
                mWimaxLayer.connectToNetwork(nspName);
                return true;

            case CONTEXT_MENU_ID_DISCONNECT:
                mWimaxLayer.disconnectFromNetwork();
                return true;

            default:
                return false;
        }*/
	return true;
    }

    private String getNspNameFromMenuInfo(ContextMenuInfo menuInfo) {
        if ((menuInfo == null) || !(menuInfo instanceof AdapterContextMenuInfo)) {
            return null;
        }

        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        if(adapterMenuInfo.position < 4) {  //skip the first two menu items.
            return null;
        }
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(
                adapterMenuInfo.position);
        if (pref == null || !(pref instanceof Preference)) {
            return null;
        }

        if(pref.getTitle() == null || pref.getTitle().length() == 0)
            return null;
        else
            return String.valueOf(pref.getTitle());
    }

    /**private int getSignalStrength(int rssi) {
        int level = mHelper.calculateSignalLevel(rssi, 4);
        switch(level) {
            case 0: return R.string.wimax_signal_0;
            case 1: return R.string.wimax_signal_1;
            case 2: return R.string.wimax_signal_2;
            case 3: return R.string.wimax_signal_3;
            default: return R.string.status_unavailable;
        }
    }*/

    //============================
    // Preference callbacks
    //============================

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (KEY_WIMAX_SCAN.equals(preference.getKey())) {
            //WiMAX scanning code as provided for the Scan button under the OptionsMenu
            try {
                Method wimaxRescan = mWimaxController.getClass().getMethod("wimaxRescan");
                if (wimaxRescan != null) {
                    wimaxRescan.invoke(mWimaxController);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to perform WiMAX rescan!", e);
            }
        }
        super.onPreferenceTreeClick(preferenceScreen, preference);
        return false;
    }

    //============================
    // Wimax callbacks
    //============================

    public void onError(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
    }

    public void onScanningStatusChanged(boolean started) {
        mNetworksCategory.setProgress(started);
    }

    /*public void onNetworkListChanged(NSPInfo nspInfo, boolean added) {

        String nspName = nspInfo.getNspName();
        Preference pref = mPrefs.get(nspName);

        if (WimaxLayer.LOGV) {
            Log.v(TAG, "onNetworkListChanged with " + nspName + " and "
                    + (added ? "added" : "removed"));
        }

        if (added) {
            if (pref == null) {
                pref = new Preference(this);
                pref.setTitle(nspName);
                mPrefs.put(nspName, pref);
            } else {
                pref.setEnabled(true);
            }
            pref.setSummary(getSignalStrength(nspInfo.getRssiInDBm()));

            mNetworksCategory.addPreference(pref);
        } else {
            mPrefs.remove(nspName);

            if (pref != null) {
                mNetworksCategory.removePreference(pref);
            }
        }
    }*/

    public void onWimaxStatusChanged(boolean enabled) {
        if (enabled) {
            //mNetworksCategory.setEnabled(true);
        } else {
            mNetworksCategory.removeAll();
            mPrefs.clear();
        }
    }

    private void refreshAll() {
        refreshDeviceInfo();
        refreshIPInfo();
    }

    private void refreshDeviceInfo() {
        //Preference wimaxSignalStrengthRSSIPref = findPreference(KEY_SIG_STR_SIMPLE);
        //wimaxSignalStrengthRSSIPref.setSummary(getString(R.string.status_unavailable));

        //Preference wimaxSignalStrengthDBPref = findPreference(KEY_SIG_STR_DB);
        //wimaxSignalStrengthDBPref.setSummary("Level: " + getString(R.string.status_unavailable));

        Preference wimaxMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        String macAddress = SystemProperties.get("persist.wimax.0.MAC", getString(R.string.status_unavailable));
        wimaxMacAddressPref.setSummary(macAddress);

        Preference wimaxSwVersionPref = findPreference(KEY_SW_VERSION);
        String swVersion = SystemProperties.get("persist.wimax.fw.version", getString(R.string.status_unavailable));
        wimaxSwVersionPref.setSummary(swVersion);
    }

    private void refreshIPInfo() {
        Preference wimaxIpAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = SystemProperties.get("dhcp.wimax0.ipaddress", getString(R.string.status_unavailable));
        wimaxIpAddressPref.setSummary(ipAddress);

        Preference wimaxGatewayPref = findPreference(KEY_GATEWAY);
        String gateway = SystemProperties.get("dhcp.wimax0.gateway", getString(R.string.status_unavailable));
        wimaxGatewayPref.setSummary(gateway);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WimaxConstants.NETWORK_STATE_CHANGED_ACTION) ||
                    action.equals(WimaxConstants.RSSI_CHANGED_ACTION)) {
                updateWimax(intent);
            }
        }
    };

    private final void updateWimax(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WimaxConstants.RSSI_CHANGED_ACTION)) {
            int rssi = intent.getIntExtra(WimaxConstants.EXTRA_NEW_RSSI_LEVEL, -200);
            Preference wimaxSignalStrengthRSSIPref = findPreference(KEY_SIG_STR_SIMPLE);
            if (rssi == 0) {
                wimaxSignalStrengthRSSIPref.setSummary("Level: " + getString(R.string.wimax_signal_0));
            } else if (rssi == 1) {
                wimaxSignalStrengthRSSIPref.setSummary("Level: " + getString(R.string.wimax_signal_1));
            } else if (rssi == 2) {
                wimaxSignalStrengthRSSIPref.setSummary("Level: " + getString(R.string.wimax_signal_2));
            } else if (rssi == 3) {
                wimaxSignalStrengthRSSIPref.setSummary("Level: " + getString(R.string.wimax_signal_3));
            } else {
                wimaxSignalStrengthRSSIPref.setSummary("Level: " + getString(R.string.status_unavailable));
            }
        }
        if (action.equals(WimaxConstants.NETWORK_STATE_CHANGED_ACTION)) {
            //final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WimaxConstants.EXTRA_NETWORK_INFO);
        }
    }
}
