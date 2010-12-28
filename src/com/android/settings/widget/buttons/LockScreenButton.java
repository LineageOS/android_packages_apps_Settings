
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.Toast;

public class LockScreenButton extends WidgetButton {

    static Boolean lockScreen = null;

    public static final String LOCK_SCREEN = "lockScreen";

    static LockScreenButton ownButton = null;

    KeyguardLock lock;

    private KeyguardLock getLock(Context context) {
        if (lock == null) {
            KeyguardManager keyguardManager = (KeyguardManager) context
                    .getSystemService(Activity.KEYGUARD_SERVICE);
            lock = keyguardManager.newKeyguardLock(Context.KEYGUARD_SERVICE);
        }
        return lock;
    }

    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        getState(context);
        if (lockScreen == null) {
            currentIcon = R.drawable.ic_appwidget_settings_lock_screen_off;
            currentState = SettingsAppWidgetProvider.STATE_INTERMEDIATE;
        } else if (lockScreen) {
            currentIcon = R.drawable.ic_appwidget_settings_lock_screen_on;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_lock_screen_off;
            currentState = SettingsAppWidgetProvider.STATE_DISABLED;
        }
    }

    /**
     * Toggles the state of GPS.
     *
     * @param context
     */
    public void toggleState(Context context) {
        getState(context);
        if (lockScreen == null) {
            Toast msg = Toast.makeText(context, "Not yet initialized", Toast.LENGTH_LONG);
            msg.setGravity(Gravity.CENTER, msg.getXOffset() / 2, msg.getYOffset() / 2);
            msg.show();
        } else {
            getLock(context);
            if (lockScreen && lock != null) {
                lock.disableKeyguard();
                lockScreen = false;
                // writeState(context);
            } else if (lock != null) {
                lock.reenableKeyguard();
                lockScreen = true;
                // writeState(context);
            }
        }
    }

    /**
     * Gets the state of GPS location.
     *
     * @param context
     * @return true if enabled.
     */
    private static boolean getState(Context context) {
        if (lockScreen == null) {
            /*
             * SharedPreferences preferencesGeneral =
             * context.getSharedPreferences
             * (WidgetSettings.WIDGET_PREF_MAIN,Context.MODE_PRIVATE); if
             * (preferencesGeneral.getInt(WidgetSettings.SAVED,
             * SettingsAppWidgetProvider
             * .WIDGET_NOT_CONFIGURED)==SettingsAppWidgetProvider.WIDGET_PRESENT
             * ) { lockScreen=preferencesGeneral.getBoolean(LOCK_SCREEN, true);
             * } else { return false; }
             */
            lockScreen = true;
        }
        return lockScreen;
    }

    public static LockScreenButton getInstance() {
        if (ownButton == null)
            ownButton = new LockScreenButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_LOCK_SCREEN;
        preferenceName = WidgetSettings.TOGGLE_LOCK_SCREEN;
    }

}
