
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.StateTracker;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class WifiApButton extends WidgetButton {

    static WifiApButton ownButton = null;

    private static final StateTracker sWifiApState = new WifiApStateTracker();

    /**
     * Subclass of StateTracker to get/set Wifi AP state.
     */
    private static final class WifiApStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiApStateToFiveState(wifiManager.getWifiApState());
            }
            return SettingsAppWidgetProvider.STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {

            final WifiManager wifiManager = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(SettingsAppWidgetProvider.TAG, "No wifiManager.");
                return;
            }

            // Actually request the Wi-Fi AP change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable Wif if enabling tethering
                     */
                    int wifiState = wifiManager.getWifiState();
                    if (desiredState
                            && ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
                        wifiManager.setWifiEnabled(false);
                    }

                    wifiManager.setWifiApEnabled(null, desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {

            if (!WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, -1);
            int widgetState = wifiApStateToFiveState(wifiState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WifiManager's state values into our
         * Wifi/WifiAP/Bluetooth-common state values.
         */
        private static int wifiApStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_AP_STATE_DISABLED:
                    return SettingsAppWidgetProvider.STATE_DISABLED;
                case WifiManager.WIFI_AP_STATE_ENABLED:
                    return SettingsAppWidgetProvider.STATE_ENABLED;
                case WifiManager.WIFI_AP_STATE_DISABLING:
                    return SettingsAppWidgetProvider.STATE_TURNING_OFF;
                case WifiManager.WIFI_AP_STATE_ENABLING:
                    return SettingsAppWidgetProvider.STATE_TURNING_ON;
                default:
                    return SettingsAppWidgetProvider.STATE_UNKNOWN;
            }
        }
    }

    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {

        currentState = sWifiApState.getTriState(context);
        switch (currentState) {
            case SettingsAppWidgetProvider.STATE_DISABLED:
                currentIcon = R.drawable.ic_appwidget_settings_wifi_ap_off;
                break;
            case SettingsAppWidgetProvider.STATE_ENABLED:
                currentIcon = R.drawable.ic_appwidget_settings_wifi_ap_on;
                break;
            case SettingsAppWidgetProvider.STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent. This is much easier to see in
                // sunlight.
                if (sWifiApState.isTurningOn()) {
                    currentIcon = R.drawable.ic_appwidget_settings_wifi_ap_on;
                } else {
                    currentIcon = R.drawable.ic_appwidget_settings_wifi_ap_off;
                }
                break;
        }
    }

    public void onReceive(Context context, Intent intent) {
        sWifiApState.onActualStateChange(context, intent);
    }

    public void toggleState(Context context) {
        sWifiApState.toggleState(context);
    }

    public static WifiApButton getInstance() {
        if (ownButton == null) {
            ownButton = new WifiApButton();
        }

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_WIFI_AP;
        preferenceName = WidgetSettings.TOGGLE_WIFI_AP;
    }

}
