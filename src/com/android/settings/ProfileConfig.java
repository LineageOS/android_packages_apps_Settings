/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Profile;
import android.app.ProfileGroup;
import android.app.ProfileManager;
import android.app.StreamSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.widget.SeekBar;
import android.widget.Toast;

public class ProfileConfig extends PreferenceActivity implements OnPreferenceChangeListener {

    static final String TAG = "ProfileConfig";

    private static final int DELETE_CONFIRM = 0;

    private ProfileManager mProfileManager;

    private Profile mProfile;

    private EditTextPreference mNamePreference;

    private PreferenceScreen mDeletePreference;

    //Below line intended for use with upcoming Status Bar Indicator functionality
    //private CheckBoxPreference mStatusBarPreference;

    private StreamItem[] mStreams;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mStreams = new StreamItem[] {
                new StreamItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_volume_title)),
                new StreamItem(AudioManager.STREAM_MUSIC, getString(R.string.media_volume_title)),
                new StreamItem(AudioManager.STREAM_RING,
                        getString(R.string.incoming_call_volume_title)),
                new StreamItem(AudioManager.STREAM_NOTIFICATION,
                        getString(R.string.notification_volume_title))
        };

        addPreferencesFromResource(R.xml.profile_config);
        getListView().setItemsCanFocus(true);

        mProfileManager = (ProfileManager) this.getSystemService(PROFILE_SERVICE);

        Intent input = getIntent();
        Bundle extras = input.getExtras();
        if (extras != null) {
            mProfile = extras.getParcelable("Profile");
        }

        if (mProfile == null) {
            mProfile = new Profile(getString(R.string.new_profile_name));
            mProfileManager.addProfile(mProfile);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        mProfile = mProfileManager.getProfile(mProfile.getUuid());

        fillList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save profile here
        if (mProfile != null) {
            mProfileManager.addProfile(mProfile);
        }
    }

    private void fillList() {

        mDeletePreference = (PreferenceScreen) findPreference("profile_delete");

        mNamePreference = (EditTextPreference) findPreference("profile_name");

        mNamePreference.setText(mProfile.getName());
        mNamePreference.setSummary(mProfile.getName());
        mNamePreference.setOnPreferenceChangeListener(this);

        //Below lines intended for use with upcoming Status Bar Indicator functionality
        //mStatusBarPreference = (CheckBoxPreference) findPreference("profile_statusbar");
        //mStatusBarPreference.setChecked(mProfile.getStatusBarIndicator());
        //mStatusBarPreference.setOnPreferenceChangeListener(this);

        PreferenceGroup streamList = (PreferenceGroup) findPreference("profile_volumeoverrides");
        streamList.removeAll();

        for (StreamItem stream : mStreams) {
            StreamSettings settings = mProfile.getSettingsForStream(stream.mStreamId);
            if (settings == null) {
                settings = new StreamSettings(stream.mStreamId);
                mProfile.setStreamSettings(settings);
            }
            stream.mSettings = settings;
            StreamVolumePreference pref = new StreamVolumePreference(this);
            pref.setKey("stream_" + stream.mStreamId);
            pref.setTitle(stream.mLabel);
            pref.setSummary(getString(R.string.profile_volumeoverrides_summary));
            pref.setPersistent(false);
            pref.setStreamItem(stream);

            stream.mCheckbox = pref;
            streamList.addPreference(pref);
        }

        PreferenceGroup groupList = (PreferenceGroup) findPreference("profile_appgroups");
        groupList.removeAll();

        for (ProfileGroup profileGroup : mProfile.getProfileGroups()) {

            PreferenceScreen pref = new PreferenceScreen(this, null);

            pref.setKey(profileGroup.getName());
            pref.setTitle(profileGroup.getName());
            // pref.setSummary(R.string.profile_summary);
            pref.setPersistent(false);
            // pref.setSelectable(true);

            groupList.addPreference(pref);

        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof StreamVolumePreference) {
            for (StreamItem stream : mStreams) {
                if (preference == stream.mCheckbox) {
                    stream.mSettings.setOverride((Boolean) newValue);
                }
            }
        }
        // Check name isn't already in use.
        if (preference == mNamePreference) {
            String value = (String) newValue;
            if (mProfileManager.profileExists(value)) {
                // Rollback the change.
                return false;
            }
            boolean active = mProfile.getUuid()
                    .equals(mProfileManager.getActiveProfile().getUuid());
            mProfileManager.removeProfile(mProfile);
            mProfile.setName(value);
            preference.setSummary(value);
            mProfileManager.addProfile(mProfile);
            if (active) {
                mProfileManager.setActiveProfile(mProfile.getUuid());
            }
        }
        //Below lines intended for use with upcoming Status Bar Indicator functionality
        /*
        if (preference == mStatusBarPreference) {
            boolean active = mProfile.getUuid()
                    .equals(mProfileManager.getActiveProfile().getUuid());
            mProfileManager.removeProfile(mProfile);
            mProfile.setStatusBarIndicator((Boolean) newValue);
            mProfileManager.addProfile(mProfile);
            if (active) {
                mProfileManager.setActiveProfile(mProfile.getUuid());
            }
        }
        */
        return true;
    }

    private StreamItem mPreferenceItem = null;

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            if (preference == mDeletePreference) {
                deleteProfile();
            } else {
                ProfileGroup profGroup = mProfile.getProfileGroup(preference.getTitle().toString());

                Intent intent = new Intent(this, ProfileGroupConfig.class);
                intent.putExtra("ProfileGroup", profGroup.getName());
                intent.putExtra("Profile", mProfile);
                startActivity(intent);
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void deleteProfile() {
        if (mProfile.getUuid().equals(mProfileManager.getActiveProfile().getUuid())) {
            Toast toast = Toast.makeText(this, getString(R.string.profile_cannot_delete),
                    Toast.LENGTH_SHORT);
            toast.show();
        } else {
            showDialog(DELETE_CONFIRM);
        }
    }

    private void doDelete() {
        mProfileManager.removeProfile(mProfile);
        mProfile = null;
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;
        switch (id) {
            case DELETE_CONFIRM:
                builder.setMessage(R.string.profile_delete_confirm);
                builder.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doDelete();
                            }
                        });
                builder.setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    static class StreamItem {
        int mStreamId;

        String mLabel;

        StreamSettings mSettings;

        StreamVolumePreference mCheckbox;

        public StreamItem(int streamId, String label) {
            mStreamId = streamId;
            mLabel = label;
        }
    }

}
