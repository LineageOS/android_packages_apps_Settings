/*
 * Copyright (C) 2026 The LineageOS Project
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
package com.android.settings.deviceinfo.legal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemProperties
import androidx.annotation.StringRes
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

class LineageLegalPreference(
    override val key: String,
    @StringRes override val purpose: Int,
    @StringRes val defaultTitle: Int = 0,
) : PreferenceMetadata, PreferenceTitleProvider, PreferenceAvailabilityProvider {

    private companion object {
        const val PROPERTY_LINEAGE_LICENSE_URL = "ro.lineagelegal.url"
    }

    private fun getLicenseUrl(): String = SystemProperties.get(PROPERTY_LINEAGE_LICENSE_URL)

    override fun getTitle(context: Context): CharSequence? = context.getText(defaultTitle)

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        val url = getLicenseUrl()
        if (url.isNullOrBlank()) return false

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return intent.resolveActivity(context.packageManager) != null
    }

    override fun intent(context: Context): Intent? {
        val url = getLicenseUrl()
        return if (url.isNotBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else null
    }
}
