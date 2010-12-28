
package com.android.settings.widget.buttons;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

public class ScreenTimeoutButton extends WidgetButton {

    public static final int SCREEN_MINIMUM_TIMEOUT = 15000;

    public static final int SCREEN_LOW_TIMEOUT = 30000;

    public static final int SCREEN_NORMAL_TIMEOUT = 60000;

    public static final int SCREEN_HI_TIMEOUT = 120000;

    public static final int SCREEN_MAX_TIMEOUT = 300000;

    private static final int MODE_15_60_300 = 0;

    private static final int MODE_30_120_300 = 1;

    private static final int DEFAULT_SETTTING = 0;

    static ScreenTimeoutButton ownButton = null;

    private int currentMode;

    public static int getScreenTtimeout(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, 0);
    }

    public void toggleState(Context context) {
        int screentimeout = getScreenTtimeout(context);
        if (screentimeout < SCREEN_MINIMUM_TIMEOUT) {
            if (currentMode == MODE_15_60_300) {
                screentimeout = SCREEN_MINIMUM_TIMEOUT;
            } else {
                screentimeout = SCREEN_LOW_TIMEOUT;
            }
        } else if (screentimeout < SCREEN_LOW_TIMEOUT) {
            if (currentMode == MODE_15_60_300) {
                screentimeout = SCREEN_NORMAL_TIMEOUT;
            } else {
                screentimeout = SCREEN_LOW_TIMEOUT;
            }
        } else if (screentimeout < SCREEN_NORMAL_TIMEOUT) {
            if (currentMode == MODE_15_60_300) {
                screentimeout = SCREEN_NORMAL_TIMEOUT;
            } else {
                screentimeout = SCREEN_HI_TIMEOUT;
            }
        } else if (screentimeout < SCREEN_HI_TIMEOUT) {
            if (currentMode == MODE_15_60_300) {
                screentimeout = SCREEN_MAX_TIMEOUT;
            } else {
                screentimeout = SCREEN_HI_TIMEOUT;
            }
        } else if (screentimeout < SCREEN_MAX_TIMEOUT) {
            screentimeout = SCREEN_MAX_TIMEOUT;
        } else if (currentMode == MODE_30_120_300) {
            screentimeout = SCREEN_LOW_TIMEOUT;
        } else {
            screentimeout = SCREEN_MINIMUM_TIMEOUT;
        }
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                screentimeout);
    }

    public static ScreenTimeoutButton getInstance() {
        if (ownButton == null)
            ownButton = new ScreenTimeoutButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_SCREEN_TIMEOUT;
        preferenceName = WidgetSettings.TOGGLE_SCREEN_TIMEOUT;
    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {

        currentMode = globalPreferences.getInt(WidgetSettings.SCREEN_TIMEOUT_SPINNER,
                DEFAULT_SETTTING);

        int timeout = getScreenTtimeout(context);
        // TODO: ADD support for the possible values
        if (timeout <= SCREEN_LOW_TIMEOUT) {
            currentIcon = R.drawable.ic_appwidget_settings_screen_timeout_off;
            currentState = SettingsAppWidgetProvider.STATE_DISABLED;
        } else if (timeout <= SCREEN_HI_TIMEOUT) {
            currentIcon = R.drawable.ic_appwidget_settings_screen_timeout_off;
            currentState = SettingsAppWidgetProvider.STATE_INTERMEDIATE;
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_screen_timeout_on;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;
        }
    }

}
