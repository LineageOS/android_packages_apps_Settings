package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

public class ProtectedAppsStatusAuth extends Activity {
    private static final String TAG = ProtectedAppsStatusAuth.class.getSimpleName();
    public static final String EXTRA_COMPONENT = "cyanogenmod.intent.extra.COMPONENT";
    public static final int REQUEST_LOCK_PATTERN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        boolean protectedApp = false;
        if (intent != null) {
            String cmp = intent.getStringExtra(EXTRA_COMPONENT);
            ComponentName cName = ComponentName.unflattenFromString(cmp);
            // upgradeTester app is explicitly considered as protected
            if (!TextUtils.isEmpty(cName.getPackageName())){
                PackageManager pm = getPackageManager();
                ApplicationInfo aInfo = null;
                try {
                    aInfo = pm.getApplicationInfo(cName.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (aInfo != null && aInfo.protect) {
                    protectedApp = true;
                }
            }
        }

        if (protectedApp) {
            // Request auth
            Intent lockPatternActivity = new Intent();
            lockPatternActivity.setClassName(
                    "com.android.settings",
                    "com.android.settings.applications.LockPatternActivity");
            startActivityForResult(lockPatternActivity, REQUEST_LOCK_PATTERN);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case (REQUEST_LOCK_PATTERN):
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
                break;
            default:
                break;
        }
    }
}
