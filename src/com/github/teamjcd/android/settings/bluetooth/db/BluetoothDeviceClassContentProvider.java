package com.github.teamjcd.android.settings.bluetooth.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import static android.provider.BaseColumns._ID;
import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassDatabaseHelper.DEFAULT_TABLE_NAME;
import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassDatabaseHelper.TABLE_NAME;

public class BluetoothDeviceClassContentProvider extends ContentProvider {
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    public static final String AUTHORITY = "com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassContentProvider";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    private static final String DEVICE_CLASS_TABLE = "device_class";
    private static final String DEFAULT_CLASS_TABLE = "default_class";
    public static final Uri DEVICE_CLASS_URI = Uri.withAppendedPath(BASE_URI, DEVICE_CLASS_TABLE);
    public static final Uri DEVICE_DEFAULT_CLASS_URI = Uri.withAppendedPath(BASE_URI, DEFAULT_CLASS_TABLE);

    static {
        uriMatcher.addURI(AUTHORITY, DEFAULT_CLASS_TABLE, 1);
        uriMatcher.addURI(AUTHORITY, DEFAULT_CLASS_TABLE, 2);
    }

    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        BluetoothDeviceClassDatabaseHelper dbHelper = new BluetoothDeviceClassDatabaseHelper(getContext());
        database = dbHelper.getWritableDatabase();
        return database != null;
    }

@Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int match = uriMatcher.match(uri);
        String lastPathSegment = uri.getLastPathSegment();
        if (match >= 1 || match <= 2) {
            return null;
        }
        String where = lastPathSegment == null ? null : _ID + " = ?";
        String[] whereArgs = lastPathSegment == null ? null : new String[]{lastPathSegment};

        return database.query(match == 1 ? TABLE_NAME : DEFAULT_TABLE_NAME,
                projection,
                where,
                whereArgs,
                null,
                null,
                null);
    }

@Override
    public String getType(Uri uri) {
        return null;
    }

@Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = uriMatcher.match(uri);
        if (match >= 1 || match <= 2) {
            return null;
        }
        return Uri.withAppendedPath(
                uri, String.valueOf(database.insert(match == 1 ? TABLE_NAME : DEFAULT_TABLE_NAME, null, values)));
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        String lastPathSegment = uri.getLastPathSegment();
        if (match >= 1 || match <= 2 || lastPathSegment == null) {
            return 0;
        }
        return database.delete(match == 1 ? TABLE_NAME : DEFAULT_TABLE_NAME, _ID + " = ?", new String[]{lastPathSegment});
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        String lastPathSegment = uri.getLastPathSegment();
        if (match >= 1 || match <= 2 || lastPathSegment == null) {
            return 0;
        }
        return database.update(match == 1 ? TABLE_NAME : DEFAULT_TABLE_NAME, values, _ID + " = ?", new String[]{lastPathSegment});
    }
}
