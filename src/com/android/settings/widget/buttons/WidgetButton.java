package com.android.settings.widget.buttons;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

public abstract class WidgetButton {


	public static final int BUTTON_WIFI = 0;
	public static final int BUTTON_BRIGHTNESS = 1;
	public static final int BUTTON_SYNC = 2;
	public static final int BUTTON_GPS = 3;
	public static final int BUTTON_BLUETOOTH = 4;
	public static final int BUTTON_DATA = 5;
	public static final int BUTTON_SOUND = 6;
	public static final int BUTTON_2G3G = 7;
	public static final int BUTTON_SCREEN_TIMEOUT = 8;
	public static final int BUTTON_AUTO_ROTATE = 9;
	public static final int BUTTON_FLASHLIGHT = 10;
	public static final int BUTTON_AIRPLANE = 11;
	public static final int BUTTON_LOCK_SCREEN = 12;
	public static final int BUTTON_WIFI_AP = 13;

	// This widget keeps track of two sets of states:
	// "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
	// "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
	// STATE_TURNING_OFF, STATE_UNKNOWN

	/*
	 * TO be defined by child classes
	 */

	//The name of the preference to be read
	String preferenceName = null;

	//If this is a default button
	boolean isDefault=false;

	//Identifier for the Button for Intents
	int buttonID;

	int currentState;
	int currentIcon;

	int buttonLayout;
	Integer buttonSep=null;
	int buttonIcon;
	int buttonState;



	public WidgetButton() {
		//SettingsAppWidgetProvider.logD("Creating WidgetButton ");		
		initButton();
	}


	abstract void initButton();
	abstract public void toggleState(Context context);
	abstract public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds);

	/**
	 * Set the button visibility and register intent
	 * 
	 * @param context
	 * @param views
	 * @param preferences
	 * @param appWidgetId
	 * @return
	 */	
	public void updateView(Context context, RemoteViews views,
			SharedPreferences globalPreferences,
			SharedPreferences widgetPreferences, int appWidgetId) {

		//SettingsAppWidgetProvider.logD(">> updateView IN. Widget: "+appWidgetId+" Button:"+buttonID);		

		if (isVisible(widgetPreferences, appWidgetId)) {
			//SettingsAppWidgetProvider.logD("updateView -> Is Visible");		

			//Set it visible
			views.setViewVisibility(buttonLayout, View.VISIBLE);
			if (buttonSep!=null) {
				views.setViewVisibility(buttonSep, View.VISIBLE);
			}

			//SettingsAppWidgetProvider.logD("updateView -> Will register Intent");		
			//Register the Intent
			views.setOnClickPendingIntent(buttonLayout, getLaunchPendingIntent(
					context, appWidgetId, buttonID));			

			//SettingsAppWidgetProvider.logD("updateView -> Set current Icon");		
			//Update icon to the one set by updateState
			views.setImageViewResource(buttonIcon,currentIcon);

			//SettingsAppWidgetProvider.logD("updateView -> Set current State");		
			views.setImageViewResource(buttonState, 
					getButtonState(widgetPreferences));

		} else {
			//If not visible, remove it from the screen
			//SettingsAppWidgetProvider.logD("updateView -> Button Not visible");		
			views.setViewVisibility(buttonLayout, View.GONE);
			if (buttonSep!=null) {
				views.setViewVisibility(buttonSep, View.GONE);
			}
		}
	}


	/**
	 * Creates PendingIntent to notify the widget of a button click.
	 * 
	 * @param context
	 * @param appWidgetId
	 * @return
	 */
	private static PendingIntent getLaunchPendingIntent(Context context,
			int appWidgetId, int buttonId) {
		Intent launchIntent = new Intent();
		launchIntent.setClass(context, SettingsAppWidgetProvider.class);
		launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		launchIntent.setData(Uri.parse("custom:" + buttonId));
		PendingIntent pi = PendingIntent.getBroadcast(context, 0 /*
		 * no
		 * requestCode
		 */,
		 launchIntent, 0 /* no flags */);
		return pi;
	}


	public int getButtonState(SharedPreferences widgetPreferences) {
		//preferences.getBoolean("useRoundCorners", true)
		boolean useRoundCorners = widgetPreferences.getBoolean(WidgetSettings.USE_ROUND_CORNERS, true);
		boolean useVerticalLayout = widgetPreferences.getBoolean(WidgetSettings.USE_VERTICAL, true);
		
		if (useRoundCorners) {
			if (isLeft(widgetPreferences)) {
				if (currentState == SettingsAppWidgetProvider.STATE_ENABLED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_on_v_l : R.drawable.appwidget_settings_ind_on_l;
				} else if (currentState == SettingsAppWidgetProvider.STATE_DISABLED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_off_v_l :R.drawable.appwidget_settings_ind_off_l;
				} else if (currentState == SettingsAppWidgetProvider.STATE_ENABLED_RED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_on_red_v_l :R.drawable.appwidget_settings_ind_on_red_l;
				} else if (currentState == SettingsAppWidgetProvider.STATE_DISABLED_RED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_mid_red_v_l :R.drawable.appwidget_settings_ind_mid_red_l;
				} else {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_mid_v_l :R.drawable.appwidget_settings_ind_mid_l;
				}
			} else if (isRight(widgetPreferences)) {
				if (currentState == SettingsAppWidgetProvider.STATE_ENABLED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_on_v_r : R.drawable.appwidget_settings_ind_on_r;
				} else if (currentState == SettingsAppWidgetProvider.STATE_DISABLED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_off_v_r : R.drawable.appwidget_settings_ind_off_r;
				} else if (currentState == SettingsAppWidgetProvider.STATE_ENABLED_RED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_on_red_v_r : R.drawable.appwidget_settings_ind_on_red_r;
				} else if (currentState == SettingsAppWidgetProvider.STATE_DISABLED_RED) {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_mid_red_v_r : R.drawable.appwidget_settings_ind_mid_red_r;
				} else {
					return useVerticalLayout? R.drawable.appwidget_settings_ind_mid_v_r : R.drawable.appwidget_settings_ind_mid_r;
				}
			}
		}

		if (currentState == SettingsAppWidgetProvider.STATE_ENABLED) {
			return useVerticalLayout? R.drawable.appwidget_settings_ind_on_v_c :R.drawable.appwidget_settings_ind_on_c;
		} else if (currentState == SettingsAppWidgetProvider.STATE_DISABLED) {
			return useVerticalLayout? R.drawable.appwidget_settings_ind_off_v_c : R.drawable.appwidget_settings_ind_off_c;
		} else if (currentState == SettingsAppWidgetProvider.STATE_ENABLED_RED) {
			return useVerticalLayout? R.drawable.appwidget_settings_ind_on_red_v_c : R.drawable.appwidget_settings_ind_on_red_c;
		} else if (currentState == SettingsAppWidgetProvider.STATE_DISABLED_RED) {
			return useVerticalLayout? R.drawable.appwidget_settings_ind_mid_red_v_c : R.drawable.appwidget_settings_ind_mid_red_c;
		} else {
			return useVerticalLayout? R.drawable.appwidget_settings_ind_mid_v_c : R.drawable.appwidget_settings_ind_mid_c;
		}
	}

	private boolean isRight(SharedPreferences widgetPreferences) {
		return buttonState==widgetPreferences.getInt(WidgetSettings.LAST_ICON_ID, -1);
	}


	private boolean isLeft(SharedPreferences widgetPreferences) {
		return buttonState==widgetPreferences.getInt(WidgetSettings.FIRST_ICON_ID, -1);
	}


	protected boolean isVisible(SharedPreferences preferences, int appWidgetId) {
		//SettingsAppWidgetProvider.logD("isVisible -> "+ preferenceName+"="+preferences.getBoolean(preferenceName, false));		
		//SettingsAppWidgetProvider.logD("isVisible -> Saved="+preferences.getBoolean(SAVED_PREFERENCES, false));		

		if (preferences.getBoolean(preferenceName, false) || (isDefault && preferences.getInt(WidgetSettings.SAVED, SettingsAppWidgetProvider.WIDGET_NOT_CONFIGURED)!=SettingsAppWidgetProvider.WIDGET_PRESENT)) {
			return true;
		} else {
			return false;
		}
	}	


}
