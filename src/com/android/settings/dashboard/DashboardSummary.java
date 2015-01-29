/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.Lte4GEnabler;
import com.android.settings.SettingsActivity;

import java.util.List;

public class DashboardSummary extends Fragment {
    private static final String LOG_TAG = "DashboardSummary";

    private LayoutInflater mLayoutInflater;
    private ViewGroup mDashboard;

    private Lte4GEnabler mLte4GEnabler;

    private static final int MSG_REBUILD_UI = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = getActivity();
                    rebuildUI(context);
                } break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)
                    || Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                Log.d(LOG_TAG, "Received ACTION_SIM_STATE_CHANGED or ACTION_AIRPLANE_MODE_CHANGED");
                sendRebuildUI();
            }
        }
    };

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rebuildUI(context);
        }
    }
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    @Override
    public void onResume() {
        super.onResume();

        mLte4GEnabler.resume();

        sendRebuildUI();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);

        // Register for intent broadcasts
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        mLte4GEnabler.pause();

        getActivity().unregisterReceiver(mHomePackageReceiver);
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayoutInflater = inflater;
        mLte4GEnabler = new Lte4GEnabler(getActivity(), new Switch(getActivity()));
        final View rootView = inflater.inflate(R.layout.dashboard, container, false);
        mDashboard = (ViewGroup) rootView.findViewById(R.id.dashboard_container);

        return rootView;
    }

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(LOG_TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        long start = System.currentTimeMillis();
        final Resources res = getResources();

        mDashboard.removeAllViews();

        List<DashboardCategory> categories =
                ((SettingsActivity) context).getDashboardCategories(true);

        final int count = categories.size();

        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);

            View categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboard,
                    false);

            TextView categoryLabel = (TextView) categoryView.findViewById(R.id.category_title);
            categoryLabel.setText(category.getTitle(res));

            ViewGroup categoryContent =
                    (ViewGroup) categoryView.findViewById(R.id.category_content);

            final int tilesCount = category.getTilesCount();
            for (int i = 0; i < tilesCount; i++) {
                DashboardTile tile = category.getTile(i);
                DashboardTileView tileView;
                if (tile.getTitle(res).equals(res.getString(R.string.lte_4g_settings_title))) {
                    tileView = new DashboardTileView(context,true);
                    mLte4GEnabler.setSwitch(tileView.getSwitch());

                    int simState = TelephonyManager.getDefault().getSimState(PhoneConstants.SUB1);
                    boolean enabled = (Settings.System.getInt(context.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON, 0) == 0)
                            && (simState == TelephonyManager.SIM_STATE_READY);
                    tileView.setEnabled(enabled);
                    tileView.getTitleTextView().setEnabled(enabled);
                    // update icons
                    if (enabled) {
                        tile.iconRes = R.drawable.ic_settings_4g;
                    } else {
                        tile.iconRes = R.drawable.ic_settings_4g_dis;
                    }

                    updateTileView(context, res, tile, tileView.getImageView(),
                            tileView.getTitleTextView(), tileView.getSwitch());

                } else {
                    tileView = new DashboardTileView(context,false);
                    updateTileView(context, res, tile, tileView.getImageView(),
                            tileView.getTitleTextView(), tileView.getStatusTextView());
                }

                tileView.setTile(tile);

                categoryContent.addView(tileView);
            }

            // Add the category
            mDashboard.addView(categoryView);
        }
        long delta = System.currentTimeMillis() - start;
        Log.d(LOG_TAG, "rebuildUI took: " + delta + " ms");
    }

    private void updateTileView(Context context, Resources res, DashboardTile tile,
            ImageView tileIcon, TextView tileTextView, TextView statusTextView) {

        if (tile.iconRes > 0) {
            tileIcon.setImageResource(tile.iconRes);
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
        }

        tileTextView.setText(tile.getTitle(res));

        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText(summary);
        } else {
            statusTextView.setVisibility(View.GONE);
        }
    }

    private void updateTileView(Context context, Resources res, DashboardTile tile,
        ImageView tileIcon, TextView tileTextView, Switch mSwitch) {

        if (tile.iconRes > 0) {
            tileIcon.setImageResource(tile.iconRes);
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
        }

        tileTextView.setText(tile.getTitle(res));
    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }
}
