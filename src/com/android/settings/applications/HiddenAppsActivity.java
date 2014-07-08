package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.cyanogenmod.HiddenAppsReceiver;

import java.util.HashSet;

public class HiddenAppsActivity extends ProtectedOrHiddenAppsActivity {

    @Override
    protected int getTitleId() {
        return R.string.hidden_apps;
    }

    @Override
    protected void restoreCheckedItems() {
        HashSet<ComponentName> hiddenApps = getComponentList(this,
                Settings.Secure.HIDDEN_COMPONENTS);

        for (int i = 0; i < mAppsAdapter.getCount(); i++) {
            AppEntry info = mAppsAdapter.getItem(i);
            if (hiddenApps.contains(info.componentName)) {
                mListView.setItemChecked(i, true);
            }
        }
    }

    public class StoreComponentHiddenStatus extends StoreComponentStatus {
        public StoreComponentHiddenStatus(Context context) {
            super(context);
        }

        @Override
        protected Void doInBackground(final AppProtectList... args) {
            for (AppProtectList appList : args) {
                HiddenAppsReceiver.updateHiddenAppComponentsAndNotify(mContext,
                        appList.componentNames, appList.state);
            }

            return null;
        }
    }

    @Override
    protected StoreComponentStatus GetStoreTask() {
        return new StoreComponentHiddenStatus(this);
    }

}
