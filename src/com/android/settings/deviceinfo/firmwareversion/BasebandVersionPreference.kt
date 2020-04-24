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

import android.content.Context
import android.os.SystemProperties
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class BasebandVersionPreference :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceBinding {

    override val key: String
        get() = "base_band"

    override val purpose: Int
        get() = R.string.base_band_purpose

    override val title: Int
        get() = R.string.baseband_version

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context): CharSequence {
        val baseband = SystemProperties.get(BASEBAND_PROPERTY,
            context.getString(R.string.device_info_default)
        )

        return baseband
            .split(",")
            .firstOrNull { it.isNotEmpty() }
            ?: baseband
    }

    override val availabilityDescription =
        "The device must be mobile data capable or voice capable."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
        preference.isCopyingEnabled = true
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val BASEBAND_PROPERTY: String = "gsm.version.baseband"
    }
}
// LINT.ThenChange(BasebandVersionPreferenceController.java)
