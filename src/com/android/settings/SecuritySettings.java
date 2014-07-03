/*
 * Copyright (c) 2012-2013 The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2007 The Android Open Source Project
 * Modifications Copyright (C) 2012-2013 CyanogenMod
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


import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.cyanogenmod.ButtonSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends RestrictedSettingsFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener {
    static final String TAG = "SecuritySettings";

    private static final String KEY_DEVICE_ADMIN_CATEGORY = "device_admin_category";
    private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SIM_LOCK_SETTINGS = "sim_lock_settings";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private static final String KEY_RESET_CREDENTIALS = "reset_credentials";
    private static final String KEY_CREDENTIALS_INSTALL = "credentials_install";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_TOGGLE_VERIFY_APPLICATIONS = "toggle_verify_applications";
    private static final String KEY_CREDENTIALS_MANAGER = "credentials_management";
    private static final String KEY_NOTIFICATION_ACCESS = "manage_notification_access";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    // CyanogenMod Additions
    private static final String KEY_APP_SECURITY_CATEGORY = "app_security";
    private static final String KEY_SMS_SECURITY_CHECK_PREF = "sms_security_check_limit";
    private PackageManager mPM;
    private DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private CheckBoxPreference mShowPassword;

    private KeyStore mKeyStore;
    private Preference mResetCredentials;

    private CheckBoxPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;
    private CheckBoxPreference mToggleVerifyApps;


    private Preference mNotificationAccess;

    private boolean mIsPrimary;

    // CyanogenMod Additions
    private ListPreference mSmsSecurityCheck;
    public SecuritySettings() {
        super(null /* Don't ask for restrictions pin on creation. */);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPM = getActivity().getPackageManager();
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add package manager to check if features are available
        PackageManager pm = getPackageManager();

        // Add options for device encryption
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;

        if (!mIsPrimary) {
            // Rename owner info settings
            Preference ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            if (ownerInfoPref != null) {
                if (UserManager.get(getActivity()).isLinkedUser()) {
                    ownerInfoPref.setTitle(R.string.profile_info_settings_title);
                } else {
                    ownerInfoPref.setTitle(R.string.user_info_settings_title);
                }
            }
        }

        if (mIsPrimary) {
            switch (mDPM.getStorageEncryptionStatus()) {
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                // The device is currently encrypted.
                addPreferencesFromResource(R.xml.security_settings_encrypted);
                break;
            case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                // This device supports encryption but isn't encrypted.
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
                break;
            }
        }

        mSmsSecurityCheck = (ListPreference) root.findPreference(KEY_SMS_SECURITY_CHECK_PREF);
        // Determine options based on device telephony support
        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            mSmsSecurityCheck = (ListPreference) root.findPreference(KEY_SMS_SECURITY_CHECK_PREF);
            mSmsSecurityCheck.setOnPreferenceChangeListener(this);
            int smsSecurityCheck = Integer.valueOf(mSmsSecurityCheck.getValue());
            updateSmsSecuritySummary(smsSecurityCheck);
        } else {
            // No telephony, remove dependent options
            PreferenceGroup appCategory = (PreferenceGroup)
                    root.findPreference(KEY_APP_SECURITY_CATEGORY);
            appCategory.removePreference(mSmsSecurityCheck);
            root.removePreference(appCategory);
        }

        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            MSimTelephonyManager tm = MSimTelephonyManager.getDefault();
            int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
            boolean disableLock = true;
            boolean removeLock = true;
            for (int i = 0; i < numPhones; i++) {
                // Do not display SIM lock for devices without an Icc card
                if (tm.hasIccCard(i)) {
                    // Disable SIM lock if sim card is missing or unknown
                    removeLock = false;
                    if (!((tm.getSimState(i) == TelephonyManager.SIM_STATE_ABSENT)
                            || (tm.getSimState(i) == TelephonyManager.SIM_STATE_UNKNOWN)
                            || (tm.getSimState(i) == TelephonyManager.SIM_STATE_CARD_IO_ERROR))) {
                        disableLock = false;
                    }
                }
            }
            if (removeLock) {
                root.removePreference(root.findPreference(KEY_SIM_LOCK));
            } else {
                if (disableLock) {
                    root.findPreference(KEY_SIM_LOCK).setEnabled(false);
                }
            }
        } else {
            // Do not display SIM lock for devices without an Icc card
            TelephonyManager tm = TelephonyManager.getDefault();
            if (!mIsPrimary || !tm.hasIccCard()) {
                root.removePreference(root.findPreference(KEY_SIM_LOCK));
            } else {
                // Disable SIM lock if sim card is missing or unknown
                if ((TelephonyManager.getDefault().getSimState() ==
                                 TelephonyManager.SIM_STATE_ABSENT) ||
                        (TelephonyManager.getDefault().getSimState() ==
                                 TelephonyManager.SIM_STATE_UNKNOWN)) {
                    root.findPreference(KEY_SIM_LOCK).setEnabled(false);
                }
            }
        }

        // Show password
        mShowPassword = (CheckBoxPreference) root.findPreference(KEY_SHOW_PASSWORD);
        mResetCredentials = root.findPreference(KEY_RESET_CREDENTIALS);

        if (root.findPreference(KEY_SIM_LOCK) != null) {
            // SIM/RUIM lock
            Preference iccLock = (Preference) root.findPreference(KEY_SIM_LOCK_SETTINGS);

            Intent intent = new Intent();
            if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                intent.setClassName("com.android.settings",
                        "com.android.settings.SelectSubscription");
                intent.putExtra(SelectSubscription.PACKAGE, "com.android.settings");
                intent.putExtra(SelectSubscription.TARGET_CLASS,
                        "com.android.settings.IccLockSettings");
            } else {
                intent.setClassName("com.android.settings", "com.android.settings.IccLockSettings");
            }
            iccLock.setIntent(intent);
        }

        // Credential storage
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        mKeyStore = KeyStore.getInstance(); // needs to be initialized for onResume()
        if (!um.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
            Preference credentialStorageType = root.findPreference(KEY_CREDENTIAL_STORAGE_TYPE);

            final int storageSummaryRes =
                mKeyStore.isHardwareBacked() ? R.string.credential_storage_type_hardware
                        : R.string.credential_storage_type_software;
            credentialStorageType.setSummary(storageSummaryRes);

        } else {
            removePreference(KEY_CREDENTIALS_MANAGER);
        }

        // Application install
        PreferenceGroup deviceAdminCategory = (PreferenceGroup)
                root.findPreference(KEY_DEVICE_ADMIN_CATEGORY);
        mToggleAppInstallation = (CheckBoxPreference) findPreference(
                KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());

        // Side loading of apps.
        mToggleAppInstallation.setEnabled(mIsPrimary);

        // Package verification, only visible to primary user and if enabled
        mToggleVerifyApps = (CheckBoxPreference) findPreference(KEY_TOGGLE_VERIFY_APPLICATIONS);
        if (mIsPrimary && showVerifierSetting()) {
            if (isVerifierInstalled()) {
                mToggleVerifyApps.setChecked(isVerifyAppsEnabled());
            } else {
                mToggleVerifyApps.setChecked(false);
                mToggleVerifyApps.setEnabled(false);
            }
        } else {
            if (deviceAdminCategory != null) {
                deviceAdminCategory.removePreference(mToggleVerifyApps);
            } else {
                mToggleVerifyApps.setEnabled(false);
            }
        }

        mNotificationAccess = findPreference(KEY_NOTIFICATION_ACCESS);
        if (mNotificationAccess != null) {
            final int total = NotificationAccessSettings.getListenersCount(mPM);
            if (total == 0) {
                if (deviceAdminCategory != null) {
                    deviceAdminCategory.removePreference(mNotificationAccess);
                }
            } else {
                final int n = getNumEnabledNotificationListeners();
                if (n == 0) {
                    mNotificationAccess.setSummary(getResources().getString(
                            R.string.manage_notification_access_summary_zero));
                } else {
                    mNotificationAccess.setSummary(String.format(getResources().getQuantityString(
                            R.plurals.manage_notification_access_summary_nonzero,
                            n, n)));
                }
            }
        }

        if (shouldBePinProtected(RESTRICTIONS_PIN_SET)) {
            protectByRestrictions(mToggleAppInstallation);
            protectByRestrictions(mToggleVerifyApps);
            protectByRestrictions(mResetCredentials);
            protectByRestrictions(root.findPreference(KEY_CREDENTIALS_INSTALL));
        }
        return root;
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

    private boolean isVerifyAppsEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.PACKAGE_VERIFIER_ENABLE, 1) > 0;
    }

    private boolean isVerifierInstalled() {
        final PackageManager pm = getPackageManager();
        final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
        verification.setType(PACKAGE_MIME_TYPE);
        verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
        return (receivers.size() > 0) ? true : false;
    }

    private boolean showVerifierSetting() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1) > 0;
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps && which == DialogInterface.BUTTON_POSITIVE) {
            setNonMarketAppsAllowed(true);
            if (mToggleAppInstallation != null) {
                mToggleAppInstallation.setChecked(true);
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
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (ensurePinRestrictedPreference(preference)) {
            return true;
        }
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else if (preference == mToggleAppInstallation) {
            if (mToggleAppInstallation.isChecked()) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
            } else {
                setNonMarketAppsAllowed(false);
            }
        } else if (KEY_TOGGLE_VERIFY_APPLICATIONS.equals(key)) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.PACKAGE_VERIFIER_ENABLE,
                    mToggleVerifyApps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mSmsSecurityCheck) {
            int smsSecurityCheck = Integer.valueOf((String) value);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.SMS_OUTGOING_CHECK_MAX_COUNT, smsSecurityCheck);
            updateSmsSecuritySummary(smsSecurityCheck);
        }
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_security;
    }
}
