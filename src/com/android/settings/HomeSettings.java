/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.List;

public class HomeSettings extends SettingsPreferenceFragment {
    static final String TAG = "HomeSettings";

    public static final String HOME_PREFS = "home_prefs";
    public static final String HOME_PREFS_DO_SHOW = "do_show";

    public static final String HOME_SHOW_NOTICE = "show";

    PreferenceGroup mPrefGroup;

    PackageManager mPm;
    ComponentName[] mHomeComponentSet;
    ArrayList<HomeAppPreference> mPrefs;
    HomeAppPreference mCurrentHome = null;
    final IntentFilter mHomeFilter;
    boolean mShowNotice;

    public HomeSettings() {
        mHomeFilter = new IntentFilter(Intent.ACTION_MAIN);
        mHomeFilter.addCategory(Intent.CATEGORY_HOME);
        mHomeFilter.addCategory(Intent.CATEGORY_DEFAULT);
    }

    OnClickListener mHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            HomeAppPreference pref = mPrefs.get(index);
            if (!pref.isChecked) {
                makeCurrentHome(pref);
            }
        }
    };

    OnClickListener mPreferencesClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            startActivity(mPrefs.get(index).prefsIntent);
        }
    };

    void makeCurrentHome(HomeAppPreference newHome) {
        if (mCurrentHome != null) {
            mCurrentHome.setChecked(false);
        }
        newHome.setChecked(true);
        mCurrentHome = newHome;

        mPm.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                mHomeComponentSet, newHome.activityName);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Rebuild the list now that we might have nuked something
        buildHomeActivitiesList();
    }

    void buildHomeActivitiesList() {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        ComponentName currentDefaultHome  = mPm.getHomeActivities(homeActivities);

        Context context = getActivity();
        mCurrentHome = null;
        mPrefGroup.removeAll();
        mPrefs = new ArrayList<HomeAppPreference>();
        mHomeComponentSet = new ComponentName[homeActivities.size()];
        int prefIndex = 0;
        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            Intent resolvedPrefsIntent = null;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mHomeComponentSet[i] = activityName;

            Intent prefsIntent = new Intent(Intent.ACTION_MAIN);
            prefsIntent.addCategory("com.cyanogenmod.category.LAUNCHER_PREFERENCES");
            prefsIntent.setPackage(info.packageName);

            if (prefsIntent.resolveActivity(mPm) != null) {
                resolvedPrefsIntent = prefsIntent;
                resolvedPrefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            }

            try {
                Drawable icon = info.loadIcon(mPm);
                CharSequence name = info.loadLabel(mPm);
                HomeAppPreference pref = new HomeAppPreference(context, activityName, prefIndex,
                        icon, name, this, info, resolvedPrefsIntent);
                mPrefs.add(pref);
                mPrefGroup.addPreference(pref);
                pref.setEnabled(true);
                if (activityName.equals(currentDefaultHome)) {
                    mCurrentHome = pref;
                }
                prefIndex++;
            } catch (Exception e) {
                Log.v(TAG, "Problem dealing with activity " + activityName, e);
            }
        }

        if (mCurrentHome != null) {
            new Handler().post(new Runnable() {
               public void run() {
                   mCurrentHome.setChecked(true);
               }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.home_selection);

        mPm = getPackageManager();
        mPrefGroup = (PreferenceGroup) findPreference("home");

        Bundle args = getArguments();
        mShowNotice = (args != null) && args.getBoolean(HOME_SHOW_NOTICE, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        buildHomeActivitiesList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        HomeAppPreference selectedPref = null;

        for (HomeAppPreference pref : mPrefs) {
            if (pref.isChecked) {
                selectedPref = pref;
                break;
            }
        }

        super.onCreateOptionsMenu(menu, inflater);

        if (selectedPref != null && selectedPref.prefsIntent != null) {
            menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.settings_label)
                    .setIntent(selectedPref.prefsIntent)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    class HomeAppPreference extends Preference {
        ComponentName activityName;
        int index;
        HomeSettings fragment;
        final ColorFilter grayscaleFilter;
        boolean isChecked;
        final Intent prefsIntent;

        public HomeAppPreference(Context context, ComponentName activity,
                int i, Drawable icon, CharSequence title,
                HomeSettings parent, ActivityInfo info, Intent prefsIntent) {
            super(context);
            setLayoutResource(R.layout.preference_home_app);
            setIcon(icon);
            setTitle(title);
            activityName = activity;
            fragment = parent;
            index = i;
            this.prefsIntent = prefsIntent;

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            RadioButton radio = (RadioButton) view.findViewById(R.id.home_radio);
            radio.setChecked(isChecked);

            Integer indexObj = new Integer(index);

            ImageView icon = (ImageView) view.findViewById(R.id.home_app_preferences);
            if (prefsIntent == null) {
                icon.setEnabled(false);
                icon.setColorFilter(grayscaleFilter);
            } else {
                icon.setOnClickListener(mPreferencesClickListener);
                icon.setTag(indexObj);
            }

            View v = view.findViewById(R.id.home_app_pref);
            v.setOnClickListener(mHomeClickListener);
            v.setTag(indexObj);
        }

        void setChecked(boolean state) {
            if (state != isChecked) {
                isChecked = state;
                notifyChanged();
                getActivity().invalidateOptionsMenu();
            }
        }
    }
}
