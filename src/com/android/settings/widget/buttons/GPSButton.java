
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.provider.Settings;

public class GPSButton extends WidgetButton {

    static GPSButton ownButton = null;

    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        if (getGpsState(context)) {
            currentIcon = R.drawable.ic_appwidget_settings_gps_on;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_gps_off;
            currentState = SettingsAppWidgetProvider.STATE_DISABLED;
        }

    }

    /**
     * Toggles the state of GPS.
     *
     * @param context
     */
    public void toggleState(Context context) {
        ContentResolver resolver = context.getContentResolver();
        boolean enabled = getGpsState(context);
        Settings.Secure
                .setLocationProviderEnabled(resolver, LocationManager.GPS_PROVIDER, !enabled);
    }

    /**
     * Gets the state of GPS location.
     *
     * @param context
     * @return true if enabled.
     */
    private static boolean getGpsState(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.Secure.isLocationProviderEnabled(resolver, LocationManager.GPS_PROVIDER);
    }

    public static GPSButton getInstance() {
        if (ownButton == null)
            ownButton = new GPSButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_GPS;
        preferenceName = WidgetSettings.TOGGLE_GPS;
    }

}
