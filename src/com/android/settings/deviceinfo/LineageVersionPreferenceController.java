/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2017 The LineageOS Project
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settingslib.RestrictedLockUtils;

public class LineageVersionPreferenceController extends PreferenceController {

    private static final String TAG = "LineageVersionPref";

    private static final String KEY_LINEAGE_VERSION = "lineage_version";
    private static final String KEY_LINEAGE_VERSION_PROP = "ro.lineage.version";

    private final UserManager mUserManager;

    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;

    long[] mHits = new long[3];

    public LineageVersionPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LINEAGE_VERSION;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(SystemProperties.get(KEY_LINEAGE_VERSION_PROP,
                mContext.getResources().getString(R.string.lineage_version_default)));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_LINEAGE_VERSION)) {
            return false;
        }
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
            if (mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext,
                            mFunDisallowedAdmin);
                }
                Log.d(TAG, "Sorry, no fun for you!");
                return false;
            }

            final Intent intent = new Intent(Intent.ACTION_MAIN)
                    .putExtra("is_lineage", true)
                    .setClassName(
                            "android", com.android.internal.app.PlatLogoActivity.class.getName());
            try {
                mContext.startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Unable to start activity " + intent.toString());
            }
        }
        return false;
    }
}
