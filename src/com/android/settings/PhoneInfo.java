/*
 * Copyright (c) 2012-2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *    Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *    Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.

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
 */

package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Toast;
import android.telephony.TelephonyManager;
import android.telephony.MSimTelephonyManager;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;

import com.codeaurora.telephony.msim.CardSubscriptionManager;
import com.codeaurora.telephony.msim.MSimPhoneFactory;
import com.codeaurora.telephony.msim.SubscriptionManager;

import java.util.ArrayList;

import android.preference.Preference.OnPreferenceClickListener;

public class PhoneInfo extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "PhoneInfo";
    private static final String TUNE_AWAY = "tune_away";
    static final int EVENT_SET_TUNE_AWAY = 6;
    static final int EVENT_SET_TUNE_AWAY_DONE = 7;

    /* tune away initial/old state */
    private boolean mTuneAwayValue = false;
    private Phone mPhone = null;
    private CheckBoxPreference mTuneAway;
    private CardSubscriptionManager mCardSubscriptionManager =
            CardSubscriptionManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.phone_info);

        mTuneAway = (CheckBoxPreference) findPreference(TUNE_AWAY);
        mTuneAway.setOnPreferenceChangeListener(this);

        mPhone = PhoneFactory.getDefaultPhone();

        MSimTelephonyManager mMSimTelephonyManager =
                (MSimTelephonyManager) getSystemService (Context.MSIM_TELEPHONY_SERVICE);

        boolean mMultiSimFlag = false;

        PreferenceScreen ps = getPreferenceScreen();
        PreferenceManager pm = getPreferenceManager();

        Intent radioInfo = new Intent(PhoneInfo.this, RadioInfo.class);
        PreferenceScreen pstemp = pm.createPreferenceScreen(this);

        pstemp.setTitle(R.string.radio_info);
        pstemp.setIntent(radioInfo);
        ps.addPreference(pstemp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateTuneAwayState() {
        boolean tuneAwayStatus = (Settings.Global.getInt(getContentResolver(),
                Settings.Global.TUNE_AWAY_STATUS, 0) == 1);
        int resId = tuneAwayStatus ? R.string.enable_text
                : R.string.disable_text;

        mTuneAway.setChecked(tuneAwayStatus);
        mTuneAway.setSummary(getResources().getString(resId));
    }

    private void updateTuneAwayStatus() {
        boolean tuneAwayValue = mTuneAway.isChecked();
        mTuneAwayValue = tuneAwayValue;
        Log.d(TAG, " updateTuneAwayStatus change tuneAwayValue to: "
                + tuneAwayValue);
        Message setTuneAwayMsg = Message.obtain(mHandler,
                EVENT_SET_TUNE_AWAY_DONE, null);
        mPhone.setTuneAway(tuneAwayValue, setTuneAwayMsg);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        String status;

        if (TUNE_AWAY.equals(key)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_TUNE_AWAY));
        }
        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch (msg.what) {
            case EVENT_SET_TUNE_AWAY:
                updateTuneAwayStatus();
                break;

            case EVENT_SET_TUNE_AWAY_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Log.e(TAG, "SET_TUNE_AWAY_DONE: returned Exception: "
                            + ar.exception);
                    updateTuneAwayState();
                    break;
                }

                mTuneAway.setChecked(mTuneAwayValue);
                mTuneAway.setSummary(mTuneAwayValue ? R.string.enable_text :
                    R.string.disable_text);
                MSimPhoneFactory.setTuneAway(mTuneAwayValue);
                break;

            default:
                Log.w(TAG, "Unknown Event " + msg.what);
                break;
            }
        }
    };

    private void updateUi() {
        boolean isCardAbsentOrError = false;
        //reset value before using it
        int IccCardCount = 0;

        for (int i = 0; i < SubscriptionManager.NUM_SUBSCRIPTIONS; i++) {
            isCardAbsentOrError = mCardSubscriptionManager.isCardAbsentOrError(i);

            /*Increment count only if card is valid*/
            if (!isCardAbsentOrError) {
                IccCardCount++;
            }
        }
        Log.d(TAG, "IccCardCount = " + IccCardCount);

        if ( (IccCardCount > 1) && (IccCardCount <= SubscriptionManager.NUM_SUBSCRIPTIONS) )  {
            mTuneAway.setEnabled(true);
            mTuneAway.setSelectable(true);
            updateTuneAwayState();
        } else {
            mTuneAway.setEnabled(false);
            mTuneAway.setSelectable(false);
            Log.d(TAG, "Invalid card count");
        }
    }
}
