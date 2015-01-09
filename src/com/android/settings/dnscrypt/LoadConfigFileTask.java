/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.dnscrypt;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * LoadConfigFileTask
 * <pre>
 *     This task will check if the config file exists in /system/etc and if so will load it for
 *     configuration display in Settings
 * </pre>
 */
public class LoadConfigFileTask extends AsyncTask<Void, Integer, Void> {

    // Constants
    private static final String TAG = LoadConfigFileTask.class.getSimpleName();

    // Members
    private ILoadConfigListener mListener;
    private File mConfigFile;
    private ProgressDialog mProgressDialog;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    /**
     * Constructor
     *
     * @param configFile {@link java.io.File}
     * @param progressDialog {@link android.app.ProgressDialog}
     * @throws IllegalArgumentException {@link java.lang.IllegalArgumentException}
     */
    public LoadConfigFileTask(File configFile, ProgressDialog progressDialog, ILoadConfigListener
            listener)
            throws IllegalArgumentException {
        if (configFile == null || !configFile.exists()) {
            throw new IllegalArgumentException("'configFile' must not be null and must exist!");
        }
        mConfigFile = configFile;
        mProgressDialog = progressDialog;
        mListener = listener;
    }

    @Override
    public void onPreExecute() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    @Override
    public void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress[0]);
        }
    }

    @Override
    public Void doInBackground(Void... params) {

        if (mConfigFile == null || !mConfigFile.exists()) {
            Log.w(TAG, "No config file found....such strange!");
            // [TODO][MSB]: Download that sucker!
            // [TODO][MSB]: Perform sig check?
            return null;
        }

        try {
            FileInputStream fis = new FileInputStream(mConfigFile);
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(reader);
            List<String> configLines = new ArrayList<String>();
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                configLines.add(line);
            }
            mEntries = new CharSequence[configLines.size() - 1];
            mEntryValues = new CharSequence[configLines.size() - 1];
            if (mProgressDialog != null) {
                mProgressDialog.setMax(configLines.size());
            }
            boolean skippedTitle = false;
            int progress = 0;
            int i = 0;
            for (String configLine : configLines) {
                if (!skippedTitle) {
                    skippedTitle = true;
                    progress++;
                    continue;
                }
                String[] configParts = configLine.split(",");
                if (configParts.length > 3) {
                    String name = configParts[0];
                    String fullName = configParts[1];
                    mEntries[i] = fullName;
                    mEntryValues[i] = name;
                }
                i++;
                progress++;
                publishProgress(progress);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    public void onPostExecute(Void result) {
        if (mListener != null) {
            mListener.onConfigLoaded(mEntries, mEntryValues);
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * ILoadConfigListener
     * <pre>
     *     Callback interface for passing back results
     * </pre>
     */
    public interface ILoadConfigListener {
        public void onConfigLoaded(CharSequence[] entries, CharSequence[] entryValues);
    }

}
