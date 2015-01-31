package com.android.settings.notification;

import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NotificationManagerSettings extends SettingsPreferenceFragment {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_manager_settings);
    }
}
