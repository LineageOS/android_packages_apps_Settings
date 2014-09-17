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
package com.android.settings.profiles;

import android.app.Fragment;
import android.app.Profile;
import android.app.ProfileManager;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.profiles.triggers.NfcTriggerFragment;

public class SetupTriggersFragment extends Fragment {

    ViewPager mPager;
    Profile mProfile;
    ProfileManager mProfileManager;
    TriggerPagerAdapter mAdapter;
    boolean mNewProfileMode;

    public static SetupTriggersFragment newInstance(Profile profile, boolean newProfile) {
        SetupTriggersFragment fragment = new SetupTriggersFragment();
        Bundle args = new Bundle();
        args.putParcelable("profile", profile);
        args.putBoolean(ProfileActivity.EXTRA_NEW_PROFILE, newProfile);

        fragment.setArguments(args);
        return fragment;
    }

    public SetupTriggersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable("profile");
            mNewProfileMode = getArguments().getBoolean(ProfileActivity.EXTRA_NEW_PROFILE, true);
        }
        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setTitle(mNewProfileMode
                ? R.string.profile_setup_setup_triggers_title
                : R.string.profile_setup_setup_triggers_title_config);
    }

    ViewGroup mContainer;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContainer = container;
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_setup_triggers, container, false);

        ViewPager pager = (ViewPager) root.findViewById(R.id.view_pager);
        mAdapter = new TriggerPagerAdapter(getActivity(), getChildFragmentManager());
        final TriggerPagerAdapter.TriggerFragments[] mFragments = TriggerPagerAdapter.TriggerFragments.values();

        Bundle profileArgs = new Bundle();
        profileArgs.putParcelable("profile", mProfile);
        for (final TriggerPagerAdapter.TriggerFragments mFragment : mFragments) {
            mAdapter.add(mFragment.getFragmentClass(), profileArgs);
        }
        pager.setAdapter(mAdapter);

        PagerTabStrip tabs = (PagerTabStrip) root.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(android.R.color.holo_blue_light);

        Button nextButton = (Button) root.findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, SetupActionsFragment.newInstance(mProfile, mNewProfileMode))
                        .addToBackStack(null)
                        .commit();
            }
        });

        // back button
        root.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContainer = null;
    }

    public void onNfcIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            if (mAdapter != null && mContainer != null) {
                ((NfcTriggerFragment) mAdapter.getFragment(2)).onNfcIntent(intent);
            }
        }
    }


}
