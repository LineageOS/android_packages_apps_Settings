
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothManager;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.StateTracker;
import com.android.settings.widget.WidgetSettings;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class BluetoothButton extends WidgetButton {

    private static LocalBluetoothManager sLocalBluetoothManager = null;

    private static final StateTracker sBluetoothState = new BluetoothStateTracker();

    static BluetoothButton ownButton = null;

    /**
     * Subclass of StateTracker to get/set Bluetooth state.
     */
    private static final class BluetoothStateTracker extends StateTracker {

        @Override
        public int getActualState(Context context) {
            if (sLocalBluetoothManager == null) {
                sLocalBluetoothManager = LocalBluetoothManager.getInstance(context);
                if (sLocalBluetoothManager == null) {
                    return SettingsAppWidgetProvider.STATE_UNKNOWN; // On
                                                                    // emulator?
                }
            }
            return bluetoothStateToFiveState(sLocalBluetoothManager.getBluetoothState());
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            if (sLocalBluetoothManager == null) {
                Log.d(SettingsAppWidgetProvider.TAG, "No LocalBluetoothManager");
                return;
            }
            // Actually request the Bluetooth change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    sLocalBluetoothManager.setBluetoothEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        }

        /**
         * Converts BluetoothAdapter's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    return SettingsAppWidgetProvider.STATE_DISABLED;
                case BluetoothAdapter.STATE_ON:
                    return SettingsAppWidgetProvider.STATE_ENABLED;
                case BluetoothAdapter.STATE_TURNING_ON:
                    return SettingsAppWidgetProvider.STATE_TURNING_ON;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return SettingsAppWidgetProvider.STATE_TURNING_OFF;
                default:
                    return SettingsAppWidgetProvider.STATE_UNKNOWN;
            }
        }
    }

    public static BluetoothButton getInstance() {
        if (ownButton == null)
            ownButton = new BluetoothButton();

        return ownButton;

    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_BLUETOOTH;
        preferenceName = WidgetSettings.TOGGLE_BLUETOOTH;
    }

    @Override
    public void toggleState(Context context) {
        sBluetoothState.toggleState(context);
    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        currentState = sBluetoothState.getTriState(context);
        switch (currentState) {
            case SettingsAppWidgetProvider.STATE_DISABLED:
                currentIcon = R.drawable.ic_appwidget_settings_bluetooth_off;
                break;
            case SettingsAppWidgetProvider.STATE_ENABLED:
                currentIcon = R.drawable.ic_appwidget_settings_bluetooth_on;
                break;
            case SettingsAppWidgetProvider.STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent. This is much easier to see in
                // sunlight.
                if (sBluetoothState.isTurningOn()) {
                    currentIcon = R.drawable.ic_appwidget_settings_bluetooth_on;
                } else {
                    currentIcon = R.drawable.ic_appwidget_settings_bluetooth_off;
                }
                break;
        }
    }

    public void onReceive(Context context, Intent intent) {
        sBluetoothState.onActualStateChange(context, intent);
    }

    public void toggleState(Context context, int newState) {
        int curState = sBluetoothState.getTriState(context);
        if (curState != SettingsAppWidgetProvider.STATE_INTERMEDIATE && curState != newState) {
            toggleState(context);
        }
    }
}
