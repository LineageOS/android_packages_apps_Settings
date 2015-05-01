/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.android.settings.profiles.triggers;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.profiles.ProfilesSettings;
import cyanogenmod.app.profiles.ProfilePluginManager;
import cyanogenmod.app.profiles.Trigger;

import java.util.ArrayList;
import java.util.List;

public class AppTriggersFragment extends ListFragment {
    Profile mProfile;
    private ProfileManager mProfileManager;
    private ProfilePluginManager mProfilePluginManager;

    private View mEmptyView;

    private List<AppTrigger> mTriggers = new ArrayList<AppTrigger>();
    private AppTriggerAdapter mListAdapter;

    public static AppTriggersFragment newInstance(Profile profile) {
        AppTriggersFragment fragment = new AppTriggersFragment();

        Bundle extras = new Bundle();
        extras.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);

        fragment.setArguments(extras);
        return fragment;
    }

    public AppTriggersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        } else {
            throw new UnsupportedOperationException("no profile!");
        }
        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
        mProfilePluginManager = ProfilePluginManager.getInstance(getActivity());
    }


    @Override
    public void onStart() {
        super.onStart();
        getListView().setEmptyView(mEmptyView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO add empty Trigger view
        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
//        view.addView(mEmptyView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadTriggerListItems();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        final String triggerId;
        final String triggerPackage;
        int currentItemIdx = -1;

        AppTrigger pref = (AppTrigger) l.getAdapter().getItem(position);
        triggerId = pref.getTitle();
        triggerPackage = pref.getTriggerPackage();

        String currentItem = pref.getCurrentTriggerState();

        final String[] values = (String[]) pref.getTriggerStates().toArray();
        for (int i = 0; i < values.length; i++) {
            if (currentItem.equals(values[i])) {
                currentItemIdx = i;
                break;
            }
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.profile_trigger_configure)
                .setSingleChoiceItems(values, currentItemIdx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProfile.setTrigger(triggerPackage, triggerId, values[which]);
                        mProfileManager.updateProfile(mProfile);
                        reloadTriggerListItems();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        reloadTriggerListItems();
        mListAdapter = new AppTriggerAdapter(getActivity());
        setListAdapter(mListAdapter);
    }

    private void reloadTriggerListItems() {
        mTriggers.clear();

        List<Trigger> registeredTriggers = mProfilePluginManager.getRegisteredTriggers();
        for (Trigger trigger : registeredTriggers) {
            AppTrigger appTrigger = new AppTrigger(trigger);
            appTrigger.setIcon(getPackageIcon(appTrigger.getTriggerPackage()));
            mTriggers.add(appTrigger);
        }

        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    private Drawable getPackageIcon(String packageName) {
        try {
            return getActivity().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class AppTriggerAdapter extends ArrayAdapter<AppTrigger> {
        public AppTriggerAdapter(Context context) {
            super(context, R.layout.abstract_trigger_row, R.id.title, mTriggers);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View rowView = inflater.inflate(R.layout.abstract_trigger_row, viewGroup, false);
            TextView title = (TextView) rowView.findViewById(R.id.title);
            TextView desc = (TextView) rowView.findViewById(R.id.desc);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            AppTrigger trigger = getItem(i);

            title.setText(trigger.getTitle());
            desc.setText(trigger.getSummary());
            imageView.setImageDrawable(trigger.getIconDrawable());

            return rowView;
        }
    }

    public static class AppTrigger extends AbstractTriggerItem {
        Trigger mTriggerInfo;

        public AppTrigger(Trigger triggerInfo) {
            this.mTriggerInfo = triggerInfo;
        }

        @Override
        public String getTitle() {
            return mTriggerInfo.getPackage();
        }

        @Override
        public String getSummary() {
            return mTriggerInfo.getCurrentState();
        }

        public String getTriggerId() {
            return mTriggerInfo.getTriggerId();
        }

        public String getTriggerPackage() {
            return mTriggerInfo.getPackage();
        }

        public String getCurrentTriggerState() {
            return mTriggerInfo.getCurrentState();
        }

        public List<String> getTriggerStates() {
            Bundle states = mTriggerInfo.getStates();
            List<String> stateList = new ArrayList<>();
            stateList.addAll(states.keySet());
            return stateList;
        }
    }

}
