
package com.android.settings.widget.buttons;

import com.android.settings.R;
import com.android.settings.widget.SettingsAppWidgetProvider;
import com.android.settings.widget.StateTracker;
import com.android.settings.widget.WidgetSettings;
import com.android.wimax.WimaxSettingsHelper;
import com.android.wimax.WimaxConstants;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class WimaxButton extends WidgetButton {

    private static final StateTracker sWimaxState = new WimaxStateTracker();

    static WimaxButton ownButton = null;

    /**
     * Subclass of StateTracker to get/set Bluetooth state.
     */
    private static final class WimaxStateTracker extends StateTracker {

        @Override
        public int getActualState(Context context) {
            final WimaxSettingsHelper helper = new WimaxSettingsHelper(context);
            if (helper.isWimaxSupported()) {
                return wimaxStateToFiveState(helper.getWimaxState());
            }
            return SettingsAppWidgetProvider.STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            final WimaxSettingsHelper helper = new WimaxSettingsHelper(context);
            if (!helper.isWimaxSupported()) {
                Log.e(SettingsAppWidgetProvider.TAG, "WiMAX is not supported");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    helper.setWimaxEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WimaxConstants.WIMAX_ENABLED_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wimaxState = intent.getIntExtra(WimaxConstants.CURRENT_WIMAX_ENABLED_STATE, WimaxConstants.WIMAX_ENABLED_STATE_UNKNOWN);
            int widgetState = wimaxStateToFiveState(wimaxState);
            setCurrentState(context, widgetState);
        }

        /**
         * Converts WimaxController's state values into our
         * WiMAX-common state values.
         */
        private static int wimaxStateToFiveState(int wimaxState) {
            switch (wimaxState) {
                case WimaxConstants.WIMAX_ENABLED_STATE_DISABLED:
                    return SettingsAppWidgetProvider.STATE_DISABLED;
                case WimaxConstants.WIMAX_ENABLED_STATE_ENABLED:
                    return SettingsAppWidgetProvider.STATE_ENABLED;
                case WimaxConstants.WIMAX_ENABLED_STATE_ENABLING:
                    return SettingsAppWidgetProvider.STATE_TURNING_ON;
                case WimaxConstants.WIMAX_ENABLED_STATE_DISABLING:
                    return SettingsAppWidgetProvider.STATE_TURNING_OFF;
                default:
                    return SettingsAppWidgetProvider.STATE_UNKNOWN;
            }
        }
    }

    public static WimaxButton getInstance() {
        if (ownButton == null)
            ownButton = new WimaxButton();

        return ownButton;
    }

    @Override
    void initButton() {
        buttonID = WidgetButton.BUTTON_WIMAX;
        preferenceName = WidgetSettings.TOGGLE_WIMAX;
    }

    @Override
    public void toggleState(Context context) {
        sWimaxState.toggleState(context);
    }

    @Override
    public void updateState(Context context, SharedPreferences globalPreferences, int[] appWidgetIds) {
        currentState = sWimaxState.getTriState(context);
        switch (currentState) {
            case SettingsAppWidgetProvider.STATE_DISABLED:
                currentIcon = R.drawable.ic_appwidget_settings_wimax_off;
                break;
            case SettingsAppWidgetProvider.STATE_ENABLED:
                currentIcon = R.drawable.ic_appwidget_settings_wimax_on;
                break;
            case SettingsAppWidgetProvider.STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent. This is much easier to see in
                // sunlight.
                if (sWimaxState.isTurningOn()) {
                    currentIcon = R.drawable.ic_appwidget_settings_wimax_on;
                } else {
                    currentIcon = R.drawable.ic_appwidget_settings_wimax_off;
                }
                break;
        }
    }

    public void onReceive(Context context, Intent intent) {
        sWimaxState.onActualStateChange(context, intent);
    }

    public void toggleState(Context context, int newState) {
        int curState = sWimaxState.getTriState(context);
        if (curState != SettingsAppWidgetProvider.STATE_INTERMEDIATE && curState != newState) {
            toggleState(context);
        }
    }
}
