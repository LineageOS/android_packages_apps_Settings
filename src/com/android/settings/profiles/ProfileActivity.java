/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.profiles;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Profile;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import com.android.settings.R;


public class ProfileActivity extends Activity {

    public static final String EXTRA_NEW_PROFILE = "new_profile_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        if (savedInstanceState == null) {
            final Profile profile = (Profile) (getIntent() != null ? getIntent().getParcelableExtra("profile") : null);
            if (profile != null) {
                // we are modifying a profile
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, SetupTriggersFragment.newInstance(profile, false),
                                "triggers")
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, SetupTriggersFragment.newInstance(new Profile("Nameless profile"), true),
                                "triggers")
                        .commit();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = fragmentManager.findFragmentByTag("triggers");
            if (fragment != null) {
                ((SetupTriggersFragment) fragment).onNfcIntent(intent);
            }
        }
    }

}
