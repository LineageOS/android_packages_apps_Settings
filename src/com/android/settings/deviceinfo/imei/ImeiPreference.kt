/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.deviceinfo.imei

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.deviceinfo.PhoneNumberUtil
import com.android.settings.wifi.utils.activeModemCount
import com.android.settings.wifi.utils.isAdminUser
import com.android.settings.wifi.utils.telephonyManager
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.PreferenceBindingPlaceholder

/** IMEI data class to store IMEI and slot ID. */
data class ImeiData(val imei: String, val slotId: Int)

/** Preference to show IMEI information for single and multi modem devices. */
class ImeiPreference(
    context: Context,
    private val index: Int,
    private val activeModemCount: Int,
    private val imeiList: List<ImeiData> = listOf(),
) :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceBindingPlaceholder,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {

    private val formattedTitle: String = context.getFormattedTitle()

    override val key: String
        get() = KEY_PREFIX + "${index + 1}"

    override val purpose: Int
        get() = R.string.imei_info_purpose

    override val availabilityDescription =
        "The user must be admin user and the device must be mobile data capable or voice capable."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean =
        context.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context))

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    override val supportsWrite = false

    override val valueType = String::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

    override fun getSummary(context: Context): CharSequence? =
        context.getString(R.string.device_info_protected_single_press)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        val preference = context.requirePreference<Preference>(key)
        preference.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val slotId = if (index < imeiList.size) imeiList[index].slotId else index
                preference.summary = getFormattedSummary()
                ImeiInfoDialogFragment.show(context.childFragmentManager, slotId, formattedTitle)
                return@OnPreferenceClickListener true
            }
    }

    private fun Context.getFormattedTitle(): String =
        if (activeModemCount <= 1) {
            getString(R.string.status_imei)
        } else {
            getString(R.string.imei_multi_sim, index + 1)
        }

    private fun getFormattedSummary(): CharSequence {
        return when {
            imeiList.isEmpty() || index >= imeiList.size -> String()
            else -> {
                PhoneNumberUtil.expandByTts(imeiList[index].imei)
            }
        }
    }

    override val sensitivityLevel
        get() = SensitivityLevel.DO_NOT_EXPOSE

    companion object {
        const val TAG = "ImeiPreference"
        const val KEY_PREFIX = "imei_info"
    }
}

/**
 * As per GSMA specification TS37, below Primary IMEI requirements are mandatory to support
 * TS37_2.2_REQ_5 TS37_2.2_REQ_8 (Attached the document has description about this test cases)
 *
 * b/434700998, using the lower IMEI as the primary IMEI. IMEI 1 = primary IMEI i.e. lower IMEI IMEI
 * 2 = non-primary IMEI
 */
val Context.getImeiList: List<ImeiData>
    get() = buildList {
        telephonyManager?.let {
            var primaryImei = String()
            try {
                primaryImei = it.primaryImei
            } catch (exception: Exception) {
                Log.e(ImeiPreference.TAG, "PrimaryImei not available.", exception)
            }
            var imeiListFromSlot: MutableList<ImeiData> = mutableListOf()
            for (slotIndex in 0 until activeModemCount) {
                try {
                    val slotImei = it.getImei(slotIndex)
                    imeiListFromSlot.add(ImeiData(slotImei ?: String(), slotIndex))
                } catch (exception: Exception) {
                    Log.e(ImeiPreference.TAG, "Slot[$slotIndex] imei not available.", exception)
                }
            }

            imeiListFromSlot.sortBy { it.imei }
            if (primaryImei.isNotEmpty() && imeiListFromSlot.size >= 2) {
                val primaryImeiData = imeiListFromSlot.find { it.imei == primaryImei }
                if (primaryImeiData != null) {
                    imeiListFromSlot.remove(primaryImeiData)
                    add(primaryImeiData)
                }
            }
            addAll(imeiListFromSlot)
        }
    }
