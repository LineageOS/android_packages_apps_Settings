
package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.android.internal.util.weather.HttpRetriever;
import com.android.internal.util.weather.WeatherInfo;
import com.android.internal.util.weather.WeatherXmlParser;
import com.android.internal.util.weather.YahooPlaceFinder;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Weather extends SettingsPreferenceFragment implements
    OnPreferenceChangeListener, OnPreferenceClickListener {

    public static final String TAG = "Weather";
    public static final String KEY_USE_METRIC = "use_metric";
    public static final String KEY_USE_CUSTOM_LOCATION = "use_custom_location";
    public static final String KEY_CUSTOM_LOCATION = "custom_location";
    public static final String KEY_SHOW_LOCATION = "show_location";
    public static final String KEY_SHOW_TIMESTAMP = "show_timestamp";
    public static final String KEY_ENABLE_WEATHER = "enable_weather";
    public static final String KEY_REFRESH_INTERVAL = "refresh_interval";
    public static final String KEY_INVERT_LOWHIGH = "invert_lowhigh";
    private static final int WEATHER_CHECK = 0;

    private CheckBoxPreference mEnableWeather;
    private CheckBoxPreference mUseCustomLoc;
    private CheckBoxPreference mShowLocation;
    private CheckBoxPreference mShowTimestamp;
    private CheckBoxPreference mUseMetric;
    private CheckBoxPreference mInvertLowHigh;
    private ListPreference mWeatherSyncInterval;
    private EditTextPreference mCustomWeatherLoc;
    private Context mContext;
    private ContentResolver mResolver;
    private ProgressDialog mProgressDialog;

    private static final int LOC_WARNING = 101;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.weather_prefs);
        mContext = getActivity();
        mResolver = getContentResolver();

        // Setup the preferences
        mEnableWeather = (CheckBoxPreference) findPreference(KEY_ENABLE_WEATHER);
        mEnableWeather.setChecked(Settings.System.getInt(mResolver,
                Settings.System.LOCKSCREEN_WEATHER, 0) == 1);

        mUseCustomLoc = (CheckBoxPreference) findPreference(KEY_USE_CUSTOM_LOCATION);
        mUseCustomLoc.setChecked(Settings.System.getInt(mResolver,
                Settings.System.WEATHER_USE_CUSTOM_LOCATION, 0) == 1);
        mCustomWeatherLoc = (EditTextPreference) findPreference(KEY_CUSTOM_LOCATION);
        updateLocationSummary();
        mCustomWeatherLoc.setOnPreferenceClickListener(this);

        mShowLocation = (CheckBoxPreference) findPreference(KEY_SHOW_LOCATION);
        mShowLocation.setChecked(Settings.System.getInt(mResolver,
                Settings.System.WEATHER_SHOW_LOCATION, 1) == 1);

        mShowTimestamp = (CheckBoxPreference) findPreference(KEY_SHOW_TIMESTAMP);
        mShowTimestamp.setChecked(Settings.System.getInt(mResolver,
                Settings.System.WEATHER_SHOW_TIMESTAMP, 1) == 1);

        mUseMetric = (CheckBoxPreference) findPreference(KEY_USE_METRIC);
        mUseMetric.setChecked(Settings.System.getInt(mResolver,
                Settings.System.WEATHER_USE_METRIC, 1) == 1);

        mInvertLowHigh = (CheckBoxPreference) findPreference(KEY_INVERT_LOWHIGH);
        mInvertLowHigh.setChecked(Settings.System.getInt(mResolver,
                Settings.System.WEATHER_INVERT_LOWHIGH, 0) == 1);

        mWeatherSyncInterval = (ListPreference) findPreference(KEY_REFRESH_INTERVAL);
        int weatherInterval = Settings.System.getInt(mResolver,
                Settings.System.WEATHER_UPDATE_INTERVAL, 60);
        mWeatherSyncInterval.setValue(String.valueOf(weatherInterval));
        mWeatherSyncInterval.setSummary(mapUpdateValue(weatherInterval));
        mWeatherSyncInterval.setOnPreferenceChangeListener(this);

        if (!Settings.Secure.isLocationProviderEnabled(mResolver,
                LocationManager.NETWORK_PROVIDER)
                && !mUseCustomLoc.isChecked()) {
            showDialog(LOC_WARNING);
        }
    }

    private void updateLocationSummary() {
        if (mUseCustomLoc.isChecked()) {
            String location = Settings.System.getString(mResolver,
                    Settings.System.WEATHER_CUSTOM_LOCATION);
            if (location != null) {
                mCustomWeatherLoc.setSummary(location);
            } else {
                mCustomWeatherLoc.setSummary(R.string.unknown);
            }
        } else {
            mCustomWeatherLoc.setSummary(R.string.weather_geolocated);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableWeather) {
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_WEATHER,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;

        } else if (preference == mUseCustomLoc) {
            Settings.System.putInt(mResolver, Settings.System.WEATHER_USE_CUSTOM_LOCATION,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            updateLocationSummary();
            return true;

        } else if (preference == mShowLocation) {
            Settings.System.putInt(mResolver, Settings.System.WEATHER_SHOW_LOCATION,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;

        } else if (preference == mUseMetric) {
            Settings.System.putInt(mResolver, Settings.System.WEATHER_USE_METRIC,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowTimestamp) {
            Settings.System.putInt(mResolver, Settings.System.WEATHER_SHOW_TIMESTAMP,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mInvertLowHigh) {
            Settings.System.putInt(mResolver, Settings.System.WEATHER_INVERT_LOWHIGH,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWeatherSyncInterval) {
            int newVal = Integer.parseInt((String) newValue);
            Settings.System.putInt(mResolver, Settings.System.WEATHER_UPDATE_INTERVAL, newVal);
            mWeatherSyncInterval.setValue((String) newValue);
            mWeatherSyncInterval.setSummary(mapUpdateValue(newVal));
            preference.setSummary(mapUpdateValue(newVal));
        }
        return false;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case WEATHER_CHECK:
                if (msg.obj == null) {
                    Toast.makeText(mContext, mContext.getString(R.string.weather_retrieve_location_dialog_title),
                            Toast.LENGTH_SHORT).show();
                } else {
                    String cLoc = mCustomWeatherLoc.getEditText().getText().toString();
                    mCustomWeatherLoc.setText(cLoc);
                    Settings.System.putString(mResolver, Settings.System.WEATHER_CUSTOM_LOCATION, cLoc);
                    mCustomWeatherLoc.setSummary(cLoc);
                    mCustomWeatherLoc.getDialog().dismiss();
                }
                mProgressDialog.dismiss();
                break;
            }
        }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference == mCustomWeatherLoc) {
            String location = Settings.System.getString(mResolver,
                    Settings.System.WEATHER_CUSTOM_LOCATION);
            if (location != null) {
                mCustomWeatherLoc.getEditText().setText(location);
                mCustomWeatherLoc.getEditText().setSelection(location.length());
            } else {
                mCustomWeatherLoc.getEditText().setText("");
            }
            mCustomWeatherLoc.getDialog().findViewById(android.R.id.button1)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressDialog = new ProgressDialog(mContext);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialog.setMessage(mContext.getString(R.string.weather_progress_title));
                    mProgressDialog.show();
                    new Thread(new Runnable(){
                        @Override
                        public void run() {
                            String woeid = null;
                            try {
                                woeid = YahooPlaceFinder.GeoCode(mContext,
                                        mCustomWeatherLoc.getEditText().getText().toString());
                            } catch (Exception e) {
                            }
                            Message msg = Message.obtain();
                            msg.what = WEATHER_CHECK;
                            msg.obj = woeid;
                            mHandler.sendMessage(msg);
                        }
                    }).start();
                }
            });
            return true;
        }

        return false;
    }

    /**
     * Utility classes and supporting methods
     */

    private String mapUpdateValue(Integer time) {
        Resources resources = mContext.getResources();

        String[] timeNames = resources.getStringArray(R.array.weather_interval_entries);
        String[] timeValues = resources.getStringArray(R.array.weather_interval_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return mContext.getString(R.string.unknown);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final Dialog dialog;

        switch (dialogId) {
            case LOC_WARNING:
                builder.setTitle(R.string.weather_retrieve_location_dialog_title);
                builder.setMessage(R.string.weather_retrieve_location_dialog_message);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.weather_retrieve_location_dialog_enable_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Settings.Secure.setLocationProviderEnabled(mResolver,
                                        LocationManager.NETWORK_PROVIDER, true);
                            }
                        });
                builder.setNegativeButton(R.string.cancel, null);
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }
}
