/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationGroup;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TabHost;

import com.android.settings.R;
import com.android.settings.Utils;

public class ProfilesSettings extends Fragment {

    private static final String TAG = "ProfilesSettings";

    private static final String TAB_PROFILES = "profiles";

    private static final String TAB_APPGROUPS = "appgroups";

    private static final String PROFILE_SERVICE = "profile";

    private static final int MENU_RESET = Menu.FIRST;

    private static final int MENU_ADD_PROFILE = Menu.FIRST + 1;

    private static final int MENU_ADD_APPGROUP = Menu.FIRST + 2;

    private static final int PROFILE_DETAILS = 1;

    private static Menu mOptionsMenu;

    private Profile mProfile;

    private ProfileManager mProfileManager;

    private static TabHost mTabHost;
    
    ViewGroup mContainer;

    TabManager mTabManager;

    static Bundle mSavedState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mContainer = container;
        mTabHost = (TabHost) inflater.inflate(R.layout.profile_tabs, container, false);
        if (mTabHost != null) {
            mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE); 

            setupTabs();

            // If we are resuming from a paused state, restore the last active tab
            if (mSavedState != null) {
                mTabHost.setCurrentTabByTag(mSavedState.getString("tab"));
            }

            setHasOptionsMenu(true);
        }

        return mTabHost;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If running on a phone, remove padding around tabs
        if (!Utils.isScreenLarge()) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    
    @Override
    public void onPause() {
        super.onPause();
        // store the current tab so we can get back to it later
        if (mSavedState == null) {
            mSavedState = new Bundle();
        }
        mSavedState.putString("tab", mTabHost.getCurrentTabTag());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mOptionsMenu = menu;

        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setIcon(R.drawable.ic_menu_refresh_holo_dark)
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_ADD_PROFILE, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add)
                .setAlphabeticShortcut('a')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_ADD_APPGROUP, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add)
                .setAlphabeticShortcut('a')
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        updateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    @Override
    public void onDestroyOptionsMenu() {
        mOptionsMenu = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetAll();
                return true;

            case MENU_ADD_PROFILE:
                addProfile();
                return true;

            case MENU_ADD_APPGROUP:
                addAppGroup();
                return true;

            default:
                return false;
        }
    }

    static void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }

        String tabId = mTabHost.getCurrentTabTag();
        if (TAB_PROFILES.equals(tabId)) {
            mOptionsMenu.findItem(MENU_ADD_PROFILE).setVisible(true);
            mOptionsMenu.findItem(MENU_ADD_APPGROUP).setVisible(false);

        } else if (TAB_APPGROUPS.equals(tabId)) {
            mOptionsMenu.findItem(MENU_ADD_PROFILE).setVisible(false);
            mOptionsMenu.findItem(MENU_ADD_APPGROUP).setVisible(true);
        }
    }

    private void setupTabs() {
        mTabHost.setup();
        mTabHost.clearAllTabs();

        mTabManager = new TabManager(getActivity(), mTabHost, android.R.id.tabcontent);
        mTabManager.addTab(mTabHost.newTabSpec(TAB_PROFILES).setIndicator(getString(R.string.profile_profiles_manage)),
                ProfilesList.class, null);
        mTabManager.addTab(mTabHost.newTabSpec(TAB_APPGROUPS).setIndicator(getString(R.string.profile_appgroups_manage)),
                AppGroupList.class, null);

        // Set the profiles tab as the default
        mTabHost.setCurrentTabByTag(TAB_PROFILES);
        updateOptionsMenu();
    }

    private void addProfile() {
        mProfile = new Profile(getString(R.string.new_profile_name));
        mProfileManager.addProfile(mProfile);

        // Start the profile details preference screen
        Bundle args = new Bundle();
        args.putParcelable("Profile", mProfile);
        PreferenceActivity pa = (PreferenceActivity) getActivity();
        pa.startPreferencePanel(ProfileConfig.class.getName(), args,
                R.string.profile_profile_manage, null, this, PROFILE_DETAILS);
    }

    private void resetAll() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.profile_reset_title);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setMessage(R.string.profile_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mProfileManager.resetAll();
                mTabManager.refreshTab(mTabHost.getCurrentTabTag());
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        alert.create().show();
    }

    private void addAppGroup() {
        Context context = getActivity();
        if (context != null) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            final EditText entry = new EditText(context);
            entry.setPadding(10, 10, 10, 10);
            dialog.setMessage(R.string.profile_appgroup_name_prompt);
            dialog.setView(entry);
            dialog.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NotificationGroup newGroup = new NotificationGroup(entry.getText().toString());
                            mProfileManager.addNotificationGroup(newGroup);
                            mTabManager.refreshTab(TAB_APPGROUPS);
                        }
                    });
            dialog.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            dialog.create().show();
        }
    }

    /**
     * This is a helper class that implements a generic mechanism for
     * associating fragments with the tabs in a tab host.
     */
    public static class TabManager implements TabHost.OnTabChangeListener {
        private final Activity mActivity;
        private final TabHost mTabHost;
        private final int mContainerId;
        private final HashMap<String, TabInfo> mTabs = new HashMap<String, TabInfo>();
        TabInfo mLastTab;

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabManager(Activity activity, TabHost tabHost, int containerId) {
            mActivity = activity;
            mTabHost = tabHost;
            mContainerId = containerId;
            mTabHost.setOnTabChangedListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mActivity));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            info.fragment = mActivity.getFragmentManager().findFragmentByTag(tag);
            if (info.fragment != null && !info.fragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(info.fragment);
                ft.commit();
            }

            mTabs.put(tag, info);
            mTabHost.addTab(tabSpec);
        }

        @Override
        public void onTabChanged(String tabId) {
            TabInfo newTab = mTabs.get(tabId);
            if (mLastTab != newTab) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                if (mLastTab != null) {
                    if (mLastTab.fragment != null) {
                        ft.detach(mLastTab.fragment);
                    }
                }

                if (newTab != null) {
                    if (newTab.fragment == null) {
                        newTab.fragment = Fragment.instantiate(mActivity,
                                newTab.clss.getName(), newTab.args);
                        ft.add(mContainerId, newTab.fragment, newTab.tag);
                    } else {
                        ft.attach(newTab.fragment);
                    }
                }

                mLastTab = newTab;
                ft.commit();

                // Toggle the appropriate menu options
                updateOptionsMenu();
            }
        }

        public void refreshTab(String tabId) {
            TabInfo currentTab = mTabs.get(tabId);

            if (currentTab != null) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                if (currentTab.fragment != null) {
                    ft.detach(currentTab.fragment);
                }

                if (currentTab.fragment == null) {
                    currentTab.fragment = Fragment.instantiate(mActivity,
                            currentTab.clss.getName(), currentTab.args);
                    ft.add(mContainerId, currentTab.fragment, currentTab.tag);

                } else {
                    ft.attach(currentTab.fragment);
                }

                ft.commit();

                // Toggle the appropriate menu options
                updateOptionsMenu();
            }
        }

    }
}