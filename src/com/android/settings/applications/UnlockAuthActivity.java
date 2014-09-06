package com.android.settings.applications;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.cyanogenmod.ProtectedAppsStatusAuth;

public class UnlockAuthActivity extends Activity implements ProtectedAppSecurityCallback {

    public static final String FACELOCK_PROTECTED_APPS = "face_lock_protected_apps";
    public static final String RECREATE_FACELOCK = "recreate_face_lock";

    private static final String REQUEST_CODE = "request_code";
    private static final int FACELOCK_REQUEST = 3;
    private static final int UNLOCK_REQUEST = 4;

    private SharedPreferences mSharedPreferences;
    private LockPatternUtils mLockPatternUtils;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private int mFailedAttemps = 0;
    private FaceUnlockView mFaceUnlockView;

    private int mRequestCode;

    public static void startForAuth(Activity context, int requestCode) {
        Intent intent = new Intent(context, UnlockAuthActivity.class);
        intent.putExtra(REQUEST_CODE, requestCode);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        boolean authenticated = false;
//        try {
//            authenticated = Settings.Secure.getInt(getContentResolver(),
//                    ProtectedAppsStatusAuth.KEY_AUTHENTICATED) == 1;
//        } catch (Settings.SettingNotFoundException e) {
//            e.printStackTrace();
//        }
//        if (authenticated) {
//            setResult(RESULT_OK);
//            finish();
//        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mLockPatternUtils = new LockPatternUtils(this);
        mRequestCode = getIntent().getIntExtra(REQUEST_CODE, -1);
        if (mLockPatternUtils.isBiometricWeakInstalled() || isFacelockSetup()) {
            setContentView(R.layout.face_unlock_view);
            mFaceUnlockView = (FaceUnlockView) findViewById(R.id.face_unlock_view);
            mFaceUnlockView.setUnlockCallback(this);
        } else if (mLockPatternUtils.isBiometricWeakInstalled() && !isFacelockSetup()) {
            startActivityForResult(getSetupFaceUnlockIntent(), FACELOCK_REQUEST);
        } else {
            startActivityForResult(getLockPatternIntent(), mRequestCode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFaceUnlockView.onResume(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FACELOCK_REQUEST:
                if (FaceUnlockStatus.getInstance().getResult() == RESULT_OK) {
                    startActivity(getFinishSetupFaceUnlockIntent());
                    mSharedPreferences.edit()
                            .putBoolean(FACELOCK_PROTECTED_APPS, true)
                            .commit();
                    setResult(RESULT_OK);
                    FaceUnlockStatus.getInstance().setResult(RESULT_CANCELED);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
               break;
            default:
                if (RESULT_OK == resultCode) {
                    Settings.Secure.putInt(getContentResolver(),
                            ProtectedAppsStatusAuth.KEY_AUTHENTICATED,
                            RESULT_OK == resultCode ? 1 : 0);
                }
                setResult(resultCode);
                finish();

        }
    }

    private boolean isFacelockSetup() {
        return  mSharedPreferences.getBoolean(FACELOCK_PROTECTED_APPS, false);
    }

    private Intent getSetupFaceUnlockIntent() {
        boolean showTutorial = !mLockPatternUtils.isBiometricWeakEverChosen();
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.SetupIntro");
        intent.putExtra("showTutorial", showTutorial);
        PendingIntent pending = PendingIntent.getActivity(this, FACELOCK_REQUEST,
                getLockPatternIntent(), 0);
        intent.putExtra("PendingIntent", pending);
        return intent;
    }

    private Intent getFinishSetupFaceUnlockIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.SetupEndScreen");
        return intent;
    }

    private Intent getLockPatternIntent() {
        return new Intent(this, LockPatternActivity.class);
    }

    @Override
    public void dismiss(boolean securityVerified) {
        if (securityVerified) {
            Settings.Secure.putInt(getContentResolver(), ProtectedAppsStatusAuth.KEY_AUTHENTICATED, 1);
        } else {
            Settings.Secure.putInt(getContentResolver(), ProtectedAppsStatusAuth.KEY_AUTHENTICATED, 0);
        }
        finish();
    }

    @Override
    public void userActivity(long timeout) {

    }

    @Override
    public void reportSuccessfulUnlockAttempt() {
        mFailedAttemps = 0;
        Settings.Secure.putInt(getContentResolver(), ProtectedAppsStatusAuth.KEY_AUTHENTICATED, 1);
        setResult(RESULT_OK);
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
        Intent lockPattern = getLockPatternIntent();
        startActivityForResult(lockPattern, UNLOCK_REQUEST);
    }

    static class FaceUnlockStatus {

        private static final FaceUnlockStatus mInstance = new FaceUnlockStatus();

        private int mResult = RESULT_CANCELED;

        private FaceUnlockStatus() {}

        static FaceUnlockStatus getInstance() {
            return mInstance;
        }

        public int getResult() {
            return mResult;
        }

        public void setResult(int result) {
            mResult = result;
        }
    }
}
