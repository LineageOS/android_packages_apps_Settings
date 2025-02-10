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

import android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.contract.KEY_MOBILE_DATA
import com.android.settings.widget.MainSwitchBarMetadata
import com.android.settings.widget.MainSwitchBarPreference
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** A catalyst main switch preference for switching SIM on/off. */
@SuppressLint("MissingPermission")
class MobileNetworkMainSwitchPreference(
    context: Context,
    private val subId: Int,
    private val scope: CoroutineScope,
    private val subscriptionActivationRepository: SubscriptionActivationRepository =
        SubscriptionActivationRepository(context),
    private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
) :
    MainSwitchBarMetadata,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider,
    Preference.OnPreferenceChangeListener {

    val isActivationChangeable = MutableStateFlow(false)

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.use_sim_switch_catalyst_purpose

    override val title: Int
        get() = R.string.mobile_network_use_sim_on

    override fun isEnabled(context: Context): Boolean {
        return isActivationChangeable.value
    }

    override val disableWidgetOnCheckedChanged: Boolean
        get() = false

    override val availabilityDescription = "The telephony stack must support disabling a pSIM."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        val subInfo =
            subscriptionRepository.getSelectableSubscriptionInfoList().firstOrNull {
                it.subscriptionId == subId
            } ?: return false
        // For eSIM, we always want the toggle. If telephony stack support disabling a pSIM
        // directly, we show the toggle.
        return subInfo.isEmbedded ||
            context.requireSubscriptionManager().canDisablePhysicalSubscription()
    }

    override fun tags(context: Context) = arrayOf(KEY_MOBILE_DATA)

    override fun createWidget(context: Context) = MainSwitchBarPreference(context, this)

    override fun getReadPermissions(context: Context) =
        Permissions.anyOf(READ_PRIVILEGED_PHONE_STATE)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override val supportsWrite = false

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        context.lifecycleScope.launch(Dispatchers.Default) {
            subscriptionActivationRepository.isActivationChangeableFlow().collect {
                isActivationChangeable.value = it
                context.notifyPreferenceChange(KEY)
            }
        }
    }

    override val sensitivityLevel: Int
        get() = SensitivityLevel.REQUIRES_CONFIRMATION

    override fun storage(context: Context): KeyValueStore =
        MobileNetworkSwitchStorage(
            context,
            subId,
            subscriptionActivationRepository,
            subscriptionRepository,
        )

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?) =
        false.also {
            scope.launch(Dispatchers.IO) {
                (newValue as? Boolean)?.let { isActive ->
                    subscriptionActivationRepository.setActive(subId, isActive)
                }
            }
        }

    @Suppress("UNCHECKED_CAST")
    @VisibleForTesting
    class MobileNetworkSwitchStorage(
        private val context: Context,
        private val subId: Int,
        private val subscriptionActivationRepository: SubscriptionActivationRepository =
            SubscriptionActivationRepository(context),
        private val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(context),
    ) : AbstractKeyedDataObservable<String>(), KeyValueStore {
        private var onSubscriptionsChangedListener: OnSubscriptionsChangedListener? = null

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
            return runBlocking { subscriptionRepository.isSubscriptionEnabledFlow(subId).first() }
                as T
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value is Boolean) {
                runBlocking { subscriptionActivationRepository.setActive(subId, value) }
            } else {
                throw IllegalArgumentException("Invalid value type - $value")
            }
        }

        override fun onFirstObserverAdded() {
            val executor = HandlerExecutor.main
            context.getSystemService(SubscriptionManager::class.java)?.let {
                val listener =
                    object : OnSubscriptionsChangedListener() {
                        override fun onSubscriptionsChanged() {
                            notifyChange(KEY, PreferenceChangeReason.STATE)
                        }
                    }
                it.addOnSubscriptionsChangedListener(executor, listener)
                onSubscriptionsChangedListener = listener
            }
        }

        override fun onLastObserverRemoved() {
            context.getSystemService(SubscriptionManager::class.java)?.apply {
                onSubscriptionsChangedListener?.let { removeOnSubscriptionsChangedListener(it) }
            }
        }
    }

    companion object {
        const val KEY = "use_sim_switch_catalyst"
    }
}
