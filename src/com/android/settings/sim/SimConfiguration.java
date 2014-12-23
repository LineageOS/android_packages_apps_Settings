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


package com.android.settings.sim;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.MultiSimSettingTab;
import com.android.settings.R;

import java.util.List;

public class SimConfiguration extends PreferenceActivity implements TextWatcher {
    private static final String LOG_TAG = "SimConfiguration";
    private static final boolean DBG = false;

    private static final String KEY_SIM_NAME = "sim_name_key";
    private static final String KEY_SIM_ICON = "sim_icon_key";
    private static final String KEY_SIM_ENABLER = "sim_enabler_key";
    private static final String KEY_NETWORK_SETTING = "mobile_network_key";
    private static final String KEY_CALL_SETTING = "call_setting_key";
    private static final String KEY_NET_SERVICE_PROVIDER = "net_service_provider_key";

    private static final Uri SYSTEM_SETTINGS_URI = Uri.parse("content://settings/system");

    private int mChannelNameMaxLength;
    //The default legth to dispaly a character
    private static final int CHAR_LEN = 1;

    private ImageListPreference mIconPreference;

    private int mSubscription;

    private EditTextPreference mNamePreference;
    private int mChangeStartPos;
    private int mChangeCount;

    private IntentFilter mIntentFilter = new IntentFilter(
            TelephonyIntents.ACTION_SIM_STATE_CHANGED);

    private class NamePreferenceChangeListener implements Preference.OnPreferenceChangeListener {
        public boolean onPreferenceChange(Preference preference, Object value) {
            logd("onPreferenceChange " + value);
            String multiSimName = (String) value;
            String theOtherSimName = MultiSimSettingTab.getMultiSimName(preference.getContext(),
                    mSubscription == 0 ? 1 : 0);
            if (multiSimName.equals(theOtherSimName)) {
                new AlertDialog.Builder(preference.getContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.same_name_alert)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                return false;
            }

            SubInfoRecord mSubInfoRecord = MultiSimSettingTab.findRecordBySlotId(
                    preference.getContext(), mSubscription);
            if (mSubInfoRecord == null) {
                return false;
            }
            SubscriptionManager.setDisplayName(multiSimName, mSubInfoRecord.subId,
                    SubscriptionManager.NAME_SOURCE_USER_INPUT);

            mNamePreference.setSummary(multiSimName);
            Intent intent = new Intent(MultiSimSettingsConstants.SUBNAME_CHANGED);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubscription);
            sendBroadcast(intent);

            // If the new name equals to default sim name, we delete the newly inserted record from
            // database so that it can display the right name corresponding to the system language.
            // here need a feature judge of FEATURE_SUBSCRIPTION_CARRIER,default value is false
            if (false) {
                if (multiSimName.equals(getString(R.string.slot_name, mSubscription))) {
                    StringBuilder where = new StringBuilder();
                    where.append("name = '" + MultiSimSettingsConstants.MULTI_SIM_NAME +
                            (mSubscription + 1) + "'");
                    getContentResolver().delete(SYSTEM_SETTINGS_URI, where.toString(), null);
                }
            }
            return true;
        }
    }

    private class NamePreferenceClickListener implements Preference.OnPreferenceClickListener {
        public boolean onPreferenceClick(Preference preference) {
            // The dialog should be created by now
            EditText et = mNamePreference.getEditText();
            if (et != null) {
                et.setText(MultiSimSettingTab.getMultiSimName(preference.getContext(),
                        mSubscription));
            }
            return true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.multi_sim_configuration);

        mChannelNameMaxLength = getResources().getInteger(R.integer.sim_name_length);
        Intent intent = getIntent();
        mSubscription = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                PhoneConstants.SUB1);

        mNamePreference = (EditTextPreference) findPreference(KEY_SIM_NAME);
        mNamePreference.setTitle(R.string.title_sim_alias);
        mNamePreference.setSummary(MultiSimSettingTab.getMultiSimName(this, mSubscription));
        mNamePreference.setOnPreferenceChangeListener(new NamePreferenceChangeListener());
        mNamePreference.setOnPreferenceClickListener(new NamePreferenceClickListener());
        EditText et = mNamePreference.getEditText();
        if (et != null) {
            et.addTextChangedListener(this);
        }

        // sim icon preference
        mIconPreference = (ImageListPreference) findPreference(KEY_SIM_ICON);
        mIconPreference.setTitle(R.string.sim_icon_title);
        int iconIndex = getMultiSimIconIndex(mSubscription);
        final TypedArray icons = this.getResources().obtainTypedArray(R.array.sim_icons);
        Log.i(LOG_TAG, "iconIndex=" + iconIndex);
        mIconPreference.setDefaultValue(iconIndex);
        mIconPreference.setIconEntries(icons);
        mIconPreference.setSimIcon(icons.getDrawable(iconIndex));
        mIconPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mIconPreference.setSimIcon(icons.getDrawable(Integer
                        .parseInt((String) newValue)));

                setMultiSimIconIndex(mSubscription, (String) newValue);
                return true;
            }
        });

        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        // if no sim card,no need to modify the sim name and icon
        if (MultiSimSettingTab.findRecordBySlotId(this, mSubscription) == null) {
            getPreferenceScreen().setEnabled(false);
        }
    }

    private int getMultiSimIconIndex(int subscription) {
        String simIconIndex = System.getString(getContentResolver(),
                MultiSimSettingsConstants.PREFERRED_SIM_ICON_INDEX);

        Log.i(LOG_TAG, "simIconIndex=" + simIconIndex);
        if (TextUtils.isEmpty(simIconIndex)) {
            return subscription;
        } else {
            String[] indexs = simIconIndex.split(",");
            if (subscription >= indexs.length) {
                return subscription;
            }
            return Integer.parseInt(indexs[subscription]);
        }
    }

    private void setMultiSimIconIndex(int subscription, String newIndex) {
        String simIconIndex = System.getString(getContentResolver(),
                MultiSimSettingsConstants.PREFERRED_SIM_ICON_INDEX);
        //Init the value if get none
        if (TextUtils.isEmpty(simIconIndex)) {
            simIconIndex = "0,1";
        }

        String[] indexs = simIconIndex.split(",");
        if (subscription >= indexs.length) {
            return;
        }

        StringBuffer sb = new StringBuffer(simIconIndex);
        sb.deleteCharAt(subscription * 2);
        sb.insert(subscription * 2, newIndex);
        Log.i(LOG_TAG, "newStringIndex=" + sb.toString());
        System.putString(getContentResolver(),
                MultiSimSettingsConstants.PREFERRED_SIM_ICON_INDEX, sb.toString());
    }

    protected void onResume() {
        super.onResume();

        // If an icon has been used by another slot, it'll be inactive in the
        // pop.can't use the same icon for two slots.
        int otherSlotIconIndex = getMultiSimIconIndex(mSubscription == 0 ? 1 : 0);
        Log.i(LOG_TAG, "otherSlotIconIndex=" + otherSlotIconIndex);
        if (mIconPreference != null) {
            mIconPreference.setOtherSlotValue(otherSlotIconIndex);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isSubActivated() {
        //take sim state ready as actived state
        return  TelephonyManager.SIM_STATE_ABSENT !=
                TelephonyManager.getDefault().getSimState(mSubscription);
    }

    private boolean isAirplaneModeOn() {
        return (System.getInt(getContentResolver(), System.AIRPLANE_MODE_ON, 0) != 0);
    }

    // TextWatcher interface
    public void afterTextChanged(Editable s) {
        limitTextSize(s.toString().trim());
        Dialog d = mNamePreference.getDialog();
        if (d instanceof AlertDialog) {
            // if user inputed whole space and saved,that is to say SLOT1 and SLOT2 may named blank
            // space, it is meaningless.
            ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(!TextUtils.isEmpty(s.toString().trim()));
        }
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
        EditText et = mNamePreference.getEditText();

        if (et != null) {
            int wholeLen = 0;
            int i = 0;

            for (i = 0; i < textString.length(); i++) {
                wholeLen += getCharacterVisualLength(textString, i);
            }

            // Too many characters,cut off the new added characters
            if (wholeLen > mChannelNameMaxLength) {
                int cutNum = wholeLen - mChannelNameMaxLength;
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
                et.setText(headStr + rearStr);
                // Move cursor to the original position
                et.setSelection(i);
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

    private void logd(String msg) {
        if (DBG) Log.d(LOG_TAG, "[" + LOG_TAG + "(" + mSubscription + ")] " + msg);
    }
}
