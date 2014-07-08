package com.android.settings.applications;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProtectedOrHiddenAppsActivity extends Activity {

    protected ListView mListView;

    private static final int MENU_RESET = 0;

    private PackageManager mPackageManager;

    protected AppsAdapter mAppsAdapter;

    protected abstract int getTitleId();

    protected abstract void restoreCheckedItems();

    protected abstract StoreComponentStatus GetStoreTask();

    public static HashSet<ComponentName> getComponentList(Context context, String settingName) {
        String components = Settings.Secure.getString(context.getContentResolver(), settingName);
        HashSet<ComponentName> cmponentList = new HashSet<ComponentName>();

        if (components != null) {
            for (String flattened : components.split("\\|")) {
                ComponentName cmp = ComponentName.unflattenFromString(flattened);
                if (cmp != null) {
                    cmponentList.add(cmp);
                }
            }
        }
        return cmponentList;
    }

    public static void putComponentList(Context context, String settingName,
            HashSet<ComponentName> componentList) {

        StringBuilder flattenedList = new StringBuilder();
        for (ComponentName cmp : componentList) {
            if (flattenedList.length() > 0) {
                flattenedList.append("|");
            }
            flattenedList.append(cmp.flattenToString());
        }
        Settings.Secure.putString(context.getContentResolver(), Settings.Secure.HIDDEN_COMPONENTS,
                flattenedList.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getTitleId());
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
                boolean state = mListView.isItemChecked(position)
                        ? PackageManager.COMPONENT_PROTECTED_STATUS
                        : PackageManager.COMPONENT_VISIBLE_STATUS;

                AppProtectList list = new AppProtectList(componentsList, state);
                StoreComponentStatus task = GetStoreTask();
                task.execute(list);
            }
        });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_RESET, 0, R.string.menu_hidden_apps_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    private void reset() {
        ArrayList<ComponentName> componentsList = new ArrayList<ComponentName>();
        for (int i = 0; i < mListView.getCount(); i++) {
            componentsList.add(mAppsAdapter.getItem(i).componentName);
            mListView.setItemChecked(i, false);
        }

        AppProtectList list = new AppProtectList(componentsList,
                PackageManager.COMPONENT_VISIBLE_STATUS);
        StoreComponentStatus task = GetStoreTask();
        task.execute(list);
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected final class AppEntry {
        public final ComponentName componentName;
        public final String title;

        public AppEntry(ResolveInfo info) {
            ActivityInfo aInfo = info.activityInfo;
            componentName = new ComponentName(aInfo.packageName, aInfo.name);
            title = info.loadLabel(mPackageManager).toString();
        }
    }

    protected final class AppProtectList {
        public final ArrayList<ComponentName> componentNames;
        public final boolean state;

        public AppProtectList(ArrayList<ComponentName> componentNames, boolean state) {
            this.componentNames = new ArrayList<ComponentName>();
            for (ComponentName cn : componentNames) {
                this.componentNames.add(cn.clone());
            }

            this.state = state;
        }
    }

    public abstract class StoreComponentStatus extends AsyncTask<AppProtectList, Void, Void> {
        private ProgressDialog mDialog;
        protected Context mContext;

        public StoreComponentStatus(Context context) {
            mContext = context;
            mDialog = new ProgressDialog(mContext);
        }

        @Override
        protected void onPreExecute() {
            mDialog.setMessage(getResources().getString(R.string.saving_protected_components));
            mDialog.setCancelable(false);
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }

            mAppsAdapter.notifyDataSetChanged();
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

            // set the default icon till the actual app icon is loaded in async task
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
                        String packageName = app.componentName.getPackageName();
                        if (mIcons.containsKey(packageName)) {
                            continue;
                        }
                        Drawable icon = mPackageManager.getApplicationIcon(packageName);
                        mIcons.put(packageName, icon);
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
