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
import android.os.SystemProperties
import android.text.format.DateFormat
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LineageVendorSecurityPatchLevelPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding {

    override val key: String
        get() = "vendor_security_key"

    override val purpose: Int
        get() = R.string.security_key_purpose

    override val title: Int
        get() = org.lineageos.platform.internal.R.string.lineage_vendor_security_patch

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        preference.isCopyingEnabled = true
    }

    override fun getSummary(context: Context): CharSequence {
        var patchLevel = SystemProperties.get(AOSP_VENDOR_SECURITY_PATCH_PROPERTY)

        if (patchLevel.isEmpty()) {
            patchLevel = SystemProperties.get(LINEAGE_VENDOR_SECURITY_PATCH_PROPERTY)
        }

        if (patchLevel.isNotEmpty()) {
            try {
                val template = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val patchLevelDate = template.parse(patchLevel)
                val format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy")
                patchLevel = DateFormat.format(format, patchLevelDate).toString()
            } catch (_: ParseException) {
                // parsing failed, use raw string
            }
        } else {
            patchLevel = context.getString(R.string.unknown)
        }

        return patchLevel
    }

    companion object {
        const val AOSP_VENDOR_SECURITY_PATCH_PROPERTY: String =
            "ro.vendor.build.security_patch"
        const val LINEAGE_VENDOR_SECURITY_PATCH_PROPERTY: String =
            "ro.lineage.build.vendor_security_patch"
    }
}
