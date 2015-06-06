/*
 * Copyright (c) 2012-2014 The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2007 The Android Open Source Project
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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.security.KeyStore;
import android.service.trust.TrustAgentService;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.os.IKillSwitchService;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener, Indexable {
    static final String TAG = "SecuritySettings";
    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

    private static final String KEY_DEVICE_ADMIN_CATEGORY = "device_admin_category";

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SIM_LOCK_SETTINGS = "sim_lock_settings";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";
    private static final String KEY_CREDENTIALS_INSTALL = "credentials_install";
    private static final String KEY_APP_OPS_SUMMARY = "app_ops_summary";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String KEY_SCREEN_PINNING = "screen_pinning_settings";

    // Cyanogen device lock
    public static final String ACCOUNT_TYPE_CYANOGEN = "com.cyanogen";
    private static final String EXTRA_CREATE_ACCOUNT = "create-account";
    private static final String LOCK_TO_CYANOGEN_ACCOUNT = "lock_to_cyanogen_account";
    private static final String EXTRA_LOGIN_FOR_KILL_SWITCH = "authCks";
    private static final String EXTRA_CKSOP = "cksOp";
    private static final int LOCK_REQUEST = 57;

    private SwitchPreference mLockDeviceToCyanogenAccount;

    private static final String KEY_SMS_SECURITY_CHECK_PREF = "sms_security_check_limit";

    // These switch preferences need special handling since they're not all stored in Settings.
    private static final String SWITCH_PREFERENCE_KEYS[] = { KEY_SHOW_PASSWORD,
            KEY_TOGGLE_INSTALL_APPLICATIONS, LOCK_TO_CYANOGEN_ACCOUNT };

    // Only allow one trust agent on the platform.
    private static final boolean ONLY_ONE_TRUST_AGENT = false;

    private PackageManager mPM;
    private DevicePolicyManager mDPM;
    private SubscriptionManager mSubscriptionManager;

    private LockPatternUtils mLockPatternUtils;

    private SwitchPreference mShowPassword;

    private KeyStore mKeyStore;
    private Preference mResetCredentials;

    private SwitchPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;

    private ListPreference mSmsSecurityCheck;

    private boolean mIsPrimary;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSubscriptionManager = SubscriptionManager.from(getActivity());

        mLockPatternUtils = new LockPatternUtils(getActivity());
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }

        // Add package manager to check if features are available
        PackageManager pm = getPackageManager();


        // Add options for device encryption
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;

        if (mIsPrimary) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                // The device is currently encrypted.
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                // This device supports encryption but isn't encrypted.
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }

        addPreferencesFromResource(R.xml.security_settings_misc);
        root = getPreferenceScreen();

        // SIM/RUIM lock
        Preference iccLock = root.findPreference(KEY_SIM_LOCK_SETTINGS);
        PreferenceGroup iccLockGroup = (PreferenceGroup) root.findPreference(KEY_SIM_LOCK);

        if (!mIsPrimary) {
            root.removePreference(iccLockGroup);
        } else {
            SubscriptionManager subMgr = SubscriptionManager.from(getActivity());
            TelephonyManager tm = TelephonyManager.getDefault();
            int numPhones = tm.getPhoneCount();
            boolean hasAnySim = false;

            for (int i = 0; i < numPhones; i++) {
                final Preference pref;

                if (numPhones > 1) {
                    SubscriptionInfo sir = subMgr.getActiveSubscriptionInfoForSimSlotIndex(i);
                    if (sir == null) {
                        continue;
                    }

                    pref = new Preference(getActivity());
                    pref.setOrder(iccLock.getOrder());
                    pref.setTitle(getString(R.string.sim_card_lock_settings_title, i + 1));
                    pref.setSummary(sir.getDisplayName());

                    Intent intent = new Intent(getActivity(), IccLockSettings.class);
                    intent.putExtra(IccLockSettings.EXTRA_SUB_ID, sir.getSubscriptionId());
                    intent.putExtra(IccLockSettings.EXTRA_SUB_DISPLAY_NAME, sir.getDisplayName());
                    pref.setIntent(intent);

                    iccLockGroup.addPreference(pref);
                } else {
                    pref = iccLock;
                }

                // Do not display SIM lock for devices without an Icc card
                hasAnySim |= tm.hasIccCard(i);

                int simState = tm.getSimState(i);
                boolean simPresent = simState != TelephonyManager.SIM_STATE_ABSENT
                        && simState != TelephonyManager.SIM_STATE_UNKNOWN
                        && simState != TelephonyManager.SIM_STATE_CARD_IO_ERROR;
                if (!simPresent) {
                    pref.setEnabled(false);
                }
            }

            if (!hasAnySim) {
                root.removePreference(iccLockGroup);
            } else if (numPhones > 1) {
                iccLockGroup.removePreference(iccLock);
            }
        }

        if (Settings.System.getInt(getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0) != 0) {
            root.findPreference(KEY_SCREEN_PINNING).setSummary(
                    getResources().getString(R.string.switch_on_text));
        }

        // SMS rate limit security check
        boolean isTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (isTelephony) {
            mSmsSecurityCheck = (ListPreference) root.findPreference(KEY_SMS_SECURITY_CHECK_PREF);
            if (mSmsSecurityCheck != null) {
                mSmsSecurityCheck.setOnPreferenceChangeListener(this);
                int smsSecurityCheck = Integer.valueOf(mSmsSecurityCheck.getValue());
                updateSmsSecuritySummary(smsSecurityCheck);
            }
        }

        // Show password
        mShowPassword = (SwitchPreference) root.findPreference(KEY_SHOW_PASSWORD);
        mResetCredentials = root.findPreference(KEY_RESET_CREDENTIALS);

        // Credential storage
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        mKeyStore = KeyStore.getInstance(); // needs to be initialized for onResume()
        if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
            Preference credentialStorageType = root.findPreference(KEY_CREDENTIAL_STORAGE_TYPE);

            final int storageSummaryRes =
                mKeyStore != null && mKeyStore.isHardwareBacked()
                        ? R.string.credential_storage_type_hardware
                        : R.string.credential_storage_type_software;
            credentialStorageType.setSummary(storageSummaryRes);
        } else {
            PreferenceGroup credentialsManager = (PreferenceGroup)
                    root.findPreference(KEY_CREDENTIALS_MANAGER);
            credentialsManager.removePreference(root.findPreference(KEY_RESET_CREDENTIALS));
            credentialsManager.removePreference(root.findPreference(KEY_CREDENTIALS_INSTALL));
            credentialsManager.removePreference(root.findPreference(KEY_CREDENTIAL_STORAGE_TYPE));
        }

        // Application install
        PreferenceGroup deviceAdminCategory = (PreferenceGroup)
                root.findPreference(KEY_DEVICE_ADMIN_CATEGORY);
        mToggleAppInstallation = (SwitchPreference) findPreference(
                KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());

        // Cyanogen kill switch
        mLockDeviceToCyanogenAccount = (SwitchPreference)
                deviceAdminCategory.findPreference(LOCK_TO_CYANOGEN_ACCOUNT);
        if (!hasKillSwitch(getActivity())) {
            deviceAdminCategory.removePreference(mLockDeviceToCyanogenAccount);
            mLockDeviceToCyanogenAccount = null;
        }

        // Side loading of apps.
        // Disable for restricted profiles. For others, check if policy disallows it.
        mToggleAppInstallation.setEnabled(!um.getUserInfo(UserHandle.myUserId()).isRestricted());
        if (um.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                || um.hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)) {
            mToggleAppInstallation.setEnabled(false);
        }

        // AppOps summary, only visible when strict mode is enabled.
        if (!AppOpsManager.isStrictEnable()) {
            Preference appOpsSummary = findPreference(KEY_APP_OPS_SUMMARY);
            if (deviceAdminCategory != null) {
                deviceAdminCategory.removePreference(appOpsSummary);
            }
        }

        // The above preferences come and go based on security state, so we need to update
        // the index. This call is expected to be fairly cheap, but we may want to do something
        // smarter in the future.
        Index.getInstance(getActivity())
                .updateFromClassNameResource(SecuritySettings.class.getName(), true, true);

        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) pref.setOnPreferenceChangeListener(this);
        }

        return root;
    }

    public static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(
            PackageManager pm, LockPatternUtils utils) {
        ArrayList<TrustAgentComponentInfo> result = new ArrayList<TrustAgentComponentInfo>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents();
        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo == null) continue;
                if (!TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) continue;
                TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(
                                TrustAgentUtils.getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) continue;
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) break;
            }
        }
        return result;
    }

    private boolean hasLoggedInCyanogenAccount(Context context) {
        AccountManager accountManager = (AccountManager)
                context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accountsByType = accountManager.getAccountsByType(ACCOUNT_TYPE_CYANOGEN);
        return accountsByType != null && accountsByType.length > 0;
    }

    public static boolean hasKillSwitch(Context context) {
        IBinder b = ServiceManager.getService(Context.KILLSWITCH_SERVICE);
        IKillSwitchService service = IKillSwitchService.Stub.asInterface(b);
        if (service != null) {
            try {
                return service.hasKillSwitch() && hasCyanogenAccountType(context);
            } catch (Exception e) {
                // silently fail
            }
        }
        return false;
    }

    public static boolean hasCyanogenAccountType(Context context) {
        AccountManager accountManager = (AccountManager)
                context.getSystemService(Context.ACCOUNT_SERVICE);
        for (AuthenticatorDescription authenticatorDescription :
                accountManager.getAuthenticatorTypes()) {
            if (authenticatorDescription.type.equals(ACCOUNT_TYPE_CYANOGEN)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDeviceLocked() {
        IBinder b = ServiceManager.getService(Context.KILLSWITCH_SERVICE);
        IKillSwitchService service = IKillSwitchService.Stub.asInterface(b);
        if (service != null) {
            try {
                return service.isDeviceLocked();
            } catch (Exception e) {
                // silently fail
            }
        }
        return false;
    }

    public static void updateCyanogenDeviceLockState(final Fragment fragment,
                                                     final boolean setCks,
                                                     final int activityRequestCode) {
        AccountManager.get(fragment.getActivity()).editProperties(ACCOUNT_TYPE_CYANOGEN, null,
                new AccountManagerCallback<Bundle>() {
                    public void run(AccountManagerFuture<Bundle> f) {
                        try {
                            Bundle b = f.getResult();
                            Intent i = b.getParcelable(AccountManager.KEY_INTENT);
                            i.putExtra(EXTRA_CKSOP, setCks ? 1 : 0);
                            i.putExtra(EXTRA_LOGIN_FOR_KILL_SWITCH, true);
                            fragment.startActivityForResult(i, activityRequestCode);
                        } catch (Throwable t) {
                            Log.e(TAG, "confirmCredentials failed", t);
                        }
                    }
                }, null);
    }


    private int getNumEnabledNotificationListeners() {
        final String flat = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);
        if (flat == null || "".equals(flat)) return 0;
        final String[] components = flat.split(":");
        return components.length;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            return;
        }
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS,
                                enabled ? 1 : 0);
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, this)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps) {
            boolean turnOn = which == DialogInterface.BUTTON_POSITIVE;
            setNonMarketAppsAllowed(turnOn);
            if (mToggleAppInstallation != null) {
                mToggleAppInstallation.setChecked(turnOn);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWarnInstallApps != null) {
            mWarnInstallApps.dismiss();
        }
    }

    private void updateSmsSecuritySummary(int selection) {
        String message = selection > 0
                ? getString(R.string.sms_security_check_limit_summary, selection)
                : getString(R.string.sms_security_check_limit_summary_none);
        mSmsSecurityCheck.setSummary(message);
    }

    private void updateDeviceLockState() {
        if (mLockDeviceToCyanogenAccount != null) {
            mLockDeviceToCyanogenAccount.setChecked(isDeviceLocked());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        if (mShowPassword != null) {
            mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
        }

        if (mResetCredentials != null) {
            mResetCredentials.setEnabled(!mKeyStore.isEmpty());
        }

        updateDeviceLockState();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean result = true;
        final String key = preference.getKey();
        if (KEY_SHOW_PASSWORD.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    ((Boolean) value) ? 1 : 0);
        } else if (KEY_TOGGLE_INSTALL_APPLICATIONS.equals(key)) {
            if ((Boolean) value) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
                // Don't change Switch status until user makes choice in dialog, so return false.
                result = false;
            } else {
                setNonMarketAppsAllowed(false);
            }
        } else if (KEY_SMS_SECURITY_CHECK_PREF.equals(key)) {
            int smsSecurityCheck = Integer.valueOf((String) value);
            Settings.Global.putInt(getContentResolver(), Settings.Global.SMS_OUTGOING_CHECK_MAX_COUNT,
                    smsSecurityCheck);
            updateSmsSecuritySummary(smsSecurityCheck);
        } else if (LOCK_TO_CYANOGEN_ACCOUNT.equals(key)) {
            if (((Boolean) value)) {
                // wants to opt in.
                if (hasLoggedInCyanogenAccount(getActivity())) {
                    updateCyanogenDeviceLockState(this, true, LOCK_REQUEST);
                } else {
                    // no account, need to create one!
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.lock_to_cyanogen_create_account_msg)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // create new account
                                            AccountManager accountManager = (AccountManager)
                                                    getActivity()
                                                        .getSystemService(Context.ACCOUNT_SERVICE);
                                            Bundle opts = new Bundle();
                                            opts.putBoolean(EXTRA_CREATE_ACCOUNT, true);
                                            opts.putInt(EXTRA_CKSOP, 1);

                                            accountManager.addAccount(ACCOUNT_TYPE_CYANOGEN,
                                                    null, null, opts, getActivity(), null, null);
                                        }
                                    });
                    builder.create().show();
                }
            } else {
                //  opt out
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setCancelable(false)
                        .setMessage(R.string.lock_to_cyanogen_disable_msg)
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        updateDeviceLockState();
                                    }
                                })
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        updateCyanogenDeviceLockState(SecuritySettings.this,
                                                false, LOCK_REQUEST);
                                    }
                                });
                builder.create().show();
            }
        }
        return result;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_security;
    }

    /**
     * For Search. Please keep it in sync when updating "createPreferenceHierarchy()"
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SecuritySearchIndexProvider();

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {

        boolean mIsPrimary;

        public SecuritySearchIndexProvider() {
            super();

            mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {

            List<SearchIndexableResource> result = new ArrayList<SearchIndexableResource>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            // Add options for lock/unlock screen
            int resId;
            SearchIndexableResource sir;

            if (mIsPrimary) {
                DevicePolicyManager dpm = (DevicePolicyManager)
                        context.getSystemService(Context.DEVICE_POLICY_SERVICE);

                switch (dpm.getStorageEncryptionStatus()) {
                    case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                        // The device is currently encrypted.
                        resId = R.xml.security_settings_encrypted;
                        sir = new SearchIndexableResource(context);
                        sir.xmlResId = resId;
                        result.add(sir);
                        break;
                    case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                        // This device supports encryption but isn't encrypted.
                        resId = R.xml.security_settings_unencrypted;
                        sir = new SearchIndexableResource(context);
                        sir.xmlResId = resId;
                        result.add(sir);
                        break;
                }
            }

            // Append the rest of the settings
            sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.security_settings_misc;
            result.add(sir);

            return result;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            final String screenTitle = res.getString(R.string.security_settings_title);

            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);

            if (!mIsPrimary) {
                int resId = (UserManager.get(context).isLinkedUser()) ?
                        R.string.profile_info_settings_title : R.string.user_info_settings_title;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(resId);
                data.screenTitle = screenTitle;
                result.add(data);
            }

            // Credential storage
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

            if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                KeyStore keyStore = KeyStore.getInstance();

                final int storageSummaryRes = keyStore.isHardwareBacked() ?
                        R.string.credential_storage_type_hardware :
                        R.string.credential_storage_type_software;

                data = new SearchIndexableRaw(context);
                data.title = res.getString(storageSummaryRes);
                data.screenTitle = screenTitle;
                result.add(data);
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = new ArrayList<String>();

            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);

            // Do not display SIM lock for devices without an Icc card
            TelephonyManager tm = TelephonyManager.getDefault();
            if (!mIsPrimary || !tm.hasIccCard()) {
                keys.add(KEY_SIM_LOCK);
            }

            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
                keys.add(KEY_CREDENTIALS_MANAGER);
            }

            return keys;
        }
    }

}
