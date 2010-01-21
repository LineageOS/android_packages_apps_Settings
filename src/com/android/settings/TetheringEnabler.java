package com.android.settings;

import android.content.Context;
import android.os.SystemProperties;
import android.os.SystemService;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

/**
 * @author shade
 *
 * TODO: Actually check if tethering started/stopped
 * 
 */
public class TetheringEnabler implements OnPreferenceChangeListener {

	private final Context mContext;
    
    private final CheckBoxPreference mCheckBoxPref;
    
    private static final String TETHER_ON = "tether_on";
    
    private static final String TETHER_OFF = "tether_off";
    
    private static final String TETHER_PROP = "tethering.enabled";
    
    public TetheringEnabler(Context context, CheckBoxPreference pref) {
    	this.mContext = context;
    	this.mCheckBoxPref = pref;
    }
    
	/* (non-Javadoc)
	 * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
	 */
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		final Boolean v = (Boolean)newValue;
		setTetheringEnabled(v);
		mCheckBoxPref.setSummary(v ? R.string.internet_tethering_summary_on : R.string.internet_tethering_summary_off);
		return true;
	}

	private synchronized void setTetheringEnabled(boolean enabled) {
		if (enabled) {
			Log.i("tether", "Enabling tethering..");
			SystemService.start(TETHER_ON);
		} else {
			Log.i("tether", "Disabling tethering..");
			SystemService.start(TETHER_OFF);
		}
	}
	
	public void pause() {
		mCheckBoxPref.setOnPreferenceChangeListener(null);
	}
	
	public void resume() {
		mCheckBoxPref.setOnPreferenceChangeListener(this);
		final boolean isEnabled = SystemProperties.getBoolean(TETHER_PROP, false);
		mCheckBoxPref.setChecked(isEnabled);
		mCheckBoxPref.setSummary(isEnabled ? R.string.internet_tethering_summary_on : R.string.internet_tethering_summary_off);
	}
}
