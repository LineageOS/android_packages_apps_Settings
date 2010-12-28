
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

public class BrightnessButton extends WidgetButton {

    /**
     * Minimum and maximum brightnesses. Don't go to 0 since that makes the
     * display unusable
     */
    private static final int MINIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_DIM + 10;

    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;

    private static int DEFAULT_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.4f);

    private static int LOW_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.25f);

    private static int MID_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.5f);

    private static int HIGH_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.75f);

    private static final int AUTO_BACKLIGHT = -1;

    private static final int MODE_AUTO_MIN_DEF_MAX = 0;

    private static final int MODE_AUTO_MIN_LOW_MID_HIGH_MAX = 1;

    private static final int MODE_AUTO_LOW_MAX = 2;

    private static final int MODE_MIN_MAX = 3;

    private static final int DEFAULT_SETTTING = 0;

    private static Boolean supportsAutomaticMode = null;

    static BrightnessButton ownButton = null;

    private static int currentMode;

    /*
     * Auto_Min_Low_Max Auto_Min . Max Auto. Min . Low High. Max Auto_Low / High
     * Auto_Low / High / Max Min / Max Min Low Max Min Low High Max Low High Low
     * High Max
     */

    public static int getMinBacklight(Context context) {
        /*
         * unimplemented... // if
         * (Settings.System.getInt(context.getContentResolver
         * (),Settings.System.LIGHT_SENSOR_CUSTOM, 0) != 0) { return
         * Settings.System
         * .getInt(context.getContentResolver(),Settings.System.LIGHT_SCREEN_DIM
         * , MINIMUM_BACKLIGHT); } else
         */{
            return MINIMUM_BACKLIGHT;
        }

    }

    private static boolean isAutomaticModeSupported(Context context) {
        if (supportsAutomaticMode == null) {
            if (context.getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available)) {
                supportsAutomaticMode = true;
            } else {
                supportsAutomaticMode = false;
            }
        }
        return supportsAutomaticMode;
    }

    /**
     * Gets state of brightness mode.
     *
     * @param context
     * @return true if auto brightness is on.
     */
    private static boolean isBrightnessSetToAutomatic(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub
                    .asInterface(ServiceManager.getService("power"));
            if (power != null) {
                int brightnessMode = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
                return brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            }
        } catch (Exception e) {
            Log.d(SettingsAppWidgetProvider.TAG, "getBrightnessMode: " + e);
        }
        return false;
    }

    private int getNextBrightnessValue(Context context) {
        int brightness = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);
        /*
         * private static final int MODE_AUTO_MIN_DEF_MAX=0; private static
         * final int MODE_AUTO_MIN_LOW_MID_HIGH_MAX=1; private static final int
         * MODE_AUTO_LOW_MAX=2;
         */
        if (isAutomaticModeSupported(context) && isBrightnessSetToAutomatic(context)) {
            if (currentMode == MODE_AUTO_LOW_MAX) {
                return LOW_BACKLIGHT;
            } else {
                return getMinBacklight(context);
            }
        } else if (brightness < LOW_BACKLIGHT) {
            if (currentMode == MODE_AUTO_LOW_MAX) {
                return LOW_BACKLIGHT;
            } else if (currentMode == MODE_MIN_MAX) {
                return MAXIMUM_BACKLIGHT;
            } else {
                return DEFAULT_BACKLIGHT;
            }
        } else if (brightness < DEFAULT_BACKLIGHT) {
            if (currentMode == MODE_AUTO_MIN_DEF_MAX) {
                return DEFAULT_BACKLIGHT;
            } else if (currentMode == MODE_AUTO_LOW_MAX || currentMode == MODE_MIN_MAX) {
                return MAXIMUM_BACKLIGHT;
            } else {
                return MID_BACKLIGHT;
            }
        } else if (brightness < MID_BACKLIGHT) {
            if (currentMode == MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return MID_BACKLIGHT;
            } else {
                return MAXIMUM_BACKLIGHT;
            }
        } else if (brightness < HIGH_BACKLIGHT) {
            if (currentMode == MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return HIGH_BACKLIGHT;
            } else {
                return MAXIMUM_BACKLIGHT;
            }
        } else if (brightness < MAXIMUM_BACKLIGHT) {
            return MAXIMUM_BACKLIGHT;
        } else if (isAutomaticModeSupported(context) && currentMode != MODE_MIN_MAX) {
            return AUTO_BACKLIGHT;
        } else if (currentMode == MODE_AUTO_LOW_MAX) {
            return LOW_BACKLIGHT;
        } else {
            return getMinBacklight(context);
        }
    }

    /**
     * Increases or decreases the brightness.
     *
     * @param context
     */
    public void toggleState(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub
                    .asInterface(ServiceManager.getService("power"));
            if (power != null) {
                int brightness = getNextBrightnessValue(context);
                ContentResolver contentResolver = context.getContentResolver();
                if (brightness == AUTO_BACKLIGHT) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                } else {
                    if (isAutomaticModeSupported(context)) {
                        Settings.System.putInt(contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    }
                    power.setBacklightBrightness(brightness);
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                            brightness);
                }
            }
        } catch (RemoteException e) {
            Log.d(SettingsAppWidgetProvider.TAG, "toggleBrightness: " + e);
        }
    }

    public static BrightnessButton getInstance() {
        if (ownButton == null)
            ownButton = new BrightnessButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_BRIGHTNESS;
        preferenceName = WidgetSettings.TOGGLE_BRIGHTNESS;
    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {

        currentMode = globalPreferences.getInt(WidgetSettings.BRIGHTNESS_SPINNER, DEFAULT_SETTTING);

        if (isBrightnessSetToAutomatic(context)) {
            currentIcon = R.drawable.ic_appwidget_settings_brightness_auto;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;

        } else if (getBrightnessState(context) == SettingsAppWidgetProvider.STATE_ENABLED) {
            currentIcon = R.drawable.ic_appwidget_settings_brightness_on;
            currentState = SettingsAppWidgetProvider.STATE_ENABLED;
        } else if (getBrightnessState(context) == SettingsAppWidgetProvider.STATE_TURNING_ON) {
            currentIcon = R.drawable.ic_appwidget_settings_brightness_on;
            currentState = SettingsAppWidgetProvider.STATE_INTERMEDIATE;
        } else if (getBrightnessState(context) == SettingsAppWidgetProvider.STATE_TURNING_OFF) {
            currentIcon = R.drawable.ic_appwidget_settings_brightness_off;
            currentState = SettingsAppWidgetProvider.STATE_INTERMEDIATE;
        } else {
            currentIcon = R.drawable.ic_appwidget_settings_brightness_off;
            currentState = SettingsAppWidgetProvider.STATE_DISABLED;
        }
    }

    private int getBrightnessState(Context context) {
        int brightness = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);

        if (brightness < LOW_BACKLIGHT) {
            return SettingsAppWidgetProvider.STATE_DISABLED;
        } else if (brightness < DEFAULT_BACKLIGHT) {
            return SettingsAppWidgetProvider.STATE_DISABLED;
        } else if (brightness < MID_BACKLIGHT) {
            if (currentMode == MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return SettingsAppWidgetProvider.STATE_DISABLED;
            } else {
                return SettingsAppWidgetProvider.STATE_TURNING_OFF;
            }
        } else if (brightness < HIGH_BACKLIGHT) {
            if (currentMode == MODE_AUTO_MIN_LOW_MID_HIGH_MAX) {
                return SettingsAppWidgetProvider.STATE_TURNING_OFF;
            } else {
                return SettingsAppWidgetProvider.STATE_TURNING_ON;
            }
        } else if (brightness < MAXIMUM_BACKLIGHT) {
            return SettingsAppWidgetProvider.STATE_TURNING_ON;
        } else {
            return SettingsAppWidgetProvider.STATE_ENABLED;
        }
    }
}
