/**
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.SubInfoRecord;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * SimEnabler is a helper to manage the slot on/off checkbox preference. It is
 * turns on/off slot and ensures the summary of the preference reflects the current state.
 */
public class MultiSimEnablerPreference extends Preference implements OnCheckedChangeListener {
    private final Context mContext;

    private String TAG = "MultiSimEnablerPreference";
    private static final boolean DBG = true;

    private static final int EVT_RESUME = 1;
    private static final int EVT_SHOW_ALERTDIALOG = 2;
    private static final int EVT_SHOW_PROGRESS_DIALOG = 3;
    private static final String SLOTID_STR = "slotId";
    private static final String ENABLED_STR = "enabled";
    private static final String MESSAGE_STR = "message";

    private int mSlotId;
    private SubInfoRecord mSir;
    private String mSummary;
    private boolean mState;
    private boolean mRequest;

    private boolean mCmdInProgress = false;
    private String mDialogString = null;
    private TextView mSubTitle, mSubSummary;
    private int mSwitchVisibility = View.VISIBLE;
    private Switch mSwitch;
    private Handler mParentHandler = null;
    private AlertDialog mAlertDialog = null;

    private static boolean mIsShowAlertDialog = false;
    public static boolean mIsShowDialog = false;
    private static String mCurrentStr = "";
    private static boolean mCurrentStatus = true;

    private static Object mSyncLock = new Object();

    private IntentFilter mIntentFilter = new IntentFilter(
            TelephonyIntents.ACTION_SIM_STATE_CHANGED);

    private void sendMessage(int event, Bundle b) {
        Message message = mParentHandler.obtainMessage(event);
        message.setData(b);
        mParentHandler.sendMessage(message);
    }

    private void handleSetUiccDone(String msg) {
        update();
        Bundle b = new Bundle();
        sendMessage(EVT_RESUME, b);
        showAlertDialogWithMessage(msg);
        mCmdInProgress = false;
    }

    private boolean hasCard() {
        return TelephonyManager.getDefault().hasIccCard(mSlotId);
    }

    public MultiSimEnablerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setWidgetLayoutResource(R.layout.custom_checkbox);
        setSwitchVisibility(View.VISIBLE);
    }

    public MultiSimEnablerPreference(Context context, SubInfoRecord sir, Handler handler,
            int slotId) {
        this(context, null, com.android.internal.R.attr.checkBoxPreferenceStyle);
        mSlotId = slotId;
        mSir = sir;
        mParentHandler = handler;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSubTitle = (TextView) view.findViewById(R.id.subtitle);
        mSubSummary = (TextView) view.findViewById(R.id.subsummary);
        mSwitch = (Switch) view.findViewById(R.id.subSwitchWidget);
        mSwitch.setOnCheckedChangeListener(this);
        update();
        // now use other config screen to active/deactive sim card\
        mSwitch.setVisibility(mSwitchVisibility);
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        mContext.unregisterReceiver(mReceiver);
    }

    public void destroy() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // May receive Receiver not registered error
            logd(e.getMessage());
        }
    }

    public void update() {
        if (isAirplaneModeOn() || !hasCard()) {
            setEnabled(false);
            return;
        }
        final Resources res = mContext.getResources();
        boolean isSubValid = isCurrentSubValid();
        setEnabled(isSubValid);
        if (isSubValid) {
            updateTitle();
            updateSummary();
        } else {
            mSubTitle.setText(res.getString(R.string.sim_card_number_title, mSlotId + 1));
            mSubSummary.setText(R.string.sim_slot_empty);
        }
    }

    private boolean isCurrentSubValid() {
        if (mSir == null || mSir.mSubId <= 0) {
            List<SubInfoRecord> sirList = SubscriptionManager.getActiveSubInfoList();
            if (sirList == null) return false;
            for (SubInfoRecord sir : sirList) {
                if (sir != null && mSlotId == sir.mSlotId) {
                    mSir = sir;
                    break;
                }
            }
        }
        if (mSir != null && mSir.mSubId > 0 && mSir.mSlotId >= 0 &&
                mSir.mStatus != SubscriptionManager.SUB_CONFIGURATION_IN_PROGRESS) {
            return true;
        }
        return false;
    }

    private void updateTitle() {
        if (mContext == null || mSubTitle == null) return;
        mSubTitle.setText(mSir.mDisplayName);
    }

    public void setSwitchVisibility (int visibility) {
        mSwitchVisibility = visibility;
    }

    private void setChecked(boolean state) {
        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(null);
            mSwitch.setChecked(state);
            mSwitch.setOnCheckedChangeListener(this);
        }
    }

    public void setEnabled(boolean isEnabled) {
        logd("setEnabled: isEnabled " + isEnabled + "sir:" + mSir);
        if (mSwitch != null) {
            mSwitch.setEnabled(isEnabled);
        }
    }

    private void sendCommand(boolean enabled) {
        if (mParentHandler == null || !mSwitch.isEnabled()) {
            return;
        }
        mIsShowDialog = true;
        mCmdInProgress = true;

        Bundle b = new Bundle();
        b.putInt(SLOTID_STR, mSlotId);
        b.putBoolean(ENABLED_STR, enabled);
        sendMessage(EVT_SHOW_PROGRESS_DIALOG, b);
        mSwitch.setEnabled(false);
        if (enabled) {
            logd("activateSubId: subId " + mSir.mSubId);
            SubscriptionManager.activateSubId(mSir.mSubId);
        } else {
            logd("deactivateSubId: subId " + mSir.mSubId);
            SubscriptionManager.deactivateSubId(mSir.mSubId);
        }
    }

    private void updateSummary() {
        Resources res = mContext.getResources();
        boolean isActivated = (mSir.mStatus == SubscriptionManager.ACTIVE);
        logd("updateSummary: subId " + mSir.mSubId + " isActivated = " + isActivated +
                " slot id = " + mSlotId);

        if (mAlertDialog != null) mIsShowAlertDialog = mAlertDialog.isShowing();

        if (mIsShowAlertDialog || mIsShowDialog) {
            mSummary = mCurrentStr;
            mState = mCurrentStatus;
        } else {
            if (isActivated) {
                mState = true;
                mSummary = mContext.getString(R.string.sim_enabler_summary,
                        res.getString(R.string.sim_enabled));
            } else {
                mState = false;
                mSummary = mContext.getString(R.string.sim_enabler_summary,
                        res.getString(hasCard() ? R.string.sim_disabled
                                : R.string.sim_missing));
            }
        }

        if (mSubSummary != null) {
            mSubSummary.setText(mSummary);
        }
        setChecked(mState);
    }


    /**
     * get count of active SubInfo on the device
     * @param context
     * @return
     */
    public static int getActivatedSubInfoCount(Context context) {
        int activeSubInfoCount = 0;
        List<SubInfoRecord> subInfoLists = getActivatedSubInfoList(context);
        if (subInfoLists != null) {
            for (SubInfoRecord subInfo : subInfoLists) {
                if (subInfo.mStatus == SubscriptionManager.ACTIVE) activeSubInfoCount++;
            }
        }
        return activeSubInfoCount;
    }

    public static List<SubInfoRecord> getActivatedSubInfoList(Context context) {
        List<SubInfoRecord> subInfoLists = SubscriptionManager.getActiveSubInfoList();
        if (subInfoLists != null) {
            Collections.sort(subInfoLists, new Comparator<SubInfoRecord>() {
                @Override
                public int compare(SubInfoRecord arg0, SubInfoRecord arg1) {
                    return arg0.mSlotId - arg1.mSlotId;
                }
            });
        }
        return subInfoLists;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mRequest = isChecked;

        synchronized (mSyncLock) {
            disableOrEnableSIMcard();
        }
        // save the current status information of Switch widget.
        mCurrentStatus = isChecked;
        mCurrentStr = mSubSummary.getText().toString();
    }

    private boolean isAirplaneModeOn() {
        return (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    private void disableOrEnableSIMcard() {
        logd("onClick: " + mRequest);
        if (isAirplaneModeOn()) {
            // do nothing but warning
            logd("airplane is on, show error!");
            showAlertDialogWithMessage(mContext.getString(R.string.sim_enabler_airplane_on));
            return;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            long[] subId = SubscriptionManager.getSubId(i);
            if (TelephonyManager.getDefault().getCallState(subId[0])
                != TelephonyManager.CALL_STATE_IDLE) {
                logd("call state " + i + " is not idle, show error!");
                showAlertDialogWithMessage(mContext.getString(R.string.sim_enabler_in_call));
                return;
            }
        }

        if (!mRequest) {
            if (getActivatedSubInfoCount(mContext) > 1) {
                logd("disable, both are active,can do");
                displayConfirmDialog();
            } else {
                logd("only one is active,can not do");
                displayErrorDialog();
                return;
            }
        } else {
            logd("enable, do it");
            sendCommand(mRequest);
        }

    }

    private void displayConfirmDialog() {
        String message = mContext.getString(R.string.sim_enabler_need_disable_sim);
        // Confirm only one AlertDialog instance to show.
        if (null != mAlertDialog) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, mDialogClickListener)
                .setNegativeButton(android.R.string.no, mDialogClickListener)
                .setOnCancelListener(mDialogCanceListener).create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();

    }

    private void displayErrorDialog() {
        String message = mContext.getString(R.string.sim_enabler_both_inactive);
        // Confirm only one AlertDialog instance to show.
        if (null != mAlertDialog) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok, mDialogClickListener)
                .setOnCancelListener(mDialogCanceListener).create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }

    private void showAlertDialogWithMessage(String msg) {
        if (mParentHandler == null) {
            return;
        }
        mIsShowDialog = true;
        Bundle b = new Bundle();
        b.putInt(SLOTID_STR, mSlotId);
        b.putString(MESSAGE_STR, msg);
        sendMessage(EVT_SHOW_ALERTDIALOG, b);
    }

    private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface
            .OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        sendCommand(mRequest);
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        setChecked(true);
                        mSubSummary.setText(mContext.getString(
                                R.string.sim_enabler_summary,
                                mContext.getString(R.string.sim_enabled)));
                    }
                }
            };

    private DialogInterface.OnCancelListener mDialogCanceListener = new DialogInterface
            .OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    setChecked(true);
                }
            };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
                String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                logd("slotId: " + slotId + " simStatus: " + simStatus);

                if (slotId != mSlotId || mCmdInProgress == false || mParentHandler == null) {
                    return;
                }
                if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(simStatus)
                        || IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(simStatus)) {
                    //SUB is activated
                    if (mRequest) {
                        handleSetUiccDone(mContext.getString(R.string.sub_activate_success));
                    }
                } else if (IccCardConstants.INTENT_VALUE_ICC_UNKNOWN.equals(simStatus)
                        || IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simStatus)){
                    //SUB is Deactivated
                    if (!mRequest) {
                        handleSetUiccDone(mContext.getString(R.string.sub_deactivate_success));
                    }
                }
            }
        }
    };

    private void logd(String msg) {
        if (DBG) Log.d(TAG + "(" + mSlotId + ")", msg);
    }
}
