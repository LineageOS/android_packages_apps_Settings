
package com.android.settings.widget;

import com.android.settings.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wimax.WimaxHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class WidgetSettings extends Activity {

    public static final String LAST_ICON_ID = "lastIconId";

    public static final String FIRST_ICON_ID = "firstIconId";

    public static final String RING_MODE_VIBRATE_AS_ON = "ringModeVibrateAsOn";

    public static final String BACKGROUND_IMAGE = "backgrounImage";

    public static final String USE_ROUND_CORNERS = "useRoundCorners";

    public static final String USE_VERTICAL = "useVertical";

    public static final String TOGGLE_BRIGHTNESS = "toggleBrightness";

    public static final String TOGGLE_AUTO_ROTATE = "toggleAutoRotate";

    public static final String TOGGLE_SCREEN_TIMEOUT = "toggleScreenTimeout";

    public static final String TOGGLE_SOUND = "toggleSound";

    public static final String TOGGLE_SYNC = "toggleSync";

    public static final String TOGGLE_2G3G = "toggle2G3G";

    public static final String TOGGLE_DATA = "toggleData";

    public static final String TOGGLE_GPS = "toggleGPS";

    public static final String TOGGLE_BLUETOOTH = "toggleBluetooth";

    public static final String TOGGLE_WIFI = "toggleWifi";

    public static final String TOGGLE_WIFI_AP = "toggleWifiAp";

    public static final String TOGGLE_WIMAX = "toggleWimax";

    public static final String TOGGLE_AIRPLANE = "toggleAirplane";

    public static final String TOGGLE_FLASHLIGHT = "toggleFlashlight";

    public static final String TOGGLE_LOCK_SCREEN = "toggleLockScreen";

    public static final String MONITOR_DATA_ROAMING = "monitorDataRoaming";

    public static final String AUTO_ENABLE_SYNC_WITH_WIFI = "autoEnableSyncWithWifi";

    public static final String AUTO_DISABLE_SYNC_WITH_WIFI = "autoDisableSyncWithWifi";

    public static final String AUTO_ENABLE_3G = "autoEnable3G";

    public static final String AUTO_DISABLE_3G = "autoDisable3G";

    public static final String AUTO_ENABLE_3G_WITH_WIFI = "autoEnable3GWithWifi";

    public static final String AUTO_DISABLE_3G_WITH_WIFI = "autoDisable3GWithWifi";

    public static final String AUTO_ENABLE_BLUETOOTH_WITH_POWER = "autoEnableBluetoothWithPower";

    public static final String AUTO_DISABLE_BLUETOOTH_WITH_POWER = "autoDisableBluetoothWithPower";

    public static final String AUTO_ENABLE_WIFI_WITH_POWER = "autoEnableWifiWithPower";

    public static final String AUTO_DISABLE_WIFI_WITH_POWER = "autoDisableWifiWithPower";

    public static final String NETWORK_MODE_SPINNER = "networkModeSpinner";

    public static final String BRIGHTNESS_SPINNER = "brightnessSpinner";

    public static final String RING_MODE_SPINNER = "ringModeSpinner";

    public static final String SCREEN_TIMEOUT_SPINNER = "screenTimeoutSpinner";

    public static final String SAVED = "saved";

    public static final String WIDGET_PREF_MAIN = "widget_MAIN";

    public static final String WIDGET_PREF_NAME = "widget_";

    protected static final int TRANSPARENT_BACKGROUND = 1;

    public static final String LAST_BUTTON = "lastButton";

    private static final int MAX_BUTTONS = 10;

    int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    SharedPreferences preferences;

    SharedPreferences preferencesGeneral;

    private ArrayList<CheckBox> selectedButtons = new ArrayList<CheckBox>();

    View.OnClickListener listener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v instanceof CheckBox) {
                toogleButtonView((CheckBox) v);
            }
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        initWidgetSettings();
        initToggleButtons();
        setDefaultReturn();
        initControls();
        initSettings();
    }

    private void initToggleButtons() {

        selectedButtons.add((CheckBox) findViewById(R.id.toggleWifi));
        selectedButtons.add((CheckBox) findViewById(R.id.toggleBluetooth));
        selectedButtons.add((CheckBox) findViewById(R.id.toggleGPS));
        selectedButtons.add((CheckBox) findViewById(R.id.toggleData));
        selectedButtons.add((CheckBox) findViewById(R.id.toggleSync));
        selectedButtons.add((CheckBox) findViewById(R.id.toggleBrightness));

        findViewById(R.id.toggleWifi).setOnClickListener(listener);
        findViewById(R.id.toggleBluetooth).setOnClickListener(listener);
        findViewById(R.id.toggleGPS).setOnClickListener(listener);
        findViewById(R.id.toggleData).setOnClickListener(listener);
        findViewById(R.id.toggle2g3g).setOnClickListener(listener);
        findViewById(R.id.toggleSync).setOnClickListener(listener);
        findViewById(R.id.toggleSound).setOnClickListener(listener);
        findViewById(R.id.toggleScreenTimeout).setOnClickListener(listener);
        findViewById(R.id.toggleAutoRotate).setOnClickListener(listener);
        findViewById(R.id.toggleBrightness).setOnClickListener(listener);
        findViewById(R.id.toggleAirplane).setOnClickListener(listener);
        findViewById(R.id.toggleLockScreen).setOnClickListener(listener);
        findViewById(R.id.toggleFlashlight).setOnClickListener(listener);
        findViewById(R.id.toggleWifiAp).setOnClickListener(listener);
        findViewById(R.id.toggleWimax).setOnClickListener(listener);
        findViewById(R.id.useRoundCorners).setOnClickListener(listener);
        ((Spinner) findViewById(R.id.backgroundImageSpinner))
                .setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                            long id) {
                        if (position == TRANSPARENT_BACKGROUND) {
                            findViewById(R.id.main).setBackgroundColor(Color.TRANSPARENT);
                        } else {
                            findViewById(R.id.main).setBackgroundResource(R.drawable.appwidget_bg);
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }

                });

    }

    private void toogleButtonView(CheckBox button) {
        if (button.getId() == R.id.backgroundImageSpinner) {
            if (((CheckBox) button).isChecked()) {
                findViewById(R.id.main).setBackgroundColor(Color.TRANSPARENT);
            } else {
                findViewById(R.id.main).setBackgroundResource(R.drawable.appwidget_bg);
            }
        } else if (button.getId() != R.id.useRoundCorners) {
            if (button.isChecked()) {

                // Add only if we still have buttons available on the layout
                if (selectedButtons.size() < MAX_BUTTONS) {
                    SettingsAppWidgetProvider.logD("Add button");
                    selectedButtons.add(button);
                } else {
                    button.setChecked(false);
                    SettingsAppWidgetProvider.logD("Button limit reached ");
                    Toast message = Toast.makeText(this, "Limited to " + MAX_BUTTONS + " buttons",
                            Toast.LENGTH_SHORT);
                    message.show();
                }
            } else {
                SettingsAppWidgetProvider.logD("Remove button");
                selectedButtons.remove(button);
            }
        }

        updateState();
    }

    private void updateState() {
        SettingsAppWidgetProvider.logD("Buttons present:" + selectedButtons.size());
        for (int posi = 1; posi <= MAX_BUTTONS; posi++) {

            if (posi <= selectedButtons.size()) {
                CheckBox buttonPresent = selectedButtons.get(posi - 1);
                View btn = getButton(posi);
                View sep = getSep(posi);
                ImageView img = getImg(posi);
                ImageView ind = getInd(posi);

                if (sep != null) {
                    sep.setVisibility(View.VISIBLE);
                }
                if (posi == 1 && ((CheckBox) findViewById(R.id.useRoundCorners)).isChecked()) {
                    ind.setImageResource(R.drawable.appwidget_settings_ind_on_l);
                } else if (posi == selectedButtons.size()
                        && ((CheckBox) findViewById(R.id.useRoundCorners)).isChecked()) {
                    ind.setImageResource(R.drawable.appwidget_settings_ind_on_r);
                } else {
                    ind.setImageResource(R.drawable.appwidget_settings_ind_on_c);
                }
                img.setImageResource(getIcon(buttonPresent));
                btn.setVisibility(View.VISIBLE);
            } else {
                View btn = getButton(posi);
                btn.setVisibility(View.GONE);
                View sep = getSep(posi);
                if (sep != null) {
                    sep.setVisibility(View.GONE);
                }
            }
        }
    }

    private void initWidgetSettings() {
        setContentView(R.layout.widget_settings);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    private void initControls() {
        findViewById(R.id.okbutton).setOnClickListener(okClickListener);
        findViewById(R.id.cancelbutton).setOnClickListener(cancelClickListener);
    }

    private void initSettings() {
        Log.d("WidgetSettings: ", "create preference for widget_" + widgetId);
        preferences = getSharedPreferences(WIDGET_PREF_NAME + widgetId, Context.MODE_PRIVATE);
        preferencesGeneral = getSharedPreferences(WIDGET_PREF_MAIN, Context.MODE_PRIVATE);
        ((CheckBox) findViewById(R.id.autoDisable3G)).setChecked(preferencesGeneral.getBoolean(
                AUTO_DISABLE_3G, false));
        ((CheckBox) findViewById(R.id.autoEnable3G)).setChecked(preferencesGeneral.getBoolean(
                AUTO_ENABLE_3G, false));
        ((CheckBox) findViewById(R.id.autoDisable3GWithWifi)).setChecked(preferencesGeneral
                .getBoolean(AUTO_DISABLE_3G_WITH_WIFI, false));
        ((CheckBox) findViewById(R.id.autoEnable3GWithWifi)).setChecked(preferencesGeneral
                .getBoolean(AUTO_ENABLE_3G_WITH_WIFI, false));
        ((CheckBox) findViewById(R.id.autoEnableBluetoothWithPower)).setChecked(preferencesGeneral
                .getBoolean(AUTO_ENABLE_BLUETOOTH_WITH_POWER, false));
        ((CheckBox) findViewById(R.id.autoDisableBluetoothWithPower)).setChecked(preferencesGeneral
                .getBoolean(AUTO_DISABLE_BLUETOOTH_WITH_POWER, false));
        ((CheckBox) findViewById(R.id.autoEnableWifiWithPower)).setChecked(preferencesGeneral
                .getBoolean(AUTO_ENABLE_WIFI_WITH_POWER, false));
        ((CheckBox) findViewById(R.id.autoDisableWifiWithPower)).setChecked(preferencesGeneral
                .getBoolean(AUTO_DISABLE_WIFI_WITH_POWER, false));
        ((CheckBox) findViewById(R.id.autoDisableSyncWithWifi)).setChecked(preferencesGeneral
                .getBoolean(AUTO_DISABLE_SYNC_WITH_WIFI, false));
        ((CheckBox) findViewById(R.id.autoEnableSyncWithWifi)).setChecked(preferencesGeneral
                .getBoolean(AUTO_ENABLE_SYNC_WITH_WIFI, false));
        ((CheckBox) findViewById(R.id.monitorDataRoaming)).setChecked(preferencesGeneral
                .getBoolean(MONITOR_DATA_ROAMING, false));
        ((CheckBox) findViewById(R.id.ringModeVibrateAsOn)).setChecked(preferencesGeneral
                .getBoolean(RING_MODE_VIBRATE_AS_ON, false));

        // disable the Wi-Fi AP preference if Wifi AP is not available
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getTetherableWifiRegexs().length <= 0) {
            findViewById(R.id.toggleWifiApPreference).setVisibility(View.GONE);
            findViewById(R.id.toggleWifiApPreferenceDivider).setVisibility(View.GONE);
        }

        // disable WiMAX preference if unsupported
        if (!WimaxHelper.isWimaxSupported(this)) {
            findViewById(R.id.toggleWimaxPreference).setVisibility(View.GONE);
            findViewById(R.id.toggleWimaxPreferenceDivider).setVisibility(View.GONE);
        }

        Spinner spinner = (Spinner) findViewById(R.id.brightnessSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.brightnessWidget, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(preferencesGeneral.getInt(BRIGHTNESS_SPINNER, 0));

        spinner = (Spinner) findViewById(R.id.screenTimeoutSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.screenTimeoutWidget,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(preferencesGeneral.getInt(SCREEN_TIMEOUT_SPINNER, 0));

        spinner = (Spinner) findViewById(R.id.networkModeSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.networkModesWidget,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(preferencesGeneral.getInt(NETWORK_MODE_SPINNER, 0));

        spinner = (Spinner) findViewById(R.id.ringModeSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.ringModeWidget,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(preferencesGeneral.getInt(RING_MODE_SPINNER, 0));

        spinner = (Spinner) findViewById(R.id.backgroundImageSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.backgroundWidget,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setDefaultReturn() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_CANCELED, returnIntent);

        // No widget id or invalid one
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    View.OnClickListener cancelClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    View.OnClickListener okClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            saveSettings();
            updateWidget();

            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
            finish();
        }

        private void saveSettings() {
            // SharedPreferences preferences =
            // getSharedPreferences("widget_"+widgetId,Context.MODE_WORLD_WRITEABLE);
            Log.d("WidgetSettings: ", "Will save widget_" + widgetId);

            Editor editor = preferences.edit();
            editor.clear();

            editor.putInt(TOGGLE_WIFI, selectedButtons.indexOf(findViewById(R.id.toggleWifi)) + 1);
            editor.putInt(TOGGLE_BLUETOOTH,
                    selectedButtons.indexOf(findViewById(R.id.toggleBluetooth)) + 1);
            editor.putInt(TOGGLE_GPS, selectedButtons.indexOf(findViewById(R.id.toggleGPS)) + 1);
            editor.putInt(TOGGLE_DATA, selectedButtons.indexOf(findViewById(R.id.toggleData)) + 1);
            editor.putInt(TOGGLE_2G3G, selectedButtons.indexOf(findViewById(R.id.toggle2g3g)) + 1);
            editor.putInt(TOGGLE_SYNC, selectedButtons.indexOf(findViewById(R.id.toggleSync)) + 1);
            editor.putInt(TOGGLE_SOUND, selectedButtons.indexOf(findViewById(R.id.toggleSound)) + 1);
            editor.putInt(TOGGLE_SCREEN_TIMEOUT,
                    selectedButtons.indexOf(findViewById(R.id.toggleScreenTimeout)) + 1);
            editor.putInt(TOGGLE_AUTO_ROTATE,
                    selectedButtons.indexOf(findViewById(R.id.toggleAutoRotate)) + 1);
            editor.putInt(TOGGLE_BRIGHTNESS,
                    selectedButtons.indexOf(findViewById(R.id.toggleBrightness)) + 1);
            editor.putInt(TOGGLE_AIRPLANE,
                    selectedButtons.indexOf(findViewById(R.id.toggleAirplane)) + 1);
            editor.putInt(TOGGLE_LOCK_SCREEN,
                    selectedButtons.indexOf(findViewById(R.id.toggleLockScreen)) + 1);
            editor.putInt(TOGGLE_FLASHLIGHT,
                    selectedButtons.indexOf(findViewById(R.id.toggleFlashlight)) + 1);
            editor.putInt(TOGGLE_WIFI_AP,
                    selectedButtons.indexOf(findViewById(R.id.toggleWifiAp)) + 1);
            editor.putInt(TOGGLE_WIMAX,
                    selectedButtons.indexOf(findViewById(R.id.toggleWimax)) + 1);
            editor.putBoolean(USE_ROUND_CORNERS,
                    ((CheckBox) findViewById(R.id.useRoundCorners)).isChecked());
            editor.putInt(BACKGROUND_IMAGE,
                    ((Spinner) findViewById(R.id.backgroundImageSpinner)).getSelectedItemPosition());
            editor.putBoolean(USE_VERTICAL, ((CheckBox) findViewById(R.id.useVertical)).isChecked());
            editor.putInt(SAVED, SettingsAppWidgetProvider.WIDGET_PRESENT);
            editor.putInt(LAST_BUTTON, selectedButtons.size());

            editor.commit();

            Editor editorGeneral = preferencesGeneral.edit();
            editorGeneral.clear();
            editorGeneral.putBoolean(AUTO_DISABLE_3G,
                    ((CheckBox) findViewById(R.id.autoDisable3G)).isChecked());
            editorGeneral.putBoolean(AUTO_ENABLE_3G,
                    ((CheckBox) findViewById(R.id.autoEnable3G)).isChecked());
            editorGeneral.putBoolean(AUTO_DISABLE_SYNC_WITH_WIFI,
                    ((CheckBox) findViewById(R.id.autoDisableSyncWithWifi)).isChecked());
            editorGeneral.putBoolean(AUTO_ENABLE_SYNC_WITH_WIFI,
                    ((CheckBox) findViewById(R.id.autoEnableSyncWithWifi)).isChecked());
            editorGeneral.putBoolean(AUTO_DISABLE_3G_WITH_WIFI,
                    ((CheckBox) findViewById(R.id.autoDisable3GWithWifi)).isChecked());
            editorGeneral.putBoolean(AUTO_ENABLE_3G_WITH_WIFI,
                    ((CheckBox) findViewById(R.id.autoEnable3GWithWifi)).isChecked());
            editorGeneral.putBoolean(AUTO_ENABLE_BLUETOOTH_WITH_POWER,
                    ((CheckBox) findViewById(R.id.autoEnableBluetoothWithPower)).isChecked());
            editorGeneral.putBoolean(AUTO_DISABLE_BLUETOOTH_WITH_POWER,
                    ((CheckBox) findViewById(R.id.autoDisableBluetoothWithPower)).isChecked());
            editorGeneral.putBoolean(AUTO_ENABLE_WIFI_WITH_POWER,
                    ((CheckBox) findViewById(R.id.autoEnableWifiWithPower)).isChecked());
            editorGeneral.putBoolean(AUTO_DISABLE_WIFI_WITH_POWER,
                    ((CheckBox) findViewById(R.id.autoDisableWifiWithPower)).isChecked());
            editorGeneral.putBoolean(MONITOR_DATA_ROAMING,
                    ((CheckBox) findViewById(R.id.monitorDataRoaming)).isChecked());
            editorGeneral.putBoolean(RING_MODE_VIBRATE_AS_ON,
                    ((CheckBox) findViewById(R.id.ringModeVibrateAsOn)).isChecked());

            editorGeneral.putInt(BRIGHTNESS_SPINNER,
                    ((Spinner) findViewById(R.id.brightnessSpinner)).getSelectedItemPosition());
            editorGeneral.putInt(SCREEN_TIMEOUT_SPINNER,
                    ((Spinner) findViewById(R.id.screenTimeoutSpinner)).getSelectedItemPosition());
            editorGeneral.putInt(NETWORK_MODE_SPINNER,
                    ((Spinner) findViewById(R.id.networkModeSpinner)).getSelectedItemPosition());
            editorGeneral.putInt(RING_MODE_SPINNER,
                    ((Spinner) findViewById(R.id.ringModeSpinner)).getSelectedItemPosition());
            editorGeneral.putInt(SAVED, SettingsAppWidgetProvider.WIDGET_PRESENT);
            editorGeneral.commit();

        }

        private void updateWidget() {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetSettings.this);
            SettingsAppWidgetProvider.buildUpdate(WidgetSettings.this, appWidgetManager, new int[] {
                widgetId
            });
        }
    };

    public View getSep(int posi) {
        switch (posi) {
            case 1:
                return findViewById(R.id.sep_1);
            case 2:
                return findViewById(R.id.sep_2);
            case 3:
                return findViewById(R.id.sep_3);
            case 4:
                return findViewById(R.id.sep_4);
            case 5:
                return findViewById(R.id.sep_5);
            case 6:
                return findViewById(R.id.sep_6);
            case 7:
                return findViewById(R.id.sep_7);
            case 8:
                return findViewById(R.id.sep_8);
            case 9:
                return findViewById(R.id.sep_9);
                // case 10:
                // No return as this will be the last button
        }
        return null;
    }

    public ImageView getInd(int posi) {

        switch (posi) {
            case 1:
                return (ImageView) findViewById(R.id.ind_1);
            case 2:
                return (ImageView) findViewById(R.id.ind_2);
            case 3:
                return (ImageView) findViewById(R.id.ind_3);
            case 4:
                return (ImageView) findViewById(R.id.ind_4);
            case 5:
                return (ImageView) findViewById(R.id.ind_5);
            case 6:
                return (ImageView) findViewById(R.id.ind_6);
            case 7:
                return (ImageView) findViewById(R.id.ind_7);
            case 8:
                return (ImageView) findViewById(R.id.ind_8);
            case 9:
                return (ImageView) findViewById(R.id.ind_9);
            case 10:
                return (ImageView) findViewById(R.id.ind_10);
        }
        return null;
    }

    public ImageView getImg(int posi) {
        switch (posi) {
            case 1:
                return (ImageView) findViewById(R.id.img_1);
            case 2:
                return (ImageView) findViewById(R.id.img_2);
            case 3:
                return (ImageView) findViewById(R.id.img_3);
            case 4:
                return (ImageView) findViewById(R.id.img_4);
            case 5:
                return (ImageView) findViewById(R.id.img_5);
            case 6:
                return (ImageView) findViewById(R.id.img_6);
            case 7:
                return (ImageView) findViewById(R.id.img_7);
            case 8:
                return (ImageView) findViewById(R.id.img_8);
            case 9:
                return (ImageView) findViewById(R.id.img_9);
            case 10:
                return (ImageView) findViewById(R.id.img_10);
        }
        return null;
    }

    public View getButton(int posi) {
        switch (posi) {
            case 1:
                return findViewById(R.id.btn_1);
            case 2:
                return findViewById(R.id.btn_2);
            case 3:
                return findViewById(R.id.btn_3);
            case 4:
                return findViewById(R.id.btn_4);
            case 5:
                return findViewById(R.id.btn_5);
            case 6:
                return findViewById(R.id.btn_6);
            case 7:
                return findViewById(R.id.btn_7);
            case 8:
                return findViewById(R.id.btn_8);
            case 9:
                return findViewById(R.id.btn_9);
            case 10:
                return findViewById(R.id.btn_10);
        }
        return null;
    }

    private int getIcon(CheckBox button) {
        switch (button.getId()) {
            case R.id.toggleWifi:
                return R.drawable.ic_appwidget_settings_wifi_on;
            case R.id.toggleWifiAp:
                return R.drawable.ic_appwidget_settings_wifi_ap_on;
            case R.id.toggleBluetooth:
                return R.drawable.ic_appwidget_settings_bluetooth_on;
            case R.id.toggleGPS:
                return R.drawable.ic_appwidget_settings_gps_on;
            case R.id.toggleData:
                return R.drawable.ic_appwidget_settings_data_on;
            case R.id.toggle2g3g:
                return R.drawable.ic_appwidget_settings_2g3g_on;
            case R.id.toggleSync:
                return R.drawable.ic_appwidget_settings_sync_on;
            case R.id.toggleSound:
                return R.drawable.ic_appwidget_settings_sound_ring_on;
            case R.id.toggleScreenTimeout:
                return R.drawable.ic_appwidget_settings_screen_timeout_on;
            case R.id.toggleAutoRotate:
                return R.drawable.ic_appwidget_settings_orientation_on;
            case R.id.toggleLockScreen:
                return R.drawable.ic_appwidget_settings_lock_screen_on;
            case R.id.toggleFlashlight:
                return R.drawable.ic_appwidget_settings_flashlight_on;
            case R.id.toggleAirplane:
                return R.drawable.ic_appwidget_settings_airplane_on;
            case R.id.toggleBrightness:
                return R.drawable.ic_appwidget_settings_brightness_on;
            case R.id.toggleWimax:
                return R.drawable.ic_appwidget_settings_wimax_on;
        }
        return 0;
    }

    public static void initDefaultWidget(SharedPreferences widgetPreferences) {
        Editor editor = widgetPreferences.edit();
        editor.clear();

        editor.putInt(TOGGLE_WIFI, 1);
        editor.putInt(TOGGLE_BLUETOOTH, 2);
        editor.putInt(TOGGLE_GPS, 3);
        editor.putInt(TOGGLE_DATA, 4);
        editor.putInt(TOGGLE_2G3G, 0);
        editor.putInt(TOGGLE_SYNC, 5);
        editor.putInt(TOGGLE_SOUND, 0);
        editor.putInt(TOGGLE_SCREEN_TIMEOUT, 0);
        editor.putInt(TOGGLE_AUTO_ROTATE, 0);
        editor.putInt(TOGGLE_BRIGHTNESS, 6);
        editor.putInt(TOGGLE_AIRPLANE, 0);
        editor.putInt(TOGGLE_LOCK_SCREEN, 0);
        editor.putInt(TOGGLE_FLASHLIGHT, 0);
        editor.putInt(TOGGLE_WIFI_AP, 0);
        editor.putInt(TOGGLE_WIMAX, 0);

        editor.putBoolean(USE_ROUND_CORNERS, true);
        editor.putInt(BACKGROUND_IMAGE, 0);
        editor.putBoolean(USE_VERTICAL, false);
        editor.putInt(SAVED, SettingsAppWidgetProvider.WIDGET_PRESENT);
        editor.putInt(LAST_BUTTON, 6);

        editor.commit();
    }

    public static void initDefaultSettings(SharedPreferences globalPreferences) {
        Editor editorGeneral = globalPreferences.edit();
        editorGeneral.clear();
        editorGeneral.putBoolean(AUTO_DISABLE_3G, false);
        editorGeneral.putBoolean(AUTO_ENABLE_3G, false);
        editorGeneral.putBoolean(AUTO_DISABLE_SYNC_WITH_WIFI, false);
        editorGeneral.putBoolean(AUTO_ENABLE_SYNC_WITH_WIFI, false);
        editorGeneral.putBoolean(AUTO_DISABLE_3G_WITH_WIFI, false);
        editorGeneral.putBoolean(AUTO_ENABLE_3G_WITH_WIFI, false);
        editorGeneral.putBoolean(AUTO_ENABLE_BLUETOOTH_WITH_POWER, false);
        editorGeneral.putBoolean(AUTO_DISABLE_BLUETOOTH_WITH_POWER, false);
        editorGeneral.putBoolean(AUTO_ENABLE_WIFI_WITH_POWER, false);
        editorGeneral.putBoolean(AUTO_DISABLE_WIFI_WITH_POWER, false);
        editorGeneral.putBoolean(MONITOR_DATA_ROAMING, false);
        editorGeneral.putBoolean(RING_MODE_VIBRATE_AS_ON, false);

        editorGeneral.putInt(BRIGHTNESS_SPINNER, 0);
        editorGeneral.putInt(SCREEN_TIMEOUT_SPINNER, 0);
        editorGeneral.putInt(NETWORK_MODE_SPINNER, 0);
        editorGeneral.putInt(RING_MODE_SPINNER, 0);
        editorGeneral.putInt(SAVED, SettingsAppWidgetProvider.WIDGET_PRESENT);
        editorGeneral.commit();
    }
}
