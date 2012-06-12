
package com.android.settings.widget.buttons;

import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings.SettingNotFoundException;
import android.widget.Toast;

public class NetworkModeButton extends WidgetButton {

    public static final String NETWORK_MODE_CHANGED = "com.android.internal.telephony.NETWORK_MODE_CHANGED";

    public static final String REQUEST_NETWORK_MODE = "com.android.internal.telephony.REQUEST_NETWORK_MODE";

    public static final String MODIFY_NETWORK_MODE = "com.android.internal.telephony.MODIFY_NETWORK_MODE";

    public static final String NETWORK_MODE = "networkMode";

    private static final int NO_NETWORK_MODE_YET = -99;

    private static final int NETWORK_MODE_UNKNOWN = -100;

    private static final int MODE_3G2G = 0;

    private static final int MODE_3GONLY = 1;

    private static final int MODE_BOTH = 2;

    private static final int DEFAULT_SETTTING = 0;

    static NetworkModeButton ownButton = null;

    private static int networkMode = NO_NETWORK_MODE_YET;

    private static int intendedNetworkMode = NO_NETWORK_MODE_YET;

    private static int currentInternalState = SettingsAppWidgetProvider.STATE_INTERMEDIATE;

    private int currentMode;

    private int networkModeToState(Context context) {
        if (currentInternalState == SettingsAppWidgetProvider.STATE_TURNING_ON
                || currentInternalState == SettingsAppWidgetProvider.STATE_TURNING_OFF)
            return SettingsAppWidgetProvider.STATE_INTERMEDIATE;

        switch (networkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
                return SettingsAppWidgetProvider.STATE_ENABLED;
            case Phone.NT_MODE_GSM_ONLY:
                return SettingsAppWidgetProvider.STATE_DISABLED;
        }
        return SettingsAppWidgetProvider.STATE_INTERMEDIATE;
    }

    /**
     * Gets the state of 2G3g // NOT working
     *
     * @param context
     * @return true if enabled.
     */

    private int get2G3G(Context context) {
        int state = 99;
        try {
            state = android.provider.Settings.Secure.getInt(context.getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE);
        } catch (SettingNotFoundException e) {
            SettingsAppWidgetProvider.logD("Settings not found");
        }
        return state;
    }

    public static NetworkModeButton getInstance() {
        if (ownButton == null)
            ownButton = new NetworkModeButton();
        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_2G3G;
        preferenceName = WidgetSettings.TOGGLE_2G3G;
    }

    private void updateStates(Context context) {
        SharedPreferences globalPreferences = context.getSharedPreferences(
                WidgetSettings.WIDGET_PREF_MAIN, Context.MODE_PRIVATE);

        currentMode = globalPreferences.getInt(WidgetSettings.NETWORK_MODE_SPINNER,
                DEFAULT_SETTTING);
        networkMode = get2G3G(context);
        currentState = networkModeToState(context);
    }

    @Override
    public void toggleState(Context context) {
        toggleState(context, false);
    }

    public void toggleState(Context context, int newState) {
        updateStates(context);
        if (currentState != SettingsAppWidgetProvider.STATE_INTERMEDIATE
                && currentState != newState) {
            toggleState(context, true);
        } else if (currentState == SettingsAppWidgetProvider.STATE_INTERMEDIATE) {
            Toast toast = Toast.makeText(context,
                    "Network mode state unknown. Please change it manually!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void toggleState(Context context, boolean switchModes) {
        SettingsAppWidgetProvider.logD("NetworkMode: toggleState");
        updateStates(context);
        Intent intent = new Intent(MODIFY_NETWORK_MODE);
        switch (networkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_GSM_UMTS:
                intent.putExtra(NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
                currentInternalState = SettingsAppWidgetProvider.STATE_TURNING_OFF;
                intendedNetworkMode = Phone.NT_MODE_GSM_ONLY;
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                if (currentMode == MODE_3GONLY || switchModes) {
                    intent.putExtra(NETWORK_MODE, Phone.NT_MODE_GSM_ONLY);
                    currentInternalState = SettingsAppWidgetProvider.STATE_TURNING_OFF;
                    intendedNetworkMode = Phone.NT_MODE_GSM_ONLY;
                } else {
                    intent.putExtra(NETWORK_MODE, Phone.NT_MODE_WCDMA_PREF);
                    currentInternalState = SettingsAppWidgetProvider.STATE_TURNING_ON;
                    intendedNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                }
                break;
            case Phone.NT_MODE_GSM_ONLY:
                if (currentMode == MODE_3GONLY || currentMode == MODE_BOTH) {
                    intent.putExtra(NETWORK_MODE, Phone.NT_MODE_WCDMA_ONLY);
                    currentInternalState = SettingsAppWidgetProvider.STATE_TURNING_ON;
                    intendedNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                } else {
                    intent.putExtra(NETWORK_MODE, Phone.NT_MODE_WCDMA_PREF);
                    currentInternalState = SettingsAppWidgetProvider.STATE_TURNING_ON;
                    intendedNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                }
                break;
        }

        networkMode = NETWORK_MODE_UNKNOWN;
        context.sendBroadcast(intent);
    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        updateStates(context);
        switch (currentState) {
            case SettingsAppWidgetProvider.STATE_DISABLED:
                currentIcon = R.drawable.ic_appwidget_settings_2g3g_off;
                break;
            case SettingsAppWidgetProvider.STATE_ENABLED:
                if (networkMode == Phone.NT_MODE_WCDMA_ONLY) {
                    currentIcon = R.drawable.ic_appwidget_settings_3g_on;

                } else {
                    currentIcon = R.drawable.ic_appwidget_settings_2g3g_on;
                }
                break;
            case SettingsAppWidgetProvider.STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent. This is much easier to see in
                // sunlight.
                if (currentInternalState == SettingsAppWidgetProvider.STATE_TURNING_ON) {
                    if (intendedNetworkMode == Phone.NT_MODE_WCDMA_ONLY) {
                        currentIcon = R.drawable.ic_appwidget_settings_3g_on;
                    } else {
                        currentIcon = R.drawable.ic_appwidget_settings_2g3g_on;
                    }
                } else {
                    currentIcon = R.drawable.ic_appwidget_settings_2g3g_off;
                }
                break;
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() != null) {
            networkMode = intent.getExtras().getInt(NETWORK_MODE);
            // Update to actual state
            intendedNetworkMode = networkMode;
        }

        // need to clear intermediate states
        currentInternalState = SettingsAppWidgetProvider.STATE_ENABLED;

        int widgetState = networkModeToState(context);
        currentInternalState = widgetState;
        if (widgetState == SettingsAppWidgetProvider.STATE_ENABLED) {
            MobileDataButton.getInstance().networkModeChanged(context, networkMode);
        }
    }

    public boolean isDisabled(Context context) {
        updateStates(context);
        return currentState == SettingsAppWidgetProvider.STATE_DISABLED;
    }

}
