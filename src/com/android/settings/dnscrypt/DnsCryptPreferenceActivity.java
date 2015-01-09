/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.dnscrypt;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.dnscrypt.LoadConfigFileTask.ILoadConfigListener;

import java.io.File;

/**
 * DnsCryptSettingsFragment
 * <pre>
 *     Screen for configuring and starting stopping encrypted dns daemon
 * </pre>
 *
 * @see {@link android.preference.PreferenceActivity}
 * @see {@link android.preference.Preference.OnPreferenceChangeListener}
 */
public class DnsCryptPreferenceActivity extends PreferenceActivity implements
        OnPreferenceChangeListener, ILoadConfigListener {

    // Constants
    private static final String TAG = DnsCryptPreferenceActivity.class.getSimpleName();
    private static final String PREF_TOGGLE = "pref_toggle_dnscrypt";
    private static final String PREF_SERVER_LIST = "pref_server_list";
    private static final String CONFIG_FILE = "/system/etc/dnscrypt-resolvers.csv";

    // Members
    private SwitchPreference mEncryptionSwitchPreference;
    private ListPreference mServerListPreference;
    private LoadConfigFileTask mLoadConfigFileTask;

    // Views
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(R.string.dns_enc_title);
        addPreferencesFromResource(R.xml.dns_crypt_settings);
        mEncryptionSwitchPreference = (SwitchPreference) findPreference(PREF_TOGGLE);
        mEncryptionSwitchPreference.setOnPreferenceChangeListener(this);
        mServerListPreference = (ListPreference) findPreference(PREF_SERVER_LIST);
        mServerListPreference.setOnPreferenceChangeListener(this);
        mProgressDialog = new ProgressDialog(this);
        // [TODO][MSB]: Add to strings
        mProgressDialog.setTitle("Loading encrypted DNS resolver configuration...");
        mLoadConfigFileTask = new LoadConfigFileTask(new File(CONFIG_FILE), mProgressDialog, this);
        mLoadConfigFileTask.execute();
        initPreferences();
    }

    private void initPreferences() {
        boolean currentCheckedState = (Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.DNS_ENCRYPTION_TOGGLE,
                Settings.Secure.DNS_ENCRYPTION_TOGGLE_DEFAULT) == 1);
        if (mEncryptionSwitchPreference != null) {
            mEncryptionSwitchPreference.setChecked(currentCheckedState);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mEncryptionSwitchPreference) {
            Boolean newCheckedState = (Boolean) o;
            mServerListPreference.setEnabled(newCheckedState);
            int setValue = (newCheckedState) ? 1 : 0;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.DNS_ENCRYPTION_TOGGLE,
                    setValue);
            return true;
        } else if (preference == mServerListPreference) {
            String value = (String) o;
            int i;
            for (i = 0; i < mServerListPreference.getEntryValues().length; i++) {
                if (value.equals(mServerListPreference.getEntryValues()[i])) {
                    break;
                }
            }
            try {
                mServerListPreference.setSummary("DNS Server: " // [TODO][MSB]: Strings.xml
                        + mServerListPreference.getEntries()[i]);
            } catch (ArrayIndexOutOfBoundsException oobe) {
                Log.e(TAG, "Error finding default value!");
                mServerListPreference.setSummary(R.string.dns_enc_server_summary);
            }
            Settings.Secure.putString(getContentResolver(), Settings.Secure
                    .DNS_ENCRYPTION_SERVER, value);
            return true;
        }
        return false;
    }

    @Override
    public void onConfigLoaded(CharSequence[] entries, CharSequence[] entryValues) {
        if (mServerListPreference != null && entries != null && entryValues != null) {
            mServerListPreference.setEntries(entries);
            mServerListPreference.setEntryValues(entryValues);
            String value = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.DNS_ENCRYPTION_SERVER);
            value = (value == null) ? Settings.Secure.DNS_ENCRYPTION_SERVER_DEFAULT : value;
            mServerListPreference.setValue(value);
            int i;
            for (i = 0; i < mServerListPreference.getEntryValues().length; i++) {
                if (value.equals(mServerListPreference.getEntryValues()[i])) {
                    break;
                }
            }
            try {
                mServerListPreference.setSummary("DNS Server: " // [TODO][MSB]: Strings.xml
                        + mServerListPreference.getEntries()[i]);
            } catch (ArrayIndexOutOfBoundsException oobe) {
                Log.e(TAG, "Error finding default value!");
                mServerListPreference.setSummary(R.string.dns_enc_server_summary);
            }
            mServerListPreference.setEnabled(mEncryptionSwitchPreference.isChecked());
        }
        mLoadConfigFileTask = null;
    }
}
