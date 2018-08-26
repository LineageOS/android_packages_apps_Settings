/*
 * Copyright (C) 2019-2025 The LineageOS Project
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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.SystemProperties
import android.os.UserHandle
import android.os.UserManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class LineageVersionDetailPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private val hits = LongArray(ACTIVITY_TRIGGER_COUNT)

    override val key: String
        get() = "lineage_version"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

    override val title: Int
        get() = org.lineageos.platform.internal.R.string.lineage_version

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_PREFERENCE)

    override fun intent(context: Context): Intent? =
        Intent(Intent.ACTION_MAIN)
            .setClassName(PLATLOGO_PACKAGE_NAME, PLATLOGO_ACTIVITY_CLASS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
        preference.onPreferenceClickListener = this
    }

    override fun getSummary(context: Context): CharSequence =
        SystemProperties.get(LINEAGE_VERSION_PROPERTY, context.getString(R.string.unknown));

    // return true swallows the click event, while return false will start the intent
    override fun onPreferenceClick(preference: Preference): Boolean {
        if (Utils.isMonkeyRunning()) return true

        // remove oldest hit and check whether there are 3 clicks within 500ms
        for (index in 1..<ACTIVITY_TRIGGER_COUNT) hits[index - 1] = hits[index]
        hits[ACTIVITY_TRIGGER_COUNT - 1] = SystemClock.uptimeMillis()
        if (hits[ACTIVITY_TRIGGER_COUNT - 1] - hits[0] > DELAY_TIMER_MILLIS) return true

        val context = preference.context
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        if (userManager?.hasUserRestriction(UserManager.DISALLOW_FUN) != true) return false

        // Sorry, no fun for you!
        val myUserId = UserHandle.myUserId()
        val enforcedAdmin =
            RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context,
                UserManager.DISALLOW_FUN,
                myUserId,
            ) ?: return true
        val disallowedBySystem =
            RestrictedLockUtilsInternal.hasBaseUserRestriction(
                context,
                UserManager.DISALLOW_FUN,
                myUserId,
            )
        if (!disallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, enforcedAdmin)
        }
        return true
    }

    companion object {
        const val ACTIVITY_TRIGGER_COUNT = 3
        const val DELAY_TIMER_MILLIS = 500L

        const val LINEAGE_VERSION_PROPERTY: String = "ro.lineage.version"

        const val PLATLOGO_PACKAGE_NAME: String = "org.lineageos.lineageparts"
        const val PLATLOGO_ACTIVITY_CLASS: String = PLATLOGO_PACKAGE_NAME + ".logo.PlatLogoActivity"
    }
}
// LINT.ThenChange(LineageVersionDetailPreferenceController.java)
