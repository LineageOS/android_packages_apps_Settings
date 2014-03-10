package com.android.settings.cyanogenmod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.cyanogenmod.support.proxy.GlobalProxyManager;
import org.cyanogenmod.support.proxy.Util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.ProxyProperties;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;

public class GlobalProxyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (action.equals(ConnectivityManager.INET_CONDITION_ACTION) ||
                action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Intent i = new Intent(context, ProxyCheckService.class);
            context.startService(i);
        } else if (action.equals(ProxyCheckService.RESET_PROXY_ACTION)) {
            // Only allow settings package to reset proxy
            if (!TextUtils.equals(context.getBasePackageName(), getSendingPackage(intent))) {
                return;
            }
            resetGlobalProxy(context);
            ProxyCheckService.cancelExistingNotifications(context);
        } else if (action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            boolean reset = Util.resetGlobalProxyIfOwnerRemoved(context);
            if (reset) {
                ProxyCheckService.cancelExistingNotifications(context);
            }
        }
    }

    private void resetGlobalProxy(Context context) {
        Settings.Global.putString(context.getContentResolver(), Settings.Global.GLOBAL_PROXY_PACKAGE_NAME, null);
        ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.setGlobalProxy(null);
        Util.broadcastProxyStateChange(context, null);
    }

}
