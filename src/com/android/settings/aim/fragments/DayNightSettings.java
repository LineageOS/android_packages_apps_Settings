package com.android.settings.aim.fragments;

import com.android.internal.logging.MetricsLogger;

import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.util.Log;
import android.util.DisplayMetrics;

import com.android.settings.util.Helpers;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.List;

public class DayNightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DayNightSettings";
    
				// DayNight
    private static final String KEY_NIGHT_MODE = "night_mode";
    private ListPreference mNightModePreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.AIM;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.daynight);

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
    			 }
     }
     
     @Override
     public boolean onPreferenceChange(Preference preference, Object objValue) {
         final String key = preference.getKey();
         if (preference == mNightModePreference) {
             try {
                 final int value = Integer.parseInt((String) objValue);
                 final UiModeManager uiManager = (UiModeManager) getSystemService(
                         Context.UI_MODE_SERVICE);
                 uiManager.setNightMode(value);
                 Helpers.restartSystemUI();
             } catch (NumberFormatException e) {
                 Log.e(TAG, "could not persist night mode setting", e);
             }
         }
         return true; 
       }
}
