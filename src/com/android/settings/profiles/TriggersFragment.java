/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Profile;
import android.app.Profile.ProfileTrigger;
import android.app.Profile.TriggerType;
import android.app.ProfileManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Settings Preference to configure triggers to switch profiles base on Wi-Fi events
 */
public class TriggersFragment extends SettingsPreferenceFragment implements ActionBar.OnNavigationListener {
    private Profile mProfile;
    private Preference mSelectedTrigger;
    private ProfileManager mProfileManager;
    private WifiManager mWifiManager;
    private BluetoothAdapter mBluetoothAdapter;

    private int mTriggerFilter = 0;
    private static final int WIFI_TRIGGER = 1;
    private static final int BT_TRIGGER = 2;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProfileManager = (ProfileManager) getSystemService(Context.PROFILE_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();;
        addPreferencesFromResource(R.xml.wifi_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPreferences();
        loadActionBar();
    }

    private void loadActionBar() {
        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        Resources res = getResources();

        String[] navItems = res.getStringArray(R.array.profile_trigger_filters);
        ArrayAdapter<String> navAdapter = new ArrayAdapter<String>(
                actionBar.getThemedContext(), android.R.layout.simple_list_item_1, navItems);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(navAdapter, this);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mProfile = args.getParcelable("profile");
        }
    }

    private void initPreference(AbstractTriggerPreference pref, int state, Resources res, int icon) {
        String[] values = res.getStringArray(R.array.profile_trigger_wifi_options_values);
        for(int i = 0; i < values.length; i++) {
            if (Integer.parseInt(values[i]) == state) {
                pref.setSummary(res.getStringArray(R.array.profile_trigger_wifi_options)[i]);
                break;
            }
        }
        pref.setTriggerState(state);
        pref.setIcon(icon);
    }

    private void loadPreferences() {
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        final Resources res = getResources();
        final List<AbstractTriggerPreference> prefs = new ArrayList<AbstractTriggerPreference>();

        getPreferenceScreen().removeAll();

        if (mTriggerFilter == WIFI_TRIGGER || mTriggerFilter == 0) {
            if (configs != null ) {
                for (WifiConfiguration config : configs) {
                    WifiTriggerAPPreference accessPoint =
                            new WifiTriggerAPPreference(getActivity(), config);
                    int state = mProfile.getTrigger(Profile.TriggerType.WIFI, accessPoint.getSSID());
                    initPreference(accessPoint, state, res, R.drawable.ic_wifi_signal_4_dark);
                    prefs.add(accessPoint);
                }
            } else {
                final List<ProfileTrigger> triggers = mProfile.getTriggersFromType(TriggerType.WIFI);
                for (ProfileTrigger trigger : triggers) {
                    WifiTriggerAPPreference accessPoint =
                            new WifiTriggerAPPreference(getActivity(), trigger.getName());
                    initPreference(accessPoint, trigger.getState(), res, R.drawable.ic_wifi_signal_4_dark);
                    prefs.add(accessPoint);
                }
            }
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (mTriggerFilter == BT_TRIGGER || mTriggerFilter == 0) {
            if (!pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    BluetoothTriggerPreference bt =
                            new BluetoothTriggerPreference(getActivity(), device);
                    int state = mProfile.getTrigger(Profile.TriggerType.BLUETOOTH, bt.getAddress());
                    initPreference(bt, state, res, R.drawable.ic_settings_bluetooth2);
                    prefs.add(bt);
                }
            } else {
                final List<ProfileTrigger> triggers = mProfile.getTriggersFromType(TriggerType.BLUETOOTH);
                for (ProfileTrigger trigger : triggers) {
                    BluetoothTriggerPreference bt = new BluetoothTriggerPreference(getActivity(),
                            trigger.getName(), trigger.getId());
                    initPreference(bt, trigger.getState(), res, R.drawable.ic_settings_bluetooth2);
                    prefs.add(bt);
                }
            }
        }

        Collections.sort(prefs, new Comparator<AbstractTriggerPreference>() {
            @Override
            public int compare(AbstractTriggerPreference o1, AbstractTriggerPreference o2) {
                if (o1.getTriggerState() == o2.getTriggerState()) {
                    return o1.compareTo(o2);
                }
                if (o1.getTriggerState() == Profile.TriggerState.DISABLED) {
                    return 1;
                } else if (o2.getTriggerState() == Profile.TriggerState.DISABLED) {
                    return -1;
                } else {
                    return o1.getTriggerState() < o2.getTriggerState() ? -1 : 1;
                }
            }
        });
        for (Preference pref: prefs) {
            getPreferenceScreen().addPreference(pref);
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        mSelectedTrigger = preference;
        if (preference instanceof WifiTriggerAPPreference) {
            showDialog(WIFI_TRIGGER);
            return true;
        } else if (preference instanceof BluetoothTriggerPreference) {
            showDialog(BT_TRIGGER);
            return true;
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    private class Trigger {
        int value;
        String name;
    }

    private void removeTrigger(List<Trigger> triggers, int value) {
        for(Trigger t : triggers) {
            if (t.value == value) {
                triggers.remove(t);
                return;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        final String id;
        final String triggerName = mSelectedTrigger.getTitle().toString();
        final int triggerType;
        String[] entries = getResources().getStringArray(R.array.profile_trigger_wifi_options);
        String[] values = getResources().getStringArray(R.array.profile_trigger_wifi_options_values);
        List<Trigger> triggers = new ArrayList<Trigger>(entries.length);
        for(int i = 0; i < entries.length; i++) {
            Trigger toAdd = new Trigger();
            toAdd.value = Integer.parseInt(values[i]);
            toAdd.name = entries[i];
            triggers.add(toAdd);
        }

        switch (dialogId) {
            case WIFI_TRIGGER:
                WifiTriggerAPPreference pref = (WifiTriggerAPPreference) mSelectedTrigger;
                id = pref.getSSID();
                triggerType = Profile.TriggerType.WIFI;
                removeTrigger(triggers, Profile.TriggerState.ON_A2DP_CONNECT);
                removeTrigger(triggers, Profile.TriggerState.ON_A2DP_DISCONNECT);
                break;
            case BT_TRIGGER:
                BluetoothTriggerPreference btpref = (BluetoothTriggerPreference) mSelectedTrigger;
                id = btpref.getAddress();
                triggerType = Profile.TriggerType.BLUETOOTH;
                BluetoothDevice dev = mBluetoothAdapter.getRemoteDevice(id);
                if (!dev.getBluetoothClass().doesClassMatch(BluetoothClass.PROFILE_A2DP)) {
                    removeTrigger(triggers, Profile.TriggerState.ON_A2DP_CONNECT);
                    removeTrigger(triggers, Profile.TriggerState.ON_A2DP_DISCONNECT);
                }
                break;
            default:
                return super.onCreateDialog(dialogId);
        }

        entries = new String[triggers.size()];
        final int[] valueInts = new int[triggers.size()];
        int currentTrigger = mProfile.getTrigger(triggerType, id);
        int currentItem = -1;
        for(int i = 0; i < triggers.size(); i++) {
            Trigger t = triggers.get(i);
            entries[i] = t.name;
            valueInts[i] = t.value;
            if (valueInts[i] == currentTrigger) {
                currentItem = i;
            }
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_trigger_configure)
                .setSingleChoiceItems(entries,
                        currentItem,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProfile.setTrigger(triggerType, id, valueInts[which], triggerName);
                        mProfileManager.updateProfile(mProfile);
                        loadPreferences();
                        dialog.dismiss();
                    }
                })
                .create();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        mTriggerFilter = itemPosition;
        loadPreferences();
        return true;
    }
}
