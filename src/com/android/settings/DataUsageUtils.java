package com.android.settings;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

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

        values.put(DataUsageProvider.DATAUSAGE_DB_UID, uid);
        values.put(DataUsageProvider.DATAUSAGE_DB_LABEL, label);

        context.getContentResolver().insert(
                DataUsageProvider.CONTENT_URI,
                values
        );
    }

    public static void removeApp(Context context, int uid) {
        if (DEBUG) {
            Log.v(TAG, "removeApp: uid:" + uid);
        }
        context.getContentResolver().delete(
                DataUsageProvider.CONTENT_URI,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
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

        values.put(DataUsageProvider.DATAUSAGE_DB_ENB, enb);
        if (label != null) {
            values.put(DataUsageProvider.DATAUSAGE_DB_LABEL, label);
        }
        context.getContentResolver().update(
                DataUsageProvider.CONTENT_URI,
                values,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String [] { String.valueOf(uid)}
        );
    }

    public static boolean getAppEnb(Context context, int uid) {
        boolean appEnb = false;
        Cursor cursor = context.getContentResolver().query(
                DataUsageProvider.CONTENT_URI,
                null,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String [] { String.valueOf(uid) },
                null
        );
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            int enbValue = cursor.getInt(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_ENB);
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
        Intent dataUsageServiceIntent = new Intent(context, DataUsageService.class);
        PendingIntent alarmIntent = PendingIntent.getService(
                context, DATAUSAGE_SERVICE_ALARM_ID, dataUsageServiceIntent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        if (enb) {
            alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    DataUsageService.START_DELAY,
                    DataUsageService.SAMPLE_PERIOD,
                    alarmIntent
            );
        } else {
            alarmManager.cancel(alarmIntent);
        }
        if (DEBUG) {
            Log.v(TAG, "enbDataUsageService: enb:" + enb);
        }
    }
}
