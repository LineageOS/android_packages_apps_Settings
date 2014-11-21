package com.android.settings.cyanogenmod;


import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class StatusBarSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.status_bar_settings);

    }
}
