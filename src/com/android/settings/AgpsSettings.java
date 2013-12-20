/*
   Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
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

package com.android.settings;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.location.LocationManager;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AgpsSettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    static final String TAG = "AGPSSettings";

    private static final int MSB_MODE = 0;
    private static final int MSA_MODE = 1;
    private static final String MSB = "0";
    private static final String MSA = "1";
    private static final String ERR_MODE = "2";
    private static final String AGPS_LOCATION_MODE_MSB = "MSB";
    private static final String AGPS_LOCATION_MODE_MSA = "MSA";

    private static final int HOME_MODE = 0;
    private static final int ALL_MODE = 1;
    private static final String HOME = "0";
    private static final String ALL = "1";
    private static final String AGPS_NETWORK_TYPE_HOME = "HOME";
    private static final String AGPS_NETWORK_TYPE_ALL = "ALL";

    private static final int HOT_MODE = 0;
    private static final int WARM_MODE = 1;
    private static final String HOT = "0";
    private static final String WARM = "1";
    private static final String COLD = "2";
    private static final String AGPS_START_MODE_HOT = "HOT";
    private static final String AGPS_START_MODE_WARM = "WARM";
    private static final String AGPS_START_MODE_COLD = "COLD";

    private static final String STRING_SUPL_HOST = "host";
    private static final String STRING_SUPL_PORT = "port";
    private static final String STRING_PROVIDER_ID = "providerid";
    private static final String STRING_ACCESS_NETWORK = "network";
    private static final String STRING_AGPS_RESET_TYPE = "resettype";

    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_RESTORE = MENU_SAVE + 1;

    private static final String PROPERTIES_FILE = "/etc/gps.conf";

    private static String sNotSet;
    private EditTextPreference mServer;
    private EditTextPreference mPort;
    private boolean mFirstTime;

    private String mAssistedType;
    private String mResetType;
    private String mNetworkType;

    private ContentResolver mContentResolver;

    // CMCC assisted gps SUPL(Secure User Plane Location) server address
    private static final String ASSISTED_GPS_SUPL_HOST = "assisted_gps_supl_host";

    // CMCC agps SUPL port address
    private static final String ASSISTED_GPS_SUPL_PORT = "assisted_gps_supl_port";

    // location agps position mode,MSB or MSA
    private static final String ASSISTED_GPS_POSITION_MODE = "assisted_gps_position_mode";

    // location agps start mode,cold start or hot start.
    private static final String ASSISTED_GPS_RESET_TYPE = "assisted_gps_reset_type";

    // location agps start network,home or all
    private static final String ASSISTED_GPS_NETWORK = "assisted_gps_network";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContentResolver = getContentResolver();
        addPreferencesFromResource(R.xml.agps_settings);
        mFirstTime = (icicle == null);
        sNotSet = getResources().getString(R.string.supl_not_set);
        mServer = (EditTextPreference) findPreference("server_addr");
        mPort = (EditTextPreference) findPreference("server_port");
        fillUi(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setPrefAgpsType() {
        final ListPreference pref = (ListPreference) findPreference("agps_pref");
        pref.setOnPreferenceChangeListener(this);
        String[] types = getResources().getStringArray(R.array.agps_si_mode_entries);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String defPref = getPrefAgpsType();
        mAssistedType = defPref;
        if (defPref.equals(AGPS_LOCATION_MODE_MSB)) {
            pref.setValue(MSB);
            pref.setSummary(types[0]);
        } else if (defPref.equals(AGPS_LOCATION_MODE_MSA)) {
            pref.setValue(MSA);
            pref.setSummary(types[1]);
        } else {
            pref.setValue(ERR_MODE);
            pref.setSummary("");
        }
    }

    private void setPrefAgpsNetwork() {
        ListPreference pref = (ListPreference) findPreference("agps_network");
        pref.setOnPreferenceChangeListener(this);
        String[] types = getResources().getStringArray(R.array.agps_network_entries);
        String defPref = getPrefNetwork();
        mNetworkType = defPref;
        if (defPref.equals(AGPS_NETWORK_TYPE_ALL)) {
            pref.setValue(ALL);
            pref.setSummary(types[1]);
        } else {
            pref.setValue(HOME);
            pref.setSummary(types[0]);
        }
    }

    private void setPrefAgpsResetType() {
        ListPreference pref = (ListPreference) findPreference("agps_reset_type");
        pref.setOnPreferenceChangeListener(this);
        String[] types = getResources().getStringArray(R.array.agps_reset_type_entries);
        String defPref = getPrefAgpsResetType();
        mResetType = defPref;
        if (defPref.equals(AGPS_START_MODE_COLD)) {
            pref.setValue(COLD);
            pref.setSummary(types[2]);
        } else if (defPref.equals(AGPS_START_MODE_WARM)) {
            pref.setValue(WARM);
            pref.setSummary(types[1]);
        } else {
            pref.setValue(HOT);
            pref.setSummary(types[0]);
        }
    }

    private void fillUi(boolean restore) {
        if (mFirstTime || restore) {
            mFirstTime = false;
            mServer.setText(getSuplServer());
            mPort.setText(getSuplPort());
        }

        mServer.setSummary(checkNull(mServer.getText()));
        mPort.setSummary(checkNull(mPort.getText()));
        setPrefAgpsType();
        setPrefAgpsNetwork();
        setPrefAgpsResetType();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
        }
    }

    private String checkNull(String value) {
        if (TextUtils.isEmpty(value)) {
            return sNotSet;
        } else {
            return value;
        }
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    private String getSuplServer() {
        String supl_host = Settings.Global.getString(mContentResolver,
                ASSISTED_GPS_SUPL_HOST);
        return (null != supl_host) ? supl_host :
                getResources().getString(R.string.location_agps_def_supl_host);
    }

    private String getSuplPort() {
        String supl_port = Settings.Global.getString(mContentResolver,
                ASSISTED_GPS_SUPL_PORT);
        return (null != supl_port) ? supl_port :
                getResources().getString(R.string.location_agps_def_supl_port);
    }

    private String getPrefNetwork() {
        String agps_network = Settings.Global.getString(mContentResolver,
                ASSISTED_GPS_NETWORK);
        return (null != agps_network) ? agps_network :
                getResources().getString(R.string.location_agps_def_network_mode);
    }

    private String getPrefAgpsResetType() {
        String agps_reset_type = Settings.Global.getString(mContentResolver,
                ASSISTED_GPS_RESET_TYPE);
        if ( agps_reset_type != null ) {
            if ( agps_reset_type.compareTo("2") == 0 ) {
                agps_reset_type = AGPS_START_MODE_HOT;
            } else if (agps_reset_type.compareTo("1") == 0 ) {
                agps_reset_type = AGPS_START_MODE_WARM;
            } else {
                agps_reset_type = AGPS_START_MODE_COLD;
            }
        }
        return (null != agps_reset_type) ? agps_reset_type :
                getResources().getString(R.string.location_agps_def_reset_type);
    }

    private String getPrefAgpsType() {
        String agps_type = Settings.Global.getString(mContentResolver,
                ASSISTED_GPS_POSITION_MODE);
        return (null != agps_type) ? agps_type :
                getResources().getString(R.string.location_agps_def_location_mode);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0,
                getResources().getString(R.string.menu_save))
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                saveAgpsParams();
                return true;

            case MENU_RESTORE:
                restoreAgpsParam();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAgpsParams() {
        Bundle bundle = new Bundle();
        bundle.putString(STRING_SUPL_HOST, checkNotSet(mServer.getText()));
        bundle.putString(STRING_SUPL_PORT, checkNotSet(mPort.getText()));
        bundle.putString(STRING_PROVIDER_ID, mAssistedType);
        bundle.putString(STRING_ACCESS_NETWORK, mNetworkType);
        SetValue(bundle);
        if ( mResetType.compareTo(AGPS_START_MODE_HOT) == 0 ) {
            bundle.putString(STRING_AGPS_RESET_TYPE, "2");
        } else if ( mResetType.compareTo(AGPS_START_MODE_WARM) == 0 ) {
            bundle.putString(STRING_AGPS_RESET_TYPE, "1");
        } else {
            bundle.putString(STRING_AGPS_RESET_TYPE, "0");
        }
        LocationManager objLocManager = (LocationManager)
                                        getSystemService(Context.LOCATION_SERVICE);
        boolean bRet = objLocManager.sendExtraCommand(LocationManager.GPS_PROVIDER,
                "agps_parms_changed", bundle);
        Log.d(TAG, "sendExtraCommand ret=" + bRet);
    }

    private void restoreAgpsParam() {
        Bundle bundle = new Bundle();
        FileInputStream stream = null;
        try {
            Properties properties = new Properties();
            File file = new File(PROPERTIES_FILE);
            stream = new FileInputStream(file);
            properties.load(stream);
            bundle.putString(STRING_SUPL_HOST, properties.getProperty("SUPL_HOST", null));
            bundle.putString(STRING_SUPL_PORT, properties.getProperty("SUPL_PORT", null));

        } catch (IOException e) {
            Log.e(TAG, "Could not open GPS configuration file " + PROPERTIES_FILE + ", e=" + e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
        mAssistedType = AGPS_LOCATION_MODE_MSB;
        mNetworkType = AGPS_NETWORK_TYPE_HOME;
        mResetType = AGPS_START_MODE_HOT;
        bundle.putString(STRING_PROVIDER_ID, mAssistedType);
        bundle.putString(STRING_ACCESS_NETWORK, mNetworkType);
        bundle.putString(STRING_AGPS_RESET_TYPE, mResetType);
        SetValue(bundle);
        fillUi(true);
        LocationManager objLocManager = (LocationManager)
                                        getSystemService(Context.LOCATION_SERVICE);
        if ( mResetType.compareTo(AGPS_START_MODE_HOT) == 0 ) {
            bundle.putString(STRING_AGPS_RESET_TYPE, "2");
        } else if ( mResetType.compareTo(AGPS_START_MODE_WARM) == 0 ) {
            bundle.putString(STRING_AGPS_RESET_TYPE, "1");
        } else {
            bundle.putString(STRING_AGPS_RESET_TYPE, "0");
        }
        boolean bRet = objLocManager.sendExtraCommand(LocationManager.GPS_PROVIDER,
                "agps_parms_changed", bundle);
        Log.d(TAG, "sendExtraCommand ret=" + bRet);
    }

    private void SetValue(Bundle bundle)
    {
        String supl_host = bundle.getString(STRING_SUPL_HOST);
        String supl_port = bundle.getString(STRING_SUPL_PORT);
        String agps_provid = bundle.getString(STRING_PROVIDER_ID);
        String agps_network = bundle.getString(STRING_ACCESS_NETWORK);
        String agps_reset_type = bundle.getString(STRING_AGPS_RESET_TYPE);
        if (null != supl_host && supl_host.length() > 0) {
            Settings.Global.putString(mContentResolver, ASSISTED_GPS_SUPL_HOST,
                    supl_host);
        }
        if (null != supl_port) {
            Settings.Global.putString(mContentResolver, ASSISTED_GPS_SUPL_PORT,
                    supl_port);
        }
        if (null != agps_provid && agps_provid.length() > 0) {
            Settings.Global.putString(mContentResolver, ASSISTED_GPS_POSITION_MODE,
                    agps_provid);
        }
        if (null != agps_network && agps_network.length() > 0) {
            Settings.Global.putString(mContentResolver, ASSISTED_GPS_NETWORK,
                    agps_network);
        }
        if (null != agps_reset_type && agps_reset_type.length() > 0) {
            if ( agps_reset_type.compareTo(AGPS_START_MODE_HOT) == 0 ) {
                agps_reset_type = "2";
            } else if ( agps_reset_type.compareTo(AGPS_START_MODE_WARM) == 0 ) {
                agps_reset_type = "1";
            } else {
                agps_reset_type = "0";
            }
            Settings.Global.putString(mContentResolver, ASSISTED_GPS_RESET_TYPE,
                    agps_reset_type);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof ListPreference)
        {
            ListPreference pref = (ListPreference)preference;
            String prefKey = pref.getKey();
            if (prefKey!=null)
            {
                final String value = newValue.toString();
                int type = Integer.valueOf(value);
                String[] types;
                if (prefKey.compareTo("agps_network")==0)
                {
                    types = getResources()
                            .getStringArray(R.array.agps_network_entries);
                    if (type == HOME_MODE) {
                        mNetworkType = AGPS_NETWORK_TYPE_HOME;
                    } else if (type == ALL_MODE) {
                        mNetworkType = AGPS_NETWORK_TYPE_ALL;
                    }
                    if (type == ALL_MODE) {
                        Toast.makeText(AgpsSettings.this, R.string.location_agps_roaming_help,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                else if (prefKey.compareTo("agps_reset_type")==0)
                {
                    types = getResources().getStringArray(
                            R.array.agps_reset_type_entries);
                    if (type == HOT_MODE) {
                        mResetType = AGPS_START_MODE_HOT;
                    } else if (type == WARM_MODE) {
                        mResetType = AGPS_START_MODE_WARM;
                    } else {
                        mResetType = AGPS_START_MODE_COLD;
                    }
                }
                else if (prefKey.compareTo("agps_pref")==0)
                {
                    types = getResources()
                            .getStringArray(R.array.agps_si_mode_entries);
                    if (type == MSB_MODE) {
                        mAssistedType = AGPS_LOCATION_MODE_MSB;
                    } else if (type == MSA_MODE) {
                        mAssistedType = AGPS_LOCATION_MODE_MSA;
                    }
                }
                else {
                    return true;
                }

                pref.setValue(value);
                pref.setSummary(types[type]);
            }
        }
        return true;
    }
}
