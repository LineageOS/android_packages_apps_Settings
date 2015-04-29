/*
 * Copyright (c) 2011-2014, The Linux Foundation. All rights reserved
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

package com.android.settings.deviceinfo;

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
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.R;
import com.android.settings.MultiSimSettingTab;
import com.android.settings.SelectSubscription;
import com.android.settings.Utils;

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

    private TelephonyManager mTelephonyManager;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private PhoneStateListener[] mPhoneStateListener;
    private Resources mRes;
    private Preference mUptime;
    private boolean mShowLatestAreaInfo = false;

    private String mUnknown = null;
    private int mNumPhones = 0;

    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    // private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private int[] mDataState = new int[] {
            TelephonyManager.DATA_DISCONNECTED, TelephonyManager.DATA_DISCONNECTED
    };

    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
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
            KEY_LATEST_AREA_INFO,
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

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

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
    private String[] mLatestAreaInfoSummary;

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

    private String getSimSummary(int phoneId, String msg) {
        //The msg get from SystemProperties.get() my be "unknown"
        if ((msg == null) || msg.equalsIgnoreCase("unknown")) {
            msg = mUnknown;
        }
        return mSim[phoneId] + ": " + msg;
    }

    private void setMSimSummary(String key, String... msgs) {
        if (mNumPhones == SINGLE_SIM) {
            if (msgs[0] == null)
                removePreferenceFromScreen(key);
            else
                setSummaryText(key, msgs[0]);
        } else {
            if (msgs[PhoneConstants.SUB1] == null && msgs[PhoneConstants.SUB2] == null)
                removePreferenceFromScreen(key);
            else {
                StringBuffer summery = new StringBuffer();
                if (msgs[PhoneConstants.SUB1] != null)
                    summery.append(msgs[PhoneConstants.SUB1]);
                if (msgs[PhoneConstants.SUB2] != null) {
                    if (summery.length() > 0) {
                        summery.append("\n");
                    }
                    summery.append(msgs[PhoneConstants.SUB2]);
                }
                setSummaryText(key, summery.toString());
            }
        }
    }

    private String getMultiSimName(int phoneId) {
        String name = MultiSimSettingTab.getMultiSimName(this, phoneId);
        if (name != null) {
            return name;
        } else {
            return getResources().getString(R.string.sim_card_number_title, phoneId + 1);
        }
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

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_msim_status);

        mNumPhones = TelephonyManager.getDefault().getPhoneCount();
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
        mLatestAreaInfoSummary = new String[mNumPhones];

        mSignalStrength = new SignalStrength[mNumPhones];
        mServiceState = new ServiceState[mNumPhones];

        mPhone = new Phone[mNumPhones];

        mSim = new String[mNumPhones];

        mNetworkSummary = new String[mNumPhones];

        int indexOfCDMA = -1;

        for (int i = 0; i < mNumPhones; i++) {
            mSim[i] = getMultiSimName(i);
            mPhone[i] = PhoneFactory.getPhone(i);
            if ("CDMA".equals(mPhone[i].getPhoneName())) {
                indexOfCDMA = i;
            } else {
                // only show area info when SIM country is Brazil
                if ("br".equals(mTelephonyManager.getSimCountryIso(0))) {
                    mShowLatestAreaInfo = true;
                }
            }
            mPhoneStateListener[i] = getPhoneStateListener(i);
        }
        if (!mShowLatestAreaInfo) {
            removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
        }

        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);

        PreferenceScreen selectSub = (PreferenceScreen) findPreference(BUTTON_SELECT_SUB_KEY);
        if (selectSub != null) {
            Intent intent = selectSub.getIntent();
            intent.putExtra(SelectSubscription.PACKAGE, "com.android.settings");
            intent.putExtra(SelectSubscription.TARGET_CLASS,
                    "com.android.settings.deviceinfo.MSimSubscriptionStatus");
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

        String serial = Build.SERIAL;
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
        initMSimSummary(mLatestAreaInfoSummary);

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
                    } else {
                        // device is not GSM/UMTS, do not display GSM/UMTS
                        // features
                        // check Null in case no specified preference in overlay
                        // xml
                        mIccIdSummary[i] = null;
                    }
                    // For cdma, do not display IMEI.
                    mImeiSummary[i] = null;

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
            setMSimSummary(KEY_LATEST_AREA_INFO, mLatestAreaInfoSummary);

            //baseband is not related to DSDS, one phone has one base band.
            String basebandVersionSummery =
                TelephonyManager.getTelephonyProperty(PhoneFactory.getDefaultSubscription(),
                        "gsm.version.baseband", null);
            setSummaryText(KEY_BASEBAND_VERSION,basebandVersionSummery);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                CB_AREA_INFO_SENDER_PERMISSION, null);
        if (!Utils.isWifiOnly(getApplicationContext())) {
            for (int i = 0; i < mNumPhones; i++) {
                mTelephonyManager.listen(mPhoneStateListener[i],
                        PhoneStateListener.LISTEN_SERVICE_STATE
                                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

                // Ask CellBroadcastReceiver to broadcast the latest area
                // info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                getLatestIntent.putExtra(PhoneConstants.PHONE_KEY, i);
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);

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
        unregisterReceiver(mAreaInfoReceiver);
    }

    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null && cbMessage.getServiceCategory() == 50) {
                    int subId = cbMessage.getSubId();
                    int phoneId = SubscriptionManager.getSlotId(subId);
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo, phoneId);
                }
            }
        }
    };

    private PhoneStateListener getPhoneStateListener(final int phoneId) {
        int subId = SubscriptionManager.getSubId(phoneId)[0];
        PhoneStateListener phoneStateListener = new PhoneStateListener(subId) {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                mSignalStrength[phoneId] = signalStrength;
                updateSignalStrength(phoneId);
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                mServiceState[phoneId] = state;
                updateServiceState(phoneId);
                updateNetworkType(phoneId);
            }
            @Override
            public void onDataConnectionStateChanged(int state) {
                mDataState[phoneId] = state;
                updateDataState(phoneId);
                updateNetworkType(phoneId);
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

    private void updateServiceState(int phoneId) {
        String display = mRes.getString(R.string.radioInfo_unknown);
        if (mServiceState[phoneId] != null) {
            int state = mServiceState[phoneId].getState();

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

            mServiceStateSummary[phoneId] = getSimSummary(phoneId, display);
            setMSimSummary(KEY_SERVICE_STATE, mServiceStateSummary);

            if (mServiceState[phoneId].getRoaming()) {
                mRoamingStateSummary[phoneId] = getSimSummary(phoneId,
                        mRes.getString(R.string.radioInfo_roaming_in));
            } else {
                mRoamingStateSummary[phoneId] = getSimSummary(phoneId,
                        mRes.getString(R.string.radioInfo_roaming_not));
            }
            setMSimSummary(KEY_ROAMING_STATE, mRoamingStateSummary);

            String operatorName = null;
            if (/*FeatureQuery.FEATURE_SHOW_CARRIER_BY_MCCMNC*/false) {
                String spn = mTelephonyManager.getDefault().getNetworkOperatorForPhone(phoneId);
                operatorName = spn;
            } else {
                operatorName = mServiceState[phoneId].getOperatorAlphaLong();
                // parse the string to current language string in public resources
                if (operatorName != null) {
                    operatorName = android.util.NativeTextHelper.getInternalLocalString(this,
                            operatorName,
                            R.array.origin_carrier_names,
                            R.array.locale_carrier_names);
                }
            }
            mOperatorNameSummary[phoneId] = getSimSummary(phoneId, operatorName);
            setMSimSummary(KEY_OPERATOR_NAME, mOperatorNameSummary);
        }
    }

    private void updateAreaInfo(String areaInfo, int phoneId) {
        if (DEBUG)
            Log.i(TAG, "updateAreaInfo areaInfo=" + areaInfo + " sub=" + phoneId);
        if (areaInfo != null) {
            mLatestAreaInfoSummary[phoneId] = getSimSummary(phoneId, areaInfo);
            setMSimSummary(KEY_LATEST_AREA_INFO, mLatestAreaInfoSummary);
        }
    }

    void updateSignalStrength(int phoneId) {
        // not loaded in some versions of the code (e.g., zaku)

        if (mSignalStrength[phoneId] != null) {
            int state = mServiceState[phoneId].getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSigStrengthSummary[phoneId] = getSimSummary(phoneId, "0");
            } else {
                int signalDbm = mSignalStrength[phoneId].getDbm();
                if (-1 == signalDbm) signalDbm = 0;

                int signalAsu = mSignalStrength[phoneId].getAsuLevel();
                if (-1 == signalAsu)
                    signalAsu = 0;

                mSigStrengthSummary[phoneId] = getSimSummary(phoneId,
                        String.valueOf(signalDbm) + " "
                                + r.getString(R.string.radioInfo_display_dbm) + "   "
                                + String.valueOf(signalAsu) + " "
                                + r.getString(R.string.radioInfo_display_asu));
            }
            setMSimSummary(KEY_SIGNAL_STRENGTH, mSigStrengthSummary);
        }
    }

    private void updateNetworkType(int phoneId) {
        // Whether EDGE, UMTS, etc...
        int[] subId = SubscriptionManager.getSubId(phoneId);
        int netwokType = mTelephonyManager.getNetworkType(subId[0]);
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != netwokType) {
            mNetworkSummary[phoneId] = getSimSummary(phoneId,
                    mTelephonyManager.getNetworkTypeName(netwokType));
        }
        setMSimSummary(KEY_NETWORK_TYPE,mNetworkSummary);
    }

    private void updateDataState(int phoneId) {
        String display = null;
        if (PhoneFactory.getDataSubscription() == SubscriptionManager.getSubId(phoneId)[0]
                && isDataServiceEnable(phoneId)) {
            switch (mDataState[phoneId]) {
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

        mDataStateSummary[phoneId] = getSimSummary(phoneId, display);
        setMSimSummary(KEY_DATA_STATE, mDataStateSummary);
    }

    private boolean isDataServiceEnable(int phoneId) {
        if (mServiceState[phoneId] != null &&
                mServiceState[phoneId].getState() == ServiceState.STATE_IN_SERVICE) {
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
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Preference ipAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = Utils.getDefaultIpAddresses(cm);
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
}
