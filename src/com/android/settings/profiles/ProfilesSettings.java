/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import java.util.UUID;

import android.app.AlertDialog;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ProfilesSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ProfilesSettings";

    public static final String EXTRA_POSITION = "position";

    public static final String PROFILE_SERVICE = "profile";

    public static final String RESTORE_CARRIERS_URI = "content://telephony/carriers/restore";

    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";

    private static final int MENU_RESET = Menu.FIRST;

    private static final int MENU_ADD = Menu.FIRST + 1;

    private Profile mProfile;

    private String mSelectedKey;

    private ProfileManager mProfileManager;

    // constant value that can be used to check return code from sub activity.
    private static final int PROFILE_DETAILS = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.profiles_settings);

            mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE); 

            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Reset all button
        MenuItem resetAll = menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setIcon(R.drawable.ic_menu_refresh_holo_dark)
                .setAlphabeticShortcut('r');
        resetAll.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem addProfile = menu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add)
                .setAlphabeticShortcut('a');
        addProfile.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetProfiles();
                return true;
            case MENU_ADD:
                addProfile();
                return true;
            default:
                return false;
        }
    }

    private void addProfile() {
        // TODO: Consider adding a random character/number to the end of the profile name to make it unique
        mProfile = new Profile(getString(R.string.new_profile_name));
        mProfileManager.addProfile(mProfile);

        // Start the profile details preference screen
        Bundle args = new Bundle();
        args.putParcelable("Profile", mProfile);
        PreferenceActivity pa = (PreferenceActivity) getActivity();
        pa.startPreferencePanel(ProfileConfig.class.getName(), args,
                R.string.profile_profile_manage, null, this, PROFILE_DETAILS);
    }

    private void resetProfiles() {
        Log.d(TAG, "resetProfiles(): entered");
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.profile_reset_title);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setMessage(R.string.profile_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mProfileManager.resetAll();
                fillList();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        alert.create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        fillList();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void fillList() {
        PreferenceScreen prefSet = getPreferenceScreen();

        PreferenceCategory plist = (PreferenceCategory) prefSet.findPreference("profiles_list");
        if (plist != null) {
            plist.removeAll();

            if (mProfileManager != null) {
                mSelectedKey = mProfileManager.getActiveProfile().getUuid().toString();
                for(Profile profile : mProfileManager.getProfiles()) {
                    Bundle args = new Bundle();
                    args.putParcelable("Profile", profile);

                    ProfilesPreference ppref = new ProfilesPreference(this, args);
                    ppref.setKey(profile.getUuid().toString());
                    ppref.setTitle(profile.getName());
                    ppref.setPersistent(false);
                    ppref.setOnPreferenceChangeListener(this);
                    ppref.setSelectable(true);

                    if ((mSelectedKey != null) && mSelectedKey.equals(ppref.getKey())) {
                        ppref.setChecked(true);
                    }

                    plist.addPreference(ppref);
                }
            }
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "Preference - " + preference + ", newValue - " + newValue);
        if (newValue instanceof String) {
            setSelectedProfile((String) newValue);
            fillList();
        }
        return true;
    }

    private void setSelectedProfile(String key) {
        try {
            UUID selectedUuid = UUID.fromString(key);
            mProfileManager.setActiveProfile(selectedUuid);
            mSelectedKey = key;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }
}
