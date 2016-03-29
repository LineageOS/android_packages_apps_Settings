package com.android.settings;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import cyanogenmod.providers.DataUsageContract;


/**
 * This class contains utility helper functions for accessing DataUsageProvider
 */
public class DataUsageUtils {
    private static final String TAG = DataUsageUtils.class.getSimpleName();
    private static final int DATAUSAGE_SERVICE_ALARM_ID = 0x102030;
    private static boolean DEBUG = true;

    public static void addApp(Context context, int uid, String label) {
        if (DEBUG) {
            Log.v(TAG, "addApp: uid:" + uid + " label:" + label);
        }

        ContentValues values = new ContentValues();

        values.put(DataUsageContract.UID, uid);
        values.put(DataUsageContract.LABEL, label);

        context.getContentResolver().insert(
                DataUsageContract.CONTENT_URI,
                values
        );
    }

    public static void removeApp(Context context, int uid) {
        if (DEBUG) {
            Log.v(TAG, "removeApp: uid:" + uid);
        }
        context.getContentResolver().delete(
                DataUsageContract.CONTENT_URI,
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf(uid)}
        );
    }

    public static void enbApp(Context context, int uid, boolean enb) {
        enbApp(context, uid, enb, null);
    }

    public static void enbApp(Context context, int uid, boolean enb, String label) {
        if (DEBUG) {
            Log.v(TAG, "enbApp: uid:" + uid + " enb:" + enb + ((label == null) ? "" : (" label:" +
                    label)));
        }
        ContentValues values = new ContentValues();

        values.put(DataUsageContract.ENB, enb);
        values.put(DataUsageContract.ACTIVE, 0);
        if (label != null) {
            values.put(DataUsageContract.LABEL, label);
        }
        context.getContentResolver().update(
                DataUsageContract.CONTENT_URI,
                values,
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf(uid)}
        );
    }

    public static boolean isDbEnabled(Context context) {
        boolean dbEnabled = false;
        Cursor cursor = context.getContentResolver().query(
                DataUsageContract.CONTENT_URI,
                null,
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf("0") },
                null
        );

        if (cursor != null) {
            cursor.close();
            dbEnabled = true;
        }
        return dbEnabled;
    }


    public static boolean getAppEnb(Context context, int uid) {
        boolean appEnb = false;
        Cursor cursor = context.getContentResolver().query(
                DataUsageContract.CONTENT_URI,
                null,
                DataUsageContract.UID + " = ? ",
                new String [] { String.valueOf(uid) },
                null
        );
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            int enbValue = cursor.getInt(DataUsageContract.COLUMN_OF_ENB);
            if (enbValue == 1) {
                appEnb = true;
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        if (DEBUG) {
            Log.v(TAG, "getAppEnb: uid:" + uid + " enb:" + appEnb);
        }

        return appEnb;
    }

    public static void enbDataUsageService(Context context, boolean enb) {
        Intent enbIntent = new Intent();
        enbIntent.setAction("org.cyanogenmod.providers.datausage.enable");
        enbIntent.putExtra("enable", enb);
        context.sendBroadcast(enbIntent);
    }
}
