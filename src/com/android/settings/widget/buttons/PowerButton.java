
package com.android.settings.widget.buttons;

import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class PowerButton extends WidgetButton {

    private static PowerButton ownButton;

    @Override
    void initButton() {
        // TODO Auto-generated method stub

    }

    @Override
    public void toggleState(Context context) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        // TODO Auto-generated method stub

    }

    public void onReceive(Context context, Intent intent) {
        SharedPreferences globalPreferences = context.getSharedPreferences(
                WidgetSettings.WIDGET_PREF_MAIN, Context.MODE_PRIVATE);

        if (globalPreferences.getBoolean(WidgetSettings.AUTO_ENABLE_BLUETOOTH_WITH_POWER, false)
                && Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            BluetoothButton.getInstance().toggleState(context,
                    SettingsAppWidgetProvider.STATE_ENABLED);
        } else if (globalPreferences.getBoolean(WidgetSettings.AUTO_DISABLE_BLUETOOTH_WITH_POWER,
                false) && Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            BluetoothButton.getInstance().toggleState(context,
                    SettingsAppWidgetProvider.STATE_DISABLED);
        }

        if (globalPreferences.getBoolean(WidgetSettings.AUTO_ENABLE_WIFI_WITH_POWER, false)
                && Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            WifiButton.getInstance().toggleState(context, SettingsAppWidgetProvider.STATE_ENABLED);
        } else if (globalPreferences.getBoolean(WidgetSettings.AUTO_DISABLE_WIFI_WITH_POWER, false)
                && Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            WifiButton.getInstance().toggleState(context, SettingsAppWidgetProvider.STATE_DISABLED);
        }
    }

    public static PowerButton getInstance() {
        if (ownButton == null)
            ownButton = new PowerButton();

        return ownButton;
    }
}
