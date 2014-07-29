/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.search;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcel;
import android.preference.PreferenceActivity.Header;
import android.text.TextUtils;

public class SettingsSearchDatabaseHelper extends SQLiteOpenHelper {
    private static SettingsSearchDatabaseHelper mInstance = null;

    // general database configuration and tables
    private static final String sDatabaseName = "search.db";

    protected static final int DATABASE_VERSION = 3;
    private Context mContext;

    public static SettingsSearchDatabaseHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SettingsSearchDatabaseHelper(
                    context.getApplicationContext(), DATABASE_VERSION);
        }
        return mInstance;
    }

    public SettingsSearchDatabaseHelper(Context context, int newVersion) {
        super(context, sDatabaseName, null, newVersion);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE " + DatabaseContract.TABLE_NAME + "(" +
                DatabaseContract.Settings._ID + " INTEGER PRIMARY KEY," +
                DatabaseContract.Settings.ACTION_TITLE + " TEXT UNIQUE ON CONFLICT REPLACE," +
                DatabaseContract.Settings.ACTION_HEADER + " TEXT," +
                DatabaseContract.Settings.ACTION_ICON + " INTEGER," +
                DatabaseContract.Settings.ACTION_LEVEL + " INTEGER," +
                DatabaseContract.Settings.ACTION_FRAGMENT + " TEXT," +
                DatabaseContract.Settings.ACTION_PARENT_TITLE + " INTEGER," +
                DatabaseContract.Settings.ACTION_KEY + " TEXT" +
                ");");
        db.execSQL(builder.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS settings");
        onCreate(db);
    }

    public void wipeTable() {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(DatabaseContract.TABLE_NAME, null, null);
    }

    public void insertHeader(Header header) {
        insertHeader(header, 0, null);
    }

    public void insertHeader(Header header, int parentTitle, String key) {
        if (header == null) {
            return;
        }
        String title = null;
        if (!TextUtils.isEmpty(header.title)) {
            title = header.title.toString();
        } else if (header.titleRes != 0) {
            title = mContext.getString(header.titleRes);
        }
        if (TextUtils.isEmpty(title)) {
            return;
        }
        insertEntry(header, title, 0, null, header.iconRes, parentTitle, key);
    }

    public void insertEntry(String title, int level, String fragment,
            int iconRes, int parentTitle, String key) {
        if (TextUtils.isEmpty(title)) {
            return;
        }
        insertEntry(null, title, level, fragment, iconRes, parentTitle, key);
    }

    private void insertEntry(Header header, String title, int level, String fragment,
            int iconRes, int parentTitle, String key) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        if (header != null) {
            Parcel p = Parcel.obtain();
            p.setDataPosition(0);
            header.writeToParcel(p, 0);
            // Marshalling will not cause an issue if the definition changes,
            // since we wipe data on hash changes and recreate it
            values.put(DatabaseContract.Settings.ACTION_HEADER, p.marshall());
        }
        values.put(DatabaseContract.Settings.ACTION_TITLE, title);
        values.put(DatabaseContract.Settings.ACTION_LEVEL, level);
        values.put(DatabaseContract.Settings.ACTION_ICON, iconRes);
        values.put(DatabaseContract.Settings.ACTION_FRAGMENT, fragment);
        values.put(DatabaseContract.Settings.ACTION_PARENT_TITLE, parentTitle);
        values.put(DatabaseContract.Settings.ACTION_KEY, key);
        database.insert(DatabaseContract.TABLE_NAME, null, values);
    }
}
