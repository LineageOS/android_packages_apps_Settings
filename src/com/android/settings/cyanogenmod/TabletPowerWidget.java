/*
 * Copyright (C) 2011 The CyanogenMod Project
 * This code has been modified. Portions copyright (C) 2012 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class TabletPowerWidget extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "TabletPowerWidget";

    private CheckBoxPreference[] mToggles = new CheckBoxPreference[TabletPowerWidgetUtil.KEY_TOGGLES.length];

    private static Context mContext;
    private static boolean mValue;
    private static String mPowerWidgets;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
                addPreferencesFromResource(R.xml.tablet_power_widget);
                mContext = getActivity();
                refreshToggles();
        }
    }


    private void refreshToggles(){
        mPowerWidgets = Settings.System.getString(mContext.getContentResolver(), Settings.System.WIDGET_BUTTONS_TABLET);
        PreferenceScreen prefSet = getPreferenceScreen();
        if(mPowerWidgets == null)
                mPowerWidgets = TabletPowerWidgetUtil.BUTTONS_DEFAULT;
        for(int i=0; i<mToggles.length; i++){
                mToggles[i] = (CheckBoxPreference) prefSet.findPreference(TabletPowerWidgetUtil.KEY_TOGGLES[i]);
                mToggles[i].setChecked(mPowerWidgets.contains(TabletPowerWidgetUtil.KEY_TOGGLES[i]));
        }
    }

    private void setWidgetButtons(){
        if(mPowerWidgets == null)
                mPowerWidgets = TabletPowerWidgetUtil.BUTTONS_DEFAULT;
        if(mPowerWidgets.equals(TabletPowerWidgetUtil.NO_TOGGLES))
                Settings.System.putString(mContext.getContentResolver(), Settings.System.WIDGET_BUTTONS_TABLET, mPowerWidgets);
        else{
                mPowerWidgets = mPowerWidgets.substring(0, mPowerWidgets.lastIndexOf("\\|"));
                Settings.System.putString(mContext.getContentResolver(), Settings.System.WIDGET_BUTTONS_TABLET, mPowerWidgets);
       }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        mPowerWidgets = "";
        for(int i=0; i<mToggles.length; i++){
               if(mToggles[i].isChecked())
                      mPowerWidgets += TabletPowerWidgetUtil.KEY_TOGGLES[i] + TabletPowerWidgetUtil.BUTTON_DELIMITER;
        }
        if(mPowerWidgets.equals(""))
               mPowerWidgets = TabletPowerWidgetUtil.NO_TOGGLES;
        setWidgetButtons();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    public static class PowerWidgetOrder extends ListFragment
    {
        private static final String TAG = "PowerWidgetOrderActivity";

        private ListView mButtonList;
        private ButtonAdapter mButtonAdapter;
        View mContentView = null;
        Context mContext;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mContentView = inflater.inflate(R.layout.order_power_widget_buttons_activity, null);
            return mContentView;
        }

        /** Called when the activity is first created. */
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mContext = getActivity().getApplicationContext();

            mButtonList = getListView();
            ((TouchInterceptor) mButtonList).setDropListener(mDropListener);
            mButtonAdapter = new ButtonAdapter(mContext);
            setListAdapter(mButtonAdapter);
        }

        @Override
        public void onDestroy() {
            ((TouchInterceptor) mButtonList).setDropListener(null);
            setListAdapter(null);
            super.onDestroy();
        }

        @Override
        public void onResume() {
            super.onResume();
            // reload our buttons and invalidate the views for redraw
            mButtonAdapter.reloadButtons();
            mButtonList.invalidateViews();
        }

        private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
            public void drop(int from, int to) {
                // get the current button list
                ArrayList<String> buttons = TabletPowerWidgetUtil.getButtonListFromString(
                        TabletPowerWidgetUtil.getCurrentButtons(mContext));

                // move the button
                if (from < buttons.size()) {
                    String button = buttons.remove(from);

                    if (to <= buttons.size()) {
                        buttons.add(to, button);

                        // save our buttons
                        TabletPowerWidgetUtil.saveCurrentButtons(mContext,
                                TabletPowerWidgetUtil.getButtonStringFromList(buttons));

                        // tell our adapter/listview to reload
                        mButtonAdapter.reloadButtons();
                        mButtonList.invalidateViews();
                    }
                }
            }
        };

        private class ButtonAdapter extends BaseAdapter {
            private Context mContext;
            private Resources mSystemUIResources = null;
            private LayoutInflater mInflater;
            private ArrayList<TabletPowerWidgetUtil.ButtonInfo> mButtons;

            public ButtonAdapter(Context c) {
                mContext = c;
                mInflater = LayoutInflater.from(mContext);

                PackageManager pm = mContext.getPackageManager();
                if (pm != null) {
                    try {
                        mSystemUIResources = pm.getResourcesForApplication("com.android.systemui");
                    } catch (Exception e) {
                        mSystemUIResources = null;
                        Log.e(TAG, "Could not load SystemUI resources", e);
                    }
                }

                reloadButtons();
            }

            public void reloadButtons() {
                ArrayList<String> buttons = TabletPowerWidgetUtil.getButtonListFromString(
                        TabletPowerWidgetUtil.getCurrentButtons(mContext));

                mButtons = new ArrayList<TabletPowerWidgetUtil.ButtonInfo>();
                for (String button : buttons) {
                    if (TabletPowerWidgetUtil.BUTTONS.containsKey(button)) {
                        mButtons.add(TabletPowerWidgetUtil.BUTTONS.get(button));
                    }
                }
            }

            public int getCount() {
                return mButtons.size();
            }

            public Object getItem(int position) {
                return mButtons.get(position);
            }

            public long getItemId(int position) {
                return position;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final View v;
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.order_power_widget_button_list_item, null);
                } else {
                    v = convertView;
                }

                TabletPowerWidgetUtil.ButtonInfo button = mButtons.get(position);

                final TextView name = (TextView) v.findViewById(R.id.name);
                final ImageView icon = (ImageView) v.findViewById(R.id.icon);

                name.setText(button.getTitleResId());

                // assume no icon first
                icon.setVisibility(View.GONE);

                // attempt to load the icon for this button
                if (mSystemUIResources != null) {
                    int resId = mSystemUIResources.getIdentifier(button.getIcon(), null, null);
                    if (resId > 0) {
                        try {
                            Drawable d = mSystemUIResources.getDrawable(resId);
                            icon.setVisibility(View.VISIBLE);
                            icon.setImageDrawable(d);
                        } catch (Exception e) {
                            Log.e(TAG, "Error retrieving icon drawable", e);
                        }
                    }
                }

                return v;
            }
        }
    }

}
