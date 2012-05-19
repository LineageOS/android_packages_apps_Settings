
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.FlashlightActivity;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.List;

public class FlashlightButton extends WidgetButton {

    static FlashlightButton ownButton = null;

    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        if (getFlashlightEnabled(context)) {
            currentIcon = R.drawable.ic_appwidget_settings_flashlight_on;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_flashlight_off;
            currentState = SettingsAppWidgetProvider.STATE_DISABLED;
        }
    }

    public void toggleState(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> l = pm.queryBroadcastReceivers(new Intent(
                "net.cactii.flash2.TOGGLE_FLASHLIGHT"), 0);
        if (!l.isEmpty()) {
            context.sendBroadcast(new Intent("net.cactii.flash2.TOGGLE_FLASHLIGHT"));
        } else {
            Intent intent = new Intent(context, FlashlightActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static FlashlightButton getInstance() {
        if (ownButton == null)
            ownButton = new FlashlightButton();
        return ownButton;
    }

    public boolean getFlashlightEnabled(Context context) {
        return Settings.System
                .getInt(context.getContentResolver(), Settings.System.TORCH_STATE, 0) == 1;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_FLASHLIGHT;
        preferenceName = WidgetSettings.TOGGLE_FLASHLIGHT;
    }

}
