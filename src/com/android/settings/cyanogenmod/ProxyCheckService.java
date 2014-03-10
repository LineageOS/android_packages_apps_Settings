package com.android.settings.cyanogenmod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.android.settings.R;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ProxyProperties;

public class ProxyCheckService extends IntentService {

    static final String RESET_PROXY_ACTION = "cyanogenmod.intent.action.RESET_PROXY";
    private static final int NOTIFICATION_ALERT_ID = 100;

    public ProxyCheckService() {
        super(ProxyCheckService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        checkIfProxyReachable();
        stopSelf();
    }

    private void checkIfProxyReachable() {
        ConnectivityManager connManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        ProxyProperties proxy = connManager.getGlobalProxy();
        if (proxy == null) {
            return;
        }

        Socket sock = null;
        try {
            SocketAddress sockaddr = new InetSocketAddress(proxy.getHost(), proxy.getPort());
            sock = new Socket();
            int timeoutMs = 10000;
            sock.connect(sockaddr, timeoutMs);
            cancelExistingNotifications(this);
        }catch(Exception e){
            showNotification();
        } finally {
            if (sock != null) {
                try {
                    sock.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    static void cancelExistingNotifications(Context context) {
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.cancel(NOTIFICATION_ALERT_ID);
    }

    private void showNotification() {
        Notification.Builder nb = new Notification.Builder(this);
        nb.setTicker(getString(R.string.global_proxy_disconnect_notification_title));
        nb.setSmallIcon(android.R.drawable.ic_menu_mapmode);
        nb.setContentTitle(getString(R.string.global_proxy_disconnect_notification_title));
        Intent settings = new Intent();
        settings.setClassName("com.android.settings", "com.android.settings.Settings$PrivacySettingsActivity");
        PendingIntent i = PendingIntent.getActivity(this, 0, settings, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(i);
        nb.setAutoCancel(true);
        Intent setDefaultProxyIntent = new Intent(this, GlobalProxyReceiver.class);
        setDefaultProxyIntent.setAction(RESET_PROXY_ACTION);
        nb.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(
                R.string.global_proxy_notification_disable_title), PendingIntent.getBroadcast(this,
                        0, setDefaultProxyIntent, 0));
        NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.notify(NOTIFICATION_ALERT_ID, nb.build());
    }
}