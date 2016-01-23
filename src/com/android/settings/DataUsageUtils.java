package com.android.settings;

import android.app.*;
import android.content.*;
import android.database.*;

/**
 * This class contains utility helper functions for accessing DataUsageProvider
 */
public class DataUsageUtils {
    private static final String TAG = DataUsageUtils.class.getSimpleName();
    private static final int DATAUSAGE_SERVICE_ALARM_ID = 0x102030;

    public static void addApp(Context context, int uid, String label) {
        ContentValues values = new ContentValues();

        values.put(DataUsageProvider.DATAUSAGE_DB_UID, uid);
        values.put(DataUsageProvider.DATAUSAGE_DB_LABEL, label);

        context.getContentResolver().insert(
                DataUsageProvider.CONTENT_URI,
                values
        );
    }

    public static void removeApp(Context context, int uid) {
        context.getContentResolver().delete(
                DataUsageProvider.CONTENT_URI,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String [] { String.valueOf(uid)}
        );
    }

    public static void enbApp(Context context, int uid, boolean enb) {
        ContentValues values = new ContentValues();

        values.put(DataUsageProvider.DATAUSAGE_DB_ENB, enb);
        context.getContentResolver().update(
                DataUsageProvider.CONTENT_URI,
                values,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String [] { String.valueOf(uid)}
        );
    }

    public static boolean getAppEnb(Context context, int uid) {
        Cursor cursor = context.getContentResolver().query(
                DataUsageProvider.CONTENT_URI,
                DataUsageProvider.PROJECTION_ENB,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String [] { String.valueOf(uid) },
                null
        );
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            int enbValue = cursor.getInt(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_ENB);
            cursor.close();
            if (enbValue == 0) {
                return false;
            } else {
                return true;
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return false;
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
    }
}
