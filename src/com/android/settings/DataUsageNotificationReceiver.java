package com.android.settings;


import android.app.*;
import android.content.*;
import android.util.*;
import android.widget.*;
import android.net.NetworkPolicyManager;
import static android.net.NetworkPolicyManager.POLICY_REJECT_APP_METERED_USAGE;

/**
 * This class implements the receiver that will handle clicks on the buttons
 * in the Data Usage Notification
 * Disable - disables the wireless network traffic for the specified uid
 * Hide - disables data usage checks for the specified uid
 */

public class DataUsageNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = DataUsageNotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int uid = 0;
        String title;
        if (intent.hasExtra(DataUsageService.DATA_USAGE_NOTIFICATION_UID)) {
            // Settings app uses long, but NetworkPolicyManager uses int
            // I guess UIDs are limited to 32 bits, so casting should not cause a problem
            uid = (int)intent.getLongExtra(DataUsageService.DATA_USAGE_NOTIFICATION_UID, 0);
        }
        if (intent.hasExtra(DataUsageService.DATA_USAGE_NOTIFICATION_TITLE)) {
            title = intent.getStringExtra(DataUsageService.DATA_USAGE_NOTIFICATION_TITLE);
        } else {
            title = "";
        }

        if (uid == 0) {
            Log.e(TAG, "Invalid UID:" + uid + " for Action:" + action);
            return;
        }

        if (DataUsageService.HIDE_ACTION.equals(action)) {
            Toast.makeText(context, "Hiding Data Usage Warnings for " + title, Toast.LENGTH_LONG)
                    .show();

            ContentValues values = new ContentValues();
            values.put(DataUsageProvider.DATAUSAGE_DB_ENB, 0);
            values.put(DataUsageProvider.DATAUSAGE_DB_ACTIVE, 0);
            values.put(DataUsageProvider.DATAUSAGE_DB_BYTES, 0);

            context.getContentResolver().update(
                    DataUsageProvider.CONTENT_URI,
                    values,
                    DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                    new String [] { String.valueOf(uid)}
            );
        } else if (DataUsageService.DISABLE_ACTION.equals(action)) {
            Toast.makeText(context, "Disabling Cellular Data Network Access for " + title,
                    Toast.LENGTH_LONG).show();
            NetworkPolicyManager policyManager = NetworkPolicyManager.from(context);
            policyManager.addUidPolicy(uid, POLICY_REJECT_APP_METERED_USAGE);
        }

        // cancel the notification
        NotificationManager notificationManager = (NotificationManager)context.getSystemService
                (Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(DataUsageService.DATA_USAGE_SERVICE_NOTIFICATION_ID);
    }
}
