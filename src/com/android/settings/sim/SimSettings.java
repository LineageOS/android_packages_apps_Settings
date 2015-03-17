/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.sim;

import android.provider.SearchIndexableResource;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telecom.PhoneAccount;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.MultiSimSettingTab;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;

import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable, TextWatcher {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = true;

    public static final String CONFIG_LTE_SUB_SELECT_MODE = "config_lte_sub_select_mode";
    private static final String CONFIG_PRIMARY_SUB_SETABLE = "config_primary_sub_setable";

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_ENABLER_CATEGORY = "sim_enablers";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String SIM_ACTIVITIES_CATEGORY = "sim_activities";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    private static final String KEY_ACTIVITIES = "activities";
    private static final String KEY_PRIMARY_SUB_SELECT = "select_primary_sub";
    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";

    private long mPreferredDataSubscription;

    private static final int EVT_UPDATE = 1;
    private static int mNumSlots = 0;
    //The default legth to dispaly a character
    private static final int CHAR_LEN = 1;
    /**
     * By UX design we have use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     */
    private List<SubInfoRecord> mAvailableSubInfos = null;
    private List<SubInfoRecord> mSubInfoList = null;
    private Preference mPrimarySubSelect = null;
    private boolean needUpdate = false;

    private static List<MultiSimEnablerPreference> mSimEnablers = null;

    private SubInfoRecord mCellularData = null;
    private SubInfoRecord mCalls = null;
    private SubInfoRecord mSMS = null;

    private int mNumSims;
    private int mPhoneCount;
    private int[] mCallState;
    private PhoneStateListener[] mPhoneStateListener;

    private boolean inActivity;
    private boolean dataDisableToastDisplayed = false;
    private AlertDialog mAlertDialog;
    private EditText nameText;
    private int mChangeStartPos;
    private int mChangeCount;

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG,"on onCreate");
        final TelephonyManager tm =
                    (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        if (mSubInfoList == null) {
            mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        }

        mNumSlots = tm.getSimCount();
        mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        mCallState = new int[mPhoneCount];
        mPhoneStateListener = new PhoneStateListener[mPhoneCount];
        listen();

        mPreferredDataSubscription = SubscriptionManager.getDefaultDataSubId();

        createPreferences();
        updateAllOptions();
        IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);

        getActivity().registerReceiver(mDdsSwitchReceiver, intentFilter);

        IntentFilter intentRadioFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        getActivity().registerReceiver(mRadioReceiver, intentRadioFilter);
    }

    @Override
    public void onDestroy() {
        final PreferenceCategory simEnablers =
                (PreferenceCategory)findPreference(SIM_ENABLER_CATEGORY);

        if (simEnablers != null) {
            for (int i = 0; i < simEnablers.getPreferenceCount(); ++i) {
                MultiSimEnablerPreference simEnabler = (MultiSimEnablerPreference) simEnablers
                        .getPreference(i);
                simEnabler.destroy();
            }
        }
        super.onDestroy();
        Log.d(TAG,"on onDestroy");
        getActivity().unregisterReceiver(mDdsSwitchReceiver);
        getActivity().unregisterReceiver(mRadioReceiver);
        unRegisterPhoneStateListener();
    }

    private void unRegisterPhoneStateListener() {
        TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            if (mPhoneStateListener[i] != null) {
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }
    }

    private BroadcastReceiver mDdsSwitchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Intent received: " + action);
            if (TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                updateCellularDataValues();
                long preferredDataSubscription = SubscriptionManager.getDefaultDataSubId();
                if (preferredDataSubscription != mPreferredDataSubscription) {
                    mPreferredDataSubscription = preferredDataSubscription;
                    String status = getResources().getString(R.string.switch_data_subscription,
                            SubscriptionManager.getSlotId(preferredDataSubscription) + 1);
                    Toast.makeText(getActivity(), status, Toast.LENGTH_SHORT).show();
                }
            } else if (TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)
                    || TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                mAvailableSubInfos.clear();
                mNumSims = 0;
                mSubInfoList = SubscriptionManager.getActiveSubInfoList();
                for (int i = 0; i < mNumSlots; ++i) {
                    final SubInfoRecord sir = findRecordBySlotId(i);
                    // Do not display deactivated subInfo in preference list
                    if ((sir != null) && (sir.mStatus == SubscriptionManager.ACTIVE)) {
                        mNumSims++;
                        mAvailableSubInfos.add(sir);
                    }
                }
                // Refresh UI whenever subinfo record gets changed
                updateAllOptions();
            }
        }
    };

    private final BroadcastReceiver mRadioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)
                    || Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                Log.d(TAG, "Received ACTION_SIM_STATE_CHANGED or ACTION_AIRPLANE_MODE_CHANGED");
                // Refresh UI like on resume
                initLTEPreference();
                updateAllOptions();
            }
        }
    };

    private void createPreferences() {
        addPreferencesFromResource(R.xml.sim_settings);

        mPrimarySubSelect = (Preference) findPreference(KEY_PRIMARY_SUB_SELECT);
        final PreferenceCategory simCards = (PreferenceCategory)findPreference(SIM_CARD_CATEGORY);
        final PreferenceCategory simEnablers =
                (PreferenceCategory)findPreference(SIM_ENABLER_CATEGORY);

        mAvailableSubInfos = new ArrayList<SubInfoRecord>(mNumSlots);
        mSimEnablers = new ArrayList<MultiSimEnablerPreference>(mNumSlots);
        for (int i = 0; i < mNumSlots; ++i) {
            final SubInfoRecord sir = findRecordBySlotId(i);
            simCards.addPreference(new SimPreference(getActivity(), sir, i));
            if (mNumSlots > 1) {
                mSimEnablers.add(i, new MultiSimEnablerPreference(
                        getActivity(), sir, mHandler, i));
                simEnablers.addPreference(mSimEnablers.get(i));
            } else {
                removePreference(SIM_ENABLER_CATEGORY);
            }
            // Do not display deactivated subInfo in preference list
            if ((sir != null) && (sir.mStatus == SubscriptionManager.ACTIVE)) {
                mNumSims++;
                mAvailableSubInfos.add(sir);
            }
        }

        // Remove SIM_CARD_CATEGORY by default for UX, use SIM_ENABLER_CATEGORY replaced
        removePreference(SIM_CARD_CATEGORY);
    }

    private void updateAllOptions() {
        Log.d(TAG,"updateAllOptions");
        mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        updateSimSlotValues();
        updateActivitesCategory();
        updateSimEnablers();
    }

    private void listen() {
        TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            long[] subId = SubscriptionManager.getSubId(i);
            if (subId != null) {
                if (subId[0] > 0) {
                    mCallState[i] = tm.getCallState(subId[0]);
                    tm.listen(getPhoneStateListener(i, subId[0]),
                            PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        }
    }

    private PhoneStateListener getPhoneStateListener(int phoneId, long subId) {
        final int i = phoneId;
        mPhoneStateListener[phoneId]  = new PhoneStateListener(subId) {
            @Override
            public void onCallStateChanged(int state, String ignored) {
                Log.d(TAG, "onCallStateChanged: " + state);
                mCallState[i] = state;
                updateCellularDataPreference();
            }
        };
        return mPhoneStateListener[phoneId];
    }

    private void updateSimSlotValues() {
        final PreferenceCategory prefScreen = (PreferenceCategory) getPreferenceScreen()
                .findPreference(SIM_CARD_CATEGORY);
        if (prefScreen != null) {
            final int prefSize = prefScreen.getPreferenceCount();
            for (int i=0; i<prefSize; ++i) {
                Preference pref = prefScreen.getPreference(i);
                if (pref instanceof SimPreference) {
                    ((SimPreference)pref).update();
                }
            }
        }
    }

    private boolean needDisableDataSub2() {
        boolean disableDataSub2 = false;
        if (getResources().getBoolean(R.bool.disable_data_sub2)) {
            if (TelephonyManager.getDefault().getMultiSimConfiguration().
                equals(TelephonyManager.MultiSimVariants.DSDS)) {
                if (mSubInfoList.size() == 2) {
                    disableDataSub2 = true;
                }
            }
        }
        return disableDataSub2;
     }

    private void updateActivitesCategory() {
        createDropDown((DropDownPreference) findPreference(KEY_CELLULAR_DATA));
        createDropDown((DropDownPreference) findPreference(KEY_CALLS));
        createDropDown((DropDownPreference) findPreference(KEY_SMS));
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
    }

    /**
     * finds a record with subId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubInfoRecord findRecordBySubId(final long subId) {
        final int availableSubInfoLength = mAvailableSubInfos.size();

        for (int i = 0; i < availableSubInfoLength; ++i) {
            final SubInfoRecord sir = mAvailableSubInfos.get(i);
            if (sir != null && sir.subId == subId) {
                return sir;
            }
        }
        return null;
    }

    /**
     * finds a record with slotId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubInfoRecord findRecordBySlotId(final int slotId) {
        if (mSubInfoList != null) {
            final int availableSubInfoLength = mSubInfoList.size();

            for (int i = 0; i < availableSubInfoLength; ++i) {
                final SubInfoRecord sir = mSubInfoList.get(i);
                if (sir.slotId == slotId) {
                    //Right now we take the first subscription on a SIM.
                    return sir;
                }
            }
        }

        return null;
    }

    private void updateSmsValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_SMS);
        long subId = SubscriptionManager.isSMSPromptEnabled() ?
                0 : SubscriptionManager.getDefaultSmsSubId();
        final SubInfoRecord sir = findRecordBySubId(subId);
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    private void updateCellularDataValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CELLULAR_DATA);
        final SubInfoRecord sir = findRecordBySubId(SubscriptionManager.getDefaultDataSubId());
        boolean isCellularDataEnabled = false;
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        if (mNumSims > 1 && !needDisableDataSub2()) {
            isCellularDataEnabled = true;
        }
        simPref.setEnabled(isCellularDataEnabled);
        updateCellularDataPreference();
    }

    private void updateCellularDataPreference() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CELLULAR_DATA);
        boolean callStateIdle = isCallStateIdle();
        // Enable data preference in msim mode and call state idle
        boolean disableCellulardata = getResources().getBoolean(R.bool.disbale_cellular_data);
        simPref.setEnabled((mNumSims > 1) && callStateIdle && (!disableCellulardata));
        // Display toast only once when the user enters the activity even though the call moves
        // through multiple call states (eg - ringing to offhook for incoming calls)
        if (callStateIdle == false && inActivity && dataDisableToastDisplayed == false) {
            Toast.makeText(getActivity(), R.string.data_disabled_in_active_call,
                    Toast.LENGTH_SHORT).show();
            dataDisableToastDisplayed = true;
        }
        // Reset dataDisableToastDisplayed
        if (callStateIdle == true) {
            dataDisableToastDisplayed = false;
        }
    }

    private boolean isCallStateIdle() {
        boolean callStateIdle = true;
        for (int i = 0; i < mCallState.length; i++) {
            if (TelephonyManager.CALL_STATE_IDLE != mCallState[i]) {
                callStateIdle = false;
            }
        }
        Log.d(TAG, "isCallStateIdle " + callStateIdle);
        return callStateIdle;
    }

    private void updateCallValues() {
        final DropDownPreference simPref = (DropDownPreference) findPreference(KEY_CALLS);
        long subId = SubscriptionManager.isVoicePromptEnabled() ?
                0 : SubscriptionManager.getDefaultVoiceSubId();
        final SubInfoRecord sir = findRecordBySubId(subId);
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        inActivity = false;
        Log.d(TAG,"on Pause");
        dataDisableToastDisplayed = false;
        for (int i = 0; i < mSimEnablers.size(); ++i) {
            MultiSimEnablerPreference simEnabler = mSimEnablers.get(i);
            if (simEnabler != null) simEnabler.cleanUp();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        inActivity = true;
        Log.d(TAG,"on Resume, number of slots = " + mNumSlots);
        initLTEPreference();
        updateAllOptions();
    }

    private void initLTEPreference() {
        boolean isPrimarySubFeatureEnable = SystemProperties
                .getBoolean("persist.radio.primarycard", false);

        boolean primarySetable = android.provider.Settings.Global.getInt(
                this.getContentResolver(), CONFIG_PRIMARY_SUB_SETABLE, 0) == 1;

        logd("isPrimarySubFeatureEnable :" + isPrimarySubFeatureEnable +
                " primarySetable :" + primarySetable);

        if (!isPrimarySubFeatureEnable || !primarySetable) {
            final PreferenceCategory simActivities =
                    (PreferenceCategory) findPreference(SIM_ACTIVITIES_CATEGORY);
            simActivities.removePreference(mPrimarySubSelect);
            return;
        }

        int primarySlot = getCurrentPrimarySlot();

        boolean isManualMode = android.provider.Settings.Global.getInt(
                this.getContentResolver(), CONFIG_LTE_SUB_SELECT_MODE, 1) == 0;

        logd("init LTE primary slot : " + primarySlot + " isManualMode :" + isManualMode);
        if (-1 != primarySlot) {
            SubInfoRecord subInfo = findRecordBySlotId(primarySlot);
            CharSequence lteSummary = (subInfo == null ) ? null : subInfo.displayName;
            mPrimarySubSelect.setSummary(lteSummary);
        } else {
            mPrimarySubSelect.setSummary("");
        }
        mPrimarySubSelect.setEnabled(isManualMode);
    }

    public int getCurrentPrimarySlot() {
        for (int index = 0; index < mNumSlots; index++) {
            int current = getPreferredNetwork(index);
            if (current == Phone.NT_MODE_TD_SCDMA_GSM_WCDMA_LTE
                    || current == Phone.NT_MODE_TD_SCDMA_GSM_WCDMA) {
                return index;
            }
        }
        return -1;
    }

    private int getPreferredNetwork(int sub) {
        int nwMode = -1;
        try {
            nwMode = TelephonyManager.getIntAtIndex(this.getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE, sub);
        } catch (SettingNotFoundException snfe) {
        }
        return nwMode;
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (preference instanceof SimPreference) {
            ((SimPreference) preference).createEditDialog((SimPreference) preference);
        } else if (preference instanceof MultiSimEnablerPreference) {
            ((MultiSimEnablerPreference) preference).createEditDialog();
        } else if (preference == mPrimarySubSelect) {
            startActivity(mPrimarySubSelect.getIntent());
        }

        return true;
    }

    public void createDropDown(DropDownPreference preference) {
        final DropDownPreference simPref = preference;
        final String keyPref = simPref.getKey();
        int mActCount = 0;
        final boolean askFirst = keyPref.equals(KEY_CALLS) || keyPref.equals(KEY_SMS);
        //If Fragment not yet attached to Activity, return
        if (!isAdded()) {
            Log.d(TAG,"Fragment not yet attached to Activity, EXIT!!" );
            return;
        }
        simPref.clearItems();

        //Get num of activated Subs
        for (SubInfoRecord subInfo : mSubInfoList) {
            if (subInfo != null && subInfo.mStatus == SubscriptionManager.ACTIVE) mActCount++;
        }

        if (askFirst && mActCount > 1) {
            simPref.addItem(getResources().getString(
                    R.string.sim_calls_ask_first_prefs_title), null);
        }

        final int subAvailableSize = mAvailableSubInfos.size();
        for (int i = 0; i < subAvailableSize; ++i) {
            final SubInfoRecord sir = mAvailableSubInfos.get(i);
            if(sir != null){
                simPref.addItem(sir.displayName, sir);
            }
        }

        simPref.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final long subId = value == null ? 0 : ((SubInfoRecord)value).subId;

                Log.d(TAG,"calling setCallback: " + simPref.getKey() + "subId: " + subId +
                        " defaultSubId: " + SubscriptionManager.getDefaultDataSubId());
                if (simPref.getKey().equals(KEY_CELLULAR_DATA)) {
                    if (SubscriptionManager.getDefaultDataSubId() != subId) {
                        SubscriptionManager.setDefaultDataSubId(subId);
                        Log.d(TAG,"subAvailableSize: "+ subAvailableSize);
                        if(subAvailableSize > 1) {
                            setUserPrefDataSubIdInDb(subId);
                        }
                   }
                } else if (simPref.getKey().equals(KEY_CALLS)) {
                    //subId 0 is meant for "Ask First"/"Prompt" option as per AOSP
                    if (subId == 0) {
                        SubscriptionManager.setVoicePromptEnabled(true);
                    } else {
                        SubscriptionManager.setVoicePromptEnabled(false);
                        if (SubscriptionManager.getDefaultVoiceSubId() != subId) {
                            SubscriptionManager.setDefaultVoiceSubId(subId);
                        }
                    }
                } else if (simPref.getKey().equals(KEY_SMS)) {
                    if (subId == 0) {
                        SubscriptionManager.setSMSPromptEnabled(true);
                    } else {
                        SubscriptionManager.setSMSPromptEnabled(false);
                        if (SubscriptionManager.getDefaultSmsSubId() != subId) {
                            SubscriptionManager.setDefaultSmsSubId(subId);
                        }
                    }
                }

                return true;
            }
        });
    }

    private void setUserPrefDataSubIdInDb(long subId) {
        android.provider.Settings.Global.putLong(getContentResolver(), SETTING_USER_PREF_DATA_SUB,
                subId);
        logd("updating data subId: " + subId + " in DB");
    }

    private void setActivity(Preference preference, SubInfoRecord sir) {
        final String key = preference.getKey();

        if (key.equals(KEY_CELLULAR_DATA)) {
            mCellularData = sir;
        } else if (key.equals(KEY_CALLS)) {
            mCalls = sir;
        } else if (key.equals(KEY_SMS)) {
            mSMS = sir;
        }

        updateActivitesCategory();
    }

    private class SimPreference extends Preference{
        private SubInfoRecord mSubInfoRecord;
        private int mSlotId;

        public SimPreference(Context context, SubInfoRecord subInfoRecord, int slotId) {
            super(context);

            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
        }

        public void update() {
            mSubInfoRecord = findRecordBySlotId(mSlotId);
            final Resources res = getResources();

            setTitle(res.getString(R.string.sim_card_number_title, mSlotId + 1));
            if (mSubInfoRecord != null) {
                setSummary(res.getString(R.string.sim_settings_summary,
                            mSubInfoRecord.displayName, mSubInfoRecord.number));
                setEnabled(true);
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
        }

        @Override
        protected void onAttachedToActivity() {
            super.onAttachedToActivity();
            if (needUpdate) {
                needUpdate = false;
                updateAllOptions();
            }
        }

        public void createEditDialog(SimPreference simPref) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final View dialogLayout = getActivity().getLayoutInflater().inflate(
                    R.layout.multi_sim_dialog, null);
            builder.setView(dialogLayout);

            nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
            nameText.setText(mSubInfoRecord.displayName);
            nameText.addTextChangedListener(SimSettings.this);

            TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
            numberView.setText(mSubInfoRecord.number);

            TextView carrierView = (TextView)dialogLayout.findViewById(R.id.carrier);
            carrierView.setText(mSubInfoRecord.displayName);

            builder.setTitle(R.string.sim_editor_title);

            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    final EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
                    final Spinner displayNumbers =
                        (Spinner)dialogLayout.findViewById(R.id.display_numbers);

                    SubscriptionManager.setDisplayNumberFormat(
                        displayNumbers.getSelectedItemPosition() == 0
                            ? SubscriptionManager.DISPLAY_NUMBER_LAST
                            : SubscriptionManager.DISPLAY_NUMBER_FIRST, mSubInfoRecord.subId);

                    mSubInfoRecord.displayName = nameText.getText().toString();
                    SubscriptionManager.setDisplayName(mSubInfoRecord.displayName,
                        mSubInfoRecord.subId, SubscriptionManager.NAME_SOURCE_USER_INPUT);

                    updateAllOptions();
                    update();
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        }
    }

    // TextWatcher interface
    public void afterTextChanged(Editable s) {
        limitTextSize(s.toString().trim());
        // if user inputed whole space and saved,that is to say SLOT1 and
        // SLOT2 may named blank space, it is meaningless.
        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(!TextUtils.isEmpty(s.toString().trim()));
    }

    // TextWatcher interface
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // not used
    }

    // TextWatcher interface
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // The start position of new added characters
        mChangeStartPos = start;
        // The number of new added characters
        mChangeCount = count;
    }

    private void limitTextSize(String textString) {

        if (nameText != null) {
            int wholeLen = 0;
            int i = 0;

            for (i = 0; i < textString.length(); i++) {
                wholeLen += getCharacterVisualLength(textString, i);
            }
           int InputNameMaxLength = getResources().getInteger(R.integer.sim_name_length);
            // Too many characters,cut off the new added characters
            if (wholeLen > InputNameMaxLength) {
                int cutNum = wholeLen - InputNameMaxLength;
                // Get start position of characters that will be cut off
                int changeEndPos = mChangeStartPos + mChangeCount - 1;
                int cutLen = 0;
                for (i = changeEndPos; i >= 0; i--) {
                    cutLen += getCharacterVisualLength(textString, i);
                    if (cutLen >= cutNum) {
                        break;
                    }
                }
                // The cut off characters is in range [i,mChangeStartPos + mChangeCount)
                int headStrEndPos = i;
                // Head substring that is before the cut off characters
                String headStr = "";
                // Rear substring that is after the cut off characters
                String rearStr = "";
                if (headStrEndPos > 0) {
                    // Get head substring if the cut off characters is not at the beginning
                    headStr = textString.substring(0, headStrEndPos);
                }
                int rearStrStartPos = mChangeStartPos + mChangeCount;
                if (rearStrStartPos < textString.length()) {
                    // Get rear substring if the cut off characters is not at the end
                    rearStr = textString.substring(rearStrStartPos, textString.length());
                }
                // headStr + rearStr is the new string after characters are cut off
                nameText.setText(headStr + rearStr);
                // Move cursor to the original position
                nameText.setSelection(i);
            }
        }
    }

    // A character beyond 0xff is twice as big as a character within 0xff in width when showing.
    private int getCharacterVisualLength(String seq, int index) {
        int cp = Character.codePointAt(seq, index);
        if (cp >= 0x00 && cp <= 0xFF) {
            return CHAR_LEN;
        } else {
            return CHAR_LEN*2;
        }
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }
                    return result;
                }
            };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            logd("msg.what = " + msg.what);
            switch(msg.what) {
                case EVT_UPDATE:
                    if (isAdded()) {
                        updateAllOptions();
                    } else {
                        needUpdate = true;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void updateSimEnablers() {
        for (int i = 0; i < mSimEnablers.size(); ++i) {
            MultiSimEnablerPreference simEnabler = mSimEnablers.get(i);
            if (simEnabler != null) simEnabler.update();
        }
    }

    private void logd(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void loge(String s) {
        Log.e(TAG, s);
    }
}
