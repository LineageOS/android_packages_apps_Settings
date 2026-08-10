/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.security;

import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

/** Preference controller for content protection toggle switch bar. */
// LINT.IfChange
public class ContentProtectionTogglePreferenceController extends TogglePreferenceController
        implements OnCheckedChangeListener {

    @VisibleForTesting
    static final String KEY_CONTENT_PROTECTION_PREFERENCE = "content_protection_user_consent";

    @Nullable private SettingsMainSwitchPreference mSwitchBar;

    @Nullable private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    @Nullable private EnforcingAdmin mEnforcingAdmin;

    @NonNull private final ContentResolver mContentResolver;

    @DevicePolicyManager.ContentProtectionPolicy
    private int mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;

    public ContentProtectionTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();

        if (android.app.admin.flags.Flags.policyTransparencyRefactorV2()) {
            mEnforcingAdmin = ContentProtectionPreferenceUtils.getContentProtectionEnforcingAdmin(
                    mContext, getManagedProfile());
        } else {
            mEnforcedAdmin = getEnforcedAdmin();
        }

        mContentProtectionPolicy = getContentProtectionPolicy(getManagedProfile());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        final boolean hasAdmin =
                android.app.admin.flags.Flags.policyTransparencyRefactorV2()
                        ? mEnforcingAdmin != null
                        : mEnforcedAdmin != null;

        if (hasAdmin) {
            if (mContentProtectionPolicy == DevicePolicyManager.CONTENT_PROTECTION_DISABLED) {
                return false;
            }
            if (mContentProtectionPolicy == DevicePolicyManager.CONTENT_PROTECTION_ENABLED) {
                return true;
            }
        }
        return Settings.Global.getInt(mContentResolver, KEY_CONTENT_PROTECTION_PREFERENCE, 0) >= 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean hasAdmin =
                    android.app.admin.flags.Flags.policyTransparencyRefactorV2()
                            ? mEnforcingAdmin != null
                            : mEnforcedAdmin != null;
        if (hasAdmin
                && mContentProtectionPolicy
                        != DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY) {
            return false;
        }

        final UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager == null || !userManager.isAdminUser()) {
            return false;
        }

        Settings.Global.putInt(
                mContentResolver, KEY_CONTENT_PROTECTION_PREFERENCE, isChecked ? 1 : -1);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());

        if (preference instanceof SettingsMainSwitchPreference) {
            mSwitchBar = (SettingsMainSwitchPreference) preference;
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    // Workaround for SettingsMainSwitchPreference.setDisabledByAdmin without user restriction.
    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (android.app.admin.flags.Flags.policyTransparencyRefactorV2()) {
            if (mSwitchBar != null
                && mEnforcingAdmin != null
                && mContentProtectionPolicy
                        != DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY) {
                mSwitchBar.setDisabledByAdmin(mEnforcingAdmin);
                return;
            }
        } else {
            if (mSwitchBar != null
                && mEnforcedAdmin != null
                && mContentProtectionPolicy
                        != DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY) {
                mSwitchBar.setDisabledByAdmin(mEnforcedAdmin);
                return;
            }
        }

        UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager != null
                && !userManager.isAdminUser()
                && mSwitchBar != null) {
            mSwitchBar.setEnabled(false);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != isChecked()) {
            setChecked(isChecked);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @VisibleForTesting
    @Nullable
    protected UserHandle getManagedProfile() {
        return ContentProtectionPreferenceUtils.getManagedProfile(mContext);
    }

    @VisibleForTesting
    @Nullable
    protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.getDeviceOwner(mContext);
    }

    @VisibleForTesting
    @DevicePolicyManager.ContentProtectionPolicy
    protected int getContentProtectionPolicy(@Nullable UserHandle userHandle) {
        return ContentProtectionPreferenceUtils.getContentProtectionPolicy(mContext, userHandle);
    }
}
// LINT.ThenChange(ContentProtectionScreenApi.kt)
