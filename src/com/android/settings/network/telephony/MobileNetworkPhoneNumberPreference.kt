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

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import kotlinx.coroutines.launch

// LINT.IfChange
class MobileNetworkPhoneNumberPreference(private val data: MobileNetworkData) :
    PreferenceMetadata,
    PreferenceLifecycleProvider,
    PersistentPreference<CharSequence>,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.status_number

    override fun isAvailable(context: Context) = data.phoneNumberDataFlow.value.isAvailable

    override fun getSummary(context: Context) =
        context.getString(R.string.device_info_protected_single_press)

    override val valueType: Class<CharSequence>
        get() = CharSequence::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = PhoneNumberStore(data)

    override fun getReadPermissions(context: Context) =
        Permissions.anyOf(
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
        )

    override fun getWritePermissions(context: Context) = Permissions.EMPTY

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.LOW_SENSITIVITY

    override fun onCreate(context: PreferenceLifecycleContext) {
        context.requirePreference<Preference>(key).onPreferenceClickListener =
            Preference.OnPreferenceClickListener { p ->
                p.summary = data.phoneNumberDataFlow.value.summary
                return@OnPreferenceClickListener true
            }
    }

    @Suppress("UNCHECKED_CAST")
    class PhoneNumberStore(private val data: MobileNetworkData) :
        AbstractKeyedDataObservable<String>(), KeyValueStore {

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            data.phoneNumberDataFlow.value.storageValue as T?

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}

        override fun onFirstObserverAdded() {
            data.coroutineScope?.launch {
                data.phoneNumberDataFlow.collect {
                    notifyChange(KEY, PreferenceChangeReason.VALUE)
                    Log.d(TAG, "collect{},it=$it")
                }
            }
        }

        override fun onLastObserverRemoved() {}
    }

    companion object {
        private const val TAG = "MobileNetworkPhoneNumberPreference"
        const val KEY = "phone_number"
    }
}
// LINT.ThenChange(MobileNetworkPhoneNumberPreferenceController.kt)
