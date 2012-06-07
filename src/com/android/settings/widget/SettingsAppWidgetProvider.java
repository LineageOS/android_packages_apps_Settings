/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.widget;

import com.android.settings.R;
import com.android.settings.SecuritySettings;
import com.android.settings.widget.buttons.AirplaneButton;
import com.android.settings.widget.buttons.AutoRotateButton;
import com.android.settings.widget.buttons.BluetoothButton;
import com.android.settings.widget.buttons.BrightnessButton;
import com.android.settings.widget.buttons.FlashlightButton;
import com.android.settings.widget.buttons.GPSButton;
import com.android.settings.widget.buttons.LockScreenButton;
import com.android.settings.widget.buttons.MobileDataButton;
import com.android.settings.widget.buttons.NetworkModeButton;
import com.android.settings.widget.buttons.PowerButton;
import com.android.settings.widget.buttons.ScreenTimeoutButton;
import com.android.settings.widget.buttons.SoundButton;
import com.android.settings.widget.buttons.SyncButton;
import com.android.settings.widget.buttons.WidgetButton;
import com.android.settings.widget.buttons.WifiApButton;
import com.android.settings.widget.buttons.WifiButton;
import com.android.settings.widget.buttons.WimaxButton;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Provides control of power-related settings from a widget.
 */
public class SettingsAppWidgetProvider extends AppWidgetProvider {

    public static final int STATE_DISABLED = 0;

    public static final int STATE_ENABLED = 1;

    public static final int STATE_TURNING_ON = 2;

    public static final int STATE_TURNING_OFF = 3;

    public static final int STATE_UNKNOWN = 4;

    public static final int STATE_INTERMEDIATE = 5;

    public static final int STATE_DISABLED_RED = 10;

    public static final int STATE_ENABLED_RED = 11;

    public static final String TAG = "SettingsAppWidgetProvider";

    static final ComponentName THIS_APPWIDGET = new ComponentName("com.android.settings",
            "com.android.settings.widget.SettingsAppWidgetProvider");

    private static final boolean DEBUG = false;

    // New version that allows moving button position. Increase the
    // WIDGET_PRESENT "version"
    public static final int WIDGET_PRESENT = 3;

    public static final int WIDGET_NOT_CONFIGURED = 1;

    public static final int WIDGET_DELETED = 0;

    @Override
    public void onEnabled(Context context) {
        logD("Received request to enable first widget");
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName("com.android.settings",
                ".widget.SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onDisabled(Context context) {
        logD("Received request to remove last widget");
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName("com.android.settings",
                ".widget.SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        logD("Received request to remove a widget");
        for (int appWidgetId : appWidgetIds) {
            SharedPreferences widgetPreferences = context.getSharedPreferences(
                    WidgetSettings.WIDGET_PREF_NAME + appWidgetId, Context.MODE_PRIVATE);
            Editor editor = widgetPreferences.edit();
            editor.clear();
            editor.putInt(WidgetSettings.SAVED, WIDGET_DELETED);
            editor.commit();
        }
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        logD(">> updateWidget IN");
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgets = manager.getAppWidgetIds(THIS_APPWIDGET);
        buildUpdate(context, manager, widgets);
        logD("<< updateWidget OUT");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        logD(">> onUpdate IN");
        buildUpdate(context, appWidgetManager, appWidgetIds);
        logD("<< onUpdate OUT");
    }

    /**
     * update main method. Process all widget. Update state and process all
     * widget instances.
     */
    static void buildUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        logD(">> buildUpdate IN");

        SharedPreferences globalPreferences = context.getSharedPreferences(
                WidgetSettings.WIDGET_PREF_MAIN, Context.MODE_PRIVATE);

        // If never initialized before. Populate the widget with defaults
        if (!globalPreferences.contains(WidgetSettings.SAVED)) {
            Log.d(TAG, "Widget is from a previous version... Let's update");
            // It should be just one when the system is clean... but we never
            // now. So let's process all
            // We shall get them from the system since the update received can
            // be for a single one.
            int[] widgets = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
            for (int widget : widgets) {
                Log.d(TAG, "Will set widget " + widget + " to default settings");
                SharedPreferences widgetPreferences = context.getSharedPreferences(
                        WidgetSettings.WIDGET_PREF_NAME + widget, Context.MODE_PRIVATE);
                WidgetSettings.initDefaultWidget(widgetPreferences);
            }

            if (widgets.length > 0) {
                WidgetSettings.initDefaultSettings(globalPreferences);
            } else {
                Log.d(TAG,
                        "No instances yet... Wait for at least one instance to exist before adding global settings");
            }
        }
        // Query for current status of multiple options. Only to be done once
        updateStates(context, globalPreferences, appWidgetIds);

        // Now process all widgets instances
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "Call buildUpdate for widget:" + appWidgetId);
            RemoteViews views = updateViews(context, globalPreferences, appWidgetId);
            if (views != null) {
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
            Log.d(TAG, "buildUpdate done for widget:" + appWidgetId);
        }
        logD("<< buildUpdate OUT");

    }

    /**
     * Method that will trigger the update for all options
     */
    private static void updateStates(Context context, SharedPreferences globalPreferences,
            int[] appWidgetIds) {
        WifiButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        WifiApButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        BluetoothButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        GPSButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        MobileDataButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        NetworkModeButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        SyncButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        SoundButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        AutoRotateButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        ScreenTimeoutButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        AirplaneButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        LockScreenButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        FlashlightButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        BrightnessButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
        WimaxButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
    }

    /**
     * Update of each widget instance. will check it's configuration
     */
    private static RemoteViews updateViews(Context context, SharedPreferences globalPreferences,
            int appWidgetId) {
        logD(">> updateViews IN - Widget:" + appWidgetId);
        SharedPreferences widgetPreferences = context.getSharedPreferences(
                WidgetSettings.WIDGET_PREF_NAME + appWidgetId, Context.MODE_PRIVATE);

        // If not configured do not refresh. Needed due to:
        // 1: Ghost instances of the launcher
        // 2: Older version no longer compatible. So widget_present was
        // increased
        if (widgetPreferences.getInt(WidgetSettings.SAVED, WIDGET_NOT_CONFIGURED) == WIDGET_PRESENT) {

            // Set the selected background. On a widget it can only be done by a
            // diferent layout.

            int widgetLayout = R.layout.widget;
            int backgroundLayout = widgetPreferences.getInt(WidgetSettings.BACKGROUND_IMAGE, 0);
            boolean verticalLayout = widgetPreferences.getBoolean(WidgetSettings.USE_VERTICAL,
                    false);
            if (!verticalLayout && backgroundLayout == WidgetSettings.TRANSPARENT_BACKGROUND) {
                widgetLayout = R.layout.widget_transparent;
            } else if (verticalLayout && backgroundLayout == WidgetSettings.TRANSPARENT_BACKGROUND) {
                widgetLayout = R.layout.widget_vertical_transparent;
                /*
                 * }else if (verticalLayout &&
                 * backgroundLayout==WidgetSettings.WHITE_BACKGROUND) {
                 * widgetLayout = R.layout.widget_white_vertical; }else if
                 * (!verticalLayout &&
                 * backgroundLayout==WidgetSettings.WHITE_BACKGROUND) {
                 * widgetLayout = R.layout.widget_white;
                 */
            } else if (verticalLayout) {
                widgetLayout = R.layout.widget_vertical;
            }

            // create the new remote views
            RemoteViews views = new RemoteViews(context.getPackageName(), widgetLayout);

            // Now call each button to update it's state on this instance.
            WifiButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            WifiApButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            BluetoothButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            GPSButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            MobileDataButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            NetworkModeButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            SyncButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            SoundButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            AutoRotateButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            ScreenTimeoutButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            BrightnessButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            FlashlightButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            LockScreenButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            AirplaneButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);
            WimaxButton.getInstance().updateView(context, views, globalPreferences,
                    widgetPreferences, appWidgetId);

            return views;
        } else {
            // Return the layout with widget configuration message.
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_configure);

            logD(">> updateViews IN - Widget:" + appWidgetId
                    + " no longer present or not configured");
            return views;
        }
    }

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        logD(">> onReceive IN");
        super.onReceive(context, intent);
        if ("net.cactii.flash2.TORCH_STATE_CHANGED".equals(intent.getAction())) {
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            logD("Received Wifi state change");
            WifiButton.getInstance().onReceive(context, intent);
        } else if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            logD("Received Wifi AP state change");
            WifiApButton.getInstance().onReceive(context, intent);
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            logD("Received Bluetooth state change");
            BluetoothButton.getInstance().onReceive(context, intent);
        } else if (NetworkModeButton.NETWORK_MODE_CHANGED.equals(intent.getAction())) {
            logD("Received Network mode state change");
            NetworkModeButton.getInstance().onReceive(context, intent);
        } else if (WimaxManagerConstants.WIMAX_ENABLED_STATUS_CHANGED.equals(intent.getAction())
                || WimaxManagerConstants.WIMAX_ENABLED_CHANGED_ACTION.equals(intent.getAction())) {
            logD("Received WiMAX change request");
            WimaxButton.getInstance().onReceive(context, intent);
        } else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())
                || Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            logD("Received power mode state change");
            PowerButton.getInstance().onReceive(context, intent);
        } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if (buttonId == WidgetButton.BUTTON_WIFI) {
                logD("Received wifi button change request");
                WifiButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_WIFI_AP) {
                logD("Received wifi ap button change request");
                WifiApButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_BLUETOOTH) {
                logD("Received bluetooth button change request");
                BluetoothButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_GPS) {
                logD("Received GPS button change request");
                GPSButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_DATA) {
                logD("Received mobile data button change request");
                MobileDataButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_2G3G) {
                logD("Received network mode button change request");
                NetworkModeButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_SYNC) {
                logD("Received sync button change request");
                SyncButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_SOUND) {
                logD("Received sound button change request");
                SoundButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_SCREEN_TIMEOUT) {
                logD("Received screen timeout change request");
                ScreenTimeoutButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_AUTO_ROTATE) {
                logD("Received auto rotate change request");
                AutoRotateButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_BRIGHTNESS) {
                logD("Received brightness change request");
                BrightnessButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_FLASHLIGHT) {
                logD("Received flahslight change request");
                FlashlightButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_LOCK_SCREEN) {
                logD("Received Lock Screen change request");
                LockScreenButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_AIRPLANE) {
                logD("Received airplane change request");
                AirplaneButton.getInstance().toggleState(context);
            } else if (buttonId == WidgetButton.BUTTON_WIMAX) {
                logD("Received WiMAX change request");
                WimaxButton.getInstance().toggleState(context);
            }
        } else if (MobileDataButton.MOBILE_DATA_CHANGED.equals(intent.getAction())) {
            logD("Received mobile data mode state change");
            MobileDataButton.getInstance().onReceive(context, intent);
        } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())
                || SecuritySettings.GPS_STATUS_CHANGED.equals(intent.getAction())) {
        } else {
            logD("Ignoring Action: " + intent.getAction());
            // Don't fall-through to updating the widget. The Intent
            // was something unrelated or that our super class took
            // care of.
            logD("<< onReceive OUT");
            return;
        }
        updateWidget(context);
        // State changes fall through
        logD("<< onReceive OUT");

    }

    public static void logD(String message) {
        if (DEBUG)
            Log.d(TAG, message);

    }
}
