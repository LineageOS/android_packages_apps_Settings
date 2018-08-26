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
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class LineageBuildDatePreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding {

    override val key: String
        get() = "os_build_date"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

    override val title: Int
        get() = org.lineageos.platform.internal.R.string.build_date

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        preference.isCopyingEnabled = true
    }

    override fun getSummary(context: Context): CharSequence? =
        SystemProperties.get(BUILD_DATE_PROPERTY, context.getString(R.string.unknown))

    companion object {
        const val BUILD_DATE_PROPERTY: String = "ro.build.date"
    }
}
