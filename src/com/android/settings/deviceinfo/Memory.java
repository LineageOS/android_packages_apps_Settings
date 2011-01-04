/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Environment;
import android.os.storage.IMountService;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageEventListener;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Memory extends PreferenceActivity implements OnCancelListener {
    private static final String TAG = "Memory";
    private static final boolean localLOGV = false;

    private static final String MEMORY_SD_SIZE = "memory_sd_size";

    private static final String MEMORY_SD_AVAIL = "memory_sd_avail";

    private static final String MEMORY_SD_MOUNT_TOGGLE = "memory_sd_mount_toggle";

    private static final String MEMORY_SD_FORMAT = "memory_sd_format";

    private static final String MEMORY_SD_GROUP = "memory_sd";

    private static final String MEMORY_ADDITIONAL_CATEGORY = "memory_additional_category";

    private static final String MEMORY_ADDITIONAL_SIZE = "memory_additional_size";

    private static final String MEMORY_ADDITIONAL_AVAIL = "memory_additional_avail";

    private static final String MEMORY_INTERNAL_SIZE = "memory_internal_size";

    private static final String MEMORY_INTERNAL_AVAIL = "memory_internal_avail";

    private static final int DLG_CONFIRM_UNMOUNT = 1;
    private static final int DLG_ERROR_UNMOUNT = 2;

    private Resources mRes;

    private Preference mSdSize;
    private Preference mSdAvail;
    private Preference mSdMountToggle;
    private Preference mSdFormat;
    private PreferenceGroup mSdMountPreferenceGroup;

    boolean mSdMountToggleAdded = true;

    private Preference mIntSize;
    private Preference mIntAvail;
    
    // Access using getMountService()
    private IMountService mMountService = null;

    private StorageManager mStorageManager = null;

    private List<String> getAdditionalVolumePaths() {
        ArrayList<String> volumes = new ArrayList<String>();
        String additionalVolumesProperty = SystemProperties.get("ro.additionalmounts");
        if (null != additionalVolumesProperty) {
            String[] additionalVolumes = additionalVolumesProperty.split(";");
            for (String additionalVolume: additionalVolumes) {
                if (!"".equals(additionalVolume)) {
                    volumes.add(additionalVolume);
                }
            }
        }
        return volumes;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            mStorageManager.registerListener(mStorageListener);
        }

        addPreferencesFromResource(R.xml.device_info_memory);
        
        mRes = getResources();

        mSdSize = findPreference(MEMORY_SD_SIZE);
        mSdAvail = findPreference(MEMORY_SD_AVAIL);
        mSdMountToggle = findPreference(MEMORY_SD_MOUNT_TOGGLE);
        mSdFormat = findPreference(MEMORY_SD_FORMAT);
        mSdMountPreferenceGroup = (PreferenceGroup)findPreference(MEMORY_SD_GROUP);

        mIntSize = findPreference(MEMORY_INTERNAL_SIZE);
        mIntAvail = findPreference(MEMORY_INTERNAL_AVAIL);

        for (String path: getAdditionalVolumePaths()) {
            PreferenceCategory category = new PreferenceCategory(this);
            category.setKey(MEMORY_ADDITIONAL_CATEGORY + path);
            category.setTitle(mRes.getString(R.string.internal_memory) + ": " + path);
            getPreferenceScreen().addPreference(category);

            Preference size = new Preference(this, null,
                    android.R.attr.preferenceInformationStyle);
            size.setKey(MEMORY_ADDITIONAL_SIZE + path);
            size.setTitle(R.string.memory_size);
            size.setSummary(R.string.sd_unavailable);
            category.addPreference(size);

            Preference available = new Preference(this, null,
                    android.R.attr.preferenceInformationStyle);
            available.setKey(MEMORY_ADDITIONAL_AVAIL + path);
            available.setTitle(R.string.memory_available);
            available.setSummary(R.string.sd_unavailable);
            category.addPreference(available);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);

        updateMemoryStatus();
    }

    StorageEventListener mStorageListener = new StorageEventListener() {

        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i(TAG, "Received storage state changed notification that " +
                    path + " changed state from " + oldState +
                    " to " + newState);
            updateMemoryStatus();
        }
    };
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
        super.onDestroy();
    }

    private synchronized IMountService getMountService() {
       if (mMountService == null) {
           IBinder service = ServiceManager.getService("mount");
           if (service != null) {
               mMountService = IMountService.Stub.asInterface(service);
           } else {
               Log.e(TAG, "Can't get mount service");
           }
       }
       return mMountService;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSdMountToggle) {
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_MOUNTED)) {
                unmount();
            } else {
                mount();
            }
            return true;
        } else if (preference == mSdFormat) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, com.android.settings.MediaFormat.class);
            startActivity(intent);
            return true;
        }
        
        return false;
    }
     
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMemoryStatus();
        }
    };

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
        case DLG_CONFIRM_UNMOUNT:
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_confirm_unmount_title)
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            doUnmount(true);
                        }})
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(R.string.dlg_confirm_unmount_text)
                    .setOnCancelListener(this)
                    .create();
        case DLG_ERROR_UNMOUNT:
            return new AlertDialog.Builder(this                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            )
            .setTitle(R.string.dlg_error_unmount_title)
            .setNeutralButton(R.string.dlg_ok, null)
            .setMessage(R.string.dlg_error_unmount_text)
            .setOnCancelListener(this)
            .create();
        }
        return null;
    }

    private void doUnmount(boolean force) {
        // Present a toast here
        Toast.makeText(this, R.string.unmount_inform_text, Toast.LENGTH_SHORT).show();
        IMountService mountService = getMountService();
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        try {
            mSdMountToggle.setEnabled(false);
            mSdMountToggle.setTitle(R.string.sd_ejecting_title);
            mSdMountToggle.setSummary(R.string.sd_ejecting_summary);
            mountService.unmountVolume(extStoragePath, force);
        } catch (RemoteException e) {
            // Informative dialog to user that
            // unmount failed.
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    private boolean hasAppsAccessingStorage() throws RemoteException {
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        IMountService mountService = getMountService();
        int stUsers[] = mountService.getStorageUsers(extStoragePath);
        if (stUsers != null && stUsers.length > 0) {
            return true;
        }
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationInfo> list = am.getRunningExternalApplications();
        if (list != null && list.size() > 0) {
            return true;
        }
        return false;
    }

    private void unmount() {
        // Check if external media is in use.
        try {
           if (hasAppsAccessingStorage()) {
               if (localLOGV) Log.i(TAG, "Do have storage users accessing media");
               // Present dialog to user
               showDialogInner(DLG_CONFIRM_UNMOUNT);
           } else {
               doUnmount(true);
           }
        } catch (RemoteException e) {
            // Very unlikely. But present an error dialog anyway
            Log.e(TAG, "Is MountService running?");
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void mount() {
        IMountService mountService = getMountService();
        try {
            if (mountService != null) {
                mountService.mountVolume(Environment.getExternalStorageDirectory().toString());
            } else {
                Log.e(TAG, "Mount service is null, can't mount");
            }
        } catch (RemoteException ex) {
        }
    }

    private void updateMemoryStatus() {
        String status = Environment.getExternalStorageState();
        String readOnly = "";
        if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            status = Environment.MEDIA_MOUNTED;
            readOnly = mRes.getString(R.string.read_only);
        }
 
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            if (!Environment.isExternalStorageRemovable()) {
                // This device has built-in storage that is not removable.
                // There is no reason for the user to unmount it.
                if (mSdMountToggleAdded) {
                    mSdMountPreferenceGroup.removePreference(mSdMountToggle);
                    mSdMountToggleAdded = false;
                }
            }
            try {
                File path = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                long availableBlocks = stat.getAvailableBlocks();
                
                mSdSize.setSummary(formatSize(totalBlocks * blockSize));
                mSdAvail.setSummary(formatSize(availableBlocks * blockSize) + readOnly);

                mSdMountToggle.setEnabled(true);
                mSdMountToggle.setTitle(R.string.sd_eject);
                mSdMountToggle.setSummary(R.string.sd_eject_summary);

            } catch (IllegalArgumentException e) {
                // this can occur if the SD card is removed, but we haven't received the 
                // ACTION_MEDIA_REMOVED Intent yet.
                status = Environment.MEDIA_REMOVED;
            }
            
        } else {
            mSdSize.setSummary(R.string.sd_unavailable);
            mSdAvail.setSummary(R.string.sd_unavailable);


            if (!Environment.isExternalStorageRemovable()) {
                if (status.equals(Environment.MEDIA_UNMOUNTED)) {
                    if (!mSdMountToggleAdded) {
                        mSdMountPreferenceGroup.addPreference(mSdMountToggle);
                        mSdMountToggleAdded = true;
                    }
                }
            }

            if (status.equals(Environment.MEDIA_UNMOUNTED) ||
                status.equals(Environment.MEDIA_NOFS) ||
                status.equals(Environment.MEDIA_UNMOUNTABLE) ) {
                mSdMountToggle.setEnabled(true);
                mSdMountToggle.setTitle(R.string.sd_mount);
                mSdMountToggle.setSummary(R.string.sd_mount_summary);
            } else {
                mSdMountToggle.setEnabled(false);
                mSdMountToggle.setTitle(R.string.sd_mount);
                mSdMountToggle.setSummary(R.string.sd_insert_summary);
            }
        }

        for (String path: getAdditionalVolumePaths()) {
            Preference size = findPreference(MEMORY_ADDITIONAL_SIZE + path);
            Preference available = findPreference(MEMORY_ADDITIONAL_AVAIL + path);
            if (null == size || null == available) {
                continue;
            }

            try {
                status = getMountService().getVolumeState(path);
            } catch (RemoteException ex) {
                status = Environment.MEDIA_UNMOUNTED;
            }
            if (status.equals(Environment.MEDIA_MOUNTED)) {
                StatFs stat = new StatFs(path);
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();
                long availableBlocks = stat.getAvailableBlocks();
                size.setSummary(formatSize(totalBlocks * blockSize));
                available.setSummary(formatSize(availableBlocks * blockSize));
            } else {
                size.setSummary(R.string.sd_unavailable);
                available.setSummary(R.string.sd_unavailable);
            }
        }

        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long availableBlocks = stat.getAvailableBlocks();
        mIntSize.setSummary(formatSize(totalBlocks * blockSize));
        mIntAvail.setSummary(formatSize(availableBlocks * blockSize));
    }
    
    private String formatSize(long size) {
        return Formatter.formatFileSize(this, size);
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }
    
}
