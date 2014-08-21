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
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.telephony.MSimTelephonyManager;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.view.View;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;

import java.lang.reflect.InvocationTargetException;

public class Lte4GEnabler {
    private static final String TAG = "Lte4GEnabler";
    private final Context mContext;
    private Switch mSwitch;

    private static MyHandler mHandler;

    private Object mPhoneServiceClient;
    private Object mProxy;

    public Lte4GEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mHandler = new MyHandler();
        loadPhoneServiceBinder();
    }

    public void resume() {
        setSwitchStatus();
        mSwitch.setOnCheckedChangeListener(mLte4GEnabledListener);
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void destroy() {
        if (mPhoneServiceClient != null) {
            invokeMethod("com.qualcomm.qti.phonefeature.PhoneServiceClient",
                    "dispose", mProxy, null, null);
        }
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
                || type == Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA
                || type == Phone.NT_MODE_LTE_GSM_WCDMA
                || type == Phone.NT_MODE_LTE_CDMA_AND_EVDO
                ) {
            isLTEMode = true;
        } else {
            isLTEMode = false;
        }
        mSwitch.setChecked(isLTEMode);
        mSwitch.setEnabled(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 0 && mPhoneServiceClient != null);
    }

    private void promptUser() {
        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(R.string.lte_4g_switch_prompt))
                .setNeutralButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                //Recover the button as disable status
                                mSwitch.setChecked(false);
                            }
                        })
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {
                                setPrefNetwork();
                            }
                        }).create();
        alertDialog.show();
    }


    private boolean isPrefTDDDataOnly(int subscription) {
        try {
            int tddEnabled = MSimTelephonyManager.getIntAtIndex(mContext.getContentResolver(),
                    "tdd_data_only_user_pref", subscription);
            return tddEnabled == 1;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private void setTDDDataOnly() {
        final Message msg = mHandler.obtainMessage(
                MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE);
        msg.replyTo = new Messenger(mHandler);
        setTDDDataOnly(MSimConstants.DEFAULT_SUBSCRIPTION, true, msg);
    }

    private void setPrefNetwork() {
        // Disable it, enable it after getting reponse
        mSwitch.setEnabled(false);
        int networkType = mSwitch.isChecked() ? Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA
                : Phone.NT_MODE_GLOBAL;
        if (isPrefTDDDataOnly(MSimConstants.DEFAULT_SUBSCRIPTION)) {
            if (mSwitch.isChecked()) {
                setTDDDataOnly();
                return;
            } else {
                networkType = Phone.NT_MODE_GLOBAL;
            }
        }

        Messenger msger = new Messenger(mHandler);
        final Message msg = mHandler.obtainMessage(
                MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE);
        msg.replyTo = msger;
        // both dsds and sss use this this interface
        setPrefNetwork(MSimConstants.DEFAULT_SUBSCRIPTION,
                networkType, msg);
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

    private void setTDDDataOnly(int sub, boolean tddOnly, Message callback) {
        invokeMethod("com.qualcomm.qti.phonefeature.IServiceBinder",
                "setTDDDataOnly", mPhoneServiceClient, new Class<?>[] {
                        int.class, boolean.class, Message.class
                }, new Object[] {
                        sub, tddOnly, callback
                });
    }

    private void setPrefNetwork(int sub, int network, Message callback) {
        invokeMethod("com.qualcomm.qti.phonefeature.IServiceBinder",
                "setPreferredNetwork", mPhoneServiceClient, new Class<?>[] {
                        int.class, int.class, Message.class
                }, new Object[] {
                        sub, network, callback
                });
    }

    private void loadPhoneServiceBinder() {
        mProxy = loadClassObj(
                "com.qualcomm.qti.phonefeature.PhoneServiceClient",
                new Class<?>[] {
                        Context.class, Message.class
                },
                new Object[] {
                        mContext,
                        mHandler.obtainMessage(MyHandler.MESSAGE_PHONE_SERVICE_BIND)
                });
    }

    private static Object loadClassObj(String className,
            Class<?>[] paramClasses, Object[] params) {
        Throwable exception = null;
        try {
            Class<?> targetClass = Class.forName(className);
            return targetClass.getDeclaredConstructor(paramClasses)
                    .newInstance(params);
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        } catch (InstantiationException e) {
            exception = e;
        }
        if (exception != null) {
            Log.e(TAG, "failed to load class obj!", exception);
        }
        return null;
    }

    private static Object invokeMethod(String className, String methodName, Object instance,
            Class<?>[] paramClasses, Object[] params) {
        Throwable exception = null;
        try {
            Class<?> targetClass = Class.forName(className);
            return targetClass.getDeclaredMethod(methodName, paramClasses).invoke(instance, params);
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (IllegalArgumentException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        }
        if (exception != null) {
            Log.e(TAG, "failed to invoke method!", exception);
        }
        return null;
    }

    private int getPreferredNetworkType() {
        int settingsNetworkMode = Phone.PREFERRED_NT_MODE;

        try {
            settingsNetworkMode = MSimTelephonyManager.getIntAtIndex(
                    mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE,
                    MSimConstants.DEFAULT_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "getPreferredNetworkType: Could not find PREFERRED_NETWORK_MODE!!!");
        }

        Log.i(TAG, "get preferred network type=" + settingsNetworkMode);
        return settingsNetworkMode;
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;
        static final int MESSAGE_PHONE_SERVICE_BIND = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
                case MESSAGE_PHONE_SERVICE_BIND:
                    mPhoneServiceClient = invokeMethod(
                            "com.qualcomm.qti.phonefeature.PhoneServiceClient",
                            "getServiceBinder", mProxy, null, null);
                    setSwitchStatus();
                    break;
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            int type = getPreferredNetworkType();
            //set it as true after mode processing
            setSwitchStatus();
        }
    }
}
