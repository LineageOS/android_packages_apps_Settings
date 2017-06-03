/*Copyright (C) 2017 AIM-ROM
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at
          http://www.apache.org/licenses/LICENSE-2.0
     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.aim;


import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.aim.tabs.StatusBar;
import com.android.settings.aim.tabs.Ui;
import com.android.settings.aim.tabs.Buttons;
import com.android.settings.aim.tabs.Recents;
import com.android.settings.aim.tabs.Misc;

import com.android.settings.aim.PagerSlidingTabStrip;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainSettings extends SettingsPreferenceFragment {

    ViewPager mViewPager;
    String titleString[];
    ViewGroup mContainer;
    PagerSlidingTabStrip mTabs;

    static Bundle mSavedState;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        View view = inflater.inflate(R.layout.amify, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
	      mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);

        StatusBarAdapter StatusBarAdapter = new StatusBarAdapter(getFragmentManager());
        mViewPager.setAdapter(StatusBarAdapter);

	mTabs.setViewPager(mViewPager);
        setHasOptionsMenu(true);
        return view;
    }

   @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
    }

    @Override
    public void onResume() {
        super.onResume();
         mContainer.setPadding(30, 30, 30, 30);
    }

    class StatusBarAdapter extends FragmentPagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public StatusBarAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new StatusBar();
            frags[1] = new Ui();
            frags[2] = new Buttons();
            frags[3] = new Recents();
            frags[4] = new Misc();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    private String[] getTitles() {
        String titleString[];
        titleString = new String[]{
            getString(R.string.aim_statusbar_title),
            getString(R.string.aim_ui_title),
            getString(R.string.aim_buttons_title),
            getString(R.string.recent_settings_title)'
            getString(R.string.aim_misc_title)};
        return titleString;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.AIM;
    }
}
