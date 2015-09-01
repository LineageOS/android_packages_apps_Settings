package com.android.settings;

import android.accounts.AccountManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;

import java.util.List;

/**
 * Helps set the device up for CTS testing. This class is disabled by default.
 *
 * <p>To enable: {@code adb shell pm enable com.android.setting/CtsSetupHelper}
 *
 * <p>To run: {@code adb shell am broadcast -a "com.android.settings.ACTION_SETUP_CTS" "com.android.settings/.CtsSetupHelper"}
 *
 * Does the following:
 * <li>Sets stay awake to while charging
 * <li>Enables 'Allow mock locations'
 * <li>Verify apps over USB
 * <li>Sets the lock screen to 'None'
 * <li>Disables all admin components and enables the two CTS ones if they are present.
 */
public class CtsSetupHelper extends BroadcastReceiver {

    public static final String ACTION_SETUP_CTS = "com.android.settings.ACTION_SETUP_CTS";

    private static final String TAG = CtsSetupHelper.class.getSimpleName();
    private static final String CTS_DEVICE_ADMIN_PREFIX
            = "android.deviceadmin.cts.CtsDeviceAdminReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Build.IS_DEBUGGABLE) {
            Log.e(TAG, "CTS HELPER NOT RUNNING ON USER BUILD!");
            return;
        }
        AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (am.getAccounts().length > 0) {
            Log.e(TAG, "CTS Helper will not run when user accounts are present.");
            return;
        }

        // set stay awake while debugging
        ContentResolver cr = context.getContentResolver();
        Settings.Global.putInt(cr, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 1);
        Log.d(TAG, "set Settings.Global.STAY_ON_WHILE_PLUGGED_IN to 1");

        // allow mock locations
        Settings.Secure.putInt(cr, Settings.Secure.ALLOW_MOCK_LOCATION, 1);
        Log.d(TAG, "set Settings.Secure.ALLOW_MOCK_LOCATION to 1");

        // disable verify apps over usb
        Settings.Global.putInt(cr, Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, 0);
        Log.d(TAG, "set Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB to 0");

        // disable lock screen
        final LockPatternUtils lock = new LockPatternUtils(context);
        lock.clearLock(false);
        lock.setLockScreenDisabled(true);
        Log.d(TAG, "disabled lock screen");

        // disable all admin components
        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final List<ComponentName> activeAdmins = dpm.getActiveAdmins();
        if (activeAdmins != null) {
            for (ComponentName activeAdmin : activeAdmins) {
                Log.d(TAG, "disabling active administrator: " + activeAdmin.flattenToString());
                dpm.removeActiveAdmin(activeAdmin);
            }
        }

        // enable the two android.deviceadmin.cts.CtsDeviceAdminReceiver* components
        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> disabledAdmin = pm.queryBroadcastReceivers(
                new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                PackageManager.GET_META_DATA | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
        if (disabledAdmin != null) {
            for (ResolveInfo adminInfo : disabledAdmin) {
                final ComponentName receiver = new ComponentName(
                        adminInfo.activityInfo.packageName,
                        adminInfo.activityInfo.name);

                if (receiver.flattenToString().contains(CTS_DEVICE_ADMIN_PREFIX)) {
                    Log.d(TAG, "found CTS admin receiver, enabling: " + receiver);
                    dpm.setActiveAdmin(receiver, true);
                }
            }

        }
        Log.d(TAG, "finished running CTS helper!");
    }
}
