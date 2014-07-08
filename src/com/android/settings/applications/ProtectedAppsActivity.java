package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.cyanogenmod.ProtectedAppsReceiver;

public class ProtectedAppsActivity extends ProtectedOrHiddenAppsActivity {
    private static final int REQ_ENTER_PATTERN = 1;
    private static final int REQ_RESET_PATTERN = 2;

    private static final int MENU_RESET_LOCK = 1;

    private boolean mWaitUserAuth = false;

    @Override
    protected int getTitleId() {
        return R.string.protected_apps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Require pattern lock
        Intent lockPattern = new Intent(this, LockPatternActivity.class);
        startActivityForResult(lockPattern, REQ_ENTER_PATTERN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_ENTER_PATTERN:
                mWaitUserAuth = true;
                switch (resultCode) {
                    case RESULT_OK:
                        //Nothing to do, proceed!
                        break;
                    case RESULT_CANCELED:
                        // user failed to define a pattern, do not lock the folder
                        finish();
                        break;
                }
                break;
            case REQ_RESET_PATTERN:
                mWaitUserAuth = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Don't stick around
        if (mWaitUserAuth) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_RESET_LOCK, 0, R.string.menu_hidden_apps_reset_lock)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    private void resetLock() {
        mWaitUserAuth = false;
        Intent lockPattern = new Intent(LockPatternActivity.RECREATE_PATTERN, null,
                this, LockPatternActivity.class);
        startActivityForResult(lockPattern, REQ_RESET_PATTERN);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET_LOCK:
                resetLock();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void restoreCheckedItems() {
        PackageManager pm = getPackageManager();

        for (int i = 0; i < mAppsAdapter.getCount(); i++) {
            AppEntry info = mAppsAdapter.getItem(i);
            try {
                if (pm.getActivityInfo(info.componentName, 0).applicationInfo.protect) {
                    mListView.setItemChecked(i, true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                continue; // ignore it and move on
            }
        }
    }

    public class StoreComponentProtectedStatus extends StoreComponentStatus {
        public StoreComponentProtectedStatus(Context context) {
            super(context);
        }

        @Override
        protected Void doInBackground(final AppProtectList... args) {
            for (AppProtectList appList : args) {
                ProtectedAppsReceiver.updateProtectedAppComponentsAndNotify(mContext,
                        appList.componentNames, appList.state);
            }

            return null;
        }
    }

    @Override
    protected StoreComponentStatus GetStoreTask() {
        return new StoreComponentProtectedStatus(this);
    }

}
