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

package com.android.settings.network.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.deviceinfo.imei.ImeiInfoDialogFragment
import com.android.settings.flags.Flags
import com.android.settings.network.SubscriptionUtil
import com.android.settings.wifi.utils.isAdminUser
import com.android.settings.wifi.utils.telephonyManager
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
@SuppressLint("MissingPermission")
class MobileNetworkImeiPreference(private val context: Context, private val subId: Int) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceSummaryProvider,
    PreferenceTitleProvider,
    PreferenceAvailabilityProvider {

    private val isAvailable =
        context.isAdminUser == true &&
            (Utils.isMobileDataCapable(context) || Utils.isVoiceCapable(context)) &&
            (Flags.isDualSimOnboardingEnabled() && SubscriptionManager.isValidSubscriptionId(subId))
    private var imei: String? = if (isAvailable) context.telephonyManager(subId)?.imei else ""
    private val formattedTitle: String = context.getFormattedTitle()

    override val key: String
        get() = KEY

    override fun getTitle(context: Context): CharSequence? = formattedTitle

    override fun getSummary(context: Context): CharSequence? =
        context.getString(R.string.device_info_protected_single_press)

    override fun isAvailable(context: Context) = isAvailable

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener { p ->
                p.summary = imei
                getSlotIndex()
                    .takeIf { it != INVALID_SIM_SLOT_INDEX }
                    ?.run {
                        ImeiInfoDialogFragment.show(
                            context.childFragmentManager,
                            this,
                            formattedTitle,
                        )
                    }
                return@OnPreferenceClickListener true
            }
    }

    private fun getSlotIndex(): Int {
        val subscription =
            SubscriptionUtil.getActiveSubscriptions(context.subscriptionManager).firstOrNull {
                it.subscriptionId == subId
            }
        return if (subscription != null) {
            Log.d(TAG, "getSlotIndex(), simSlotIndex=${subscription.simSlotIndex}")
            subscription.simSlotIndex
        } else {
            Log.e(TAG, "getSlotIndex(), simSlotIndex=INVALID_SIM_SLOT_INDEX")
            INVALID_SIM_SLOT_INDEX
        }
    }

    private fun Context.getFormattedTitle(): String =
        try {
            val titleId =
                if (imei == telephonyManager?.primaryImei) R.string.imei_primary
                else R.string.status_imei
            getString(titleId)
        } catch (exception: Exception) {
            Log.e(TAG, "PrimaryImei not available.", exception)
            getString(R.string.status_imei)
        }

    companion object {
        private const val TAG = "MobileNetworkImeiPreference"
        const val KEY = "network_mode_imei_info"
    }
}
// LINT.ThenChange(MobileNetworkImeiPreferenceController.java)
