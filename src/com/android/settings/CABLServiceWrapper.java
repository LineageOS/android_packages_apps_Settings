/*
 * Copyright (c) 2014, The Linux Foundation. All Rights Reserved.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import com.android.cabl.ICABLService;
import android.os.SystemProperties;

public class CABLServiceWrapper {
    protected Context mContext;
    protected CABLServiceConnection mCABLServiceConn;

    private static final String CABL_SERVICE_INTENT = "org.codeaurora.cabl.ICABLService";

    private static boolean mCablAvailable;
    /**
    * Int value to specify if CABL is enabled.
    * 0 = Disable  1 = Enable
    */
    public static final String CABL_ENABLED = "cabl_enabled";

    private static ICABLService mCABLService = null;

    private static final int CABL_CON_TYPE_DISABLE = 0;
    private static final int CABL_CON_TYPE_ENABLE = 1;

    protected static final String TAG = "CABLServiceWrapper";
    private static boolean sCABLEnabled = true;

    CABLServiceWrapper(Context context) {
        mContext = context;
    }

    public void initCABLService() {
        Log.d(TAG, "initCABLService");
        mCablAvailable = SystemProperties.getBoolean("ro.qualcomm.cabl", false);
        mCABLServiceConn = new CABLServiceConnection();
        Intent i = new Intent(CABL_SERVICE_INTENT);
        boolean ret = mContext.bindService(i, mCABLServiceConn, Context.BIND_AUTO_CREATE);
    }

    public boolean isCABLAvailable() {
        boolean available = false;
        available = mContext.getResources().getBoolean(R.bool.cabl_enabled);
        return available;
    }

    public void startCABL() {
        if (mCablAvailable) {
            if (null != mCABLService) {
                final ContentResolver resolver = mContext.getContentResolver();
                // Create a new thread execution
                // mCABLService.startCABL().
                // sleep will execution in the this thread.dosen't
                // Obstruction main thread.
                new Thread() {
                    public void run() {
                        try {
                            Log.d(TAG, "startCABL");
                            boolean result = mCABLService.control(CABL_CON_TYPE_ENABLE);
                            // 0-disable cabl 1-enable cabl
                            Settings.Global.putInt(resolver,
                                    CABL_ENABLED,
                                    result ? 1 : 0);
                        } catch (RemoteException e) {
                            Log.e(TAG, "startCABL, exception");
                        }
                    }
                }.start();
            }
        }
    }

    public void stopCABL() {
        if (mCablAvailable
                && SystemProperties.get("init.svc.ppd").equals(
                        "running")) {
            if (null != mCABLService) {
                final ContentResolver resolver = mContext.getContentResolver();
                // Create a new thread execution
                // mCABLService.stopCABL().
                // sleep will execution in the this thread.dosen't
                // Obstruction main thread.
                new Thread() {
                    public void run() {
                        try {
                            Log.d(TAG, "stopCABL");
                            boolean result = mCABLService.control(CABL_CON_TYPE_DISABLE);
                            // 0-disable cabl 1-enable cabl
                            Settings.Global.putInt(resolver,
                                    CABL_ENABLED,
                                    result ? 0 : 1);
                        } catch (RemoteException e) {
                            Log.e(TAG, "stopCABL, exception");
                        }
                    }
                }.start();
            }
        }
    }

    private class CABLServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCABLService = ICABLService.Stub.asInterface((IBinder) service);
            Log.d(TAG, "onServiceConnected, service=" + mCABLService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCABLService = null;
        }

    }

    public void onDestory() {
        mContext.unbindService(mCABLServiceConn);
    }
}
