/*
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

package com.android.settings;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.TextUtils;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.MSimTelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.codeaurora.telephony.msim.MSimPhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.codeaurora.telephony.msim.MSimUiccController;

import java.util.ArrayList;

public class ApnSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";
    public static final String OPERATOR_NUMERIC_EXTRA = "operator";

    public static final String APN_ID = "apn_id";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int RO_INDEX = 4;
    private static final int LOCALIZED_NAME_INDEX = 5;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private Uri mPreferApnUri;

    private static boolean mRestoreDefaultApnMode;

    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;

    private int mSubscription = 0;
    private String mSelectedKey;

    private boolean mUseNvOperatorForEhrpd = SystemProperties.getBoolean(
            "persist.radio.use_nv_for_ehrpd", false);

    private IntentFilter mMobileStateFilter;

    private static final String ACTION_APN_RESRORE_COMPLETE = "com.android.apnsettings.RESRORE_COMPLETE";

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
            }
            if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED) ||
                    intent.getAction().equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                setScreenEnabled();
                invalidateOptionsMenu();
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.apn_settings);
        getListView().setItemsCanFocus(true);
        mSubscription = getIntent().getIntExtra(SelectSubscription.SUBSCRIPTION_KEY,
                MSimTelephonyManager.getDefault().getDefaultSubscription());
        Log.d(TAG, "onCreate received sub :" + mSubscription);
        mMobileStateFilter = new IntentFilter();
        mMobileStateFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mMobileStateFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            mPreferApnUri = Uri.parse(PREFERRED_APN_URI + "/" + mSubscription);
        } else {
            mPreferApnUri = Uri.parse(PREFERRED_APN_URI);
        }
        Log.d(TAG, "Preferred APN Uri is set to '" + mPreferApnUri.toString() + "'");
    }

    @Override
    protected void onResume() {
        super.onResume();
        setScreenEnabled();
        registerReceiver(mMobileStateReceiver, mMobileStateFilter);

        if (!mRestoreDefaultApnMode) {
            fillList();
        } else {
            showDialog(DIALOG_RESTORE_DEFAULTAPN);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mSubscription = intent.getIntExtra(SelectSubscription.SUBSCRIPTION_KEY,
                MSimTelephonyManager.getDefault().getDefaultSubscription());
        Log.d(TAG, "onNewIntent received sub :" + mSubscription);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mMobileStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
    }

    private int getRadioTechnology(){
        ServiceState serviceState = null;
        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            serviceState = MSimPhoneFactory.getPhone(mSubscription).getServiceState();
        } else {
            serviceState = PhoneFactory.getDefaultPhone().getServiceState();
        }
        int netType = serviceState.getRilDataRadioTechnology();
        if (netType == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
            netType = serviceState.getRilVoiceRadioTechnology();
        }
        return netType;
    }

    private String getIccOperatorNumeric() {
        String iccOperatorNumeric = null;
        int family = getFamily();
        if (UiccController.APP_FAM_UNKNOWN != family) {
            IccRecords iccRecords = null;
            if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                iccRecords = MSimUiccController.getInstance().getIccRecords(mSubscription, family);
            } else {
                iccRecords = UiccController.getInstance().getIccRecords(family);
            }
            if (iccRecords != null) {
                iccOperatorNumeric = iccRecords.getOperatorNumeric();
            }

        }
        return iccOperatorNumeric;
    }

    private int getFamily() {
        int family = UiccController.getFamilyFromRadioTechnology(getRadioTechnology());
        if (UiccController.APP_FAM_UNKNOWN == family) {
            int phoneType = TelephonyManager.PHONE_TYPE_NONE;
            if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                phoneType = MSimTelephonyManager.getDefault().getPhoneType(mSubscription);
            } else {
                phoneType = TelephonyManager.getDefault().getPhoneType();
            }
            switch (phoneType) {
                case TelephonyManager.PHONE_TYPE_GSM:
                    family = UiccController.APP_FAM_3GPP;
                    break;
                case TelephonyManager.PHONE_TYPE_CDMA:
                    family = UiccController.APP_FAM_3GPP2;
                    break;
            }
        }
        return family;
    }

    private void fillList() {
        boolean isSelectedKeyMatch = false;
        String where = getOperatorNumericSelection();

        // Hide nothing if property is false, default is true
        if (SystemProperties.getBoolean("persist.sys.hideapn", true)) {
            // remove the filtered items, no need to show in UI
            where += " and type <>\"" + PhoneConstants.APN_TYPE_FOTA + "\"";
            where += " and type <>\"" + PhoneConstants.APN_TYPE_IA + "\"";

            if (getResources().getBoolean(R.bool.config_hidesupl_enable)) {
                boolean needHideSupl = false;
                for (String plmn : getResources().getStringArray(
                        R.array.hidesupl_plmn_list)) {
                    if (plmn.equals(MSimTelephonyManager.getDefault()
                            .getSimOperator(mSubscription))) {
                        needHideSupl = true;
                        break;
                    }
                }

                if (needHideSupl) {
                    where += " and type <>\"" + PhoneConstants.APN_TYPE_SUPL + "\"";
                }
            }

            // Hide mms if config is true
            if (getResources().getBoolean(R.bool.config_mms_enable)) {
                where += " and type <>\"" + PhoneConstants.APN_TYPE_MMS + "\"";
            }
        }

        //UI should filter APN by bearer and enable status
        where += " and (bearer=\"" + getRadioTechnology() + "\" or bearer =\"" + 0 + "\")";
        where += " and carrier_enabled = 1";
        Log.d(TAG, "fillList: where= " + where);

        if (TextUtils.isEmpty(where)) {
            Log.d(TAG, "getOperatorNumericSelection is empty ");
            return;
        }

        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn", "type", "read_only", "localized_name"}, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            ArrayList<Preference> mmsApnList = new ArrayList<Preference>();

            mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                boolean readOnly = (cursor.getInt(RO_INDEX) == 1);

                String localizedName = getLocalizedName(this, cursor, LOCALIZED_NAME_INDEX);
                if (!TextUtils.isEmpty(localizedName)) {
                    name = localizedName;
                }

                ApnPreference pref = new ApnPreference(this);

                pref.setApnReadOnly(readOnly);
                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);

                boolean selectable = ((type == null) || !type.equals("mms"));
                pref.setSelectable(selectable);
                if (selectable) {
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                        isSelectedKeyMatch = true;
                        Log.d(TAG, "find select key = " + mSelectedKey);
                    }
                    apnList.addPreference(pref);
                } else {
                    mmsApnList.add(pref);
                }
                cursor.moveToNext();
            }

            //if find no selectedKey, set the first one as selected key 291
            if (!isSelectedKeyMatch && apnList.getPreferenceCount() > 0) {
                ApnPreference pref = (ApnPreference) apnList.getPreference(0);
                setSelectedApnKey(pref.getKey());
                Log.d(TAG, "find no select key = " + mSelectedKey);
                Log.d(TAG, "set key to  " +pref.getKey());
            }
            cursor.close();

            for (Preference preference : mmsApnList) {
                apnList.addPreference(preference);
            }
        }
    }

    public static String getLocalizedName(Context context, Cursor cursor, int index) {
        // If can find a localized name, replace the APN name with it
        String resName = cursor.getString(index);
        String localizedName = null;
        if (resName != null && !resName.isEmpty()) {
            int resId = context.getResources().getIdentifier(resName, "string",
                    context.getPackageName());
            try {
                localizedName = context.getResources().getString(resId);
                Log.d(TAG, "Replaced apn name with localized name");
            } catch (NotFoundException e) {
                Log.e(TAG, "Got execption while getting the localized apn name.", e);
            }
        }
        return localizedName;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new))
                .setIcon(android.R.drawable.ic_menu_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
        return true;
    }

    /*
     * If airplane mode is on or SIM card don't prepar, set options menu don't
     * pop up. Restrict user to new APN, reset to default or click the APN list.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String where = getOperatorNumericSelection();
        if (TextUtils.isEmpty(where) || isAirplaneOn()) {
            return false;
        } else {
            super.onPrepareOptionsMenu(menu);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        intent.putExtra(OPERATOR_NUMERIC_EXTRA, getOperatorNumeric()[0]);
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(mPreferApnUri, values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        Cursor cursor = getContentResolver().query(mPreferApnUri, new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    sendBroadcast(new Intent(ACTION_APN_RESRORE_COMPLETE));
                    if (getResources().getBoolean(R.bool.config_restore_finish)) {
                        finish();
                    } else {
                        fillList();
                    }
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    dismissDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        ApnSettings.this,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(DEFAULTAPN_URI, null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            getPreferenceScreen().setEnabled(false);
        }
    }

    private String getOperatorNumericSelection() {
        String[] mccmncs = getOperatorNumeric();
        String where;
        where = (mccmncs[0] != null) ? "numeric=\"" + mccmncs[0] + "\"" : "";
        where += (mccmncs[1] != null) ? " or numeric=\"" + mccmncs[1] + "\"" : "";
        Log.d(TAG, "getOperatorNumericSelection: " + where);
        return where;
    }

    private String[] getOperatorNumeric() {
        ArrayList<String> result = new ArrayList<String>();
        if (mUseNvOperatorForEhrpd) {
            String mccMncForEhrpd = SystemProperties.get("ro.cdma.home.operator.numeric", null);
            if (mccMncForEhrpd != null && mccMncForEhrpd.length() > 0) {
                result.add(mccMncForEhrpd);
            }
        }
        String iccOperatorNumeric = getIccOperatorNumeric();
        Log.d(TAG, "getOperatorNumeric: sub= " + mSubscription + " mcc-mnc= " + iccOperatorNumeric);
        if (iccOperatorNumeric != null && iccOperatorNumeric.length() > 0) {
            result.add(iccOperatorNumeric);
        }
        return result.toArray(new String[2]);
    }

    /**
     * If airplane mode is on or SIM card don't prepar, make sure user can't
     * edit the Apn. So we disable the screen.
     */
    private void setScreenEnabled() {
        String where = getOperatorNumericSelection();
        if (TextUtils.isEmpty(where) || isAirplaneOn()) {
            getPreferenceScreen().setEnabled(false);
        } else {
            getPreferenceScreen().setEnabled(true);
        }
    }

    /**
     * Add the method to check the phone state is airplane mode or not. Return
     * true, if the phone state is airplane mode.
     */
    private boolean isAirplaneOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }
}
