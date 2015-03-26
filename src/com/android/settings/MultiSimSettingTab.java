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

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.SelectSubscription;
import com.android.settings.R;

import java.util.List;

public class MultiSimSettingTab extends TabActivity {

    private static final String LOG_TAG = "MultiSimSettingWidget";

    private static final boolean DBG = true;

    private int[] tabIcons = {
            R.drawable.ic_tab_sim1, R.drawable.ic_tab_sim2
    };

    private String[] tabSpecTags = {
            "sim1", "sim2"
    };

    /*
     * Activity class methods
     */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        logd("Creating activity");
        Intent preIntent = getIntent();
        String title = preIntent.getStringExtra("Title");
        setTitle(TextUtils.isEmpty(title) ? getString(R.string.subs_settings) : title);

        setContentView(R.layout.multi_sim_setting_tab);
        // Resource object to get Drawables
        Resources res = getResources();
        // The activity TabHost
        TabHost tabHost = getTabHost();
        // Resusable TabSpec for each tab
        TabHost.TabSpec spec;
        // Reusable Intent for each tab
        Intent intent;

        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            String packageName = preIntent.getStringExtra(SelectSubscription.PACKAGE);
            String className = preIntent.getStringExtra(SelectSubscription.TARGET_CLASS);

            // come in from shortcut packagename and classname is null
            if (packageName == null || className == null) {
                Log.e(LOG_TAG, "Enter into MultiSimSettingTab with null packageName or className");
                return;
            }

            // Create an Intent to launch an Activity for the tab (to be reused)
            intent = new Intent().setClassName(packageName, className)
                    .setAction(preIntent.getAction()).putExtra(PhoneConstants.SUBSCRIPTION_KEY, i);
            // Initialize a TabSpec for each tab and add it to the TabHost
            spec = tabHost.newTabSpec(tabSpecTags[i])
                    .setIndicator(getMultiSimName(this, i),
                    res.getDrawable(tabIcons[i])).setContent(intent);
            // Add new spec to Tab
            tabHost.addTab(spec);
            TextView TempsimName = (TextView) getTabHost().getTabWidget().getChildAt(i)
                    .findViewById(com.android.internal.R.id.title);
            TempsimName.setAllCaps(false);
        }
        tabHost.setCurrentTab(preIntent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                PhoneConstants.SUB1));
    }

    private static void logd(String msg) {
        if (DBG) Log.d(LOG_TAG, msg);
    }

    public static String getMultiSimName(Context context, int subscription) {
        final SubscriptionInfo sir = findRecordBySlotId(context, subscription);

        if (sir != null) {
            return sir.getDisplayName().toString();
        } else {
            return context.getResources().getString(R.string.sim_card_number_title,
                    subscription + 1);
        }
    }

    /**
     * finds a record with slotId.
     * Since the number of SIMs are few, an array is fine.
     */
    public static SubscriptionInfo findRecordBySlotId(Context context, final int slotId) {
        List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();

        if (subInfoList != null){
            final int availableSubInfoLength = subInfoList.size();

            for (int i = 0; i < availableSubInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    //Right now we take the first subscription on a SIM.
                    return sir;
                }
            }
        }
        return null;
    }

    // When user click the home icon we need finish current activity.
    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
