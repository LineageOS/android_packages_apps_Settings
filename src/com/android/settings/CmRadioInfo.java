/*
 *     Copyright (C) 2016 The CyanogenMod Project
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.app.QueuedWork;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class CmRadioInfo extends Activity {
    private final String TAG = "CmRadioInfo";

    private TextView tbd1;
    private TextView tbd2;
    private TextView tbd3;

    private Button mbnAutoLoadButton;
    private Button volteAvailOvrButton;
    private Button vtAvailOvrButton;
    private Button wfcAvailOvrButton;

    private Button btnTbd1;

    private Spinner spinTbd1;
    private Button btnTbd2;

    private TelephonyManager mTelephonyManager;
    private Phone phone = null;

    static final String PROPERTY_SW_MBN_UPDATE = "persist.radio.sw_mbn_update";
    static final String PROPERTY_SW_MBN_VOLTE = "persist.radio.sw_mbn_volte";
    static final String PROPERTY_VOLTE_AVAIL_OVR = "persist.dbg.volte_avail_ovr";
    static final String PROPERTY_VT_AVAIL_OVR = "persist.dbg.vt_avail_ovr";
    static final String PROPERTY_DATA_IWLAN_ENABLE = "persist.data.iwlan.enable";
    static final String PROPERTY_WFC_AVAIL_OVR = "persist.dbg.wfc_avail_ovr";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.cm_radio_info);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        phone = PhoneFactory.getDefaultPhone();

        tbd1 = (TextView) findViewById(R.id.tbd1);
        tbd2 = (TextView) findViewById(R.id.tbd2);
        tbd3 = (TextView) findViewById(R.id.tbd2);

        mbnAutoLoadButton = (Button) findViewById(R.id.mbn_auto_load);
        mbnAutoLoadButton.setOnClickListener(mMbnAutoLoadHandler);

        volteAvailOvrButton = (Button) findViewById(R.id.volte_avail_ovr);
        volteAvailOvrButton.setOnClickListener(mVolteAvailOvrHandler);

        vtAvailOvrButton = (Button) findViewById(R.id.vt_avail_ovr);
        vtAvailOvrButton.setOnClickListener(mVtAvailOvrHandler);

        wfcAvailOvrButton = (Button) findViewById(R.id.wfc_avail_ovr);
        wfcAvailOvrButton.setOnClickListener(mWfcAvailOvrHandler);

        btnTbd1 = (Button) findViewById(R.id.btn_tbd1);

        spinTbd1 = (Spinner)findViewById(R.id.spin_tbd1);
        /*
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
                android.R.layout.simple_spinner_item, TBD);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        */
        btnTbd2 = (Button) findViewById(R.id.btn_tbd2);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateMbnAutoLoadState();
        updateVolteAvailOvrState();
        updateVtAvailOvrState();
        updateWfcAvailOvrState();

        log("onResume: update cm radio info");
    }

    @Override
    public void onPause() {
        super.onPause();

        log("onPause: cm radio info");
    }

    OnClickListener mMbnAutoLoadHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_SW_MBN_UPDATE, (isMbnAutoLoad() ? "0" : "1"));
            updateMbnAutoLoadState();
        }
    };

    private boolean isMbnAutoLoad() {
        return SystemProperties.getBoolean(PROPERTY_SW_MBN_UPDATE, false);
    }

    private void updateMbnAutoLoadState() {
        log("updateMbnAutoLoadState isMbnAutoLoad()=" + isMbnAutoLoad());
        String buttonText = isMbnAutoLoad() ?
                            getString(R.string.radio_info_mbn_auto_load_on_label) :
                            getString(R.string.radio_info_mbn_auto_load_off_label);
        mbnAutoLoadButton.setText(buttonText);
    }

    OnClickListener mVolteAvailOvrHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_SW_MBN_VOLTE, (isVolteAvailOvr() ? "0" : "1"));
            SystemProperties.set(PROPERTY_VOLTE_AVAIL_OVR, (isVolteAvailOvr() ? "0" : "1"));
            updateVolteAvailOvrState();
        }
    };

    private boolean isVolteAvailOvr() {
        return SystemProperties.getBoolean(PROPERTY_VOLTE_AVAIL_OVR, false);
    }

    private void updateVolteAvailOvrState() {
        log("updateVolteAvailOvrState isVolteAvailOvr()=" + isVolteAvailOvr());
        String buttonText = isVolteAvailOvr() ?
                            getString(R.string.radio_info_volte_avail_ovr_on_label) :
                            getString(R.string.radio_info_volte_avail_ovr_off_label);
        volteAvailOvrButton.setText(buttonText);
    }

    OnClickListener mVtAvailOvrHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_VT_AVAIL_OVR, (isVtAvailOvr() ? "0" : "1"));
            updateVtAvailOvrState();
        }
    };

    private boolean isVtAvailOvr() {
        return SystemProperties.getBoolean(PROPERTY_VT_AVAIL_OVR, false);
    }

    private void updateVtAvailOvrState() {
        log("updateVtAvailOvrState isVtAvailOvr()=" + isVtAvailOvr());
        String buttonText = isVtAvailOvr() ?
                            getString(R.string.radio_info_vt_avail_ovr_on_label) :
                            getString(R.string.radio_info_vt_avail_ovr_off_label);
        vtAvailOvrButton.setText(buttonText);
    }

    OnClickListener mWfcAvailOvrHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SystemProperties.set(PROPERTY_DATA_IWLAN_ENABLE, (isWfcAvailOvr() ? "false" : "true"));
            SystemProperties.set(PROPERTY_WFC_AVAIL_OVR, (isWfcAvailOvr() ? "0" : "1"));
            updateWfcAvailOvrState();
        }
    };

    private boolean isWfcAvailOvr() {
        return SystemProperties.getBoolean(PROPERTY_WFC_AVAIL_OVR, false);
    }

    private void updateWfcAvailOvrState() {
        log("updateWfcAvailOvrState isWfcAvailOvr()=" + isWfcAvailOvr());
        String buttonText = isWfcAvailOvr() ?
                            getString(R.string.radio_info_wfc_avail_ovr_on_label) :
                            getString(R.string.radio_info_wfc_avail_ovr_off_label);
        wfcAvailOvrButton.setText(buttonText);
    }

    private void log(String s) {
        Log.d(TAG, "[RadioInfo] " + s);
    }
}
