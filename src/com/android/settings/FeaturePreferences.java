/*
 * Copyright (C) 2016 The Paranoid Android Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import com.android.internal.logging.MetricsLogger;

import java.util.ArrayList;

public class FeaturePreferences extends InstrumentedFragment {

    private static final String TAG = "FeaturePreferences";

    private SettingsListAdapter mSettingsAdapter;
    private Button mResetAllButton;
    private Button mResetSelectedButton;

    private ContentResolver mCr;

    private ArrayList<CheckBoxPreference> customPrefs = new ArrayList<>();

    private final Button.OnClickListener mButtonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mResetAllButton) {
                for (String setting : Settings.Secure.SETTINGS_TO_RESET) {
                    Settings.Secure.putInt(mCr, setting, 0);
                }
            } else if (v == mResetSelectedButton) {
                if (customPrefs != null) {
                    for (int i = 0; i < customPrefs.size(); i++) {
                        CheckBoxPreference pref = mSettingsAdapter.getItem(i);
                        if(pref.isChecked()) {
                            Settings.Secure.putInt(mCr, customPrefs.get(i).getKey(), 0);
                        }
                    }
                }
            }
            updateActiveCustomPreferences();
            mResetSelectedButton.setEnabled(false);
        }
    };

    private void updateActiveCustomPreferences() {
        customPrefs.clear();
        try {
            Context con = getActivity().getApplicationContext()
                    .createPackageContext("com.android.systemui", 0);
            Resources r = con.getResources();
            for (String setting : Settings.Secure.SETTINGS_TO_RESET) {
                String key = setting.toLowerCase();
                int nameResId = r.getIdentifier(setting + "_name", "string", "com.android.systemui");
                int descResId = r.getIdentifier(setting + "_summary", "string", "com.android.systemui");
                if (nameResId != 0 && descResId != 0) {
                    try {
                        String name = (String) r.getText(nameResId);
                        String desc = (String) r.getText(descResId);
                        CheckBoxPreference item = new CheckBoxPreference(getContext());
                        item.setKey(key);
                        item.setTitle(name);
                        item.setSummary(desc);
                        item.setEnabled(!(Settings.Secure.getInt(mCr, setting, 0) == 0));
                        customPrefs.add(item);
                    } catch (Resources.NotFoundException e) {
                        Log.e(TAG, "Resource not found for: " + setting, e);
                    }
                } else {
                    Log.v(TAG, "Missing strings for: " + setting);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException", e);
        }
        mSettingsAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mCr = getContext().getContentResolver();

        View mainView = inflater.inflate(R.layout.feature_preferences, null);

        mResetAllButton = (Button) mainView.findViewById(R.id.reset_all_prefs);
        mResetSelectedButton = (Button) mainView.findViewById(R.id.reset_selected_prefs);
        ListView settingsList = (ListView) mainView.findViewById(R.id.setting_list);

        mSettingsAdapter = new SettingsListAdapter(getContext(), customPrefs);
        settingsList.setAdapter(mSettingsAdapter);
        settingsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBoxPreference pref = (CheckBoxPreference) parent.getItemAtPosition(position);
                CheckBox cb = (CheckBox) view.findViewById(android.R.id.checkbox);
                if (pref.isEnabled()) {
                    pref.setChecked(!pref.isChecked());
                    cb.setChecked(pref.isChecked());
                }
                updateResetSelectedButton();
            }
        });

        mResetAllButton.setOnClickListener(mButtonListener);
        mResetSelectedButton.setOnClickListener(mButtonListener);

        updateActiveCustomPreferences();

        return mainView;
    }

    private void updateResetSelectedButton() {
        for(CheckBoxPreference pref : customPrefs) {
            if(pref.isChecked()) {
                mResetSelectedButton.setEnabled(true);
                return;
            }
        }
        mResetSelectedButton.setEnabled(false);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FEATURE_PREFERENCES;
    }

    private class SettingsListAdapter extends ArrayAdapter<CheckBoxPreference> {

        public SettingsListAdapter(Context context, ArrayList<CheckBoxPreference> items) {
            super(context, R.layout.preference, items);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CheckBoxPreference preference = getItem(position);
            convertView = preference.getView(convertView, parent);
            convertView.setBackgroundColor(android.R.color.transparent);

            return convertView;
        }
    }
}
