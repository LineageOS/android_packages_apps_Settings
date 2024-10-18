/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import static android.net.NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.NetworkPolicyManager;
import android.util.SparseIntArray;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DataSaverBackend {

    private static final String TAG = "DataSaverBackend";

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private final NetworkPolicyManager mPolicyManager;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private SparseIntArray mUidPolicies = new SparseIntArray();
    private boolean mAllowlistInitialized;
    private boolean mDenylistInitialized;

    // TODO: Staticize into only one.
    public DataSaverBackend(@NotNull Context context) {
        // TODO(b/246537614):Use fragment context to DataSaverBackend class will caused memory leak
        mContext = context.getApplicationContext();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mPolicyManager = NetworkPolicyManager.from(mContext);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
        if (mListeners.size() == 1) {
            mPolicyManager.registerListener(mPolicyListener);
        }
        listener.onDataSaverChanged(isDataSaverEnabled());
    }

    public void remListener(Listener listener) {
        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            mPolicyManager.unregisterListener(mPolicyListener);
        }
    }

    public boolean isDataSaverEnabled() {
        return mPolicyManager.getRestrictBackground();
    }

    public void setDataSaverEnabled(boolean enabled) {
        mPolicyManager.setRestrictBackground(enabled);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_DATA_SAVER_MODE, enabled ? 1 : 0);
    }

    public void refreshAllowlist() {
        loadAllowlist();
    }

    public void setIsAllowlisted(int uid, String packageName, boolean allowlisted) {
        final int policy = allowlisted ? POLICY_ALLOW_METERED_BACKGROUND : POLICY_NONE;
        mUidPolicies.put(uid, policy);
        if (allowlisted) {
            mPolicyManager.addUidPolicy(uid, POLICY_ALLOW_METERED_BACKGROUND);
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_DATA_SAVER_WHITELIST, packageName);
        } else {
            mPolicyManager.removeUidPolicy(uid, POLICY_ALLOW_METERED_BACKGROUND);
        }
        mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
    }

    public boolean isAllowlisted(int uid) {
        loadAllowlist();
        return mUidPolicies.get(uid, POLICY_NONE) == POLICY_ALLOW_METERED_BACKGROUND;
    }

    private void loadAllowlist() {
        if (mAllowlistInitialized) {
            return;
        }

        for (int uid : mPolicyManager.getUidsWithPolicy(POLICY_ALLOW_METERED_BACKGROUND)) {
            mUidPolicies.put(uid, POLICY_ALLOW_METERED_BACKGROUND);
        }
        mAllowlistInitialized = true;
    }

    public void refreshDenylist() {
        loadDenylist();
    }

    public void setIsDenylisted(int uid, String packageName, boolean denylisted) {
        final int policy = denylisted ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE;
        mUidPolicies.put(uid, policy);
        if (denylisted) {
            mPolicyManager.addUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
            mMetricsFeatureProvider.action(
                    mContext, SettingsEnums.ACTION_DATA_SAVER_BLACKLIST, packageName);
        } else {
            mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
        }
        mPolicyManager.removeUidPolicy(uid, POLICY_ALLOW_METERED_BACKGROUND);
    }

    public boolean isDenylisted(int uid) {
        loadDenylist();
        return mUidPolicies.get(uid, POLICY_NONE) == POLICY_REJECT_METERED_BACKGROUND;
    }

    private void loadDenylist() {
        if (mDenylistInitialized) {
            return;
        }
        for (int uid : mPolicyManager.getUidsWithPolicy(POLICY_REJECT_METERED_BACKGROUND)) {
            mUidPolicies.put(uid, POLICY_REJECT_METERED_BACKGROUND);
        }
        mDenylistInitialized = true;
    }

    private void handleRestrictBackgroundChanged(boolean isDataSaving) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onDataSaverChanged(isDataSaving);
        }
    }

    private void handleAllowlistChanged(int uid, boolean isAllowlisted) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onAllowlistStatusChanged(uid, isAllowlisted);
        }
    }

    private void handleDenylistChanged(int uid, boolean isDenylisted) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onDenylistStatusChanged(uid, isDenylisted);
        }
    }

    private void handleUidPoliciesChanged(int uid, int newPolicy) {
        loadAllowlist();
        loadDenylist();

        // We only care about allow/reject metered background policy here.
        newPolicy &= POLICY_ALLOW_METERED_BACKGROUND | POLICY_REJECT_METERED_BACKGROUND;

        final int oldPolicy = mUidPolicies.get(uid, POLICY_NONE);
        if (newPolicy == POLICY_NONE) {
            mUidPolicies.delete(uid);
        } else {
            mUidPolicies.put(uid, newPolicy);
        }

        final boolean wasAllowlisted = oldPolicy == POLICY_ALLOW_METERED_BACKGROUND;
        final boolean wasDenylisted = oldPolicy == POLICY_REJECT_METERED_BACKGROUND;
        final boolean isAllowlisted = newPolicy == POLICY_ALLOW_METERED_BACKGROUND;
        final boolean isDenylisted = newPolicy == POLICY_REJECT_METERED_BACKGROUND;

        if (wasAllowlisted != isAllowlisted) {
            handleAllowlistChanged(uid, isAllowlisted);
        }

        if (wasDenylisted != isDenylisted) {
            handleDenylistChanged(uid, isDenylisted);
        }

    }

    private final NetworkPolicyManager.Listener mPolicyListener =
            new NetworkPolicyManager.Listener() {
        @Override
        public void onUidPoliciesChanged(final int uid, final int uidPolicies) {
            ThreadUtils.postOnMainThread(() -> handleUidPoliciesChanged(uid, uidPolicies));
        }

        @Override
        public void onRestrictBackgroundChanged(final boolean isDataSaving) {
            ThreadUtils.postOnMainThread(() -> handleRestrictBackgroundChanged(isDataSaving));
        }
    };

    public interface Listener {
        void onDataSaverChanged(boolean isDataSaving);

        /** This is called when allow list status is changed. */
        default void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {}

        /** This is called when deny list status is changed. */
        default void onDenylistStatusChanged(int uid, boolean isDenylisted) {}
    }
}
