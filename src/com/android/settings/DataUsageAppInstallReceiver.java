package com.android.settings;


import android.app.*;
import android.content.*;
import android.net.*;
import android.util.*;
import android.widget.*;
import com.android.settings.net.*;


/**
 * This class implements the receiver that will handle app installs & uninstalls
 * when an app is installed, add an entry in the datausage table
 * when an app is removed, remove the entry from the datausage table
 */

public class DataUsageAppInstallReceiver extends BroadcastReceiver {
    private static final String TAG = DataUsageAppInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();

        final boolean added;
        final boolean changed;
        final boolean removed;
        if (action.equalsIgnoreCase("ANDROID.INTENT.ACTION.PACKAGE_ADDED")) {
            added = true;
            removed = false;
        } else if (action.equalsIgnoreCase("ANDROID.INTENT.ACTION.PACKAGE_CHANGED")) {
            added = false;
            removed = false;
            changed = true;
        } else if (action.equalsIgnoreCase("ANDROID.INTENT.ACTION.PACKAGE_REPLACED")) {
            added = false;
            removed = false;
            changed = true;
        } else if (action.equalsIgnoreCase("ANDROID.INTENT.ACTION.PACKAGE_REMOVED")) {
            added = false;
            removed = true;
        } else if (action.equalsIgnoreCase("ANDROID.INTENT.ACTION.PACKAGE_FULLY_REMOVED")) {
            added = false;
            removed = true;
        } else {
            Log.e(TAG, "Unknown Action:" + action);
            return;
        }

        final int uid;
        if (intent.hasExtra("android.intent.extra.UID")) {
            uid = intent.getIntExtra("android.intent.extra.UID", 0);
        } else {
            uid = 0;
        }

        if (uid == 0) {
            Log.e(TAG, "Invalid UID:" + uid + " for Action:" + action);
            return;
        }

        // run on a background thread - since UidDetailProvider and/or DB access can take long time
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (added) {
                    UidDetailProvider uidDetailProvider = new UidDetailProvider(context);
                    UidDetail uidDetail = uidDetailProvider.getUidDetail(uid, true);
                    String label = "";
                    if (uidDetail != null) {
                        label = uidDetail.label.toString();
                    }

                    ContentValues values = new ContentValues();
                    values.put(DataUsageProvider.DATAUSAGE_DB_UID, uid);
                    values.put(DataUsageProvider.DATAUSAGE_DB_ENB, 1);
                    values.put(DataUsageProvider.DATAUSAGE_DB_LABEL, label);
                    context.getContentResolver().insert(
                            DataUsageProvider.CONTENT_URI,
                            values
                    );
                } else if (removed) {
                    context.getContentResolver().delete(
                            DataUsageProvider.CONTENT_URI,
                            DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                            new String [] {String.valueOf(uid)}
                    );
                }
            }
        }).start();

    }
}
