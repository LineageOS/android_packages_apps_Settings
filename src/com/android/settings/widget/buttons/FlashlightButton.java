package com.android.settings.widget.buttons;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import com.android.settings.R;
import com.android.settings.widget.FlashlightActivity;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

public class FlashlightButton extends WidgetButton {

	static FlashlightButton ownButton=null;

	static Camera camera=null;
	static Boolean isLedSupported=null;

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
		int state=SettingsAppWidgetProvider.STATE_INTERMEDIATE;
		if (isLedSupported!=null && isLedSupported==true) {
			Camera tmpCam= camera;
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
			}
		} else {
			return SettingsAppWidgetProvider.STATE_DISABLED;
		}
		return state;
	}

	/**
	 * Toggles the state 
	 * 
	 * @param context
	 */
	public void toggleState(Context context) {
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

		if (isLedSupported) {
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
		} else {
			Intent intent = new Intent(context, FlashlightActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
			context.startActivity(intent);
		}
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

