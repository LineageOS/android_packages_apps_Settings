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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.search.SettingsSearchFilterAdapter.SearchInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ResultReceiver;
import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

public class SearchPopulator extends IntentService {

    private static final String TAG = SearchPopulator.class.getSimpleName();
    private static final String LAST_PACKAGE_HASH = "last_package_hash";
    private ResultReceiver mNotifier;

    public SearchPopulator() {
        super(SearchPopulator.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mNotifier = intent.getParcelableExtra(Settings.NOTIFIER_EXTRA);
        SharedPreferences sharedPreferences = getSharedPreferences(
                getPackageName(), Context.MODE_PRIVATE);
        int lastHash = sharedPreferences.getInt(LAST_PACKAGE_HASH, -1);
        int currentHash = getPackageHashCode();

        if (lastHash != currentHash) {
            populateDatabase();
            sharedPreferences.edit().putInt(LAST_PACKAGE_HASH, currentHash).commit();
            mNotifier.send(0, null);
        }
    }

    private void populateDatabase() {
        SettingsSearchDatabaseHelper dbHelper = SettingsSearchDatabaseHelper.getInstance(this);
        dbHelper.wipeTable();

        XmlResourceParser parser = null;
        try {
            parser = getResources().getXml(R.xml.settings_headers);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"preference-headers".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <preference-headers> tag; found"
                        + nodeName + " at " + parser.getPositionDescription());
            }

            Bundle curBundle = null;

            final int outerDepth = parser.getDepth();
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                   && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("header".equals(nodeName)) {
                    Header header = new Header();

                    TypedArray sa = getResources().obtainAttributes(attrs,
                            com.android.internal.R.styleable.PreferenceHeader);
                    header.id = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_id,
                            (int)PreferenceActivity.HEADER_ID_UNDEFINED);

                    // Fetch title
                    TypedValue tv = sa.peekValue(
                            com.android.internal.R.styleable.PreferenceHeader_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.titleRes = tv.resourceId;
                        } else {
                            header.title = tv.string;
                        }
                    }

                    // Fetch breadcrumb title
                    tv = sa.peekValue(
                            com.android.internal.R.styleable.PreferenceHeader_breadCrumbTitle);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.breadCrumbTitleRes = tv.resourceId;
                        } else {
                            header.breadCrumbTitle = tv.string;
                        }
                    }

                    // Fetch breadcrumb short title
                    tv = sa.peekValue(
                            com.android.internal.R.styleable.PreferenceHeader_breadCrumbShortTitle);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            header.breadCrumbShortTitleRes = tv.resourceId;
                        } else {
                            header.breadCrumbShortTitle = tv.string;
                        }
                    }

                    // Fetch icon
                    header.iconRes = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_icon, 0);
                    if (header.iconRes == R.drawable.empty_icon) {
                        header.iconRes = 0;
                    }

                    // Fetch fragment
                    header.fragment = sa.getString(
                            com.android.internal.R.styleable.PreferenceHeader_fragment);
                    sa.recycle();

                    if (curBundle == null) {
                        curBundle = new Bundle();
                    }

                    // Fetch xml the fragment inflates
                    TypedArray se = getResources().obtainAttributes(attrs,
                            com.android.settings.R.styleable.SearchableInfo);
                    int xmlResId = se.getResourceId(
                            com.android.settings.R.styleable.SearchableInfo_includeXmlForSearch, 0);

                    boolean excludeFromSearch = se.getBoolean(com.android.settings.R.styleable.SearchableInfo_excludeFromSearch, false);
                    if (excludeFromSearch) {
                        continue;
                    }

                    final int innerDepth = parser.getDepth();
                    while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                           && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
                        if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                            continue;
                        }

                        String innerNodeName = parser.getName();
                        if (innerNodeName.equals("extra")) {
                            getResources().parseBundleExtra("extra", attrs, curBundle);
                            XmlUtils.skipCurrentTag(parser);

                        } else if (innerNodeName.equals("intent")) {
                            header.intent = Intent.parseIntent(getResources(), parser, attrs);

                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }

                    if (curBundle.size() > 0) {
                        header.fragmentArguments = curBundle;
                        curBundle = null;
                    }

                    if (TextUtils.isEmpty(header.fragment)) {
                        continue;
                    }

                    dbHelper.insertHeader(header);
                    if (xmlResId != 0) {
                        populateFromXml(xmlResId, header, 1, header.iconRes, header.fragment, header.titleRes);
                    }
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    private void populateFromXml(int xmlResId, Header header,
                                 int level, int iconRes, String prefFragment, int titleRes) {
        SettingsSearchDatabaseHelper dbHelper = SettingsSearchDatabaseHelper.getInstance(this);
        AttributeSet attributeSet;
        int type;
        XmlResourceParser xmlParser;

        try {
            xmlParser = getResources().getXml(xmlResId);
            while ((type=xmlParser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String tagName = xmlParser.getName();
            if(!"PreferenceScreen".equals(tagName)) {
                throw new RuntimeException(
                        "XML document must start with <PreferenceScreen> tag; found"
                                + tagName + " at " + xmlParser.getPositionDescription());
            }

            int nodeDepth = xmlParser.getDepth();
            attributeSet = Xml.asAttributeSet(((XmlPullParser)xmlParser));

            while ((type = xmlParser.next()) != XmlPullParser.END_DOCUMENT) {
                if(type == XmlPullParser.END_TAG && xmlParser.getDepth() <= nodeDepth) {
                    continue;
                }

                if(type == XmlPullParser.END_TAG) {
                    continue;
                }

                if(type == XmlPullParser.TEXT) {
                    continue;
                }

                String preferenceTitle = null;

                TypedArray sa = obtainStyledAttributes(attributeSet,
                        com.android.internal.R.styleable.Preference);
                TypedArray se = obtainStyledAttributes(attributeSet,
                        com.android.settings.R.styleable.SearchableInfo);

                TypedValue title = sa.peekValue(
                        com.android.internal.R.styleable.Preference_title);
                if (title != null && title.type == TypedValue.TYPE_STRING) {
                    if (title.resourceId != 0) {
                        preferenceTitle = getResources().getString(title.resourceId);
                    } else {
                        preferenceTitle = (String) title.string;
                    }
                }

                boolean excludeFromSearch = se.getBoolean(com.android.settings.R.styleable.SearchableInfo_excludeFromSearch, false);
                if (excludeFromSearch) {
                    continue;
                }
                String fragment = sa.getString(com.android.internal.R.styleable.Preference_fragment);
                int subXmlId = se.getResourceId(
                        com.android.settings.R.styleable.SearchableInfo_includeXmlForSearch, 0);
                if (subXmlId != 0 && !TextUtils.isEmpty(fragment)) {
                    populateFromXml(subXmlId, null, level + 1, header.iconRes, fragment, title.resourceId);
                    dbHelper.insertEntry(preferenceTitle, level, fragment, header.iconRes, getString(titleRes));
                } else if (header != null) {
                    header.title = preferenceTitle;
                    header.titleRes = 0;
                    dbHelper.insertHeader(header);
                } else {
                    dbHelper.insertEntry(preferenceTitle, level, prefFragment, iconRes, getString(titleRes));
                }
            }
        } catch(IOException v38) {
            v38.printStackTrace();
        } catch(Throwable v3) {
            v3.printStackTrace();
        }
    }

    public static ArrayList<SearchInfo> getTitles(Context ctx) {
        SettingsSearchDatabaseHelper dbHelper = SettingsSearchDatabaseHelper.getInstance(ctx);
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.query(DatabaseContract.TABLE_NAME, null, null, null, null, null, null);
        ArrayList<SearchInfo> infos = new ArrayList<SearchInfo>();
        if (c != null) {
            while (c.moveToNext()) {
                byte[] data = c.getBlob(c.getColumnIndex(DatabaseContract.Settings.ACTION_HEADER));
                SearchInfo info = new SearchInfo();
                if (data != null) {
                    Parcel p = Parcel.obtain();
                    p.setDataPosition(0);
                    p.unmarshall(data, 0, data.length);
                    p.setDataPosition(0);
                    Header h = new Header();
                    h.readFromParcel(p);
                    info.header = h;
                }
                info.level = c.getInt(c.getColumnIndex(DatabaseContract.Settings.ACTION_LEVEL));
                info.fragment = c.getString(c.getColumnIndex(DatabaseContract.Settings.ACTION_FRAGMENT));
                info.title = c.getString(c.getColumnIndex(DatabaseContract.Settings.ACTION_TITLE));
                info.iconRes = c.getInt(c.getColumnIndex(DatabaseContract.Settings.ACTION_ICON));
                info.parentTitle = c.getString(c.getColumnIndex(DatabaseContract.Settings.ACTION_PARENT_TITLE));
                infos.add(info);
            }
            c.close();
        }
        return infos;
    }

    /**
     * Get a 32 bit hashcode for the given package.
     * @param pkg
     * @return
     */
    private int getPackageHashCode() {
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getBasePackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }

        String apkPath = pInfo.applicationInfo.sourceDir;
        byte[] crc = getFileCrC(apkPath);
        if (crc == null) return 0;
        return Arrays.hashCode(crc);
    }

    private byte[] getFileCrC(String path) {
        ZipFile zfile = null;
        try {
            zfile = new ZipFile(path);
            ZipEntry entry = zfile.getEntry("META-INF/MANIFEST.MF");
            if (entry == null) {
                Log.e(TAG, "Unable to get MANIFEST.MF from " + path);
                return null;
            }

            long crc = entry.getCrc();
            if (crc == -1) Log.e(TAG, "Unable to get CRC for " + path);
            return ByteBuffer.allocate(8).putLong(crc).array();
        } catch (Exception e) {
        } finally {
            if (zfile != null) {
                try {
                    zfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
