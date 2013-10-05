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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Handler;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.settings.cyanogenmod.LtoService;

import org.cyanogenmod.hardware.LongTermOrbits;

import java.util.Observable;
import java.util.Observer;

/**
 * Gesture lock pattern settings.
 */
public class LocationSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // Location Settings
    public static final String KEY_LOCATION_TOGGLE = "location_toggle";
    private static final String KEY_LOCATION_NETWORK = "location_network";
    private static final String KEY_LOCATION_GPS = "location_gps";
    private static final String KEY_ASSISTED_GPS = "assisted_gps";
    public static final String KEY_GPS_DOWNLOAD_DATA_WIFI_ONLY = "gps_download_data_wifi_only";

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;
    private CheckBoxPreference mGpsDownloadDataWifiOnly;
    private SwitchPreference mLocationAccess;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;

    @Override
    public void onStart() {
        super.onStart();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
        mContentQueryMap.close();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = getPreferenceScreen();

        mLocationAccess = (SwitchPreference) root.findPreference(KEY_LOCATION_TOGGLE);
        mNetwork = (CheckBoxPreference) root.findPreference(KEY_LOCATION_NETWORK);
        mGps = (CheckBoxPreference) root.findPreference(KEY_LOCATION_GPS);
        mAssistedGps = (CheckBoxPreference) root.findPreference(KEY_ASSISTED_GPS);
        mGpsDownloadDataWifiOnly =
                (CheckBoxPreference) root.findPreference(KEY_GPS_DOWNLOAD_DATA_WIFI_ONLY);

        // Only enable these controls if this user is allowed to change location
        // sharing settings.
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        boolean isToggleAllowed = !um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
        if (mLocationAccess != null) mLocationAccess.setEnabled(isToggleAllowed);
        if (mNetwork != null) mNetwork.setEnabled(isToggleAllowed);
        if (mGps != null) mGps.setEnabled(isToggleAllowed);
        if (mAssistedGps != null) mAssistedGps.setEnabled(isToggleAllowed);
        if (mGpsDownloadDataWifiOnly != null) mGpsDownloadDataWifiOnly.setEnabled(isToggleAllowed);

        if (!LongTermOrbits.isSupported()) {
            root.removePreference(mGpsDownloadDataWifiOnly);
            mGpsDownloadDataWifiOnly = null;
        } else {
            if (saveDownloadDataWifiOnlyPref(getActivity())) {
                root.removePreference(mGpsDownloadDataWifiOnly);
                mGpsDownloadDataWifiOnly = null;
            }
        }

        mLocationAccess.setOnPreferenceChangeListener(this);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
        updateLocationToggles();

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    updateLocationToggles();
                }
            };
        }

        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final ContentResolver cr = getContentResolver();
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (preference == mNetwork) {
            if (!um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
                Settings.Secure.setLocationProviderEnabled(cr,
                        LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
            }
        } else if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            if (!um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
                Settings.Secure.setLocationProviderEnabled(cr,
                        LocationManager.GPS_PROVIDER, enabled);
                if (mAssistedGps != null) {
                    mAssistedGps.setEnabled(enabled);
                }
            }
        } else if (preference == mAssistedGps) {
            Settings.Global.putInt(cr, Settings.Global.ASSISTED_GPS_ENABLED,
                    mAssistedGps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateLocationToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER);
        mGps.setChecked(gpsEnabled);
        mNetwork.setChecked(networkEnabled);
        mLocationAccess.setChecked(gpsEnabled || networkEnabled);
        if (mAssistedGps != null) {
            mAssistedGps.setChecked(Settings.Global.getInt(res,
                    Settings.Global.ASSISTED_GPS_ENABLED, 2) == 1);
            mAssistedGps.setEnabled(gpsEnabled);
        }
        if (mGpsDownloadDataWifiOnly != null) {
            mGpsDownloadDataWifiOnly.setEnabled(gpsEnabled);
        }
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    /** Enable or disable all providers when the master toggle is changed. */
    private void onToggleLocationAccess(boolean checked) {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
            return;
        }
        final ContentResolver cr = getContentResolver();
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.GPS_PROVIDER, checked);
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.NETWORK_PROVIDER, checked);
        updateLocationToggles();
        updateLtoServiceStatus(getActivity(), checked);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (pref.getKey().equals(KEY_LOCATION_TOGGLE)) {
            onToggleLocationAccess((Boolean) newValue);
        }
        return true;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    private static void updateLtoServiceStatus(Context context, boolean start) {
        Intent intent = new Intent(context, LtoService.class);
        if (start) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    /**
     * Restore the properties associated with this preference on boot
     * @param ctx A valid context
     */
    public static void restore(final Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Start the Lto Service
        if (LongTermOrbits.isSupported() && prefs.getBoolean(KEY_LOCATION_TOGGLE, false)) {
            saveDownloadDataWifiOnlyPref(context);

            // Starts the LtoService, but delayed 2 minutes after boot (this should give a
            // proper time to start all device services)
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, LtoService.class);
            PendingIntent pi = PendingIntent.getService(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            long nextLtoDownload = System.currentTimeMillis() + (1000 * 60 * 2L);
            am.set(AlarmManager.RTC, nextLtoDownload, pi);
        }
    }

    private static boolean saveDownloadDataWifiOnlyPref(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean supportsTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        boolean supportsWifi = pm.hasSystemFeature(PackageManager.FEATURE_WIFI);
        if (!supportsWifi || !supportsTelephony) {
            SharedPreferences.Editor editor =
                    PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean(KEY_GPS_DOWNLOAD_DATA_WIFI_ONLY, supportsWifi);
            editor.apply();
            return true;
        }
        return false;
    }
}

class WrappingSwitchPreference extends SwitchPreference {

    public WrappingSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WrappingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }
}

class WrappingCheckBoxPreference extends CheckBoxPreference {

    public WrappingCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WrappingCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }
}
