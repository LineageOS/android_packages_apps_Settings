package com.android.settings.applications;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.cyanogenmod.ProtectedAppsReceiver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectedAppsActivity extends Activity {
    private static final int REQ_ENTER_PATTERN = 1;

    private ListView mListView;

    private static final int MENU_RESET = 0;
    private static final int MENU_RESET_LOCK = 1;

    private PackageManager mPackageManager;

    private AppsAdapter mAppsAdapter;

    private ArrayList<ComponentName> mProtect;

    private boolean mWaitUserAuth = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.protected_apps);
        setContentView(R.layout.hidden_apps_list);

        mPackageManager = getPackageManager();
        mAppsAdapter = new AppsAdapter(this, R.layout.hidden_apps_list_item);
        mAppsAdapter.setNotifyOnChange(true);

        mListView = (ListView) findViewById(R.id.protected_apps_list);
        mListView.setAdapter(mAppsAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ComponentName cn = mAppsAdapter.getItem(position).componentName;
                ArrayList<ComponentName> componentsList = new ArrayList<ComponentName>();
                componentsList.add(cn);
                boolean protect = !mListView.isItemChecked(position);

                AppProtectList list = new AppProtectList(componentsList, protect);
                StoreComponentProtectedStatus task = new StoreComponentProtectedStatus(ProtectedAppsActivity.this);
                task.execute(list);
            }
        });

        mProtect = new ArrayList<ComponentName>();

        // Require pattern lock
        Intent lockPattern = new Intent(this, LockPatternActivity.class);
        startActivityForResult(lockPattern, REQ_ENTER_PATTERN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AsyncTask<Void, Void, List<AppEntry>> refreshAppsTask =
                new AsyncTask<Void, Void, List<AppEntry>>() {

                    @Override
                    protected void onPostExecute(List<AppEntry> apps) {
                        mAppsAdapter.clear();
                        mAppsAdapter.addAll(apps);
                        restoreCheckedItems();
                    }

                    @Override
                    protected List<AppEntry> doInBackground(Void... params) {
                        return refreshApps();
                    }
                };
        refreshAppsTask.execute(null, null, null);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Don't stick around
        if (mWaitUserAuth){
            finish();
        }
    }

    private void restoreCheckedItems() {
        AppsAdapter listAdapter = (AppsAdapter) mListView.getAdapter();

        for (int i = 0; i < listAdapter.getCount(); i++) {
            AppEntry info = listAdapter.getItem(i);
            try {
                if (getPackageManager().getActivityInfo(info.componentName, 0)
                        .applicationInfo.protect) {
                    mListView.setItemChecked(i, true);
                    mProtect.add(info.componentName);
                }
            } catch (PackageManager.NameNotFoundException e) {
                continue; //ignore it and move on
            }
        }
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
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_RESET, 0, R.string.menu_hidden_apps_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_RESET_LOCK, 0, R.string.menu_hidden_apps_reset_lock)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    private void reset() {
        ArrayList<ComponentName> componentsList = new ArrayList<ComponentName>();
        for (int i = 0; i < mListView.getCount(); i++) {
            componentsList.add(mAppsAdapter.getItem(i).componentName);
            mListView.setItemChecked(i, false);
        }

        AppProtectList list = new AppProtectList(componentsList, true);
        StoreComponentProtectedStatus task = new StoreComponentProtectedStatus(this);
        task.execute(list);
    }

    private void resetLock() {
        // Clear out the saved pattern lock
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(LockPatternActivity.PATTERN_LOCK_PROTECTED_APPS);
        editor.commit();

        // Reset this activity
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private List<AppEntry> refreshApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(mPackageManager));
        List<AppEntry> appEntries = new ArrayList<AppEntry>(apps.size());
        for (ResolveInfo info : apps) {
            appEntries.add(new AppEntry(info));
        }
        return appEntries;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                reset();
                return true;
            case MENU_RESET_LOCK:
                resetLock();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final class AppEntry {

        public final ComponentName componentName;
        public final String title;

        public AppEntry(ResolveInfo info) {
            componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            title = info.loadLabel(mPackageManager).toString();
        }
    }

    private final class AppProtectList {

        public final ArrayList<ComponentName> componentNames;
        public final boolean protect;

        public AppProtectList(ArrayList<ComponentName> componentNames, boolean protect) {
            this.componentNames = new ArrayList<ComponentName>();
            for (ComponentName cn : componentNames) {
                this.componentNames.add(cn.clone());
            }

            this.protect = protect;
        }
    }

    public class StoreComponentProtectedStatus extends AsyncTask<AppProtectList, Void, Void> {
        private ProgressDialog mDialog;
        private Context mContext;

        public StoreComponentProtectedStatus(Context context) {
            mContext = context;
            mDialog = new ProgressDialog(mContext);
        }

        @Override
        protected void onPreExecute() {
            mDialog.setMessage(getResources().getString(R.string.saving_protected_components));
            mDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }

            mAppsAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(final AppProtectList... args) {
            for (AppProtectList appList : args) {
                String components = "";

                for (ComponentName cn : appList.componentNames) {
                    components += cn.flattenToShortString() + "|";
                }

                ProtectedAppsReceiver.protectedAppComponents(components.split("\\|"),
                        appList.protect, mContext);
                ProtectedAppsReceiver.updateSettingsSecure(components.split("\\|"),
                        appList.protect, mContext);
                ProtectedAppsReceiver.notifyProtectedChanged(components,
                        appList.protect, mContext);
            }

            return null;
        }
    }

    /**
     * App view holder used to reuse the views inside the list.
     */
    private static class AppViewHolder {
        public final TextView title;
        public final ImageView icon;

        public AppViewHolder(View parentView) {
            icon = (ImageView) parentView.findViewById(R.id.icon);
            title = (TextView) parentView.findViewById(R.id.title);
        }
    }

    public class AppsAdapter extends ArrayAdapter<AppEntry> {

        private final LayoutInflater mInflator;

        private ConcurrentHashMap<String, Drawable> mIcons;
        private Drawable mDefaultImg;
        private List<AppEntry> mApps;

        public AppsAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);

            mApps = new ArrayList<AppEntry>();

            mInflator = LayoutInflater.from(context);

            // set the default icon till the actual app icon is loaded in async
            // task
            mDefaultImg = context.getResources().getDrawable(android.R.mipmap.sym_def_app_icon);
            mIcons = new ConcurrentHashMap<String, Drawable>();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            AppViewHolder viewHolder;

            if (convertView == null) {
                convertView = mInflator.inflate(R.layout.hidden_apps_list_item, parent, false);
                viewHolder = new AppViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (AppViewHolder) convertView.getTag();
            }

            AppEntry app = getItem(position);

            viewHolder.title.setText(app.title);

            Drawable icon = mIcons.get(app.componentName.getPackageName());
            viewHolder.icon.setImageDrawable(icon != null ? icon : mDefaultImg);

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            // If we have new items, we have to load their icons
            // If items were deleted, remove them from our mApps
            List<AppEntry> newApps = new ArrayList<AppEntry>(getCount());
            List<AppEntry> oldApps = new ArrayList<AppEntry>(getCount());
            for (int i = 0; i < getCount(); i++) {
                AppEntry app = getItem(i);
                if (mApps.contains(app)) {
                    oldApps.add(app);
                } else {
                    newApps.add(app);
                }
            }

            if (newApps.size() > 0) {
                new LoadIconsTask().execute(newApps.toArray(new AppEntry[] {}));
                newApps.addAll(oldApps);
                mApps = newApps;
            } else {
                mApps = oldApps;
            }
        }

        /**
         * An asynchronous task to load the icons of the installed applications.
         */
        private class LoadIconsTask extends AsyncTask<AppEntry, Void, Void> {
            @Override
            protected Void doInBackground(AppEntry... apps) {
                for (AppEntry app : apps) {
                    try {
                        if (mIcons.containsKey(app.componentName.getPackageName())) {
                            continue;
                        }
                        Drawable icon = mPackageManager.getApplicationIcon(app.componentName
                                .getPackageName());
                        mIcons.put(app.componentName.getPackageName(), icon);
                        publishProgress();
                    } catch (PackageManager.NameNotFoundException e) {
                        // ignored; app will show up with default image
                    }
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Void... progress) {
                notifyDataSetChanged();
            }
        }
    }
}
