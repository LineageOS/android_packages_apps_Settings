/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.FirmwareVersionActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(FirmwareVersionScreen.KEY)
open class FirmwareVersionScreen : PreferenceScreenMixin, PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED, TAG_DEVICE_STATE_SCREEN)

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.firmware_version_purpose

    override val title: Int
        get() = R.string.firmware_version

    override fun getSummary(context: Context): CharSequence? =
        Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY

    override val keywords: Int
        get() = R.string.keywords_android_version

    override val indexable
        get() = true

    // Once fully launch, change to PreferenceFragment and clean up FirmwareVersionScreenTest
    override fun fragmentClass(): Class<out Fragment>? = FirmwareVersionSettings::class.java

    override fun getMetricsCategory() = SettingsEnums.DIALOG_FIRMWARE_VERSION

    override val highlightMenuKey: Int
        get() = R.string.menu_key_about_device

    override fun hasCompleteHierarchy() = true

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, FirmwareVersionActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +FirmwareVersionDetailPreference()
            +LineageVersionDetailPreference()
            +SecurityPatchLevelPreference()
            +LineageVendorSecurityPatchLevelPreference()
            +MainlineModuleVersionPreference()
            +BasebandVersionPreference()
            +KernelVersionPreference()
            +LineageBuildDatePreference()
            +SimpleBuildNumberPreference()
        }

    companion object {
        const val KEY = "firmware_version"
    }
}
