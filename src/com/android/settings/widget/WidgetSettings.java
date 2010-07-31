package com.android.settings.widget;



import com.android.settings.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RemoteViews;

public class WidgetSettings extends Activity {

	private static final String TAG = "WidgetSettings";		
    int widgetId  = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        initWidgetSettings();
        setDefaultReturn();
        initControls();
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
        	SharedPreferences preferences = getSharedPreferences("widget_",Context.MODE_PRIVATE);

        	Editor editor =preferences.edit();
        	editor.clear();
        	editor.putBoolean("toggleWifi", ((CheckBox)findViewById(R.id.toggleWifi)).isChecked());
        	editor.putBoolean("toggleBluetooth", ((CheckBox)findViewById(R.id.toggleBluetooth)).isChecked());
        	editor.putBoolean("toggleGPS", ((CheckBox)findViewById(R.id.toggleGPS)).isChecked());
        	editor.putBoolean("toggleData", ((CheckBox)findViewById(R.id.toggleData)).isChecked());
        	editor.putBoolean("toggle2G3G", ((CheckBox)findViewById(R.id.toggle2g3g)).isChecked());
        	editor.putBoolean("toggleSync", ((CheckBox)findViewById(R.id.toggleSync)).isChecked());
        	editor.putBoolean("toggleSound", ((CheckBox)findViewById(R.id.toggleSound)).isChecked());
        	editor.putBoolean("toggleBrightness", ((CheckBox)findViewById(R.id.toggleBrightness)).isChecked());
        	editor.putBoolean("autoDisable3G", ((CheckBox)findViewById(R.id.autoDisable3G)).isChecked());
        	editor.putBoolean("autoEnable3G", ((CheckBox)findViewById(R.id.autoEnable3G)).isChecked());
        	editor.putBoolean("useRoundCorners", ((CheckBox)findViewById(R.id.useRoundCorners)).isChecked());
        	editor.putBoolean("useTransparent", ((CheckBox)findViewById(R.id.useTransparent)).isChecked());

        	editor.putBoolean("autoDisableSyncWithWifi", ((CheckBox)findViewById(R.id.autoDisableSyncWithWifi)).isChecked());
        	editor.putBoolean("autoEnableSyncWithWifi", ((CheckBox)findViewById(R.id.autoEnableSyncWithWifi)).isChecked());

        	editor.putBoolean("ringModeVibrateAsOn", ((CheckBox)findViewById(R.id.ringModeVibrateAsOn)).isChecked());

        	editor.putInt("firstIconId", getFirstIconId());
        	editor.putInt("lastIconId", getLastIconId());
        	editor.commit();
        	
        	
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


