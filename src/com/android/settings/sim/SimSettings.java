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
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telecom.PhoneAccount;
import android.telephony.CellInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = true;

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_ENABLER_CATEGORY = "sim_enablers";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    private static final String KEY_ACTIVITIES = "activities";
    private static final String SLOTID_STR = "slotId";
    private static final String ENABLED_STR = "enabled";
    private static final String MESSAGE_STR = "message";

    private static final int SIM_ENABLER_PROGRESS_DIALOG_ID = 1;
    private static final int SIM_ENABLER_ALERT_DIALOG_ID = 2;

    private static final int EVT_RESUME = 1;
    private static final int EVT_SHOW_ALERT_DIALOG = 2;
    private static final int EVT_SHOW_PROGRESS_DIALOG = 3;
    // time out to dismiss progress dialog
    private static final int EVT_PROGRESS_DLG_TIME_OUT = 4;
    // 30 seconds for progress dialog time out
    private static final int PROGRESS_DLG_TIME_OUT = 45000;

    private static int mNumSlots = 0;
    private boolean mIsPause = false;

    /**
     * By UX design we have use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     */
    private List<SubInfoRecord> mAvailableSubInfos = null;
    private List<SubInfoRecord> mSubInfoList = null;

    private static List<MultiSimEnablerPreference> mSimEnablers = null;

    private SubInfoRecord mCellularData = null;
    private SubInfoRecord mCalls = null;
    private SubInfoRecord mSMS = null;

    private int mNumSims;

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        final TelephonyManager tm =
                    (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        if (mSubInfoList == null) {
            mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        }

        mNumSlots = tm.getSimCount();

        createPreferences();
        updateAllOptions();
    }

    private void createPreferences() {
        addPreferencesFromResource(R.xml.sim_settings);

        final PreferenceCategory simCards = (PreferenceCategory)findPreference(SIM_CARD_CATEGORY);
        final PreferenceCategory simEnablers =
                (PreferenceCategory)findPreference(SIM_ENABLER_CATEGORY);

        mAvailableSubInfos = new ArrayList<SubInfoRecord>(mNumSlots);
        for (int i = 0; i < mNumSlots; ++i) {
            final SubInfoRecord sir = findRecordBySlotId(i);
            simCards.addPreference(new SimPreference(getActivity(), sir, i));
            if (mNumSlots > 1) {
                simEnablers.addPreference(new MultiSimEnablerPreference(
                        getActivity(), sir, mHandler, i));
            } else {
                removePreference(SIM_ENABLER_CATEGORY);
            }
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mNumSims++;
            }
        }
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int prefSize = prefScreen.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = prefScreen.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            } else if (pref instanceof MultiSimEnablerPreference) {
                ((MultiSimEnablerPreference)pref).update();
            }
        }
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
        if (mSubInfoList != null){
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
        if (sir != null) {
            simPref.setSelectedValue(sir, false);
        }
        simPref.setEnabled(mNumSims > 1);
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
        mIsPause = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        updateAllOptions();
        mIsPause = false;
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (preference instanceof SimPreference) {
            ((SimPreference)preference).createEditDialog((SimPreference)preference);
        }

        return true;
    }

    public void createDropDown(DropDownPreference preference) {
        final DropDownPreference simPref = preference;
        final String keyPref = simPref.getKey();
        int mActCount = 0;
        final boolean askFirst = keyPref.equals(KEY_CALLS) || keyPref.equals(KEY_SMS);

        simPref.clearItems();

        //Get num of activated Subs
        for (SubInfoRecord subInfo : mAvailableSubInfos) {
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

                Log.d(TAG,"calling setCallback: " + simPref.getKey() + "subId: " + subId);
                if (simPref.getKey().equals(KEY_CELLULAR_DATA)) {
                    if (SubscriptionManager.getDefaultDataSubId() != subId) {
                        SubscriptionManager.setDefaultDataSubId(subId);
                    }
                } else if (simPref.getKey().equals(KEY_CALLS)) {
                    //subId 0 is meant for "Ask First"/"Prompt" option as per AOSP
                    if (subId == 0) {
                        SubscriptionManager.setVoicePromptEnabled(true);
                    } else {
                        if (SubscriptionManager.getDefaultVoiceSubId() != subId) {
                            SubscriptionManager.setVoicePromptEnabled(false);
                            SubscriptionManager.setDefaultVoiceSubId(subId);
                        }
                    }
                } else if (simPref.getKey().equals(KEY_SMS)) {
                    if (subId == 0) {
                        SubscriptionManager.setSMSPromptEnabled(true);
                    } else {
                        if (SubscriptionManager.getDefaultSmsSubId() != subId) {
                            SubscriptionManager.setSMSPromptEnabled(false);
                            SubscriptionManager.setDefaultSmsSubId(subId);
                        }
                    }
                }

                return true;
            }
        });
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

        public void createEditDialog(SimPreference simPref) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final View dialogLayout = getActivity().getLayoutInflater().inflate(
                    R.layout.multi_sim_dialog, null);
            builder.setView(dialogLayout);

            EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
            nameText.setText(mSubInfoRecord.displayName);

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
                        mSubInfoRecord.subId);

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

            builder.create().show();
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

    public static class MultiSimDialog extends DialogFragment {
        public final static String TAG = "MultiSimDialog";
        static int sSlotId = -1;
        static boolean sEnabled;

        // Argument bundle keys
        private static final String BUNDLE_KEY_DIALOG_ID = "MultiSimDialog.id";
        private static final String BUNDLE_KEY_DIALOG_MSG = "MultiSimDialog.msg";
        private DialogInterface.OnClickListener mDialogClickListener =
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };

        //Create the dialog with parameters
        public static MultiSimDialog newInstance(int id, String message) {
            MultiSimDialog fragment = new MultiSimDialog();
            Bundle bundle = new Bundle();
            bundle.putInt(BUNDLE_KEY_DIALOG_ID, id);
            bundle.putString(BUNDLE_KEY_DIALOG_MSG, message);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            Log.d(TAG, "onCreatedialog");
            int dialogId = getArguments().getInt(BUNDLE_KEY_DIALOG_ID);
            String dialogMsg = getArguments().getString(BUNDLE_KEY_DIALOG_MSG);
            switch(dialogId) {
                case SIM_ENABLER_PROGRESS_DIALOG_ID:
                    List<SubInfoRecord> subInfoList =
                            SubscriptionManager.getSubInfoUsingSlotId(sSlotId);
                    if ( subInfoList == null) {
                        return null;
                    }
                    String title = subInfoList.get(0).displayName;

                    String msg = getString(sEnabled ? R.string.sim_enabler_enabling
                            : R.string.sim_enabler_disabling);
                    ProgressDialog progressDiallog = new ProgressDialog(context);
                    progressDiallog.setIndeterminate(true);
                    progressDiallog.setTitle(title);
                    progressDiallog.setMessage(msg);
                    progressDiallog.setCancelable(false);
                    progressDiallog.setCanceledOnTouchOutside(false);
                    return progressDiallog;
                case SIM_ENABLER_ALERT_DIALOG_ID:
                    AlertDialog alertDialog = new AlertDialog.Builder(context)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(dialogMsg)
                            .setCancelable(false)
                            .setNeutralButton(R.string.close_dialog, mDialogClickListener)
                            .create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    return alertDialog;
                default:
                    return null;
            }
        }


        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            int dialogId = getArguments().getInt(BUNDLE_KEY_DIALOG_ID);
            switch(dialogId) {
                case SIM_ENABLER_ALERT_DIALOG_ID:
                    MultiSimEnablerPreference.mIsShowDialog = false;
                    break;
                case SIM_ENABLER_PROGRESS_DIALOG_ID:
                    MultiSimEnablerPreference.mIsShowDialog = false;
                    break;
                default:
                    break;
            }
        }

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            logd("msg.what = " + msg.what);
            switch(msg.what) {
                case EVT_RESUME:
                    Bundle bundle = msg.getData();
                    dismissProgressDialog(SIM_ENABLER_PROGRESS_DIALOG_ID);
                    MultiSimEnablerPreference.mIsShowDialog = false;
                    updateAllOptions();
                    break;
                case EVT_SHOW_PROGRESS_DIALOG:
                    showProgressDialog(SIM_ENABLER_PROGRESS_DIALOG_ID, msg);
                    break;

                case EVT_SHOW_ALERT_DIALOG:
                    Bundle b = msg.getData();
                    String disEnableMessage = b.getString(MESSAGE_STR);
                    int subId = b.getInt(SLOTID_STR);
                    showAlertDialog(SIM_ENABLER_ALERT_DIALOG_ID, disEnableMessage, subId);
                    break;
                case EVT_PROGRESS_DLG_TIME_OUT:
                    dismissProgressDialog(SIM_ENABLER_PROGRESS_DIALOG_ID);
                    break;
                default:
                    loge("Unknown Event " + msg.what);
                    break;
            }
        }
    };

    private void showAlertDialog(int dialogId, String dialogMsg, int slotId) {
        logd("showAlertDialogId = " + dialogId + ",mIsPause = " + mIsPause);
        if (dialogId == SIM_ENABLER_ALERT_DIALOG_ID){
            MultiSimDialog dialog = MultiSimDialog.newInstance(dialogId, dialogMsg);
            dialog.sSlotId = slotId;
            if (!mIsPause){
                dialog.show(getFragmentManager(), "DisableEnableAlertDialog");
            }
        }
    }

    private void showProgressDialog(int dialogId, Message msg) {
        if (dialogId == SIM_ENABLER_PROGRESS_DIALOG_ID) {
            if (null != msg){
                Bundle b = msg.getData();
                int slotId = b.getInt(SLOTID_STR);
                Boolean enabled = b.getBoolean(ENABLED_STR);
                String message = b.getString(MESSAGE_STR);
                MultiSimDialog dialog = MultiSimDialog.newInstance(
                        SIM_ENABLER_PROGRESS_DIALOG_ID, message);
                dialog.sEnabled = enabled;
                dialog.sSlotId = slotId;
                dialog.show(getFragmentManager(), "DisableEnableProgressDialog");
                mHandler.sendEmptyMessageDelayed(EVT_PROGRESS_DLG_TIME_OUT,
                        PROGRESS_DLG_TIME_OUT);
            }
        }
    }

    private void dismissProgressDialog(int dialogId) {
        if (dialogId == SIM_ENABLER_PROGRESS_DIALOG_ID && !mIsPause) {
            MultiSimDialog dialog = (MultiSimDialog)(getFragmentManager()
                    .findFragmentByTag("DisableEnableProgressDialog"));
            if (dialog != null) {
                dialog.dismiss();
                mHandler.removeMessages(EVT_PROGRESS_DLG_TIME_OUT);
            }
        }
    }

    private void logd(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void loge(String s) {
        Log.e(TAG, s);
    }
}
