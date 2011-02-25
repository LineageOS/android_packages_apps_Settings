/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.android.settings.wimax;

import static com.android.wimax.WimaxConstants.CURRENT_WIMAX_ENABLED_STATE;
import static com.android.wimax.WimaxConstants.EXTRA_NETWORK_INFO;
import static com.android.wimax.WimaxConstants.NETWORK_STATE_CHANGED_ACTION;
import static com.android.wimax.WimaxConstants.PREVIOUS_WIMAX_ENABLED_STATE;
import static com.android.wimax.WimaxConstants.WIMAX_ENABLED_CHANGED_ACTION;
import static com.android.wimax.WimaxConstants.WIMAX_ENABLED_STATE_DISABLED;
import static com.android.wimax.WimaxConstants.WIMAX_ENABLED_STATE_DISABLING;
import static com.android.wimax.WimaxConstants.WIMAX_ENABLED_STATE_ENABLED;
import static com.android.wimax.WimaxConstants.WIMAX_ENABLED_STATE_ENABLING;
import static com.android.wimax.WimaxConstants.WIMAX_ENABLED_STATE_UNKNOWN;

import com.android.settings.R;
import com.android.wimax.WimaxSettingsHelper;

import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;


public class WimaxEnabler implements Preference.OnPreferenceChangeListener {

    private static final boolean LOCAL_LOGD = Config.LOGD || WimaxLayer.LOGV;

    private static final String TAG = "SettingsWimaxEnabler";

    private final Context mContext;

    private final CheckBoxPreference mWimaxCheckBoxPref;

    private final CharSequence mOriginalSummary;

    private WimaxLayer mWimaxLayer = null;

    private boolean mIsWimaxPushed = false;

    private final IntentFilter mWimaxStatusFilter;

    private final WimaxSettingsHelper mHelper;

    private final BroadcastReceiver mWimaxStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "WimaxEnabler::onReceive() - intent action = " + intent.getAction());
            if (intent.getAction().equals(WIMAX_ENABLED_CHANGED_ACTION)) {
                handleWimaxStatusChanged(intent.getIntExtra(CURRENT_WIMAX_ENABLED_STATE,
                        WIMAX_ENABLED_STATE_UNKNOWN), intent.getIntExtra(
                        PREVIOUS_WIMAX_ENABLED_STATE, WIMAX_ENABLED_STATE_UNKNOWN));
            } else if (intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION)) {
                handleWimaxStateChanged((NetworkInfo) intent.getParcelableExtra(EXTRA_NETWORK_INFO));
            }
        }
    };

    public WimaxEnabler(Context context, Object wimaxController,
            CheckBoxPreference wimaxCheckBoxPreference) {
        mContext = context;
        mWimaxCheckBoxPref = wimaxCheckBoxPreference;
        mHelper = new WimaxSettingsHelper(context);
        if (mHelper.isWimaxSupported()) {
            mOriginalSummary = wimaxCheckBoxPreference.getSummary();
            wimaxCheckBoxPreference.setPersistent(false);

            mWimaxStatusFilter = new IntentFilter(WIMAX_ENABLED_CHANGED_ACTION);
            mWimaxStatusFilter.addAction(NETWORK_STATE_CHANGED_ACTION);
            // mWimaxStatusFilter.addAction(WimaxController.WXCM_STATE_CHANGED_ACTION);
        } else {
            mWimaxStatusFilter = null;
            mOriginalSummary = null;
        }
    }

    public void resume() {
        if (mHelper.isWimaxSupported()) {
            int status = mHelper.getWimaxState();

            // This is the widget enabled status, not the preference toggled
            // status
            mWimaxCheckBoxPref.setEnabled(status == WIMAX_ENABLED_STATE_ENABLED
                    || status == WIMAX_ENABLED_STATE_DISABLED
                    || status == WIMAX_ENABLED_STATE_UNKNOWN);

            mContext.registerReceiver(mWimaxStatusReceiver, mWimaxStatusFilter);
            mWimaxCheckBoxPref.setOnPreferenceChangeListener(this);
        }
    }

    public void pause() {
        if (mHelper.isWimaxSupported()) {
            mContext.unregisterReceiver(mWimaxStatusReceiver);
            mWimaxCheckBoxPref.setOnPreferenceChangeListener(null);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn on/off Wimax
        setWimaxState((Boolean) value);

        // Don't update UI to opposite status until we're sure
        return false;
    }

    private void setWimaxState(final boolean enable) {
        // Disable button
        mWimaxCheckBoxPref.setEnabled(false);
        boolean tmp = mHelper.setWimaxEnabled(enable);
        if (enable && !mIsWimaxPushed) {
            Log.d(TAG, "Setting isWimaxPushed = true");
            mIsWimaxPushed = true;
            SystemProperties.set("wimax.dualmode.1xrtt", "0");
            SystemProperties.set("wimax.wifi.disable", "1");
            // SystemProperties.set("wimax.restart_service", "1");
            // SystemProperties.set("wimax.dual.wa","1");
            // SystemProperties.set("wimax.dualmode.disconnect","1");
        } else if (enable) {
            // Log.d(TAG, "calling connectToDcs();");
            // mWimaxController.connectToDcs();
        } else if (!enable) {
            ((StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE))
                    .setIconVisibility("wimax", false);
        }
        Log.d(TAG, "WimaxEnabler::setWimaxEnabled(" + enable + ") - call returned " + tmp);
        if (!tmp) {
            mWimaxCheckBoxPref.setSummary(enable ? R.string.error_wimax_starting
                    : R.string.error_wimax_stopping);
        }
    }

    public void setWimaxLayer(WimaxLayer wimaxLayer) {
        mWimaxLayer = wimaxLayer;
    }

    private void handleWimaxStatusChanged(int wimaxStatus, int previousWimaxStatus) {

        if (LOCAL_LOGD) {
            Log.d(TAG, "Received wimax status changed from "
                    + getHumanReadableWimaxStatus(previousWimaxStatus) + " to "
                    + getHumanReadableWimaxStatus(wimaxStatus));
        }

        if (wimaxStatus == WIMAX_ENABLED_STATE_DISABLED
                || wimaxStatus == WIMAX_ENABLED_STATE_ENABLED) {
            boolean wimaxEnabled = (wimaxStatus == WIMAX_ENABLED_STATE_ENABLED);
            mWimaxCheckBoxPref.setChecked(wimaxEnabled);
            mWimaxCheckBoxPref
                    .setSummary(wimaxStatus == WIMAX_ENABLED_STATE_DISABLED ? mOriginalSummary
                            : null);

            mWimaxCheckBoxPref.setEnabled(isEnabledByDependency());

            if (wimaxStatus == WIMAX_ENABLED_STATE_DISABLED) {
                // mWimaxController.disconnectFromDcs();
                // ConnectivityManager cManager =
                // (ConnectivityManager)this.mContext.getSystemService("connectivity");
                // cManager.resetWimaxService();
            }

        } else if (wimaxStatus == WIMAX_ENABLED_STATE_DISABLING
                || wimaxStatus == WIMAX_ENABLED_STATE_ENABLING) {
            mWimaxCheckBoxPref
                    .setSummary(wimaxStatus == WIMAX_ENABLED_STATE_ENABLING ? R.string.wimax_starting
                            : R.string.wimax_stopping);

        } else if (wimaxStatus == WIMAX_ENABLED_STATE_UNKNOWN) {
            int message = R.string.wimax_error;
            if (previousWimaxStatus == WIMAX_ENABLED_STATE_ENABLING)
                message = R.string.error_wimax_starting;
            else if (previousWimaxStatus == WIMAX_ENABLED_STATE_DISABLING)
                message = R.string.error_wimax_stopping;

            mWimaxCheckBoxPref.setChecked(false);
            mWimaxCheckBoxPref.setSummary(message);
            mWimaxCheckBoxPref.setEnabled(true);
        }
    }

    private void handleWimaxStateChanged(NetworkInfo info) {

        DetailedState state = info.getDetailedState();

        if (LOCAL_LOGD) {
            Log.d(TAG, "Received wimax state changed to " + state);
        }

        if (mHelper.isWimaxEnabled()) {
            mWimaxCheckBoxPref.setSummary(getPrintableSummary(state));
        }
    }

    private boolean isEnabledByDependency() {
        Preference dep = getDependencyPreference();
        if (dep == null) {
            return true;
        }

        return !dep.shouldDisableDependents();
    }

    private Preference getDependencyPreference() {
        String depKey = mWimaxCheckBoxPref.getDependency();
        if (TextUtils.isEmpty(depKey)) {
            return null;
        }

        return mWimaxCheckBoxPref.getPreferenceManager().findPreference(depKey);
    }

    private static String getHumanReadableWimaxStatus(int wimaxStatus) {
        switch (wimaxStatus) {
            case WIMAX_ENABLED_STATE_DISABLED:
                return "Disabled";
            case WIMAX_ENABLED_STATE_DISABLING:
                return "Disabling";
            case WIMAX_ENABLED_STATE_ENABLED:
                return "Enabled";
            case WIMAX_ENABLED_STATE_ENABLING:
                return "Enabling";
            case WIMAX_ENABLED_STATE_UNKNOWN:
                return "Unknown";
            default:
                return "Some other state!";
        }
    }

    private String getPrintableSummary(DetailedState wimaxState) {
        String summary = null;
        if (wimaxState == DetailedState.SCANNING) {
            summary = mContext.getString(R.string.wimax_status_scanning);
        } else if (wimaxState == DetailedState.CONNECTING) {
            String nspName = mWimaxLayer != null ? mWimaxLayer.getNspToConnect() : null;
            if (nspName == null || "".equals(nspName))
                summary = mContext.getString(R.string.wimax_status_connecting);
            else
                summary = String.format(mContext.getString(R.string.wimax_status_connecting_to),
                        nspName);
        }
        if (wimaxState == DetailedState.CONNECTED) {
            String nspName = mWimaxLayer != null ? mWimaxLayer.getCurrentNspName() : null;
            if (nspName == null || "".equals(nspName))
                summary = mContext.getString(R.string.wimax_status_connected);
            else
                summary = String.format(mContext.getString(R.string.wimax_status_connected_to),
                        nspName);
        }
        if (wimaxState == DetailedState.DISCONNECTING) {
            String nspName = mWimaxLayer != null ? mWimaxLayer.getCurrentNspName() : null;
            if (nspName == null || "".equals(nspName))
                summary = mContext.getString(R.string.wimax_status_disconnecting);
            else
                summary = String.format(
                        mContext.getString(R.string.wimax_status_disconnecting_from), nspName);
        }
        if (wimaxState == DetailedState.DISCONNECTED) {
            summary = mContext.getString(R.string.wimax_status_disconnected);
        }

        return summary;
    }

}
