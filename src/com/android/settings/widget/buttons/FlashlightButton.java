package com.android.settings.widget.buttons;

import java.io.File;
import java.io.FileWriter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import com.android.settings.R;
import com.android.settings.widget.FlashlightActivity;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class FlashlightButton extends WidgetButton {

	static FlashlightButton ownButton=null;

	static Camera camera=null;
	static Boolean isLedSupported=null;

	public static final File LOCATION= new File("/sys/devices/platform/flashlight.0/leds/flashlight/brightness");

	
	public static final int MODE_SCREEN=0;
	public static final int MODE_N1=1;
	public static final int MODE_HTC=2;
	public static final int MODE_LED=3;
	
	public static final String N1_VALUE="3";
	public static final String HTC_VALUE="128";
	public static final String OFF="0";


	
	public static final String GOOGLE_N1="passion";
	public static final String HTC_EVO="supersonic";
	public static final String HTC_DESIRE="bravo";
	public static final String HTC_MAGIC="sapphire";
	public static final String HTC_G1="dream";

	public static Boolean isFlashlightOn=null;
	public static Integer currentMode=null;
	
		
	public void updateState(Context context,
			SharedPreferences globalPreferences, int[] appWidgetIds) {

		int state = getState(context);

		if (state==SettingsAppWidgetProvider.STATE_DISABLED) {
			currentIcon=R.drawable.ic_appwidget_settings_flashlight_off;
			currentState=SettingsAppWidgetProvider.STATE_DISABLED;			
		} else if (state==SettingsAppWidgetProvider.STATE_ENABLED) {
			currentIcon=R.drawable.ic_appwidget_settings_flashlight_on;
			currentState=SettingsAppWidgetProvider.STATE_ENABLED;			
		} else {
			currentIcon=R.drawable.ic_appwidget_settings_flashlight_on;			
			currentState=SettingsAppWidgetProvider.STATE_INTERMEDIATE;			
		}
	}

	
	private int getState(Context context) {
		switch (getMode(context)) {
		case MODE_SCREEN: //return SettingsAppWidgetProvider.STATE_DISABLED;
		case MODE_N1:
		case MODE_HTC:
			// For now set it as disabled. it will be visiable that the flashlight 
			// If the screen gets off. we will not get any update state.
			return SettingsAppWidgetProvider.STATE_DISABLED;
//			return (isFlashlightOn==null || !isFlashlightOn)? SettingsAppWidgetProvider.STATE_DISABLED: SettingsAppWidgetProvider.STATE_ENABLED ;
		case MODE_LED:
			return getStockStatus(context);
		}
		return SettingsAppWidgetProvider.STATE_INTERMEDIATE;
	}


	
	private int getStockStatus(Context context) {
		int state=SettingsAppWidgetProvider.STATE_INTERMEDIATE;
		Camera tmpCam= camera;
		try {
			Parameters parameters;
			if (camera!=null) {
				parameters = camera.getParameters();			
			} else {
				tmpCam=Camera.open();
				parameters = tmpCam.getParameters();			
			}

			String mode = parameters.getFlashMode();

			if (mode.equals(Camera.Parameters.FLASH_MODE_OFF)) {
				state=SettingsAppWidgetProvider.STATE_DISABLED;
			} else if (mode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
				state=SettingsAppWidgetProvider.STATE_ENABLED;
			} else {
				state=SettingsAppWidgetProvider.STATE_INTERMEDIATE;
			}

			if (tmpCam!=null) {
				tmpCam.release();
				tmpCam=null;
			}			
		} catch (Exception e) {
			Log.e(SettingsAppWidgetProvider.TAG, "Something wrong getting camera state", e);
			currentMode=MODE_SCREEN;
			try {
				if (tmpCam !=null) {
					tmpCam.release();
				}
			} catch (Exception e1) {				
			}
		}
		return state;	
	}

	public int getMode (Context context) {
		if (currentMode==null) {
		SettingsAppWidgetProvider.logD("Flashlight: Test: "+Build.DEVICE);
		if (HTC_DESIRE.equalsIgnoreCase(Build.DEVICE) || HTC_EVO.equalsIgnoreCase(Build.DEVICE)) {
			currentMode=MODE_HTC;
		} else if (GOOGLE_N1.equalsIgnoreCase(Build.DEVICE)) {
			currentMode=MODE_N1;
		} else if (HTC_MAGIC.equalsIgnoreCase(Build.DEVICE)  ||
					HTC_G1.equalsIgnoreCase(Build.DEVICE)  ||
				!isLedSupported()) {
			currentMode=MODE_SCREEN;
		} else {
			currentMode=MODE_LED;
		}
		}
		return currentMode;
	}

	
	/**
	 * Toggles the state 
	 * 
	 * @param context
	 */
	public void toggleState(Context context) {
		SettingsAppWidgetProvider.logD("Flashlight: Test: "+Build.DEVICE);
		if (HTC_DESIRE.equalsIgnoreCase(Build.DEVICE) || HTC_EVO.equalsIgnoreCase(Build.DEVICE)) {
			toogleViaFile(context, HTC_VALUE);
		} else if (GOOGLE_N1.equalsIgnoreCase(Build.DEVICE)) {
			toogleViaFile(context, N1_VALUE);
		} else if (HTC_MAGIC.equalsIgnoreCase(Build.DEVICE)  ||
					HTC_G1.equalsIgnoreCase(Build.DEVICE)  ||
				!isLedSupported()) {
			toogleViaScreen(context);
		} else {
			toogleViaCamera();
		}
	}

	private void toogleViaFile(Context context, String value) {
		FileWriter fwriter=null;
		try {
			if (LOCATION.exists()) {
				fwriter = new FileWriter(LOCATION);
				if (isFlashlightOn==null || !isFlashlightOn) {
					fwriter.write(value);
					isFlashlightOn=true;
				} else {
					fwriter.write(OFF);				
					isFlashlightOn=false;
				}
				fwriter.flush();
				fwriter.close();
				fwriter=null;
			} else {
				currentMode=MODE_SCREEN;
				SettingsAppWidgetProvider.logD("Flashlight. File not found...");
				Toast t = Toast.makeText(context, "Could not find brightness file", Toast.LENGTH_SHORT);	
				t.show();
			}
		} catch (Exception e) {
			currentMode=MODE_SCREEN;
			Log.e(SettingsAppWidgetProvider.TAG," Flashlight: Error with file...",e);
			Toast t = Toast.makeText(context, "Error with file", Toast.LENGTH_SHORT);	
			t.show();
		} finally {
			try {
				if (fwriter!=null) {
					fwriter.close();				
				}				
			} catch (Exception e2) {
			}
		}
	}

	private void toogleViaScreen(Context context) {
		Intent intent = new Intent(context, FlashlightActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
		context.startActivity(intent);
	}

	private void toogleViaCamera() {
		boolean cameraWasSet=true;	
		if (camera==null) {
			cameraWasSet=false;
			camera = Camera.open();
		}
		Parameters parameters = camera.getParameters();
		String mode = parameters.getFlashMode();
		if (mode.equals(Camera.Parameters.FLASH_MODE_OFF)) {
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			camera.setParameters(parameters);
		} else {
			if (!cameraWasSet) {
				SettingsAppWidgetProvider.logD("Someting wrong. Camera show have been present");				
			}
			try {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				camera.setParameters(parameters);
			} finally {
				camera.release();
				camera=null;
			}
		}
	}

	private boolean isLedSupported() {
		if (isLedSupported==null) {
			camera = Camera.open();
			if (camera.getParameters().getFlashMode() == null) {
				SettingsAppWidgetProvider.logD("Flashlight Led not supported");
				isLedSupported=false;
			} else {
				SettingsAppWidgetProvider.logD("Flashlight Led is supported");
				isLedSupported=true;
			}			
		}
		return isLedSupported;
	}

	public static FlashlightButton getInstance() {
		if (ownButton==null)
			ownButton = new FlashlightButton();

		return ownButton;
	}

	@Override
	void initButton() {
		buttonID=WidgetButton.BUTTON_FLASHLIGHT;
		isDefault=false;
		preferenceName=WidgetSettings.TOGGLE_FLASHLIGHT;

		buttonLayout=R.id.btn_flashlight;
		buttonSep=R.id.sep_flashlight;
		buttonIcon=R.id.img_flashlight;
		buttonState=R.id.ind_flashlight;

	}

}

