package com.android.settings.widget;



import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.net.ConnectivityManager;

import com.android.settings.R;

public class WidgetSettings extends Activity {

	public static final String LAST_ICON_ID = "lastIconId";
	public static final String FIRST_ICON_ID = "firstIconId";
	public static final String RING_MODE_VIBRATE_AS_ON = "ringModeVibrateAsOn";
	public static final String USE_TRANSPARENT = "useTransparent";
	public static final String USE_ROUND_CORNERS = "useRoundCorners";
	public static final String USE_VERTICAL = "useVertical";

	public static final String TOGGLE_BRIGHTNESS = "toggleBrightness";
	public static final String TOGGLE_AUTO_ROTATE = "toggleAutoRotate";
	public static final String TOGGLE_SCREEN_TIMEOUT = "toggleScreenTimeout";
	public static final String TOGGLE_SOUND = "toggleSound";
	public static final String TOGGLE_SYNC = "toggleSync";
	public static final String TOGGLE_2G3G = "toggle2G3G";
	public static final String TOGGLE_DATA = "toggleData";
	public static final String TOGGLE_GPS = "toggleGPS";
	public static final String TOGGLE_BLUETOOTH = "toggleBluetooth";
	public static final String TOGGLE_WIFI = "toggleWifi";
	public static final String TOGGLE_WIFI_AP = "toggleWifiAp";

	public static final String TOGGLE_AIRPLANE = "toggleAirplane";
	public static final String TOGGLE_FLASHLIGHT = "toggleFlashlight";
	public static final String TOGGLE_LOCK_SCREEN = "toggleLockScreen";

	
	public static final String MONITOR_DATA_ROAMING = "monitorDataRoaming";
	public static final String AUTO_ENABLE_SYNC_WITH_WIFI = "autoEnableSyncWithWifi";
	public static final String AUTO_DISABLE_SYNC_WITH_WIFI = "autoDisableSyncWithWifi";
	public static final String AUTO_ENABLE_3G = "autoEnable3G";
	public static final String AUTO_DISABLE_3G = "autoDisable3G";

	public static final String AUTO_ENABLE_3G_WITH_WIFI = "autoEnable3GWithWifi";
	public static final String AUTO_DISABLE_3G_WITH_WIFI = "autoDisable3GWithWifi";

	public static final String AUTO_ENABLE_BLUETOOTH_WITH_POWER = "autoEnableBluetoothWithPower";
	public static final String AUTO_DISABLE_BLUETOOTH_WITH_POWER = "autoDisableBluetoothWithPower";

	public static final String AUTO_ENABLE_WIFI_WITH_POWER = "autoEnableWifiWithPower";
	public static final String AUTO_DISABLE_WIFI_WITH_POWER = "autoDisableWifiWithPower";

	public static final String NETWORK_MODE_SPINNER = "networkModeSpinner";
	public static final String BRIGHTNESS_SPINNER = "brightnessSpinner";
	public static final String RING_MODE_SPINNER = "ringModeSpinner";
	public static final String SCREEN_TIMEOUT_SPINNER = "screenTimeoutSpinner";
	
	public static final String SAVED = "saved";
	
	
	public static final String TOGGLE_3G_MODE = "toggle3GMode";

	public static final String WIDGET_PREF_MAIN = "widget_MAIN";
	public static final String WIDGET_PREF_NAME = "widget_";
	
    int widgetId  = AppWidgetManager.INVALID_APPWIDGET_ID;
	SharedPreferences preferences;
	SharedPreferences preferencesGeneral;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        initWidgetSettings();
        setDefaultReturn();
        initControls();
        initSettings();
    }

	private void initWidgetSettings() {
        setContentView(R.layout.widget_settings);
		Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
	}
    
	private void initControls() {
		findViewById(R.id.okbutton).setOnClickListener(okClickListener);
		findViewById(R.id.cancelbutton).setOnClickListener(cancelClickListener);
	}

	private void initSettings() {
		Log.d("WidgetSettings: ", "create preference for widget_"+widgetId);
		preferences = getSharedPreferences(WIDGET_PREF_NAME+widgetId,Context.MODE_PRIVATE);
		preferencesGeneral = getSharedPreferences(WIDGET_PREF_MAIN,Context.MODE_PRIVATE);
		((CheckBox)findViewById(R.id.autoDisable3G)).setChecked(preferencesGeneral.getBoolean(AUTO_DISABLE_3G, false));
		((CheckBox)findViewById(R.id.autoEnable3G)).setChecked(preferencesGeneral.getBoolean(AUTO_ENABLE_3G, false));
		((CheckBox)findViewById(R.id.autoDisable3GWithWifi)).setChecked(preferencesGeneral.getBoolean(AUTO_DISABLE_3G_WITH_WIFI, false));
		((CheckBox)findViewById(R.id.autoEnable3GWithWifi)).setChecked(preferencesGeneral.getBoolean(AUTO_ENABLE_3G_WITH_WIFI, false));
		((CheckBox)findViewById(R.id.autoEnableBluetoothWithPower)).setChecked(preferencesGeneral.getBoolean(AUTO_ENABLE_BLUETOOTH_WITH_POWER, false));
		((CheckBox)findViewById(R.id.autoDisableBluetoothWithPower)).setChecked(preferencesGeneral.getBoolean(AUTO_DISABLE_BLUETOOTH_WITH_POWER, false));
		((CheckBox)findViewById(R.id.autoEnableWifiWithPower)).setChecked(preferencesGeneral.getBoolean(AUTO_ENABLE_WIFI_WITH_POWER, false));
		((CheckBox)findViewById(R.id.autoDisableWifiWithPower)).setChecked(preferencesGeneral.getBoolean(AUTO_DISABLE_WIFI_WITH_POWER, false));
		((CheckBox)findViewById(R.id.autoDisableSyncWithWifi)).setChecked(preferencesGeneral.getBoolean(AUTO_DISABLE_SYNC_WITH_WIFI, false));
		((CheckBox)findViewById(R.id.autoEnableSyncWithWifi)).setChecked(preferencesGeneral.getBoolean(AUTO_ENABLE_SYNC_WITH_WIFI, false));
		((CheckBox)findViewById(R.id.monitorDataRoaming)).setChecked(preferencesGeneral.getBoolean(MONITOR_DATA_ROAMING, false));		
		((CheckBox)findViewById(R.id.ringModeVibrateAsOn)).setChecked(preferencesGeneral.getBoolean(RING_MODE_VIBRATE_AS_ON, false));
		
		// disable the Wi-Fi AP preference if Wifi AP is not available
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getTetherableWifiRegexs().length <= 0) {
			findViewById(R.id.toggleWifiApPreference).setVisibility(View.GONE);
			findViewById(R.id.toggleWifiApPreferenceDivider).setVisibility(View.GONE);
		}		
		
		Spinner spinner = (Spinner) findViewById(R.id.brightnessSpinner);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.brightnessWidget, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setSelection(preferencesGeneral.getInt(BRIGHTNESS_SPINNER, 0));

	    spinner = (Spinner) findViewById(R.id.screenTimeoutSpinner);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.screenTimeoutWidget, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setSelection(preferencesGeneral.getInt(SCREEN_TIMEOUT_SPINNER, 0));

	    spinner = (Spinner) findViewById(R.id.networkModeSpinner);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.networkModesWidget, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setSelection(preferencesGeneral.getInt(NETWORK_MODE_SPINNER, 0));

	    spinner = (Spinner) findViewById(R.id.ringModeSpinner);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.ringModeWidget, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setSelection(preferencesGeneral.getInt(RING_MODE_SPINNER, 0));		
	}
	
	
	private void setDefaultReturn() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);		
		setResult(RESULT_CANCELED,returnIntent);		
		
        //No widget id or invalid one
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
	}

    View.OnClickListener cancelClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            finish();
        }        
    };


    View.OnClickListener okClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            saveSettings();
            updateWidget();
            
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
            finish();
        }
        
        
        private void saveSettings() {
        	//SharedPreferences preferences = getSharedPreferences("widget_"+widgetId,Context.MODE_WORLD_WRITEABLE);
    		Log.d("WidgetSettings: ", "Will save widget_"+widgetId);

        	Editor editor =preferences.edit();
        	editor.clear();
        	editor.putBoolean(TOGGLE_WIFI, ((CheckBox)findViewById(R.id.toggleWifi)).isChecked());
        	editor.putBoolean(TOGGLE_BLUETOOTH, ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked());
        	editor.putBoolean(TOGGLE_GPS, ((CheckBox)findViewById(R.id.toggleGPS)).isChecked());
        	editor.putBoolean(TOGGLE_DATA, ((CheckBox)findViewById(R.id.toggleData)).isChecked());
        	editor.putBoolean(TOGGLE_2G3G, ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked());
        	editor.putBoolean(TOGGLE_SYNC, ((CheckBox)findViewById(R.id.toggleSync)).isChecked());
        	editor.putBoolean(TOGGLE_SOUND, ((CheckBox)findViewById(R.id.toggleSound)).isChecked());
        	editor.putBoolean(TOGGLE_SCREEN_TIMEOUT, ((CheckBox)findViewById(R.id.toggleScreenTimeout)).isChecked());
        	editor.putBoolean(TOGGLE_AUTO_ROTATE, ((CheckBox)findViewById(R.id.toggleAutoRotate)).isChecked());
        	editor.putBoolean(TOGGLE_BRIGHTNESS, ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked());
        	editor.putBoolean(TOGGLE_AIRPLANE, ((CheckBox)findViewById(R.id.toggleAirplane)).isChecked());
        	editor.putBoolean(TOGGLE_LOCK_SCREEN, ((CheckBox)findViewById(R.id.toggleLockScreen)).isChecked());
        	editor.putBoolean(TOGGLE_FLASHLIGHT, ((CheckBox)findViewById(R.id.toggleFlashlight)).isChecked());
        	editor.putBoolean(TOGGLE_WIFI_AP, ((CheckBox)findViewById(R.id.toggleWifiAp)).isChecked());

        	editor.putBoolean(USE_ROUND_CORNERS, ((CheckBox)findViewById(R.id.useRoundCorners)).isChecked());
        	editor.putBoolean(USE_TRANSPARENT, ((CheckBox)findViewById(R.id.useTransparent)).isChecked());
        	editor.putBoolean(USE_VERTICAL, ((CheckBox)findViewById(R.id.useVertical)).isChecked());
        	editor.putInt(SAVED, SettingsAppWidgetProvider.WIDGET_PRESENT);
        	editor.putInt(FIRST_ICON_ID, getFirstIconId());
        	editor.putInt(LAST_ICON_ID, getLastIconId());
        	editor.commit();

        	Editor editorGeneral =preferencesGeneral.edit();
        	editorGeneral.clear();
        	editorGeneral.putBoolean(AUTO_DISABLE_3G, ((CheckBox)findViewById(R.id.autoDisable3G)).isChecked());
        	editorGeneral.putBoolean(AUTO_ENABLE_3G, ((CheckBox)findViewById(R.id.autoEnable3G)).isChecked());
        	editorGeneral.putBoolean(AUTO_DISABLE_SYNC_WITH_WIFI, ((CheckBox)findViewById(R.id.autoDisableSyncWithWifi)).isChecked());
        	editorGeneral.putBoolean(AUTO_ENABLE_SYNC_WITH_WIFI, ((CheckBox)findViewById(R.id.autoEnableSyncWithWifi)).isChecked());
        	editorGeneral.putBoolean(AUTO_DISABLE_3G_WITH_WIFI, ((CheckBox)findViewById(R.id.autoDisable3GWithWifi)).isChecked());
        	editorGeneral.putBoolean(AUTO_ENABLE_3G_WITH_WIFI, ((CheckBox)findViewById(R.id.autoEnable3GWithWifi)).isChecked());
        	editorGeneral.putBoolean(AUTO_ENABLE_BLUETOOTH_WITH_POWER, ((CheckBox)findViewById(R.id.autoEnableBluetoothWithPower)).isChecked());
        	editorGeneral.putBoolean(AUTO_DISABLE_BLUETOOTH_WITH_POWER, ((CheckBox)findViewById(R.id.autoDisableBluetoothWithPower)).isChecked());
        	editorGeneral.putBoolean(AUTO_ENABLE_WIFI_WITH_POWER, ((CheckBox)findViewById(R.id.autoEnableWifiWithPower)).isChecked());
        	editorGeneral.putBoolean(AUTO_DISABLE_WIFI_WITH_POWER, ((CheckBox)findViewById(R.id.autoDisableWifiWithPower)).isChecked());
        	editorGeneral.putBoolean(MONITOR_DATA_ROAMING, ((CheckBox)findViewById(R.id.monitorDataRoaming)).isChecked());
        	editorGeneral.putBoolean(RING_MODE_VIBRATE_AS_ON, ((CheckBox)findViewById(R.id.ringModeVibrateAsOn)).isChecked());

        	editorGeneral.putInt(BRIGHTNESS_SPINNER,((Spinner) findViewById(R.id.brightnessSpinner)).getSelectedItemPosition());
        	editorGeneral.putInt(SCREEN_TIMEOUT_SPINNER,((Spinner) findViewById(R.id.screenTimeoutSpinner)).getSelectedItemPosition());
        	editorGeneral.putInt(NETWORK_MODE_SPINNER,((Spinner) findViewById(R.id.networkModeSpinner)).getSelectedItemPosition());
        	editorGeneral.putInt(RING_MODE_SPINNER,((Spinner) findViewById(R.id.ringModeSpinner)).getSelectedItemPosition());
        	editorGeneral.putInt(SAVED, SettingsAppWidgetProvider.WIDGET_PRESENT);
        	editorGeneral.commit();


        	
        	
        	// transparency
        	// icon colors        	
        }

        private int getLastIconId() {
			if ( ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked()) return R.id.ind_brightness;			
			if ( ((CheckBox)findViewById(R.id.toggleAirplane)).isChecked()) return R.id.ind_airplane;
			if ( ((CheckBox)findViewById(R.id.toggleFlashlight)).isChecked()) return R.id.ind_flashlight;
			if ( ((CheckBox)findViewById(R.id.toggleLockScreen)).isChecked()) return R.id.ind_lock_screen;
			if ( ((CheckBox)findViewById(R.id.toggleAutoRotate)).isChecked()) return R.id.ind_auto_rotate;
			if ( ((CheckBox)findViewById(R.id.toggleScreenTimeout)).isChecked()) return R.id.ind_screen_timeout;
			if ( ((CheckBox)findViewById(R.id.toggleSound)).isChecked()) return R.id.ind_sound;
			if ( ((CheckBox)findViewById(R.id.toggleSync)).isChecked()) return R.id.ind_sync;
			if ( ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked()) return R.id.ind_2G3G;
			if ( ((CheckBox)findViewById(R.id.toggleData)).isChecked()) return R.id.ind_data;
			if ( ((CheckBox)findViewById(R.id.toggleGPS)).isChecked()) return R.id.ind_gps;
			if ( ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked()) return R.id.ind_bluetooth;
			if ( ((CheckBox)findViewById(R.id.toggleWifiAp)).isChecked()) return R.id.ind_wifi_ap;
			if ( ((CheckBox)findViewById(R.id.toggleWifi)).isChecked()) return R.id.ind_wifi;
			return 0;
		}


		private int getFirstIconId() {
			if ( ((CheckBox)findViewById(R.id.toggleWifi)).isChecked()) return R.id.ind_wifi;
			if ( ((CheckBox)findViewById(R.id.toggleWifiAp)).isChecked()) return R.id.ind_wifi_ap;
			if ( ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked()) return R.id.ind_bluetooth;
			if ( ((CheckBox)findViewById(R.id.toggleGPS)).isChecked()) return R.id.ind_gps;
			if ( ((CheckBox)findViewById(R.id.toggleData)).isChecked()) return R.id.ind_data;
			if ( ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked()) return R.id.ind_2G3G;
			if ( ((CheckBox)findViewById(R.id.toggleSync)).isChecked()) return R.id.ind_sync;
			if ( ((CheckBox)findViewById(R.id.toggleSound)).isChecked()) return R.id.ind_sound;
			if ( ((CheckBox)findViewById(R.id.toggleScreenTimeout)).isChecked()) return R.id.ind_screen_timeout;
			if ( ((CheckBox)findViewById(R.id.toggleAutoRotate)).isChecked()) return R.id.ind_auto_rotate;
			if ( ((CheckBox)findViewById(R.id.toggleLockScreen)).isChecked()) return R.id.ind_lock_screen;
			if ( ((CheckBox)findViewById(R.id.toggleFlashlight)).isChecked()) return R.id.ind_flashlight;
			if ( ((CheckBox)findViewById(R.id.toggleAirplane)).isChecked()) return R.id.ind_airplane;
			if ( ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked()) return R.id.ind_brightness;			
			return 0;
		}


		private void updateWidget() {
        	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetSettings.this);
        	SettingsAppWidgetProvider.buildUpdate(WidgetSettings.this, appWidgetManager, new int[]{widgetId});
        }        
    };
}