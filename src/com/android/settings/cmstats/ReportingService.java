/*
 * Copyright (C) 2015 The CyanogenMod Project
 *           (C) 2017 The LineageOS Project
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

package com.android.settings.cmstats;

import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.Log;
import cyanogenmod.providers.CMSettings;

import java.util.List;

public class ReportingService extends IntentService {
    /* package */ static final String TAG = "CMStats";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public ReportingService() {
        super(ReportingService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        String deviceId = Utilities.getUniqueID(getApplicationContext());
        String deviceName = Utilities.getDevice();
        String deviceVersion = Utilities.getModVersion();
        String deviceCountry = Utilities.getCountryCode(getApplicationContext());
        String deviceCarrier = Utilities.getCarrier(getApplicationContext());
        String deviceCarrierId = Utilities.getCarrierId(getApplicationContext());

        final int cmOrgJobId = AnonymousStats.getNextJobId(getApplicationContext());

        if (DEBUG) Log.d(TAG, "scheduling job id: " + cmOrgJobId);

        PersistableBundle cmBundle = new PersistableBundle();
        cmBundle.putString(StatsUploadJobService.KEY_DEVICE_NAME, deviceName);
        cmBundle.putString(StatsUploadJobService.KEY_UNIQUE_ID, deviceId);
        cmBundle.putString(StatsUploadJobService.KEY_VERSION, deviceVersion);
        cmBundle.putString(StatsUploadJobService.KEY_COUNTRY, deviceCountry);
        cmBundle.putString(StatsUploadJobService.KEY_CARRIER, deviceCarrier);
        cmBundle.putString(StatsUploadJobService.KEY_CARRIER_ID, deviceCarrierId);
        cmBundle.putLong(StatsUploadJobService.KEY_TIMESTAMP, System.currentTimeMillis());

        // set job types
        cmBundle.putInt(StatsUploadJobService.KEY_JOB_TYPE,
                StatsUploadJobService.JOB_TYPE_CMORG);

        // schedule cmorg stats upload
        js.schedule(new JobInfo.Builder(cmOrgJobId, new ComponentName(getPackageName(),
                StatsUploadJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000)
                .setExtras(cmBundle)
                .setPersisted(true)
                .build());

        // reschedule
        AnonymousStats.updateLastSynced(this);
        ReportingServiceManager.setAlarm(this);
    }
}
