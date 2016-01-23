package com.android.settings;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.net.*;
import android.text.*;
import android.util.*;

/**
 * ContentProvider for the DataUsage statistics gathering of the Settings App
 * Keeps track of various per App configuration/state variables that are used to determine
 * if and when to generate an App specific DataUsage warning
 */

public class DataUsageProvider extends ContentProvider {
    private static final boolean DEBUG = true;
    private static final String TAG = DataUsageProvider.class.getSimpleName();
    private static final String DATABASE_NAME = "datausage.db";
    private static final int DATABASE_VERSION = 1;
    private static final String DATAUSAGE_TABLE = "datausage";
    private static final String DATAUSAGE = "datausage";
    private static final String DATAUSAGE_AUTHORITY = "com.android.settings.datausage";

    public static final Uri CONTENT_URI =
            Uri.parse("content://" + DATAUSAGE_AUTHORITY + "/" + DATAUSAGE_TABLE);

    private DatabaseHelper mOpenHelper;

    // define database columns
    public static final String DATAUSAGE_DB_ID              = "_id";
    public static final String DATAUSAGE_DB_UID             = "uid";
    public static final String DATAUSAGE_DB_ENB             = "enb";     // warning generation enabled
    public static final String DATAUSAGE_DB_ACTIVE          = "active";  // warning currently active
    public static final String DATAUSAGE_DB_LABEL           = "label";   // app label - for debugging
    public static final String DATAUSAGE_DB_BYTES           = "bytes";   // prev sample bytes
    // consumed bw avg over samples - slow moving
    public static final String DATAUSAGE_DB_SLOW_AVG        = "slow_avg";
    // accumulated samples - slow moving average
    public static final String DATAUSAGE_DB_SLOW_SAMPLES    = "slow_samples";
    // consumed bw avg over samples - fast moving
    public static final String DATAUSAGE_DB_FAST_AVG        = "fast_avg";
    // accumulated samples - fast moving average
    public static final String DATAUSAGE_DB_FAST_SAMPLES    = "fast_samples";
    public static final String DATAUSAGE_DB_EXTRA           = "extra";   // extra samples for debugging


    public static final int DATAUSAGE_DB_COLUMN_OF_ID           = 0;
    public static final int DATAUSAGE_DB_COLUMN_OF_UID          = 1;
    public static final int DATAUSAGE_DB_COLUMN_OF_ENB          = 2;
    public static final int DATAUSAGE_DB_COLUMN_OF_ACTIVE       = 3;
    public static final int DATAUSAGE_DB_COLUMN_OF_LABEL        = 4;
    public static final int DATAUSAGE_DB_COLUMN_OF_BYTES        = 5;
    public static final int DATAUSAGE_DB_COLUMN_OF_SLOW_AVG     = 6;
    public static final int DATAUSAGE_DB_COLUMN_OF_SLOW_SAMPLES = 7;
    public static final int DATAUSAGE_DB_COLUMN_OF_FAST_AVG     = 8;
    public static final int DATAUSAGE_DB_COLUMN_OF_FAST_SAMPLES = 9;
    public static final int DATAUSAGE_DB_COLUMN_OF_EXTRA        = 10;

    public static final String [] PROJECTION_ENB = {
            DATAUSAGE_DB_ENB
    };

    // define database matching constants
    private static final int DATAUSAGE_ALL      = 0;
    private static final int DATAUSAGE_ID       = 1;
    private static final int DATAUSAGE_UID      = 2;

    // build a URI matcher - add routes to it (if any)
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(DATAUSAGE_AUTHORITY, DATAUSAGE_TABLE,            DATAUSAGE_ALL);
        sURIMatcher.addURI(DATAUSAGE_AUTHORITY, DATAUSAGE_TABLE + "/#",     DATAUSAGE_ID);
        sURIMatcher.addURI(DATAUSAGE_AUTHORITY, DATAUSAGE_TABLE + "/uid/*", DATAUSAGE_UID);
    }

    // Database Helper Class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseHelper (Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // setup database schema
            db.execSQL(
                    "CREATE TABLE " + DATAUSAGE_TABLE +
                    "(" + DATAUSAGE_DB_ID + " INTEGER PRIMARY KEY, " +
                    DATAUSAGE_DB_UID + " INTEGER, " +
                    DATAUSAGE_DB_ENB + " INTEGER DEFAULT 1, " +   // TODO - change default to 0
                    DATAUSAGE_DB_ACTIVE + " INTEGER DEFAULT 0, " +
                    DATAUSAGE_DB_LABEL + " STRING, " +
                    DATAUSAGE_DB_BYTES + " INTEGER DEFAULT 0, " +
                    DATAUSAGE_DB_SLOW_AVG + " INTEGER DEFAULT 0, " +
                    DATAUSAGE_DB_SLOW_SAMPLES + " INTEGER DEFAULT 0, " +
                    DATAUSAGE_DB_FAST_AVG + " INTEGER DEFAULT 0, " +
                    DATAUSAGE_DB_FAST_SAMPLES + " INTEGER DEFAULT 0, " +
                    DATAUSAGE_DB_EXTRA + " STRING );"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            return;
        }
    }


    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATAUSAGE_TABLE);

        int match = sURIMatcher.match(uri);

        if (DEBUG) {
            Log.v(TAG, "Query uri=" + uri + ", match=" + match);
        }

        switch (match) {
            case DATAUSAGE_ALL:
                break;

            case DATAUSAGE_ID:
                break;

            case DATAUSAGE_UID:
                break;

            default:
                Log.e(TAG, "query: invalid request: " + uri);
                return null;
        }

        Cursor cursor;
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);

        switch(match) {
            case DATAUSAGE_ALL:
                return "vnd.android.cursor.dir/datausage_entry";
            case DATAUSAGE_ID:
            case DATAUSAGE_UID:
                return "vnd.android.cursor.item/datausage_entry";
            default:
                throw new IllegalArgumentException("UNKNOWN URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = sURIMatcher.match(uri);
        if (DEBUG) {
            Log.v(TAG, "Insert uri=" + uri + ", match=" + match);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowID = db.insert(DATAUSAGE_TABLE, null, values);

        if (DEBUG) {
            Log.v(TAG, "inserted " + values + " rowID=" + rowID);
        }

        return ContentUris.withAppendedId(CONTENT_URI, rowID);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        if (DEBUG) {
            Log.v(TAG, "Delete uri=" + uri + ", match=" + match);
        }

        switch(match) {
            case DATAUSAGE_ALL:
                break;
            case DATAUSAGE_UID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot delete URI:" + uri + " with a select clause"
                    );
                }
                String uidNumber = uri.getLastPathSegment();
                selection = DATAUSAGE_DB_UID + " = ? ";
                selectionArgs = new String [] {uidNumber};
                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot delete URI:" + uri
                );
        }
        int count = db.delete(DATAUSAGE_TABLE, selection, selectionArgs);
        return count;
    }

    // update is always done by UID
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        long count = 0;
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String uid;

        if (DEBUG) {
            Log.v(TAG, "Update uri=" + uri + ", match=" + match);
        }

        switch(match) {
            case DATAUSAGE_ALL:
                uid = selectionArgs[0];
                break;
            case DATAUSAGE_UID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                        "Cannot update URI " + uri + " with a select clause"
                    );
                }
                selection = DATAUSAGE_DB_UID + " = ? ";
                uid = uri.getLastPathSegment();
                selectionArgs = new String [] { uid };
                break;
            default:
                throw new UnsupportedOperationException("Cannot update that URI: " + uri);

        }

        // if no record is found, then perform an insert, so make the db transaction atomic
        if (DEBUG) {
            Log.v(TAG, "Update: Values:" + values.toString() + " selection:" + selection + " " +
                    " selectionArgs:" + selectionArgs[0]);
        }
        // count = db.update(DATAUSAGE_TABLE, values, selection, selectionArgs);

        db.beginTransaction();
        try {
            count = db.update(DATAUSAGE_TABLE, values, selection, selectionArgs);

            if (DEBUG) {
                Log.v(TAG, "Update count:" + count);
            }
            if (count == 0) {
                Log.v(TAG, "Count==0, Performing Insert");
                values.put(DATAUSAGE_DB_UID, uid);
                count = db.insert(DATAUSAGE_TABLE, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            Log.v(TAG, "dbEndTransaction");
            db.endTransaction();
        }
        if (DEBUG) {
            Log.v(TAG, "Update result for uri=" + uri + " count=" + count);
        }
        return (int)count;
    }
}
