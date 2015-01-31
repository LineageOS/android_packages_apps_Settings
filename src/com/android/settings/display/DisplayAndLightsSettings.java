package com.android.settings.display;

import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;


public class DisplayAndLightsSettings extends SettingsPreferenceFragment {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.display_and_sound_settings);
    }
}
