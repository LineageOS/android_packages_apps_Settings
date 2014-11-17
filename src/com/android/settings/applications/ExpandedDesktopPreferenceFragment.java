package com.android.settings.applications;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.WindowManagerPolicyControl;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandedDesktopPreferenceFragment extends SettingsPreferenceFragment implements AdapterView.OnItemClickListener {
    private static final int STATE_DISABLED = 0;
    private static final int STATE_STATUS_HIDDEN = 1;
    private static final int STATE_NAVIGATION_HIDDEN = 2;
    private static final int STATE_BOTH_HIDDEN = 3;

    private PackageAdapter mAdapter;
    private ApplicationsState mApplicationsState;
    private View mProgressBar;
    private View mEmptyView;
    private ListView mListView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());

        mAdapter = new PackageAdapter(getActivity());
        WindowManagerPolicyControl.reloadFromSetting(getActivity());

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.expanded_desktop, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ListView) view.findViewById(R.id.list_view);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mEmptyView = view.findViewById(R.id.nothing_to_show);

        if (!mAdapter.isEmpty()) {
            mProgressBar.setVisibility(View.GONE);
            mListView.setEmptyView(mEmptyView);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.mode.performClick();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.expanded_desktop, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enable_for_all:
                enableForAll();
                return true;
            case R.id.disable_for_all:
                disableForAll();
                return true;
            case R.id.show_enabled:
                boolean isChecked = !item.isChecked();
                item.setChecked(isChecked);
                mAdapter.setShowOnlyEnabled(isChecked);
                return true;
        }

        return false;
    }

    private void enableForAll() {
        writeValue("immersive.full=*");
    }

    private void disableForAll() {
        writeValue("");
        mAdapter.notifyDataSetInvalidated();
    }

    private void writeValue(String value) {
        Settings.Global.putString(getContentResolver(), Settings.Global.POLICY_CONTROL, value);
    }

    private static int getStateForPackage(String packageName) {
        int state = STATE_DISABLED;

        if (WindowManagerPolicyControl.immersiveStatusFilterMatches(packageName)) {
            state = STATE_STATUS_HIDDEN;
        }
        if (WindowManagerPolicyControl.immersiveNavigationFilterMatches(packageName)) {
            if (state == STATE_DISABLED) {
                state = STATE_NAVIGATION_HIDDEN;
            } else {
                state = STATE_BOTH_HIDDEN;
            }
        }
        return state;
    }

    private class PackageAdapter extends BaseAdapter implements ApplicationsState.Callbacks,
            AdapterView.OnItemSelectedListener {

        private final LayoutInflater inflater;
        private final ModeAdapter modesAdapter;
        private ApplicationsState.Session session;
        private List<ApplicationsState.AppEntry> entries = new ArrayList<>();
        private ActivityFilter activityFilter;

        public PackageAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
            this.session = mApplicationsState.newSession(this);

            this.modesAdapter = new ModeAdapter(context);
            this.activityFilter = new ActivityFilter(context.getPackageManager());

            session.resume();
            rebuild();
        }

        public void setShowOnlyEnabled(boolean onlyEnabled) {
            activityFilter.setOnlyEnabled(onlyEnabled);
            rebuild();
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder(inflater.inflate(R.layout.expanded_item, parent, false));
                holder.mode.setAdapter(modesAdapter);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApplicationsState.AppEntry entry = entries.get(position);

            holder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);
            holder.mode.setOnItemSelectedListener(null);
            holder.mode.setSelection(getStateForPackage(entry.info.packageName), false);
            holder.mode.setOnItemSelectedListener(this);
            holder.mode.setTag(entry);

            return holder.rootView;
        }

        private void rebuild() {
            ArrayList<ApplicationsState.AppEntry> newEntries = session.rebuild(
                    activityFilter, ApplicationsState.ALPHA_COMPARATOR);
            if (newEntries != null) {
                setEntries(newEntries);
            }
        }

        private void setEntries(ArrayList<ApplicationsState.AppEntry> entries) {
            this.entries = entries;
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
            if (mListView != null && mListView.getEmptyView() != mEmptyView) {
                mListView.setEmptyView(mEmptyView);
            }
            notifyDataSetChanged();
        }

        @Override
        public void onRunningStateChanged(boolean running) {
        }

        @Override
        public void onPackageListChanged() {
            rebuild();
        }

        @Override
        public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
            setEntries(apps);
        }

        @Override
        public void onPackageIconChanged() {
        }

        @Override
        public void onPackageSizeChanged(String packageName) {

        }

        @Override
        public void onAllSizesComputed() {
        }

        public void onDestroy() {
            session.pause();
            session.release();
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) parent.getTag();

            WindowManagerPolicyControl.removeFromWhiteLists(entry.info.packageName);
            switch (position) {
                case STATE_STATUS_HIDDEN:
                    WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    break;
                case STATE_NAVIGATION_HIDDEN:
                    WindowManagerPolicyControl.addToNavigationWhiteList(entry.info.packageName);
                    break;
                case STATE_BOTH_HIDDEN:
                    WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    WindowManagerPolicyControl.addToNavigationWhiteList(entry.info.packageName);
                    break;
            }
            WindowManagerPolicyControl.saveToSettings(parent.getContext());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private static class ViewHolder {
        private TextView title;
        private Spinner mode;
        private ImageView icon;
        private View rootView;

        private ViewHolder(View view) {
            this.title = (TextView) view.findViewById(R.id.app_name);
            this.mode = (Spinner) view.findViewById(R.id.app_mode);
            this.icon = (ImageView) view.findViewById(R.id.app_icon);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private static class ModeAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private boolean hasNavigationBar = true;
        private final int[] items = {R.string.expanded_hide_nothing, R.string.expanded_hide_status,
                R.string.expanded_hide_navigation, R.string.expanded_hide_both};

        private ModeAdapter(Context context) {
            inflater = LayoutInflater.from(context);

            try {
                hasNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
            } catch (RemoteException e) {
                // Do nothing
            }
        }

        @Override
        public int getCount() {
            return hasNavigationBar ? 4 : 2;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = (TextView) inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            view.setText(items[position]);

            return view;
        }
    }

    private static class ActivityFilter implements ApplicationsState.AppFilter {

        private static final Map<String, Boolean> cache = new HashMap<>();
        private final PackageManager mPackageManager;
        private boolean onlyEnabled = false;

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            return hasActivities(info) && (!onlyEnabled || isExpandedEnabledFor(info));
        }

        private boolean hasActivities(ApplicationInfo info) {
            synchronized (cache) {
                Boolean fromCache = cache.get(info.packageName);
                if (fromCache != null) {
                    return fromCache;
                }

                try {
                    PackageInfo pi = mPackageManager.getPackageInfo(
                            info.packageName, PackageManager.GET_ACTIVITIES);
                    boolean hasActivities = pi.activities != null && pi.activities.length > 0;
                    cache.put(info.packageName, hasActivities);
                    return hasActivities;
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }

            return true;
        }

        private boolean isExpandedEnabledFor(ApplicationInfo info) {
            return WindowManagerPolicyControl.immersiveStatusFilterMatches(info.packageName)
                    || WindowManagerPolicyControl.immersiveNavigationFilterMatches(info.packageName);
        }

        public void setOnlyEnabled(boolean onlyEnabled) {
            this.onlyEnabled = onlyEnabled;
        }
    }
}
