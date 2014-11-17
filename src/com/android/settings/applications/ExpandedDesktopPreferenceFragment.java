/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.applications;

import android.animation.LayoutTransition;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.WindowManagerPolicyControl;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.SpinnerBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandedDesktopPreferenceFragment extends SettingsPreferenceFragment
        implements AdapterView.OnItemClickListener, ApplicationsState.Callbacks,
        SpinnerBar.OnSpinnerItemSelectedListener {

    private static final int STATE_DISABLED = 0;
    private static final int STATE_STATUS_HIDDEN = 1;
    private static final int STATE_NAVIGATION_HIDDEN = 2;
    private static final int STATE_BOTH_HIDDEN = 3;

    private static final int STATE_DISABLE_FOR_ALL = 0;
    private static final int STATE_ENABLE_FOR_ALL = 1;
    private static final int STATE_USER_CONFIGURABLE = 2;

    private static final String USER_APPS = "user_apps";

    private AllPackagesAdapter mAllPackagesAdapter;
    private ApplicationsState mApplicationsState;
    private View mEmptyView;
    private ListView mUserListView;
    private ApplicationsState.Session mSession;
    private ActivityFilter mActivityFilter;
    private Map<String, ApplicationsState.AppEntry> mEntryMap =
            new HashMap<String, ApplicationsState.AppEntry>();
    private Interpolator mInterpolator;
    private int mExpandedDesktopState;
    private SpinnerBar mSpinnerBar;

    private int getExpandedDesktopState(ContentResolver cr) {
        String value = Settings.Global.getString(cr, Settings.Global.POLICY_CONTROL);
        if (TextUtils.isEmpty(value)) {
            return STATE_DISABLE_FOR_ALL;
        }
        if ("immersive.full=*".equals(value)) {
            return STATE_ENABLE_FOR_ALL;
        }
        return STATE_USER_CONFIGURABLE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.resume();
        mActivityFilter = new ActivityFilter(getActivity().getPackageManager());

        mAllPackagesAdapter = new AllPackagesAdapter(getActivity());
        WindowManagerPolicyControl.reloadFromSetting(getActivity(),
                Settings.Global.POLICY_CONTROL_SELECTED);

        if (savedInstanceState == null || !savedInstanceState.containsKey(USER_APPS)) {
            ArrayList<String> whitelistEntries =
                    new ArrayList<String>(WindowManagerPolicyControl.getWhiteLists());
            mAllPackagesAdapter.setEntriesEnabled(whitelistEntries);
        } else {
            mAllPackagesAdapter.setEntriesEnabled(
                    savedInstanceState.getStringArrayList(USER_APPS));
        }
        mAllPackagesAdapter.notifyDataSetChanged();

        mInterpolator = AnimationUtils.loadInterpolator(getActivity(),
                android.R.interpolator.fast_out_slow_in);

        mExpandedDesktopState = getExpandedDesktopState(getActivity().getContentResolver());

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuild();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.expanded_desktop, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.pause();
        mSession.release();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSpinnerBar != null) {
            mSpinnerBar.removeOnItemSelectedListener(this);
        }
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUserListView = (ListView) view.findViewById(R.id.user_list_view);
        mUserListView.setAdapter(mAllPackagesAdapter);
        mUserListView.setOnItemClickListener(this);

        mSpinnerBar = ((SettingsActivity) getActivity()).getSpinnerBar();
        mSpinnerBar.show();
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.expanded_desktop_states,
                R.layout.spinner_simple_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBar.setAdapter(arrayAdapter);
        mSpinnerBar.addOnItemSelectedListener(this);
        mSpinnerBar.setTextViewLabel(getString(R.string.expanded_desktop_state));
        mSpinnerBar.setSpinnerPosition(mExpandedDesktopState);

        mEmptyView = view.findViewById(R.id.nothing_to_show);

        if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
            showListView();
        } else {
            hideListView();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.mode.performClick();
    }

    private void enableForAll() {
        mExpandedDesktopState = STATE_ENABLE_FOR_ALL;
        mAllPackagesAdapter.notifyDataSetInvalidated();
        writeValue("immersive.full=*");
        hideListView();
    }

    private void disableForAll() {
        mExpandedDesktopState = STATE_DISABLE_FOR_ALL;
        mAllPackagesAdapter.notifyDataSetInvalidated();
        writeValue("");
        hideListView();
    }

    private void userConfigurableSettings() {
        mExpandedDesktopState = STATE_USER_CONFIGURABLE;
        mAllPackagesAdapter.notifyDataSetInvalidated();
        WindowManagerPolicyControl.saveToSettings(getActivity());
        showListView();
    }

    private void hideListView() {
        mUserListView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
    }

    private void showListView() {
        mUserListView.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
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

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
        mActivityFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        handleAppEntries(entries);
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

    @Override
    public void onSpinnerItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case STATE_DISABLE_FOR_ALL:
                disableForAll();
                break;
            case STATE_ENABLE_FOR_ALL:
                enableForAll();
                break;
            case STATE_USER_CONFIGURABLE:
                userConfigurableSettings();
                break;
        }
    }

    @Override
    public void onSpinnerNothingSelected(AdapterView<?> parent) {
        // Ignore
    }

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        mAllPackagesAdapter.setEntries(entries);
        mEntryMap.clear();
        for (ApplicationsState.AppEntry e : entries) {
            mEntryMap.put(e.info.packageName, e);
        }

        mAllPackagesAdapter.notifyDataSetInvalidated();

        if (mExpandedDesktopState != STATE_USER_CONFIGURABLE) {
            hideListView();
        }
    }

    private void rebuild() {
        ArrayList<ApplicationsState.AppEntry> newEntries = mSession.rebuild(
                mActivityFilter, ApplicationsState.ALPHA_COMPARATOR);
        if (newEntries != null) {
            handleAppEntries(newEntries);
        }
    }

    public int getStateDrawable(int state) {
        switch (state) {
            case STATE_STATUS_HIDDEN:
                return R.drawable.ic_settings_extdesk_hidestatusbar;
            case STATE_NAVIGATION_HIDDEN:
                return R.drawable.ic_settings_extdesk_hidenavbar;
            case STATE_BOTH_HIDDEN:
                return R.drawable.ic_settings_extdesk_hideboth;
            case STATE_DISABLED:
            default:
                return R.drawable.ic_settings_extdesk_hidenone;
        }
    }

    private class AllPackagesAdapter extends BaseAdapter
            implements AdapterView.OnItemSelectedListener {

        private final LayoutInflater inflater;
        private List<ApplicationsState.AppEntry> entries = new ArrayList<>();
        private final PackageManager mPackageManager;
        private final ModeAdapter mModesAdapter;

        public AllPackagesAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
            mPackageManager = context.getPackageManager();
            mModesAdapter = new ModeAdapter(context);
            mActivityFilter = new ActivityFilter(context.getPackageManager());
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
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return entries.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder(inflater.inflate(R.layout.expanded_item, parent, false));
                holder.mode.setAdapter(mModesAdapter);
                holder.mode.setOnItemSelectedListener(this);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApplicationsState.AppEntry entry = entries.get(position);

            if (entry == null) {
                return holder.rootView;
            }

            holder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);
            holder.mode.setSelection(getStateForPackage(entry.info.packageName), false);
            holder.mode.setTag(entry);
            holder.stateIcon.setImageResource(getStateDrawable(
                    getStateForPackage(entry.info.packageName)));
            return holder.rootView;
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            this.entries = entries;
            if (mUserListView != null && mUserListView.getEmptyView() != mEmptyView) {
                mUserListView.setEmptyView(mEmptyView);
            }
            notifyDataSetChanged();
        }

        private void setEntriesEnabled(ArrayList<String> newEntries) {
            for (int i = 0; i < newEntries.size(); i++) {
                String packageName = newEntries.get(i);
                ApplicationsState.AppEntry entry = mEntryMap.get(packageName);
                if (entry == null) {
                    try {
                        ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
                        if (info != null) {
                            entry = new ApplicationsState.AppEntry(getActivity(), info, i);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // Do nothing
                    }

                    if (this.entries.contains(entry)) {
                        // update
                        WindowManagerPolicyControl.removeFromWhiteLists(entry.info.packageName);
                        WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public ArrayList<String> getEntriesAsStringList() {
            ArrayList<String> stringEntries = new ArrayList<String>();
            for (ApplicationsState.AppEntry entry : entries) {
                stringEntries.add(entry.getNormalizedLabel());
            }
            return stringEntries;
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
            String key = mExpandedDesktopState == STATE_USER_CONFIGURABLE ?
                    Settings.Global.POLICY_CONTROL : Settings.Global.POLICY_CONTROL_SELECTED;
            WindowManagerPolicyControl.saveToSettings(parent.getContext(), key);
            notifyDataSetChanged();
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
        private ImageView stateIcon;

        private ViewHolder(View view) {
            this.title = (TextView) view.findViewById(R.id.app_name);
            this.mode = (Spinner) view.findViewById(R.id.app_mode);
            this.icon = (ImageView) view.findViewById(R.id.app_icon);
            this.stateIcon = (ImageView) view.findViewById(R.id.state);
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

    private class ActivityFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> launcherResolveInfoList = new ArrayList<String>();
        private boolean onlyLauncher = true;

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(i, 0);

            synchronized (launcherResolveInfoList) {
                launcherResolveInfoList.clear();
                for (ResolveInfo ri : resolveInfoList) {
                    launcherResolveInfoList.add(ri.activityInfo.packageName);
                }
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            boolean show = !mAllPackagesAdapter.entries.contains(info.packageName);
            if (show && onlyLauncher) {
                synchronized (launcherResolveInfoList) {
                    show = launcherResolveInfoList.contains(info.packageName);
                }
            }
            return show;
        }
    }
}