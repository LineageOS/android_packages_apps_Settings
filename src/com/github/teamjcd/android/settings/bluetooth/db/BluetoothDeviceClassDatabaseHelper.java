package com.github.teamjcd.android.settings.bluetooth.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.provider.BaseColumns._ID;

public class BluetoothDeviceClassDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Bluetooth";
    public static final String TABLE_NAME = "DeviceClass";
    public static final int DATABASE_VERSION = 1;

    public static final String DEVICE_CLASS_NAME = "name";
    public static final String DEVICE_CLASS_VALUE = "class";

    public static final String[] PROJECTION = new String[]{
            _ID,
            DEVICE_CLASS_NAME,
            DEVICE_CLASS_VALUE,
    };

    public static final int ID_INDEX = 0;
    public static final int DEVICE_CLASS_NAME_INDEX = 1;
    public static final int DEVICE_CLASS_VALUE_INDEX = 2;

    public BluetoothDeviceClassDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                DEVICE_CLASS_NAME + " TEXT NOT NULL," +
                DEVICE_CLASS_VALUE + " INTEGER NOT NULL" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException();
    }
}
