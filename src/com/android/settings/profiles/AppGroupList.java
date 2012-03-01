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
import android.app.NotificationGroup;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AppGroupList extends SettingsPreferenceFragment {

    private static final String TAG = "AppGroupSettings";

    public static final String PROFILE_SERVICE = "profile";

    private static final int MENU_ADD = Menu.FIRST;

    private ProfileManager mProfileManager;

    // constant value that can be used to check return code from sub activity.
    private static final int APP_GROUP_CONFIG = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.appgroup_list);
            mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem addAppGroup = menu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add)
                .setAlphabeticShortcut('a');
        addAppGroup.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                addAppGroup();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fillList();
    }

    private void addAppGroup() {
        Context context = getActivity();
        if (context != null) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            final EditText entry = new EditText(context);
            entry.setPadding(10, 10, 10, 10);
            dialog.setMessage(R.string.profile_appgroup_name_prompt);
            dialog.setView(entry);
            dialog.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NotificationGroup newGroup = new NotificationGroup(entry.getText().toString());
                            mProfileManager.addNotificationGroup(newGroup);
                            fillList();
                        }
                    });
            dialog.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            dialog.create().show();
        }
    }

    private void fillList() {
        PreferenceScreen appgroupList = getPreferenceScreen();
        appgroupList.removeAll();

        // Add the existing app groups
        for (NotificationGroup group : mProfileManager.getNotificationGroups()) {
            PreferenceScreen pref = new PreferenceScreen(getActivity(), null);
            pref.setKey(group.getUuid().toString());
            pref.setTitle(group.getName());
            pref.setPersistent(false);
            appgroupList.addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            NotificationGroup group = mProfileManager.getNotificationGroup(
                    UUID.fromString(preference.getKey()));
            editGroup(group);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void editGroup(NotificationGroup group) {
        Bundle args = new Bundle();
        args.putParcelable("NotificationGroup", group);

        PreferenceActivity pa = (PreferenceActivity) getActivity();
        pa.startPreferencePanel(AppGroupConfig.class.getName(), args,
                0, group.getName().toString(), this, APP_GROUP_CONFIG);
    }
}
