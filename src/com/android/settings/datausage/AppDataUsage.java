/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_REJECT_ALL;
import static android.net.NetworkPolicyManager.POLICY_REJECT_CELLULAR;
import static android.net.NetworkPolicyManager.POLICY_REJECT_VPN;
import static android.net.NetworkPolicyManager.POLICY_REJECT_WIFI;

import static com.android.settings.datausage.lib.AppDataUsageRepository.getAppUid;
import static com.android.settings.datausage.lib.AppDataUsageRepository.getAppUidList;

import android.Manifest;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.datausage.lib.AppDataUsageDetailsRepository;
import com.android.settings.datausage.lib.NetworkTemplates;
import com.android.settings.fuelgauge.datasaver.DynamicDenylistManager;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.AppItem;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;

import kotlin.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AppDataUsage extends DataUsageBaseFragment implements OnPreferenceChangeListener,
        DataSaverBackend.Listener {

    private static final String TAG = "AppDataUsage";

    static final String ARG_APP_ITEM = "app_item";
    static final String ARG_NETWORK_TEMPLATE = "network_template";
    static final String ARG_NETWORK_CYCLES = "network_cycles";
    static final String ARG_SELECTED_CYCLE = "selected_cycle";

    private static final String KEY_RESTRICT_ALL = "restrict_all";
    private static final String KEY_RESTRICT_BACKGROUND = "restrict_background";
    private static final String KEY_RESTRICT_CELLULAR = "restrict_cellular";
    private static final String KEY_RESTRICT_VPN = "restrict_vpn";
    private static final String KEY_RESTRICT_WIFI = "restrict_wifi";
    private static final String KEY_UNRESTRICTED_DATA = "unrestricted_data_saver";

    private PackageManager mPackageManager;
    private final ArraySet<String> mPackages = new ArraySet<>();
    private RestrictedSwitchPreference mRestrictAll;
    private RestrictedSwitchPreference mRestrictBackground;
    private RestrictedSwitchPreference mRestrictCellular;
    private RestrictedSwitchPreference mRestrictVpn;
    private RestrictedSwitchPreference mRestrictWifi;

    private Drawable mIcon;
    @VisibleForTesting
    CharSequence mLabel;
    @VisibleForTesting
    String mPackageName;

    @VisibleForTesting
    NetworkTemplate mTemplate;
    private AppItem mAppItem;
    private RestrictedSwitchPreference mUnrestrictedData;
    private DataSaverBackend mDataSaverBackend;
    private Context mContext;
    private ArrayList<Long> mCycles;
    private long mSelectedCycle;
    private boolean mIsLoading;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mContext = getContext();
        mPackageManager = getPackageManager();
        final Bundle args = getArguments();

        mAppItem = (args != null) ? (AppItem) args.getParcelable(ARG_APP_ITEM) : null;
        mTemplate = (args != null) ? (NetworkTemplate) args.getParcelable(ARG_NETWORK_TEMPLATE)
                : null;
        mCycles = (args != null) ? (ArrayList) args.getSerializable(ARG_NETWORK_CYCLES)
            : null;
        mSelectedCycle = (args != null) ? args.getLong(ARG_SELECTED_CYCLE) : 0L;

        if (mTemplate == null) {
            mTemplate = NetworkTemplates.INSTANCE.getDefaultTemplate(mContext);
        }
        final Activity activity = requireActivity();
        activity.setTitle(NetworkTemplates.getTitleResId(mTemplate));
        if (mAppItem == null) {
            int uid = (args != null) ? args.getInt(AppInfoBase.ARG_PACKAGE_UID, -1)
                    : getActivity().getIntent().getIntExtra(AppInfoBase.ARG_PACKAGE_UID, -1);
            if (uid == -1) {
                // TODO: Log error.
                activity.finish();
            } else {
                addUid(uid);
                mAppItem = new AppItem(uid);
                mAppItem.addUid(uid);
            }
        } else {
            for (int i = 0; i < mAppItem.uids.size(); i++) {
                addUid(mAppItem.uids.keyAt(i));
            }
        }

        final List<Integer> uidList = getAppUidList(mAppItem.uids);
        initCycle(uidList);

        final UidDetailProvider uidDetailProvider = getUidDetailProvider();

        if (mAppItem.key > 0) {
            if (!UserHandle.isApp(mAppItem.key)) {
                final UidDetail uidDetail = uidDetailProvider.getUidDetail(mAppItem.key, true);
                mIcon = uidDetail.icon;
                mLabel = uidDetail.label;
                removePreference(KEY_UNRESTRICTED_DATA);
                removePreference(KEY_RESTRICT_ALL);
                removePreference(KEY_RESTRICT_BACKGROUND);
                removePreference(KEY_RESTRICT_CELLULAR);
                removePreference(KEY_RESTRICT_VPN);
                removePreference(KEY_RESTRICT_WIFI);
            } else {
                if (mPackages.size() != 0) {
                    int userId = UserHandle.getUserId(mAppItem.key);
                    try {
                        final ApplicationInfo info = mPackageManager.getApplicationInfoAsUser(
                                mPackages.valueAt(0), 0, userId);
                        mIcon = IconDrawableFactory.newInstance(getActivity()).getBadgedIcon(info);
                        mLabel = info.loadLabel(mPackageManager);
                        mPackageName = info.packageName;
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    use(AppDataUsageAppSettingsController.class).init(mPackages, userId);
                }
                mRestrictAll = findPreference(KEY_RESTRICT_ALL);
                mRestrictAll.setOnPreferenceChangeListener(this);
                mRestrictBackground = findPreference(KEY_RESTRICT_BACKGROUND);
                mRestrictBackground.setOnPreferenceChangeListener(this);
                mRestrictCellular = findPreference(KEY_RESTRICT_CELLULAR);
                mRestrictCellular.setOnPreferenceChangeListener(this);
                mRestrictVpn = findPreference(KEY_RESTRICT_VPN);
                mRestrictVpn.setOnPreferenceChangeListener(this);
                mRestrictWifi = findPreference(KEY_RESTRICT_WIFI);
                mRestrictWifi.setOnPreferenceChangeListener(this);
                mUnrestrictedData = findPreference(KEY_UNRESTRICTED_DATA);
                mUnrestrictedData.setOnPreferenceChangeListener(this);
            }
            mDataSaverBackend = new DataSaverBackend(mContext);

            use(AppDataUsageListController.class).init(uidList);
        } else {
            final Context context = getActivity();
            final UidDetail uidDetail = uidDetailProvider.getUidDetail(mAppItem.key, true);
            mIcon = uidDetail.icon;
            mLabel = uidDetail.label;
            mPackageName = context.getPackageName();

            removePreference(KEY_UNRESTRICTED_DATA);
            removePreference(KEY_RESTRICT_ALL);
            removePreference(KEY_RESTRICT_BACKGROUND);
            removePreference(KEY_RESTRICT_CELLULAR);
            removePreference(KEY_RESTRICT_VPN);
            removePreference(KEY_RESTRICT_WIFI);
        }

        addEntityHeader();
    }

    @Override
    public void onStart() {
        super.onStart();
        // No animations will occur before bindData() initially updates the cycle.
        // This is mainly for the cycle spinner, because when the page is entered from the
        // AppInfoDashboardFragment, there is no way to know whether the cycle data is available
        // before finished the async loading.
        // The animator will be set back if any page updates happens after loading, in
        // setBackPreferenceListAnimatorIfLoaded().
        mIsLoading = true;
        getListView().setItemAnimator(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDataSaverBackend != null) {
            mDataSaverBackend.addListener(this);
        }
        updatePrefs();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDataSaverBackend != null) {
            mDataSaverBackend.remListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference == mRestrictBackground) {
            mDataSaverBackend.setIsDenylisted(mAppItem.key, mPackageName, !(Boolean) newValue);
            updatePrefs();
            return true;
        } else if (preference == mRestrictAll) {
            setAppRestrictAll(!(Boolean) newValue);
            updatePrefs();
            return true;
        } else if (preference == mRestrictCellular) {
            setAppRestrictCellular(!(Boolean) newValue);
            updatePrefs();
            return true;
        } else if (preference == mRestrictVpn) {
            setAppRestrictVpn(!(Boolean) newValue);
            updatePrefs();
            return true;
        } else if (preference == mRestrictWifi) {
            setAppRestrictWifi(!(Boolean) newValue);
            updatePrefs();
            return true;
        } else if (preference == mUnrestrictedData) {
            mDataSaverBackend.setIsAllowlisted(mAppItem.key, mPackageName, (Boolean) newValue);
            return true;
        }
        return false;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_data_usage;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @VisibleForTesting
    void updatePrefs() {
        updatePrefs(getAppRestrictBackground(), getUnrestrictData(), getAppRestrictAll(),
                getAppRestrictCellular(), getAppRestrictVpn(), getAppRestrictWifi(),
                hasInternetPermission());
    }

    @VisibleForTesting
    UidDetailProvider getUidDetailProvider() {
        return new UidDetailProvider(mContext);
    }

    @VisibleForTesting
    void initCycle(List<Integer> uidList) {
        var cycleController = use(AppDataUsageCycleController.class);
        var summaryController = use(AppDataUsageSummaryController.class);
        var repository = new AppDataUsageDetailsRepository(mContext, mTemplate, mCycles, uidList);
        cycleController.init(repository, data -> {
            mIsLoading = false;
            summaryController.update(data);
            return Unit.INSTANCE;
        });
        if (mCycles != null) {
            Log.d(TAG, "setInitialCycles: " + mCycles + " " + mSelectedCycle);
            cycleController.setInitialCycles(mCycles, mSelectedCycle);
        }
    }

    /**
     * Sets back the preference list's animator if the loading is finished.
     *
     * The preference list's animator was temporarily removed before loading in onResume().
     * When need to update the preference visibility in this page after the loading, adding the
     * animator back to keeping the usual animations.
     */
    private void setBackPreferenceListAnimatorIfLoaded() {
        if (mIsLoading) {
            return;
        }
        RecyclerView recyclerView = getListView();
        if (recyclerView.getItemAnimator() == null) {
            recyclerView.setItemAnimator(new DefaultItemAnimator());
        }
    }

    private void updatePrefs(boolean restrictBackground, boolean unrestrictData,
            boolean restrictAll, boolean restrictCellular, boolean restrictVpn,
            boolean restrictWifi, boolean hasInternetPermission) {
        setBackPreferenceListAnimatorIfLoaded();
        final EnforcedAdmin admin = RestrictedLockUtilsInternal
                .checkIfMeteredDataUsageUserControlDisabled(mContext, mPackageName,
                        UserHandle.getUserId(mAppItem.key));
        if (mRestrictAll != null) {
            mRestrictAll.setEnabled(hasInternetPermission);
            mRestrictAll.setChecked(!restrictAll);
        }
        if (mRestrictBackground != null) {
            mRestrictBackground.setDisabledByAdmin(admin);
            mRestrictBackground.setEnabled(hasInternetPermission &&
                    !mRestrictBackground.isDisabledByAdmin() && !restrictAll &&
                    !restrictCellular);
            mRestrictBackground.setChecked(!restrictBackground && !restrictAll &&
                    !restrictCellular);
        }
        if (mRestrictCellular != null) {
            mRestrictCellular.setEnabled(hasInternetPermission && !restrictAll);
            mRestrictCellular.setChecked(!restrictAll && !restrictCellular);
        }
        if (mRestrictVpn != null) {
            mRestrictVpn.setEnabled(hasInternetPermission && !restrictAll);
            mRestrictVpn.setChecked(!restrictAll && !restrictVpn);
        }
        if (mRestrictWifi != null) {
            mRestrictWifi.setEnabled(hasInternetPermission && !restrictAll);
            mRestrictWifi.setChecked(!restrictAll && !restrictWifi);
        }
        if (mUnrestrictedData != null) {
            mUnrestrictedData.setDisabledByAdmin(admin);
            mUnrestrictedData.setEnabled(hasInternetPermission &&
                    !mUnrestrictedData.isDisabledByAdmin() && !restrictBackground && !restrictAll &&
                    !restrictCellular);
            mUnrestrictedData.setChecked(unrestrictData && !restrictBackground && !restrictAll &&
                    !restrictCellular);
        }
    }

    private void addUid(int uid) {
        String[] packages = mPackageManager.getPackagesForUid(getAppUid(uid));
        if (packages != null) {
            Collections.addAll(mPackages, packages);
        }
    }

    private boolean getAppRestrictBackground() {
        return getAppRestriction(POLICY_REJECT_METERED_BACKGROUND);
    }

    private boolean getAppRestrictCellular() {
        return getAppRestriction(POLICY_REJECT_CELLULAR);
    }

    private boolean getAppRestrictVpn() {
        return getAppRestriction(POLICY_REJECT_VPN);
    }

    private boolean getAppRestrictWifi() {
        return getAppRestriction(POLICY_REJECT_WIFI);
    }

    private boolean getAppRestrictAll() {
        return getAppRestriction(POLICY_REJECT_ALL);
    }

    private boolean getUnrestrictData() {
        if (mDataSaverBackend != null) {
            return mDataSaverBackend.isAllowlisted(mAppItem.key);
        }
        return false;
    }

    private boolean getAppRestriction(int policy) {
        final int uid = mAppItem.key;
        final int uidPolicy = services.mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & policy) != 0;
    }

    private boolean hasInternetPermission() {
        return mPackageManager.checkPermission(Manifest.permission.INTERNET, mPackageName)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setAppRestrictAll(boolean restrict) {
        setAppRestriction(POLICY_REJECT_ALL, restrict);
    }

    private void setAppRestrictCellular(boolean restrict) {
        setAppRestriction(POLICY_REJECT_CELLULAR, restrict);
    }

    private void setAppRestrictVpn(boolean restrict) {
        setAppRestriction(POLICY_REJECT_VPN, restrict);
    }

    private void setAppRestrictWifi(boolean restrict) {
        setAppRestriction(POLICY_REJECT_WIFI, restrict);
    }

    private void setAppRestriction(int policy, boolean restrict) {
        final int uid = mAppItem.key;
        if (restrict) {
            services.mPolicyManager.addUidPolicy(uid, policy);
        } else {
            services.mPolicyManager.removeUidPolicy(uid, policy);
        }
    }

    @VisibleForTesting
    void addEntityHeader() {
        String pkg = mPackages.size() != 0 ? mPackages.valueAt(0) : null;
        int uid = 0;
        if (pkg != null) {
            try {
                uid = mPackageManager.getPackageUidAsUser(pkg,
                        UserHandle.getUserId(mAppItem.key));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Skipping UID because cannot find package " + pkg);
            }
        }

        final boolean showInfoButton = mAppItem.key > 0;

        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* header */)
                .setUid(uid)
                .setHasAppInfoLink(showInfoButton)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .setIcon(mIcon)
                .setLabel(mLabel)
                .setPackageName(pkg)
                .done(getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APP_DATA_USAGE;
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {

    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
        if (mAppItem.uids.get(uid, false)) {
            updatePrefs(getAppRestrictBackground(), isAllowlisted, getAppRestrictAll(),
                    getAppRestrictCellular(), getAppRestrictVpn(), getAppRestrictWifi(),
                    hasInternetPermission());
        }
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted) {
        if (mAppItem.uids.get(uid, false)) {
            updatePrefs(isDenylisted, getUnrestrictData(), getAppRestrictAll(),
                    getAppRestrictCellular(), getAppRestrictVpn(), getAppRestrictWifi(),
                    hasInternetPermission());
        }
    }
}
