
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.provider.Settings;

public class MobileDataButton extends WidgetButton {

    public static final String MOBILE_DATA_CHANGED = "com.android.internal.telephony.MOBILE_DATA_CHANGED";

    static MobileDataButton ownButton = null;

    static boolean stateChangeRequest = false;
    static boolean intendedState = false;

    public static boolean getDataRomingEnabled(Context context) {
        return Settings.Secure
                .getInt(context.getContentResolver(), Settings.Secure.DATA_ROAMING, 0) > 0;
    }

    /**
     * Gets the state of data
     *
     * @return true if enabled.
     */
    private static boolean getDataState(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getMobileDataEnabled();
    }

    /**
     * Toggles the state of data.
     */
    @Override
    public void toggleState(Context context) {
        boolean enabled = getDataState(context);

        SharedPreferences preferences = context.getSharedPreferences(
                WidgetSettings.WIDGET_PREF_MAIN, Context.MODE_PRIVATE);

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (enabled) {
            cm.setMobileDataEnabled(false);
            intendedState = false;
            if (preferences.getBoolean(WidgetSettings.AUTO_DISABLE_3G, false)) {
                NetworkModeButton.getInstance().toggleState(context,
                        SettingsAppWidgetProvider.STATE_DISABLED);
            }
        } else {
            if (preferences.getBoolean(WidgetSettings.AUTO_ENABLE_3G, false)
                    && NetworkModeButton.getInstance().isDisabled(context)) {
                SettingsAppWidgetProvider.logD("MobileData: Will enable 3G first");
                NetworkModeButton.getInstance().toggleState(context,
                        SettingsAppWidgetProvider.STATE_ENABLED);
                stateChangeRequest = true;
            } else {
                cm.setMobileDataEnabled(true);
                intendedState = true;
            }
        }
    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        boolean state = getDataState(context);

        if (stateChangeRequest || state != intendedState) {
            currentIcon = R.drawable.ic_appwidget_settings_data_on;
            if (globalPreferences.getBoolean(WidgetSettings.MONITOR_DATA_ROAMING, true)
                    && getDataRomingEnabled(context)) {
                currentState = SettingsAppWidgetProvider.STATE_DISABLED_RED;
            } else {
                currentState = SettingsAppWidgetProvider.STATE_INTERMEDIATE;
            }

        } else if (state) {
            currentIcon = R.drawable.ic_appwidget_settings_data_on;
            if (globalPreferences.getBoolean(WidgetSettings.MONITOR_DATA_ROAMING, true)
                    && getDataRomingEnabled(context)) {
                currentState = SettingsAppWidgetProvider.STATE_ENABLED_RED;
            } else {
                currentState = SettingsAppWidgetProvider.STATE_ENABLED;
            }
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_data_off;
            if (globalPreferences.getBoolean(WidgetSettings.MONITOR_DATA_ROAMING, true)
                    && getDataRomingEnabled(context)) {
                currentState = SettingsAppWidgetProvider.STATE_DISABLED_RED;
            } else {
                currentState = SettingsAppWidgetProvider.STATE_DISABLED;
            }
        }
    }

    public static MobileDataButton getInstance() {
        if (ownButton == null)
            ownButton = new MobileDataButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_DATA;
        preferenceName = WidgetSettings.TOGGLE_DATA;
    }

    public void onReceive(Context context, Intent intent) {
        intendedState = intent.getBooleanExtra("enabled", false);
    }

    public void networkModeChanged(Context context, int networkMode) {
        if (stateChangeRequest) {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.setMobileDataEnabled(true);
            intendedState = true;
            stateChangeRequest = false;
        }
    }

}
