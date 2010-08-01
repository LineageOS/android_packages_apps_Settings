package com.android.settings.widget;



import com.android.settings.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RemoteViews;

public class WidgetSettings extends Activity {

	private static final String TAG = "WidgetSettings";		
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
        setContentView(R.layout.widgetsettings);
		Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
	}
    
	private void initControls() {
		Button ok = (Button) findViewById(R.id.okbutton);		
		findViewById(R.id.okbutton).setOnClickListener(okClickListener);
		findViewById(R.id.cancelbutton).setOnClickListener(cancelClickListener);
	}

	private void initSettings() {
		Log.d("WidgetSettings: ", "create preference for widget_"+widgetId);
		preferences = getSharedPreferences("widget_"+widgetId,Context.MODE_PRIVATE);
		preferencesGeneral = getSharedPreferences("widget_MAIN",Context.MODE_PRIVATE);
		((CheckBox)findViewById(R.id.autoDisable3G)).setChecked(preferencesGeneral.getBoolean("autoDisable3G", false));
		((CheckBox)findViewById(R.id.autoEnable3G)).setChecked(preferencesGeneral.getBoolean("autoEnable3G", false));
		((CheckBox)findViewById(R.id.autoDisableSyncWithWifi)).setChecked(preferencesGeneral.getBoolean("autoDisableSyncWithWifi", false));
		((CheckBox)findViewById(R.id.autoEnableSyncWithWifi)).setChecked(preferencesGeneral.getBoolean("autoEnableSyncWithWifi", false));
		((CheckBox)findViewById(R.id.monitorDataRoaming)).setChecked(preferencesGeneral.getBoolean("monitorDataRoaming", false));		
		
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
        	editor.putBoolean("toggleWifi", ((CheckBox)findViewById(R.id.toggleWifi)).isChecked());
        	editor.putBoolean("toggleBluetooth", ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked());
        	editor.putBoolean("toggleGPS", ((CheckBox)findViewById(R.id.toggleGPS)).isChecked());
        	editor.putBoolean("toggleData", ((CheckBox)findViewById(R.id.toggleData)).isChecked());
        	editor.putBoolean("toggle2G3G", ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked());
        	editor.putBoolean("toggleSync", ((CheckBox)findViewById(R.id.toggleSync)).isChecked());
        	editor.putBoolean("toggleSound", ((CheckBox)findViewById(R.id.toggleSound)).isChecked());
        	editor.putBoolean("toggleScreenTimeout", ((CheckBox)findViewById(R.id.toggleScreenTimeout)).isChecked());
        	editor.putBoolean("toggleAutoRotate", ((CheckBox)findViewById(R.id.toggleAutoRotate)).isChecked());
        	editor.putBoolean("toggleBrightness", ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked());
        	editor.putBoolean("useRoundCorners", ((CheckBox)findViewById(R.id.useRoundCorners)).isChecked());
        	editor.putBoolean("useTransparent", ((CheckBox)findViewById(R.id.useTransparent)).isChecked());
        	editor.putBoolean("ringModeVibrateAsOn", ((CheckBox)findViewById(R.id.ringModeVibrateAsOn)).isChecked());
        	editor.putInt("firstIconId", getFirstIconId());
        	editor.putInt("lastIconId", getLastIconId());
        	editor.commit();

        	Editor editorGeneral =preferencesGeneral.edit();
        	editorGeneral.clear();
        	editorGeneral.putBoolean("autoDisable3G", ((CheckBox)findViewById(R.id.autoDisable3G)).isChecked());
        	editorGeneral.putBoolean("autoEnable3G", ((CheckBox)findViewById(R.id.autoEnable3G)).isChecked());
        	editorGeneral.putBoolean("autoDisableSyncWithWifi", ((CheckBox)findViewById(R.id.autoDisableSyncWithWifi)).isChecked());
        	editorGeneral.putBoolean("autoEnableSyncWithWifi", ((CheckBox)findViewById(R.id.autoEnableSyncWithWifi)).isChecked());
        	editorGeneral.putBoolean("monitorDataRoaming", ((CheckBox)findViewById(R.id.monitorDataRoaming)).isChecked());
        	editorGeneral.commit();


        	
        	
        	// transparency
        	// icon colors        	
        }

        private int getLastIconId() {
			if ( ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked()) return R.id.ind_brightness;			
			if ( ((CheckBox)findViewById(R.id.toggleSound)).isChecked()) return R.id.ind_sound;
			if ( ((CheckBox)findViewById(R.id.toggleSync)).isChecked()) return R.id.ind_sync;
			if ( ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked()) return R.id.ind_2G3G;
			if ( ((CheckBox)findViewById(R.id.toggleData)).isChecked()) return R.id.ind_data;
			if ( ((CheckBox)findViewById(R.id.toggleGPS)).isChecked()) return R.id.ind_gps;
			if ( ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked()) return R.id.ind_bluetooth;
			if ( ((CheckBox)findViewById(R.id.toggleWifi)).isChecked()) return R.id.ind_wifi;
			return 0;
		}


		private int getFirstIconId() {
			if ( ((CheckBox)findViewById(R.id.toggleWifi)).isChecked()) return R.id.ind_wifi;
			if ( ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked()) return R.id.ind_bluetooth;
			if ( ((CheckBox)findViewById(R.id.toggleGPS)).isChecked()) return R.id.ind_gps;
			if ( ((CheckBox)findViewById(R.id.toggleData)).isChecked()) return R.id.ind_data;
			if ( ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked()) return R.id.ind_2G3G;
			if ( ((CheckBox)findViewById(R.id.toggleSync)).isChecked()) return R.id.ind_sync;
			if ( ((CheckBox)findViewById(R.id.toggleSound)).isChecked()) return R.id.ind_sound;
			if ( ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked()) return R.id.ind_brightness;			
			return 0;
		}


		private void updateWidget() {
        	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetSettings.this);
        	RemoteViews views = SettingsAppWidgetProvider.buildUpdate(WidgetSettings.this, widgetId);
        	appWidgetManager.updateAppWidget(widgetId, views);
        }        
    };
}


