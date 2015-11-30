package com.android.settings.cyanogenmod;

import com.android.internal.util.cm.LockscreenShortcutsHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class ShortcutCleanupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            // they sent us a bad intent
            return;
        }

        final String packageName = uri.getSchemeSpecificPart();
        if (TextUtils.isEmpty(packageName)) {
            // they sent us a bad intent
            return;
        }

        cleanupLockscreenShortcuts(context, packageName);
    }

    private void cleanupLockscreenShortcuts(Context context, String packageName) {
        LockscreenShortcutsHelper helper = new LockscreenShortcutsHelper(context, null);
        helper.removeTargetsForPackage(packageName);
    }
}
