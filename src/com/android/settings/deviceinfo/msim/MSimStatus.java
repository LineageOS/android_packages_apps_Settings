/*
 * Copyright (c) 2011-13, The Linux Foundation. All rights reserved
 * Not a Contribution.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.deviceinfo.msim;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.R;
import com.android.settings.SelectSubscription;
import com.codeaurora.telephony.msim.MSimPhoneFactory;
import com.android.settings.Utils;

import org.cyanogenmod.hardware.SerialNumber;

import java.lang.ref.WeakReference;

/**
 * Display the following information
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class MSimStatus extends PreferenceActivity {

    private static final String TAG = "MSimStatus";
    private static final boolean DEBUG = true;
    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String[] PHONE_RELATED_ENTRIES = {
        KEY_DATA_STATE,
        KEY_NETWORK_TYPE
    };

    public static String MULTI_SIM_NAME = "perferred_name_sub";

    private static final int SINGLE_SIM = 1;
    private static final int EVENT_UPDATE_STATS = 500;
    private static final int GSM_SIGNAL_UNKNOWN = 99;
    private static final int GSM_SIGNAL_NULL = -113;
    private static final String BUTTON_SELECT_SUB_KEY = "button_aboutphone_msim_status";

    private MSimTelephonyManager mTelephonyManager;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private PhoneStateListener[] mPhoneStateListener;
    private Resources mRes;
    private Preference mUptime;

    private String mUnknown = null;
    private int mNumPhones = 0;

    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    // private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private int[] mDataState = new int[] {
            TelephonyManager.DATA_DISCONNECTED, TelephonyManager.DATA_DISCONNECTED
    };

    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_ESN_NUMBER = "esn_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";

    private static final String[] RELATED_ENTRIES = {
            KEY_SERVICE_STATE,
            KEY_OPERATOR_NAME,
            KEY_ROAMING_STATE,
            KEY_PHONE_NUMBER,
            KEY_IMEI,
            KEY_IMEI_SV,
            KEY_ICC_ID,
            KEY_PRL_VERSION,
            KEY_MIN_NUMBER,
            KEY_ESN_NUMBER,
            KEY_MEID_NUMBER,
            KEY_SIGNAL_STRENGTH,
            KEY_BASEBAND_VERSION
    };

    private String[] mEsnNumberSummary;
    private String[] mMeidNumberSummary;
    private String[] mMinNumberSummary;
    private String[] mPrlVersionSummary;
    private String[] mImeiSVSummary;
    private String[] mImeiSummary;
    private String[] mIccIdSummary;
    private String[] mNumberSummary;
    private String[] mServiceStateSummary;
    private String[] mRoamingStateSummary;
    private String[] mOperatorNameSummary;
    private String[] mSigStrengthSummary;
    private String[] mDataStateSummary;

    private SignalStrength[] mSignalStrength;
    private ServiceState[] mServiceState;

    private Phone[] mPhone;

    private String[] mSim;

    private String[] mNetworkSummary;

    private void initMSimSummary(String[] str) {
        for (int i = 0; i < mNumPhones; i++) {
            if (str[i] == null) {
                str[i] = mSim[i] + ": " + mUnknown;
            }
        }
    }

    private String getSimSummary(int subscription, String msg) {
        //The msg get from SystemProperties.get() my be "unknown"
        if ((msg == null) || msg.equalsIgnoreCase("unknown")) {
            msg = mUnknown;
        }
        return mSim[subscription] + ": " + msg;
    }

    private void setMSimSummary(String key, String... msgs) {
        if (mNumPhones == SINGLE_SIM) {
            if (msgs[0] == null)
                removePreferenceFromScreen(key);
            else
                setSummaryText(key, msgs[0]);
        } else {
            if (msgs[MSimConstants.SUB1] == null && msgs[MSimConstants.SUB2] == null)
                removePreferenceFromScreen(key);
            else {
                StringBuffer summery = new StringBuffer();
                if (msgs[MSimConstants.SUB1] != null)
                    summery.append(msgs[MSimConstants.SUB1]);
                if (msgs[MSimConstants.SUB2] != null) {
                    if (summery.length() > 0) {
                        summery.append("\n");
                    }
                    summery.append(msgs[MSimConstants.SUB2]);
                }
                setSummaryText(key, summery.toString());
            }
        }
    }

    private String getMultiSimName(int subscription) {
        return Settings.System.getString(getContentResolver(),
                MULTI_SIM_NAME + subscription);
    }

    private Handler mHandler;

    private static class MyHandler extends Handler {
        private WeakReference<MSimStatus> mStatus;

        public MyHandler(MSimStatus activity) {
            mStatus = new WeakReference<MSimStatus>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MSimStatus status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new MyHandler(this);

        mTelephonyManager = (MSimTelephonyManager)getSystemService(MSIM_TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_msim_status);

        mNumPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        mPhoneStateListener = new PhoneStateListener[mNumPhones];
        mEsnNumberSummary = new String[mNumPhones];
        mMeidNumberSummary = new String[mNumPhones];
        mMinNumberSummary = new String[mNumPhones];
        mPrlVersionSummary = new String[mNumPhones];
        mImeiSVSummary = new String[mNumPhones];
        mImeiSummary = new String[mNumPhones];
        mIccIdSummary = new String[mNumPhones];
        mNumberSummary = new String[mNumPhones];
        mServiceStateSummary = new String[mNumPhones];
        mRoamingStateSummary = new String[mNumPhones];
        mOperatorNameSummary = new String[mNumPhones];
        mSigStrengthSummary = new String[mNumPhones];
        mDataStateSummary = new String[mNumPhones];

        mSignalStrength = new SignalStrength[mNumPhones];
        mServiceState = new ServiceState[mNumPhones];

        mPhone = new Phone[mNumPhones];

        mSim = new String[mNumPhones];

        mNetworkSummary = new String[mNumPhones];

        int indexOfCDMA = -1;

        for (int i = 0; i < mNumPhones; i++) {
            mSim[i] = getMultiSimName(i);
            mPhone[i] = MSimPhoneFactory.getPhone(i);
            if ("CDMA".equals(mPhone[i].getPhoneName()))
                indexOfCDMA = i;
            mPhoneStateListener[i] = getPhoneStateListener(i);
        }

        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);

        PreferenceScreen selectSub = (PreferenceScreen) findPreference(BUTTON_SELECT_SUB_KEY);
        if (selectSub != null) {
            Intent intent = selectSub.getIntent();
            intent.putExtra(SelectSubscription.PACKAGE, getPackageName());
            intent.putExtra(SelectSubscription.TARGET_CLASS,
                    MSimSubscriptionStatus.class.getName());
        }

        mRes = getResources();
        // Real-time update language for icon.
        mUnknown = mRes.getString(R.string.device_info_default);

        mUptime = findPreference("up_time");

        if (Utils.isWifiOnly(getApplicationContext())) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        }

        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();

        String serial = getSerialNumber();
        if (serial != null && !serial.equals("")) {
            setSummaryText(KEY_SERIAL_NUMBER, serial);
        } else {
            removePreferenceFromScreen(KEY_SERIAL_NUMBER);
        }

        initMSimSummary(mEsnNumberSummary);
        initMSimSummary(mMeidNumberSummary);
        initMSimSummary(mMinNumberSummary);
        initMSimSummary(mPrlVersionSummary);
        initMSimSummary(mImeiSVSummary);
        initMSimSummary(mImeiSummary);
        initMSimSummary(mIccIdSummary);
        initMSimSummary(mNumberSummary);
        initMSimSummary(mServiceStateSummary);
        initMSimSummary(mRoamingStateSummary);
        initMSimSummary(mOperatorNameSummary);
        initMSimSummary(mSigStrengthSummary);
        initMSimSummary(mDataStateSummary);
        initMSimSummary(mNetworkSummary);

        updateMSimSummery(indexOfCDMA);
    }

    private void updateMSimSummery(int indexOfCDMA) {
        if (DEBUG)
            Log.d(TAG, "cdma index is " + indexOfCDMA);

        if (Utils.isWifiOnly(getApplicationContext())) {
            for (String key : RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        } else {
            for (int i = 0; i < mNumPhones; i++) {

                String rawNumber = mPhone[i].getLine1Number(); // may be null or empty
                String formattedNumber = null;
                if (!TextUtils.isEmpty(rawNumber)) {
                    formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
                }
                mNumberSummary[i] = getSimSummary(i, formattedNumber);

                if (i == indexOfCDMA) {
                    mPrlVersionSummary[i] = getSimSummary(i, mPhone[i].getCdmaPrlVersion());
                    mEsnNumberSummary[i] = getSimSummary(i, mPhone[i].getEsn());
                    mMeidNumberSummary[i] = getSimSummary(i, mPhone[i].getMeid());
                    mMinNumberSummary[i] = getSimSummary(i, mPhone[i].getCdmaMin());

                    if (getResources().getBoolean(R.bool.config_msid_enable)) {
                        findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                    }

                    mImeiSVSummary[i] = null;

                    if (mPhone[i].getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                        // Show ICC ID and IMEI for LTE device
                        mIccIdSummary[i] = getSimSummary(i, mPhone[i].getIccSerialNumber());
                        mImeiSummary[i] = getSimSummary(i, mPhone[i].getImei());
                    } else {
                        // device is not GSM/UMTS, do not display GSM/UMTS
                        // features
                        // check Null in case no specified preference in overlay
                        // xml
                        mIccIdSummary[i] = null;
                        mImeiSummary[i] = null;
                    }

                } else {
                    mPrlVersionSummary[i] = null;
                    mEsnNumberSummary[i] = null;
                    mMeidNumberSummary[i] = null;
                    mMinNumberSummary[i] = null;
                    mIccIdSummary[i] = null;
                    mImeiSummary[i] = getSimSummary(i, mPhone[i].getDeviceId());
                    mImeiSVSummary[i] = getSimSummary(i, mPhone[i].getDeviceSvn());
                }
            }
            setMSimSummary(KEY_PRL_VERSION, mPrlVersionSummary);
            setMSimSummary(KEY_ESN_NUMBER, mEsnNumberSummary);
            setMSimSummary(KEY_MEID_NUMBER, mMeidNumberSummary);
            setMSimSummary(KEY_MIN_NUMBER, mMinNumberSummary);
            setMSimSummary(KEY_ICC_ID, mIccIdSummary);
            setMSimSummary(KEY_IMEI, mImeiSummary);
            setMSimSummary(KEY_IMEI_SV, mImeiSVSummary);
            setMSimSummary(KEY_PHONE_NUMBER, mNumberSummary);

            //baseband is not related to DSDS, one phone has one base band.
            String basebandVersionSummery =
                MSimTelephonyManager.getTelephonyProperty("gsm.version.baseband",
                        MSimTelephonyManager.getDefault().getDefaultSubscription(), null);
            setSummaryText(KEY_BASEBAND_VERSION,basebandVersionSummery);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            for (int i = 0; i < mNumPhones; i++) {
                mTelephonyManager.listen(mPhoneStateListener[i],
                        PhoneStateListener.LISTEN_SERVICE_STATE
                                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
                updateSignalStrength(i);
                updateServiceState(i);
                updateDataState(i);
                updateNetworkType(i);
            }
        }
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            for (int i=0; i < mNumPhones; i++) {
                mTelephonyManager.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
            }
        }
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    private PhoneStateListener getPhoneStateListener(final int subscription) {
        PhoneStateListener phoneStateListener = new PhoneStateListener(subscription) {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                mSignalStrength[subscription] = signalStrength;
                updateSignalStrength(subscription);
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                mServiceState[subscription] = state;
                updateServiceState(subscription);
                updateNetworkType(subscription);
            }
            @Override
            public void onDataConnectionStateChanged(int state) {
                mDataState[subscription] = state;
                updateDataState(subscription);
                updateNetworkType(subscription);
            }
        };
        return phoneStateListener;
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, alt));
        } catch (RuntimeException e) {

        }
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
               text = mUnknown;
            }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    private void updateServiceState(int subscription) {
        String display = mRes.getString(R.string.radioInfo_unknown);
        if (mServiceState[subscription] != null) {
            int state = mServiceState[subscription].getState();

            switch (state) {
                case ServiceState.STATE_IN_SERVICE:
                    display = mRes.getString(R.string.radioInfo_service_in);
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    display = mRes.getString(R.string.radioInfo_service_out);
                    break;
                case ServiceState.STATE_POWER_OFF:
                    display = mRes.getString(R.string.radioInfo_service_off);
                    break;
            }

            mServiceStateSummary[subscription] = getSimSummary(subscription, display);
            setMSimSummary(KEY_SERVICE_STATE, mServiceStateSummary);

            if (mServiceState[subscription].getRoaming()) {
                mRoamingStateSummary[subscription] = getSimSummary(subscription,
                        mRes.getString(R.string.radioInfo_roaming_in));
            } else {
                mRoamingStateSummary[subscription] = getSimSummary(subscription,
                        mRes.getString(R.string.radioInfo_roaming_not));
            }
            setMSimSummary(KEY_ROAMING_STATE, mRoamingStateSummary);

            String operatorName = null;
            if (/*FeatureQuery.FEATURE_SHOW_CARRIER_BY_MCCMNC*/false) {
                String spn = mTelephonyManager.getDefault().getNetworkOperator(subscription);
                operatorName = spn;
            } else {
                operatorName = mServiceState[subscription].getOperatorAlphaLong();
                // parse the string to current language string in public resources
                if (operatorName != null) {
                    operatorName = android.util.NativeTextHelper.getInternalLocalString(this,
                            operatorName,
                            R.array.origin_carrier_names,
                            R.array.locale_carrier_names);
                }
            }
            mOperatorNameSummary[subscription] = getSimSummary(subscription, operatorName);
            setMSimSummary(KEY_OPERATOR_NAME, mOperatorNameSummary);
        }
    }

    void updateSignalStrength(int subscription) {
        // not loaded in some versions of the code (e.g., zaku)
        int signalDbm = 0;

        if (mSignalStrength[subscription] != null) {
            int state = mServiceState[subscription].getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSigStrengthSummary[subscription] = getSimSummary(subscription, "0");
            } else {
                if (!mSignalStrength[subscription].isGsm()) {
                    signalDbm = mSignalStrength[subscription].getCdmaDbm();
                } else {
                    int gsmSignalStrength = mSignalStrength[subscription].getGsmSignalStrength();
                    int asu = (gsmSignalStrength == GSM_SIGNAL_UNKNOWN ? -1 : gsmSignalStrength);
                    if (asu != -1) {
                        signalDbm = GSM_SIGNAL_NULL + 2 * asu;
                    }
                }
                if (-1 == signalDbm)
                    signalDbm = 0;

                int signalAsu = mSignalStrength[subscription].getGsmSignalStrength();
                if (-1 == signalAsu)
                    signalAsu = 0;

                mSigStrengthSummary[subscription] = getSimSummary(subscription,
                        String.valueOf(signalDbm) + " "
                                + r.getString(R.string.radioInfo_display_dbm) + "   "
                                + String.valueOf(signalAsu) + " "
                                + r.getString(R.string.radioInfo_display_asu));
            }
            setMSimSummary(KEY_SIGNAL_STRENGTH, mSigStrengthSummary);
        }
    }

    private void updateNetworkType(int subscription) {
        // Whether EDGE, UMTS, etc...
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN !=
                mTelephonyManager.getNetworkType(subscription)) {
            mNetworkSummary[subscription] = getSimSummary(subscription,
                    mTelephonyManager.getNetworkTypeName(subscription));
        }
        setMSimSummary(KEY_NETWORK_TYPE,mNetworkSummary);
    }

    private void updateDataState(int subscription) {
        String display = null;
        if (MSimPhoneFactory.getDataSubscription() == subscription
                && isDataServiceEnable(subscription)) {
            switch (mDataState[subscription]) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }
        } else {
            display = mRes.getString(R.string.radioInfo_data_disconnected);
        }

        mDataStateSummary[subscription] = getSimSummary(subscription, display);
        setMSimSummary(KEY_DATA_STATE, mDataStateSummary);
    }

    private boolean isDataServiceEnable(int subscription) {
        if (mServiceState[subscription] != null &&
            mServiceState[subscription].getState() == ServiceState.STATE_IN_SERVICE) {
            ConnectivityManager connMgr =
                   (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if (connMgr != null && connMgr.getMobileDataEnabled()) {
                return true;
            }
        }

        return false;
    }

    private void setWimaxStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

        if (ni == null) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_MAC_ADDRESS);
            if (ps != null) root.removePreference(ps);
        } else {
            Preference wimaxMacAddressPref = findPreference(KEY_WIMAX_MAC_ADDRESS);
            String macAddress = SystemProperties.get("net.wimax.mac.address",
                    getString(R.string.status_unavailable));
            wimaxMacAddressPref.setSummary(macAddress);
        }
    }
    private void setWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_WIFI_MAC_ADDRESS);

        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getString(R.string.status_unavailable));
    }

    private void setIpAddressStatus() {
        Preference ipAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = Utils.getDefaultIpAddresses(this);
        if (ipAddress != null) {
            ipAddressPref.setSummary(ipAddress);
        } else {
            ipAddressPref.setSummary(getString(R.string.status_unavailable));
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        Preference btAddressPref = findPreference(KEY_BT_ADDRESS);

        if (bluetooth == null) {
            // device not BT capable
            getPreferenceScreen().removePreference(btAddressPref);
        } else {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            btAddressPref.setSummary(!TextUtils.isEmpty(address) ? address
                    : getString(R.string.status_unavailable));
        }
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }

    private String getSerialNumber() {
        try {
            if (SerialNumber.isSupported()) {
                return SerialNumber.getSerialNumber();
            }
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed; fall through
        }

        return Build.SERIAL;
    }
}
