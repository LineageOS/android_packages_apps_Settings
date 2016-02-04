package com.android.settings;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class implements the receiver that will handle app installs & uninstalls
 * when an app is installed, add an entry in the datausage table
 * when an app is removed, remove the entry from the datausage table
 */

public class DataUsageAppInstallReceiver extends BroadcastReceiver {
    private static final String TAG = DataUsageAppInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v(TAG, "AppInstallReceiver: onReceive");

        Intent appInstallServiceIntent = new Intent(context, DataUsageAppInstallService.class);
        appInstallServiceIntent.setAction(intent.getAction());
        if (intent.hasExtra(Intent.EXTRA_UID)) {
            appInstallServiceIntent.putExtra(
                    Intent.EXTRA_UID,
                    intent.getIntExtra(Intent.EXTRA_UID, 0));
        }
        context.startService(appInstallServiceIntent);
    }
}
