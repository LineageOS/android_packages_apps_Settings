package com.android.settings.widget.buttons;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.android.settings.R;
import com.android.settings.widget.FlashlightActivity;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.WidgetSettings;

public class FlashlightButton extends WidgetButton {

	static FlashlightButton ownButton=null;

	public void updateState(Context context,
			SharedPreferences globalPreferences, int[] appWidgetIds) {
		
		currentIcon=R.drawable.ic_appwidget_settings_flashlight_off;
		currentState=SettingsAppWidgetProvider.STATE_DISABLED;					
	}
	
	/**
	 * Toggles the state 
	 * 
	 * @param context
	 */
	public void toggleState(Context context) {
			toogleViaScreen(context);
	}

	private void toogleViaScreen(Context context) {
		Intent intent = new Intent(context, FlashlightActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			
		context.startActivity(intent);
	}

	public static FlashlightButton getInstance() {
		if (ownButton==null)
			ownButton = new FlashlightButton();

		return ownButton;
	}

	@Override
	void initButton() {
		buttonID=WidgetButton.BUTTON_FLASHLIGHT;
		preferenceName=WidgetSettings.TOGGLE_FLASHLIGHT;
	}

}

