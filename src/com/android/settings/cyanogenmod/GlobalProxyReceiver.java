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

    private static final int NOTIFICATION_ALERT_ID = 100;
    private static final String RESET_PROXY_ACTION = "cyanogenmod.intent.action.RESET_PROXY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (action.equals(ConnectivityManager.INET_CONDITION_ACTION) ||
                action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            checkIfProxyReachable(context);
        } else if (action.equals(RESET_PROXY_ACTION)) {
            // Only allow settings package to reset proxy
            if (!TextUtils.equals(context.getBasePackageName(), getSendingPackage(intent))) {
                return;
            }
            resetGlobalProxy(context);
            cancelExistingNotifications(context);
        } else if (action.equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            boolean reset = Util.resetGlobalProxyIfOwnerRemoved(context);
            if (reset) {
                cancelExistingNotifications(context);
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

    private void checkIfProxyReachable(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        ProxyProperties proxy = connManager.getGlobalProxy();
        if (proxy == null) {
            return;
        }

        Socket sock = null;
        try {
            SocketAddress sockaddr = new InetSocketAddress(proxy.getHost(), proxy.getPort());
            sock = new Socket();
            int timeoutMs = 2000;
            sock.connect(sockaddr, timeoutMs);
            cancelExistingNotifications(context);
        }catch(Exception e){
            showNotification(context);
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void cancelExistingNotifications(Context context) {
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.cancel(NOTIFICATION_ALERT_ID);
    }

    private void showNotification(Context context) {
        Notification.Builder nb = new Notification.Builder(context);
        nb.setTicker(context.getString(R.string.global_proxy_disconnect_notification_title));
        nb.setSmallIcon(android.R.drawable.ic_menu_mapmode);
        nb.setContentTitle(context.getString(R.string.global_proxy_disconnect_notification_title));
        Intent settings = new Intent();
        settings.setClassName("com.android.settings", "com.android.settings.Settings$PrivacySettingsActivity");
        PendingIntent i = PendingIntent.getActivity(context, 0, settings, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(i);
        nb.setAutoCancel(true);
        Intent setDefaultProxyIntent = new Intent(context, GlobalProxyReceiver.class);
        setDefaultProxyIntent.setAction(RESET_PROXY_ACTION);
        nb.addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(
                R.string.global_proxy_notification_disable_title), PendingIntent.getBroadcast(context,
                        0, setDefaultProxyIntent, 0));
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(NOTIFICATION_ALERT_ID, nb.build());
    }
}
