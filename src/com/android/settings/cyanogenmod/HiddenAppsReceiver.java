package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.applications.ProtectedOrHiddenAppsActivity;

import java.util.ArrayList;
import java.util.HashSet;

public class HiddenAppsReceiver extends BroadcastReceiver {
    private static final String TAG = "HiddenAppsReceiver";

    public static final String HIDDEN_ACTION = "cyanogenmod.intent.action.PACKAGE_HIDDEN";
    private static final String HIDDEN_CHANGED_ACTION =
            "cyanogenmod.intent.action.HIDDEN_COMPONENT_UPDATE";
    public static final String HIDDEN_STATE =
            "cyanogenmod.intent.action.PACKAGE_HIDDEN_STATE";
    public static final String HIDDEN_COMPONENTS =
            "cyanogenmod.intent.action.PACKAGE_HIDDEN_COMPONENTS";
    private static final String HIDDEN_APP_PERMISSION = "cyanogenmod.permission.HIDDEN_APP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (HIDDEN_ACTION.equals(intent.getAction())) {
            boolean state = intent.getBooleanExtra(HIDDEN_STATE,
                    PackageManager.COMPONENT_VISIBLE_STATUS);
            ArrayList<ComponentName> components =
                    intent.getParcelableArrayListExtra(HIDDEN_COMPONENTS);
            updateHiddenAppComponentsAndNotify(context, components, state);
        }
    }

    public static void updateHiddenAppComponentsAndNotify(Context context,
            ArrayList<ComponentName> components, boolean state) {
        updateSettingsSecure(context, components, state);
        notifyHiddenChanged(context, components, state);
    }

    private static void updateSettingsSecure(Context context,
            ArrayList<ComponentName> components, boolean state) {
        HashSet<ComponentName> newComponentList = ProtectedOrHiddenAppsActivity.getComponentList(
                context, Settings.Secure.HIDDEN_COMPONENTS);

        boolean update = state != PackageManager.COMPONENT_VISIBLE_STATUS
            ? newComponentList.addAll(components)
            : newComponentList.removeAll(components);

        if (update) {
            ProtectedOrHiddenAppsActivity.putComponentList(context,
                    Settings.Secure.HIDDEN_COMPONENTS, newComponentList);
        }
    }

    private static void notifyHiddenChanged(Context context,
            ArrayList<ComponentName> components, boolean state) {
        Intent intent = new Intent(HIDDEN_CHANGED_ACTION);
        intent.putExtra(HIDDEN_STATE, state);
        intent.putExtra(HIDDEN_COMPONENTS, components);

        context.sendBroadcast(intent, HIDDEN_APP_PERMISSION);
    }
}
