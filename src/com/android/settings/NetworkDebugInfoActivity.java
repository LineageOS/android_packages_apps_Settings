/*
    Copyright (c) 2014, The Linux Foundation. All rights reserved.

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

import java.nio.BufferUnderflowException;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceActivity;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import com.qualcomm.qcrilhook.IQcRilHook;
import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;

public class NetworkDebugInfoActivity extends PreferenceActivity {

    private static final String TAG = "NetworkDebugInfoActivity";

    private final int QCRILHOOK_EVT_HOOK_GET_CDMA = 16 + this.QCRIL_EVT_OEM_BASE;
    private final int QCRILHOOK_EVT_HOOK_GET_GSM = 34 + this.QCRIL_EVT_OEM_BASE;
    private final int QCRILHOOK_EVT_HOOK_GET_HRPD = 31 + this.QCRIL_EVT_OEM_BASE;
    private final int QCRILHOOK_EVT_HOOK_GET_LTE = 33 + this.QCRIL_EVT_OEM_BASE;
    private final int QCRILHOOK_EVT_HOOK_GET_WCDMA = 32 + this.QCRIL_EVT_OEM_BASE;
    private final int QCRIL_EVT_OEM_BASE = 0x80000;

    private final String KEY_PN = "info_pn";
    private final String KEY_CHANNEL = "info_channel";
    private final String KEY_BAND = "info_band";
    private final String KEY_RSSI = "info_rssi";
    private final String KEY_RX_AGC = "info_rx_agc";
    private final String KEY_RX_DBM = "info_rx_dbm";
    private final String KEY_TX_AGC = "info_tx_agc";
    private final String KEY_TX_DBM = "info_tx_dbm";
    private final String KEY_SID = "info_sid";
    private final String KEY_NID = "info_nid";
    private final String KEY_EC_IO = "info_ec_io";

    private ServiceState mSS = new ServiceState();
    private SignalStrength mSth = new SignalStrength();
    private TelephonyManager mTelephonyManager;
    private ITelephony mPhone;
    private boolean mIsQcRilHookReady = false;
    // Currently in C+G combo, the CDMA sub is always 0
    private static final int CDMA_SUB = 0;
    private static final String MSIM_PHONE_SERVICE = "phone_msim";
    private static final String PHONE_SERVICE = "phone";

    private Context mContext;
    private QcRilHook mQcRilHook = null;

    /**
     * We are listening to this because we want to disable the following option
     * when phone is not in service. - Avoid the current network
     */
    PhoneStateListener mPhoneStateListener = new PhoneStateListener(CDMA_SUB) {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSth = signalStrength;
            // TODO update signal strength.
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {

            mSS = state;
            // TODO update sid nid.
        }
    };

    private QcRilHookCallback mQcrilHookCb = new QcRilHookCallback() {
        public void onQcRilHookReady() {
            mIsQcRilHookReady = true;
            Log.d(TAG, "In Qcril hook cb");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        addPreferencesFromResource(R.xml.network_debug_info);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mPhone = ITelephony.Stub.asInterface(ServiceManager
                    .getService(PHONE_SERVICE));
        if (mPhone == null || mTelephonyManager == null) {
                Log.e(TAG, "Unable to get Phone Services. mPhone = " + mPhone +
                        " mTelephonyManager = " + mTelephonyManager);
                this.finish();
        }
        // register for phone state notifications.
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_CALL_STATE);

        String str = null;
        mQcRilHook = new QcRilHook(mContext, mQcrilHookCb);
        AsyncResult localAsyncResult = this.mQcRilHook
                .sendQcRilHookMsg(this.QCRILHOOK_EVT_HOOK_GET_CDMA);
        if (localAsyncResult.exception != null)
        {
            Log.e(TAG, "QCRILHOOK_EVT_HOOK_GET_CDMA failed w/ "
                    + localAsyncResult.exception);
            return;
        }
        if (localAsyncResult.result == null)
        {
            Log.e(TAG, "QCRILHOOK_EVT_HOOK_GET_CDMA failed w/ null result");
            return;
        }
        try
        {
            byte[] arrayOfByte = (byte[]) (byte[]) localAsyncResult.result;
            Log.d(TAG, "------" + arrayOfByte.toString());
        } catch (BufferUnderflowException localBufferUnderflowException)
        {
            Log.e(TAG,
                    "QCRIL_EVT_HOOK_GET_AVAILABLE_CONFIGS failed to parse payload w/ "
                            + localBufferUnderflowException);
        }
        try {
            byte[] abc = mQcRilHook.qcRilCdmaGetAvoidanceList();
            Log.d(TAG, "------" + abc.toString());
        } catch (Exception ee) {
            Log.e(TAG, "" + ee);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO PN CHANNEL BAND RSSI ECIO RXAGC RXDBM TXAGC TXDBM need support.
        findPreference(KEY_PN).setSummary("1");
        findPreference(KEY_CHANNEL).setSummary("1");
        findPreference(KEY_BAND).setSummary("1");
        int cdmaRssi = mSth.getCdmaDbm();
        int evdoRssi = mSth.getEvdoDbm();
        StringBuilder rssiSB = new StringBuilder();
        rssiSB.append("CDMA: ");
        rssiSB.append(cdmaRssi);
        rssiSB.append("; ");
        rssiSB.append("EVDO: ");
        rssiSB.append(evdoRssi);
        rssiSB.append(".");
        findPreference(KEY_RSSI).setSummary(rssiSB.toString());
        findPreference(KEY_RX_AGC).setSummary("1");
        findPreference(KEY_RX_DBM).setSummary("1");
        findPreference(KEY_TX_AGC).setSummary("1");
        findPreference(KEY_TX_DBM).setSummary("1");
        int systemId = mSS.getSystemId();
        Log.d(TAG, "we can get system id is: " + systemId);
        findPreference(KEY_SID).setSummary(Integer.toString(systemId));
        int nid = mSS.getNetworkId();
        Log.d(TAG, "we can get network id : " + nid);
        findPreference(KEY_NID).setSummary(Integer.toString(nid));
        int evdoEcio = mSth.getEvdoEcio();
        int cdmaEcio = mSth.getCdmaEcio();
        StringBuilder ecioSB = new StringBuilder();
        ecioSB.append("CDMA: ");
        ecioSB.append(cdmaEcio);
        ecioSB.append("; ");
        ecioSB.append("EVDO: ");
        ecioSB.append(evdoEcio);
        ecioSB.append(".");
        findPreference(KEY_EC_IO).setSummary(ecioSB.toString());
    }
}
