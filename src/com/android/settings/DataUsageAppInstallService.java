package com.android.settings;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;

public class DataUsageAppInstallService extends IntentService {
    private static final String TAG = DataUsageAppInstallService.class.getSimpleName();
    private static final boolean DEBUG = true;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DataUsageAppInstallService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Context context = this;

        final boolean added;
        final boolean changed;
        final boolean removed;
        if ("ANDROID.INTENT.ACTION.PACKAGE_ADDED".equalsIgnoreCase(action)) {
            added = true;
            removed = false;
        } else if ("ANDROID.INTENT.ACTION.PACKAGE_CHANGED".equalsIgnoreCase(action)) {
            added = false;
            removed = false;
            changed = true;
        } else if ("ANDROID.INTENT.ACTION.PACKAGE_REPLACED".equalsIgnoreCase(action)) {
            added = false;
            removed = false;
            changed = true;
        } else if ("ANDROID.INTENT.ACTION.PACKAGE_REMOVED".equalsIgnoreCase(action)) {
            added = false;
            removed = true;
        } else if ("ANDROID.INTENT.ACTION.PACKAGE_FULLY_REMOVED".equalsIgnoreCase(action)) {
            added = false;
            removed = true;
        } else {
            Log.e(TAG, "Unknown Action:" + action);
            return;
        }

        int uid = -1;
        if (intent.hasExtra(Intent.EXTRA_UID)) {
            uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
        }

        if (uid <= 0) {
            Log.e(TAG, "Invalid UID:" + uid + " for Action:" + action);
            return;
        }

        UidDetailProvider uidDetailProvider = new UidDetailProvider(context);
        UidDetail uidDetail = uidDetailProvider.getUidDetail(uid, true);
        String label = "";
        if (uidDetail != null) {
            label = uidDetail.label.toString();
        }

        if (added) {
            if (DEBUG) {
                Log.v(TAG, "Adding " + label + " to DataUsage DB");
            }
            DataUsageUtils.addApp(context, uid, label);
        } else if (removed) {
            if (DEBUG) {
                Log.v(TAG, "Removing " + label + " to DataUsage DB");
            }
            DataUsageUtils.removeApp(context, uid);
        }
    }
}