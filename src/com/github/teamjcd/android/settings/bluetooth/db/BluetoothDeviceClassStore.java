package com.github.teamjcd.android.settings.bluetooth.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import static android.provider.BaseColumns._ID;
import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassContentProvider.DEVICE_CLASS_URI;
import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassContentProvider.DEVICE_DEFAULT_CLASS_URI;
import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassData.readFromCursor;
import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassDatabaseHelper.PROJECTION;


public class BluetoothDeviceClassStore {
    private Context context;

    private BluetoothDeviceClassStore(Context context) {
        this.context = context;
    }

    public static BluetoothDeviceClassStore getBluetoothDeviceClassStore(Context context) {
        return new BluetoothDeviceClassStore(context);
    }

    public List<BluetoothDeviceClassData> getAll() {
        Cursor cursor = context.getContentResolver().query(
                DEVICE_CLASS_URI,
                PROJECTION,
                null, //selection
                null, //selectionArgs
                null //sortOrder
        );

        List<BluetoothDeviceClassData> btDevices = new ArrayList<>();
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                btDevices.add(readFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return btDevices;
    }

    public BluetoothDeviceClassData get(int id) {
        return get(Uri.withAppendedPath(DEVICE_CLASS_URI, String.valueOf(id)));
    }

    public BluetoothDeviceClassData getDefault() {
        return get(DEVICE_DEFAULT_CLASS_URI);
    }

    public BluetoothDeviceClassData get(Uri btDeviceClassUri) {
        Cursor cursor = context.getContentResolver().query(
                btDeviceClassUri,
                PROJECTION,
                null,
                null,
                null
        );
        if (cursor == null) {
            return null;
        }
        try {
            return getFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    private BluetoothDeviceClassData getFromCursor(Cursor cursor) {
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return readFromCursor(cursor);
        } else {
            return null;
        }
    }

    public Uri saveDefault(BluetoothDeviceClassData btDeviceClass) {
        ContentValues values = btDeviceClass.toContentValues();
        return context.getContentResolver().insert(DEVICE_DEFAULT_CLASS_URI, values);
    }

    public Uri save(BluetoothDeviceClassData btDeviceClass) {
        ContentValues values = btDeviceClass.toContentValues();
        return context.getContentResolver().insert(DEVICE_CLASS_URI, values);
    }

    public int update(BluetoothDeviceClassData btDeviceClass) {
        return update(btDeviceClass.getId(), btDeviceClass);
    }

    public int update(int id, BluetoothDeviceClassData btDeviceClass) {
        return update(Uri.withAppendedPath(DEVICE_CLASS_URI, String.valueOf(id)), btDeviceClass);
    }

    public int update(Uri btDeviceClassUri, BluetoothDeviceClassData btDeviceClass) {
        return context.getContentResolver().update(
                btDeviceClassUri,
                btDeviceClass.toContentValues(),
                null,
                null
        );
    }

    public int delete(int id) {
        return delete(Uri.withAppendedPath(DEVICE_CLASS_URI, Integer.toString(id)));
    }

    public int delete(Uri btDeviceClassUri) {
        return context.getContentResolver().delete(
                btDeviceClassUri,
                null,
                null
        );
    }
}
