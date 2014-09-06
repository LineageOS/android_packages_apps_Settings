package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.applications.FaceUnlockView;
import com.android.settings.applications.LockPatternActivity;
import com.android.settings.applications.ProtectedAppSecurityCallback;

public class ProtectedAppsStatusAuth extends Activity implements ProtectedAppSecurityCallback {
    private static final String TAG = ProtectedAppsStatusAuth.class.getSimpleName();
    public static final String EXTRA_COMPONENT = "cyanogenmod.intent.extra.COMPONENT";
    public static final int REQUEST_LOCK_PATTERN = 1;

    public static final String KEY_AUTHENTICATED = "authenticated_key";

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private int mFailedAttemps = 0;

    private FaceUnlockView mFaceUnlockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean authenticated = false;
        try {
            authenticated = Settings.Secure.getInt(getContentResolver(), KEY_AUTHENTICATED) == 1;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (authenticated) {
            setResult(RESULT_OK);
            finish();
        }

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
            setContentView(R.layout.face_unlock_view);
            mFaceUnlockView = (FaceUnlockView) findViewById(R.id.face_unlock_view);
            mFaceUnlockView.setUnlockCallback(this);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFaceUnlockView.onResume(0);
    }

    @Override
    public void dismiss(boolean securityVerified) {
        if (securityVerified) {
            Settings.Secure.putInt(getContentResolver(), KEY_AUTHENTICATED, 1);
        } else {
            Settings.Secure.putInt(getContentResolver(), KEY_AUTHENTICATED, 0);
        }
        finish();
    }

    @Override
    public void userActivity(long timeout) {

    }

    @Override
    public void reportSuccessfulUnlockAttempt() {
        mFailedAttemps = 0;
        Settings.Secure.putInt(getContentResolver(), KEY_AUTHENTICATED, 1);
        finish();
    }

    @Override
    public void reportFailedUnlockAttempt() {
        if (mFailedAttemps > MAX_FAILED_ATTEMPTS) {
            showBackupSecurity();
        }
        mFailedAttemps++;
    }

    @Override
    public int getFailedAttempts() {
        return mFailedAttemps;
    }

    @Override
    public void showBackupSecurity() {
        Intent lockPattern = new Intent(this, LockPatternActivity.class);
        startActivityForResult(lockPattern, REQUEST_LOCK_PATTERN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LOCK_PATTERN:
                switch (resultCode) {
                    case RESULT_OK:
                        dismiss(true);
                        break;
                    case RESULT_CANCELED:
                        dismiss(false);
                        break;
                }
                break;
        }
    }
}