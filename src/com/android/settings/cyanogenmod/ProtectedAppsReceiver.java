package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;

public class ProtectedAppsReceiver extends BroadcastReceiver {
    private static final String TAG = "ProtectedAppsReceiver";

    private static final String PROTECTED_ACTION = "cyanogenmod.intent.action.PACKAGE_PROTECTED";
    private static final String PROTECTED_CHANGED_ACTION =
            "cyanogenmod.intent.action.PROTECTED_COMPONENT_UPDATE";
    private static final String PROTECTED_STATE =
            "cyanogenmod.intent.action.PACKAGE_PROTECTED_STATE";
    private static final String PROTECTED_COMPONENT =
            "cyanogenmod.intent.action.PACKAGE_PROTECTED_COMPONENT";
    private static final String PROTECTED_APP_PERMISSION = "cyanogenmod.permission.PROTECTED_APP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PROTECTED_ACTION.equals(intent.getAction())) {
            boolean protect = intent.getBooleanExtra(PROTECTED_STATE, true);
            String components = intent.getStringExtra(PROTECTED_COMPONENT);
            components = components == null ? "" : components;
            String [] cName = components.split("\\|");

            protectedAppComponents(cName, protect, context);
            updateSettingsSecure(cName, protect, context);
            notifyProtectedChanged(components, protect, context);
        }
    }

    public static void protectedAppComponents(String [] componentNames, boolean protect, Context context) {
        for (String flat : componentNames) {
            ComponentName cmp = ComponentName.unflattenFromString(flat);
            if (cmp != null) {
                try{
                    context.getPackageManager().setComponentProtectedSetting(cmp,
                            protect);
                } catch (NoSuchMethodError nsm) {
                    Log.e(TAG, "Unable to protected app via PackageManager");
                }
            }
        }
    }

    public static void updateSettingsSecure(String [] componentNames, boolean protect, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String hiddenComponents = Settings.Secure.getString(contentResolver,
                Settings.Secure.PROTECTED_COMPONENTS);
        hiddenComponents = hiddenComponents == null ? "" : hiddenComponents;
        String [] hiddenComponentNames = hiddenComponents.split("\\|");

        ArrayList<ComponentName> hiddenComponentsList = new ArrayList<ComponentName>();
        for (String flat : hiddenComponentNames) {
            ComponentName cmp = ComponentName.unflattenFromString(flat);
            if (cmp != null) {
                hiddenComponentsList.add(cmp);
            }
        }

        ArrayList<ComponentName> newComponentsList = new ArrayList<ComponentName>();
        for (String flat : componentNames) {
            ComponentName cmp = ComponentName.unflattenFromString(flat);
            if (cmp != null) {
                newComponentsList.add(cmp);
            }
        }

        boolean update = false;
        if (protect) {
            update = hiddenComponentsList.removeAll(newComponentsList);
        } else {
            update = hiddenComponentsList.addAll(newComponentsList);
        }

        if (update) {
            String newSave = "";
            for (ComponentName cmp : hiddenComponentsList) {
                newSave += cmp.flattenToString() + "|";
            }
            Settings.Secure.putString(contentResolver, Settings.Secure.PROTECTED_COMPONENTS, newSave);
        }

    }

    public static void notifyProtectedChanged(String componentName, boolean protect, Context context) {
        Intent intent = new Intent();
        intent.setAction(PROTECTED_CHANGED_ACTION);
        intent.putExtra(PROTECTED_STATE, protect);
        intent.putExtra(PROTECTED_COMPONENT, componentName);

        context.sendBroadcast(intent, PROTECTED_APP_PERMISSION);
    }
}
