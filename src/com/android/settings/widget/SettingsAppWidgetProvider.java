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
import android.util.Log;
import android.widget.RemoteViews;

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
import com.android.settings.widget.buttons.ScreenTimeoutButton;
import com.android.settings.widget.buttons.SoundButton;
import com.android.settings.widget.buttons.SyncButton;
import com.android.settings.widget.buttons.WidgetButton;
import com.android.settings.widget.buttons.WifiButton;
import com.android.settings.widget.buttons.WifiApButton;


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

	static final ComponentName THIS_APPWIDGET = new ComponentName(
			"com.android.settings",
			"com.android.settings.widget.SettingsAppWidgetProvider");

	private static final boolean DEBUG=true;

	public static final int WIDGET_PRESENT = 2;
	public static final int WIDGET_NOT_CONFIGURED = 1;
	public static final int WIDGET_DELETED = 0;
		
	
	@Override
	public void onEnabled(Context context) {
		logD("Received request to enable first widget");
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName("com.android.settings",
				".widget.SettingsAppWidgetProvider"),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onDisabled(Context context) {
		logD("Received request to remove last widget");
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName("com.android.settings",
				".widget.SettingsAppWidgetProvider"),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		logD("Received request to remove a widget");
		for (int appWidgetId:appWidgetIds) {
			SharedPreferences widgetPreferences = context.getSharedPreferences(WidgetSettings.WIDGET_PREF_NAME+appWidgetId,
					Context.MODE_PRIVATE);
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
	public static void updateWidget(Context context, Integer button) {
		logD(">> updateWidget IN");
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		int[] widgets = manager.getAppWidgetIds(THIS_APPWIDGET);
		buildUpdate(context, manager, widgets);
		logD("<< updateWidget OUT");
	}


	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {		
		logD(">> onUpdate IN");
		buildUpdate(context, appWidgetManager, appWidgetIds);
		logD("<< onUpdate OUT");
	}

	
	
	/**
	 * Load image for given widget and build {@link RemoteViews} for it.
	 */
	static void buildUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		logD(">> buildUpdate IN");

		SharedPreferences globalPreferences = context.getSharedPreferences(WidgetSettings.WIDGET_PREF_MAIN,Context.MODE_PRIVATE);

		//Query for current status of multiple options
		updateStates(context, globalPreferences, appWidgetIds);
		
		for (int appWidgetId:appWidgetIds) {
			Log.d(TAG,"Call buildUpdate for widget:"+appWidgetId);
			RemoteViews views = updateViews(context, globalPreferences, appWidgetId);
			if (views!=null) {
				appWidgetManager.updateAppWidget(appWidgetId, views);
			}
			Log.d(TAG,"buildUpdate done for widget:"+appWidgetId);
		}		
		logD("<< buildUpdate OUT");

	}

	private static void updateStates(Context context, SharedPreferences globalPreferences,  int[] appWidgetIds) {
		WifiButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		WifiApButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		BluetoothButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		GPSButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		MobileDataButton.getInstance().updateState(context, globalPreferences, appWidgetIds);;
		NetworkModeButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		SyncButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		SoundButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		AutoRotateButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		ScreenTimeoutButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		AirplaneButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		LockScreenButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		FlashlightButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
		BrightnessButton.getInstance().updateState(context, globalPreferences, appWidgetIds);
	}

	private static RemoteViews updateViews(Context context, SharedPreferences globalPreferences, int appWidgetId) {
		logD(">> updateViews IN - Widget:"+appWidgetId);
		SharedPreferences widgetPreferences = context.getSharedPreferences(WidgetSettings.WIDGET_PREF_NAME+appWidgetId,
				Context.MODE_PRIVATE);

		
		if(widgetPreferences.contains(WidgetSettings.TOGGLE_WIFI)) {
			int widgetLayout = R.layout.widget;
			boolean transparentLayout= widgetPreferences.getBoolean(WidgetSettings.USE_TRANSPARENT, false);
			boolean verticalLayout = widgetPreferences.getBoolean(WidgetSettings.USE_VERTICAL, false);
			if (!verticalLayout && transparentLayout) {
				widgetLayout = R.layout.widget_transparent;
			}else if (verticalLayout && transparentLayout) {
				widgetLayout = R.layout.widget_vertical_transparent;
			}else if (verticalLayout) {
				widgetLayout = R.layout.widget_vertical;
			}		
			
			// create the new remote views
			RemoteViews views = new RemoteViews(context.getPackageName(),
					widgetLayout);
			
			
				WifiButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				WifiApButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				BluetoothButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				GPSButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				MobileDataButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				NetworkModeButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				SyncButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				SoundButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				AutoRotateButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				ScreenTimeoutButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				BrightnessButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				FlashlightButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				LockScreenButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);
				AirplaneButton.getInstance().updateView(context, views, globalPreferences, widgetPreferences, appWidgetId);

			return views;
		} else {
			logD(">> updateViews IN - Widget:"+appWidgetId+" no longer present or not configured");
			return null;
		}
	}

	
	/**
	 * Receives and processes a button pressed intent or state change.
	 * 
	 * @param context
	 * @param intent
	 *            Indicates the pressed button.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		logD(">> onReceive IN");		
		super.onReceive(context, intent);
		if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			WifiButton.getInstance().onReceive(context, intent);
			updateWidget(context,WidgetButton.BUTTON_WIFI);
		} else if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			WifiApButton.getInstance().onReceive(context, intent);
			updateWidget(context,WidgetButton.BUTTON_WIFI_AP);
		} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
			BluetoothButton.getInstance().onReceive(context, intent);
			updateWidget(context,WidgetButton.BUTTON_BLUETOOTH);
		} else if (NetworkModeButton.NETWORK_MODE_CHANGED.equals(intent.getAction())) {
			NetworkModeButton.getInstance().onReceive(context, intent);
			updateWidget(context,WidgetButton.BUTTON_2G3G);
		} else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
			Uri data = intent.getData();
			int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
			if (buttonId == WidgetButton.BUTTON_WIFI) {
				WifiButton.getInstance().toggleState(context);
				updateWidget(context,WidgetButton.BUTTON_WIFI);
			} else if (buttonId == WidgetButton.BUTTON_WIFI_AP) {
				WifiApButton.getInstance().toggleState(context);
				updateWidget(context,WidgetButton.BUTTON_WIFI_AP);
			} else if (buttonId == WidgetButton.BUTTON_BLUETOOTH) {
				BluetoothButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_BLUETOOTH);
			} else if (buttonId == WidgetButton.BUTTON_GPS) {
				GPSButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_GPS);
			} else if (buttonId == WidgetButton.BUTTON_DATA) {
				MobileDataButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_DATA);
			} else if (buttonId == WidgetButton.BUTTON_2G3G) {
				NetworkModeButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_2G3G);
			} else if (buttonId == WidgetButton.BUTTON_SYNC) {
				SyncButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_SYNC);
			} else if (buttonId == WidgetButton.BUTTON_SOUND) {
				SoundButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_SOUND);
			} else if (buttonId == WidgetButton.BUTTON_SCREEN_TIMEOUT) {
				ScreenTimeoutButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_SCREEN_TIMEOUT);
			} else if (buttonId == WidgetButton.BUTTON_AUTO_ROTATE) {
				AutoRotateButton.getInstance().toggleState(context);
				updateWidget(context,WidgetButton.BUTTON_AUTO_ROTATE);
			} else if (buttonId == WidgetButton.BUTTON_BRIGHTNESS) {
				BrightnessButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_BRIGHTNESS);
			} else if (buttonId == WidgetButton.BUTTON_FLASHLIGHT) {
				FlashlightButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_FLASHLIGHT);
			} else if (buttonId == WidgetButton.BUTTON_LOCK_SCREEN) {
				LockScreenButton.getInstance().toggleState(context);
				updateWidget(context,WidgetButton.BUTTON_LOCK_SCREEN);
			} else if (buttonId == WidgetButton.BUTTON_AIRPLANE) {
				AirplaneButton.getInstance().toggleState(context);				
				updateWidget(context,WidgetButton.BUTTON_AIRPLANE);
			}
		} else if (MobileDataButton.MOBILE_DATA_CHANGED.equals(intent.getAction())) {  
			updateWidget(context,WidgetButton.BUTTON_DATA);
		} else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
			updateWidget(context,null);
		} else if (SecuritySettings.GPS_STATUS_CHANGED.equals(intent.getAction())) {
			updateWidget(context,null);
		} else {
			logD("Ignoring Action: "+intent.getAction());
			// Don't fall-through to updating the widget. The Intent
			// was something unrelated or that our super class took
			// care of.
			logD("<< onReceive OUT");		
			return;
		}

		// State changes fall through
		logD("<< onReceive OUT");		
		
	}
	
	public static void logD(String message) {
		if (DEBUG) Log.d(TAG,message);
		
	}
}
