
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

public class AirplaneButton extends WidgetButton {

    static AirplaneButton ownButton = null;

    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        if (getState(context)) {
            currentIcon = R.drawable.ic_appwidget_settings_airplane_on;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_airplane_off;
            currentState = SettingsAppWidgetProvider.STATE_DISABLED;
        }

    }

    /**
     * Toggles the state of Airplane
     *
     * @param context
     */
    public void toggleState(Context context) {
        boolean state = getState(context);
        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
                state ? 0 : 1);
        // notify change
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", !state);
        context.sendBroadcast(intent);
    }

    /**
     * Gets the state of Airplane.
     *
     * @param context
     * @return true if enabled.
     */
    private static boolean getState(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    public static AirplaneButton getInstance() {
        if (ownButton == null)
            ownButton = new AirplaneButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_AIRPLANE;
        preferenceName = WidgetSettings.TOGGLE_AIRPLANE;
    }

}
