package com.android.settings.aim.tabs;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import android.os.Bundle;
import com.android.settings.R;

import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.Utils;

import android.telephony.TelephonyManager;
public class Misc extends SettingsPreferenceFragment {

private static final String AIM_INCALL = "aim_incall";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aim_misc_tab);
        PreferenceScreen prefScreen = getPreferenceScreen();
        PreferenceCategory incallVibCategory = (PreferenceCategory) findPreference(AIM_INCALL);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(incallVibCategory);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATION;
    }
}
