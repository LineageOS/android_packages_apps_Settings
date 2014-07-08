package com.android.settings.applications;

import android.os.Bundle;

import com.android.settings.R;

public class HiddenAppsActivity extends ProtectedAppsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIsProtectedApps = false;
        super.onCreate(savedInstanceState);
        setTitle(R.string.hidden_apps);
    }

}
