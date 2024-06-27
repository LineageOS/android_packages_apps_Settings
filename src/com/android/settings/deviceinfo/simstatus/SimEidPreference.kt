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
package com.android.settings.deviceinfo.simstatus

import android.content.Context
import android.os.UserManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.deviceinfo.PhoneNumberUtil
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preference to show EID information. */
// LINT.IfChange
class SimEidPreference(private val context: Context) :
    PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {

    private var eidMetadata = getEidMetadata()
    private var eidUpdateJob: Job? = null

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.eid_info_purpose

    override val availabilityDescription =
        "The user must be an admin user and the device must be mobile data capable or voice capable. " +
            "The device must also have an EID."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean =
        context.applicationContext.getSystemService(UserManager::class.java)?.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
            eidMetadata?.eid?.isNotEmpty() == true

    override fun getTitle(context: Context): CharSequence? = eidMetadata.getTitle(context)

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
                preference.summary =
                    eidMetadata?.let { PhoneNumberUtil.expandByTts(it.eid).toString() }
                SimEidDialogFragment.show(
                    context.childFragmentManager,
                    it.title.toString(),
                    eidMetadata?.eid ?: "",
                )
                true
            }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        eidUpdateJob?.cancel()
        eidUpdateJob =
            context.lifecycleScope.launch {
                val eid = withContext(Dispatchers.Default) { getEidMetadata() }
                if (eid != eidMetadata) {
                    eidMetadata = eid
                    context.notifyPreferenceChange(key)
                }
                eidUpdateJob = null
            }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        eidUpdateJob?.cancel()
    }

    private fun getEidMetadata(): EidMetadata? {
        val metadata = getEidMetadataWithAssociatedSlotId()
        if (metadata != null) return metadata
        val euiccManager = context.getSystemService(EuiccManager::class.java)
        if (euiccManager != null && euiccManager.isEnabled) {
            val eid = euiccManager.eid
            if (eid != null) return EidMetadata(eid, null)
        }
        return null
    }

    override val sensitivityLevel
        get() = SensitivityLevel.DO_NOT_EXPOSE

    private fun getEidMetadataWithAssociatedSlotId(): EidMetadata? {
        val subscriptionManager =
            context.getSystemService(SubscriptionManager::class.java) ?: return null
        val telephonyManager = context.getSystemService(TelephonyManager::class.java) ?: return null
        @Suppress("DEPRECATION") val phoneCount = telephonyManager.phoneCount
        if (phoneCount <= MAX_PHONE_COUNT_SINGLE_SIM) return null

        val activeSubscriptionInfoList =
            subscriptionManager.getActiveSubscriptionInfoList() ?: return null
        val euiccCardInfoList = telephonyManager.uiccCardsInfo.filter { it.isEuicc }
        val euiccManager = context.getSystemService(EuiccManager::class.java)
        for (subInfo in
            activeSubscriptionInfoList.filter { it.isEmbedded }.sortedBy { it.simSlotIndex }) {
            for (euiccCardInfo in euiccCardInfoList) {
                if (subInfo.cardId == euiccCardInfo.cardId) {
                    val eid = euiccCardInfo.eid
                    if (!eid.isNullOrEmpty()) return EidMetadata(eid, subInfo.simSlotIndex)
                    if (euiccManager != null) {
                        val eid = euiccManager.createForCardId(euiccCardInfo.cardId).getEid()
                        if (eid != null) return EidMetadata(eid, subInfo.simSlotIndex)
                    }
                }
            }
        }

        return null
    }

    private data class EidMetadata(val eid: String, val associatedSlotId: Int?)

    private fun EidMetadata?.getTitle(context: Context): String {
        // Since there is the MEP feature, there is one EID item.
        return context.getString(R.string.status_eid)
    }

    companion object {
        const val KEY = "eid_info"
        const val MAX_PHONE_COUNT_SINGLE_SIM = 1
    }
}
// LINT.ThenChange(SimEidPreferenceController.kt)
