/*
   Copyright (c) 2014, The Linux Foundation. All Rights Reserved.
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.view.View;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

public class Lte4GEnabler {
    private static final String TAG = "Lte4GEnabler";
    private final Context mContext;
    private Switch mSwitch;
    private boolean mDialogClicked = false;
    private static MyHandler mHandler;

    public static final String SETTING_PRE_NW_MODE_DEFAULT = "preferred_network_mode_default";
    public static final String SETTING_PREF_NETWORK_BAND = "network_band_preferred";

    private static final int LTE_FULL = 1;
    private static final int LTE_TDD = 2;

    private static final Uri URI_PHONE_FEATURE = Uri
            .parse("content://com.qualcomm.qti.phonefeature.FEATURE_PROVIDER");

    public Lte4GEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mHandler = new MyHandler();
    }

    public void resume() {
        setSwitchStatus();
        mSwitch.setOnCheckedChangeListener(mLte4GEnabledListener);
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_)
            return;
        mSwitch = switch_;
        setSwitchStatus();
        mSwitch.setOnCheckedChangeListener(mLte4GEnabledListener);
    }

    // Adjust the switch component's availability
    // according to the "AirPlane" mode.
    private void setSwitchStatus() {
        boolean isLTEMode = false;
        int type = getPreferredNetworkType();
        if (type == Phone.NT_MODE_TD_SCDMA_LTE_CDMA_EVDO_GSM_WCDMA
                || type == Phone.NT_MODE_TD_SCDMA_GSM_WCDMA_LTE
                || type == Phone.NT_MODE_TD_SCDMA_WCDMA_LTE
                || type == Phone.NT_MODE_TD_SCDMA_GSM_LTE
                || type == Phone.NT_MODE_TD_SCDMA_LTE
                || type == Phone.NT_MODE_LTE_WCDMA
                || type == Phone.NT_MODE_LTE_ONLY
                || type == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                || type == Phone.NT_MODE_LTE_GSM_WCDMA
                || type == Phone.NT_MODE_LTE_CDMA_AND_EVDO
                || type == Phone.NT_MODE_LTE_CDMA_EVDO_GSM
                ) {
            isLTEMode = true;
        } else {
            isLTEMode = false;
        }

        mSwitch.setChecked(isLTEMode);
        int simState = TelephonyManager.getDefault().getSimState(PhoneConstants.SUB1);
        mSwitch.setEnabled((Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 0)
                && (simState == TelephonyManager.SIM_STATE_READY));
    }

    private void promptUser() {
        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(R.string.lte_4g_switch_prompt))
                .setNeutralButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                //dismiss dialog set switch to disable status
                            }
                        })
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                setPrefNetwork();
                                mDialogClicked = true;
                            }
                        }).create();
        alertDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (!mDialogClicked)
                            mSwitch.setChecked(false);
                    }
                });
        alertDialog.show();
    }


    private boolean isPrefTDDDataOnly(int subscription) {
        try {
            int tddEnabled = TelephonyManager.getIntAtIndex(mContext.getContentResolver(),
                    SETTING_PREF_NETWORK_BAND, subscription);
            return tddEnabled == LTE_TDD;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private OnCheckedChangeListener mLte4GEnabledListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
            // TODO Auto-generated method stub
          //Only prompt user when enable 4G
            if (mSwitch.isChecked()){
                promptUser();
            } else {
                setPrefNetwork();
            }
        }
    };

    private void setPrefNetwork() {
        // Disable it, enable it after getting reponse
        mSwitch.setEnabled(false);
        int networkType = mSwitch.isChecked() ? Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                : Phone.NT_MODE_GLOBAL;

        if (mSwitch.isChecked() && isPrefTDDDataOnly(PhoneConstants.SUB1)){
            TelephonyManager.putIntAtIndex(mContext.getContentResolver(),
                    SETTING_PRE_NW_MODE_DEFAULT, PhoneConstants.SUB1, networkType);

            setPrefNetwork(PhoneConstants.SUB1, Phone.NT_MODE_LTE_ONLY, LTE_TDD);
        } else {
            // both dsds and sss use this this interface
            setPrefNetwork(PhoneConstants.SUB1, networkType);
        }
    }

    private void setPrefNetwork(int sub, int network) {
        Messenger msger = new Messenger(mHandler);
        final Message msg = mHandler.obtainMessage(
                MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE);
        msg.replyTo = msger;

        Bundle extras = new Bundle();
        extras.putInt(PhoneConstants.SLOT_KEY, sub);
        extras.putInt("network", network);
        extras.putParcelable("callback", msg);
        callBinder("set_pref_network", extras);
    }

    public void setPrefNetwork(int sub, int network, int band) {
        final Message msg = mHandler.obtainMessage(
                     MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE);
        if (msg != null) {
            msg.replyTo = new Messenger(msg.getTarget());
        }

        Bundle params = new Bundle();
        params.putInt(PhoneConstants.SLOT_KEY, sub);
        params.putInt("network", network);
        params.putInt("band", band);
        params.putParcelable("callback", msg);
        callBinder("set_pref_network", params);
    }

    private Bundle callBinder(String method, Bundle extras) {
        if (mContext.getContentResolver().acquireProvider(URI_PHONE_FEATURE) == null) {
            return null;
        }
        return mContext.getContentResolver().call(URI_PHONE_FEATURE, method, null, extras);
    }

    private int getPreferredNetworkType() {
        int settingsNetworkMode = Phone.PREFERRED_NT_MODE;

        try {
            settingsNetworkMode = TelephonyManager.getIntAtIndex(
                    mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE,
                    PhoneConstants.SUB1);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "getPreferredNetworkType: Could not find PREFERRED_NETWORK_MODE!!!");
        }

        Log.i(TAG, "get preferred network type=" + settingsNetworkMode);
        return settingsNetworkMode;
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            int type = getPreferredNetworkType();
            //set it as true after mode processing
            mDialogClicked = false;
            setSwitchStatus();
        }
    }
}
