/*
Copyright (c) 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

public class DBReadAsyncTask extends AsyncTask<Void, Void, Boolean> {

    /**
     * SMQ preferences key.
     */
    public static final String SMQ_KEY_VALUE = "app_status";

    /**
     * The authority of the provider.
     */
    public static final String AUTHORITY = "com.qti.smq.qualcommFeedback.provider";
    /**
     * The content URI.
     */
    final  Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    /**
     * The Content URI for this table.
     */
    final Uri SNAP_CONTENT_URI = Uri.withAppendedPath(
            CONTENT_URI, "smq_settings");

    Context mContext;

    public static final String KEY_VALUE = "app_status";

    public DBReadAsyncTask(Context mContext) {
        super();
        this.mContext = mContext;
    }

    @Override
    protected Boolean doInBackground(final Void... params) {
        final String whereClause = "key" + "=?";
        final String[] selectionArgs = { KEY_VALUE };

        final Cursor c = mContext.getContentResolver().query(
                SNAP_CONTENT_URI, null, whereClause,
                selectionArgs, null);
        final SharedPreferences sharedPreferences = mContext
                    .getSharedPreferences(SmqSettings.SMQ_PREFS_NAME, Context.MODE_PRIVATE);

        if (c!= null && c.getCount() > 0) {
            c.moveToFirst();
            final int value = c.getInt(1);

            final int appStatus = sharedPreferences.getInt(KEY_VALUE, 0);
            if (appStatus == value) {
                // Do nothing
            } else {
                //Save preference and notify.
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(KEY_VALUE, value);
                editor.commit();

            }

        }
        else{
            //No such table. don't show menu.
            final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(KEY_VALUE, 0);
                editor.commit();
        }
        if(c != null){
            c.close();
        }

        return true;
    }

}