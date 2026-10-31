/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.development.bluetooth

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.os.UserManager
import android.util.Log
import com.android.settings.R
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.development.BluetoothA2dpConfigStore
import com.android.settings.development.BluetoothA2dpHwOffloadPreferenceController
import com.android.settings.development.BluetoothAbsoluteVolumePreferenceController
import com.android.settings.development.BluetoothAvrcpVersionPreferenceController
import com.android.settings.development.BluetoothDeviceNoNamePreferenceController
import com.android.settings.development.BluetoothLeAudioAllowListPreferenceController
import com.android.settings.development.BluetoothLeAudioDeviceDetailsPreferenceController
import com.android.settings.development.BluetoothLeAudioHwOffloadPreferenceController
import com.android.settings.development.BluetoothLeAudioModePreferenceController
import com.android.settings.development.BluetoothMapVersionPreferenceController
import com.android.settings.development.BluetoothMaxConnectedAudioDevicesPreferenceController
import com.android.settings.development.BluetoothRebootDialog
import com.android.settings.development.BluetoothServiceConnectionListener
import com.android.settings.development.BluetoothSnoopLogFilterProfileMapPreferenceController
import com.android.settings.development.BluetoothSnoopLogFilterProfilePbapPreferenceController
import com.android.settings.development.BluetoothSnoopLogHost
import com.android.settings.development.BluetoothSnoopLogPreferenceController
import com.android.settings.development.BluetoothSnoopLogSocketPreferenceController
import com.android.settings.development.DefaultLaunchPreferenceController
import com.android.settings.development.DeveloperOptionAwareMixin
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class BluetoothDevelopmentSettingsFragment :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_BLUETOOTH),
    BluetoothRebootDialog.OnRebootDialogListener,
    AbstractBluetoothPreferenceController.Callback,
    BluetoothSnoopLogHost,
    DeveloperOptionAwareMixin {

    private val adapter: BluetoothAdapter? by lazy {
        requireContext().getSystemService(BluetoothManager::class.java)?.adapter
    }
    private val a2dpConfigStore = BluetoothA2dpConfigStore()
    private var a2dp: BluetoothA2dp? = null

    private val a2dpReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "a2dpReceiver.onReceive intent=$intent")
                preferenceControllers
                    .flatten()
                    .filterIsInstance<BluetoothServiceConnectionListener>()
                    .forEach { it.onBluetoothCodecUpdated() }
            }
        }

    private val a2dpServiceListener =
        object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                a2dp = proxy as BluetoothA2dp
                preferenceControllers
                    .flatten()
                    .filterIsInstance<BluetoothServiceConnectionListener>()
                    .forEach { it.onBluetoothServiceConnected(a2dp) }
            }

            override fun onServiceDisconnected(profile: Int) {
                a2dp = null
                preferenceControllers
                    .flatten()
                    .filterIsInstance<BluetoothServiceConnectionListener>()
                    .forEach { it.onBluetoothServiceDisconnected() }
            }
        }

    override fun getMetricsCategory() = SettingsEnums.DEVELOPMENT_BLUETOOTH

    override fun getLogTag() = TAG

    override fun getPreferenceScreenResId() = R.xml.bluetooth_development_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter?.getProfileProxy(context, a2dpServiceListener, BluetoothProfile.A2DP)

        val filter =
            IntentFilter().apply {
                addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED)
            }
        requireContext().registerReceiver(a2dpReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        a2dp?.let { proxy ->
            adapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy)
            a2dp = null
        }
        requireContext().unregisterReceiver(a2dpReceiver)
    }

    override fun onRebootDialogConfirmed() {
        use(BluetoothA2dpHwOffloadPreferenceController::class.java)?.onRebootDialogConfirmed()
        use(BluetoothLeAudioHwOffloadPreferenceController::class.java)?.onRebootDialogConfirmed()
        use(BluetoothLeAudioModePreferenceController::class.java)?.onRebootDialogConfirmed()
        context?.getSystemService(PowerManager::class.java)?.reboot(null)
    }

    override fun onRebootDialogCanceled() {
        use(BluetoothA2dpHwOffloadPreferenceController::class.java)?.onRebootDialogCanceled()
        use(BluetoothLeAudioHwOffloadPreferenceController::class.java)?.onRebootDialogCanceled()
        use(BluetoothLeAudioModePreferenceController::class.java)?.onRebootDialogCanceled()
    }

    override fun onBluetoothCodecChanged() {
        preferenceControllers
            .flatten()
            .filterIsInstance<AbstractBluetoothDialogPreferenceController>()
            .forEach { it.onBluetoothCodecUpdated() }
    }

    override fun onBluetoothHDAudioEnabled(enabled: Boolean) {
        preferenceControllers
            .flatten()
            .filterIsInstance<AbstractBluetoothDialogPreferenceController>()
            .forEach { it.onHDAudioEnabled(enabled) }
    }

    override fun onSettingChanged() {
        use(BluetoothSnoopLogFilterProfileMapPreferenceController::class.java)?.onSettingChanged()
        use(BluetoothSnoopLogFilterProfilePbapPreferenceController::class.java)?.onSettingChanged()
    }

    override fun createPreferenceControllers(context: Context) =
        buildPreferenceControllers(context, settingsLifecycle, this, a2dpConfigStore)

    companion object {
        private const val TAG = "BtDevSettingsFragment"

        private fun buildPreferenceControllers(
            context: Context,
            lifecycle: Lifecycle?,
            fragment: BluetoothDevelopmentSettingsFragment?,
            a2dpConfigStore: BluetoothA2dpConfigStore?,
        ): List<AbstractPreferenceController> {
            return listOf(
                BluetoothSnoopLogPreferenceController(context, fragment),
                BluetoothSnoopLogSocketPreferenceController(context),
                BluetoothStackLogPreferenceController(context),
                DefaultLaunchPreferenceController(context, "snoop_logger_filters_dashboard"),
                BluetoothSnoopLogFilterProfilePbapPreferenceController(context),
                BluetoothSnoopLogFilterProfileMapPreferenceController(context),
                BluetoothDeviceNoNamePreferenceController(context),
                BluetoothAbsoluteVolumePreferenceController(context),
                BluetoothAvrcpVersionPreferenceController(context),
                BluetoothMapVersionPreferenceController(context),
                BluetoothLeAudioModePreferenceController(context, fragment),
                BluetoothLeAudioDeviceDetailsPreferenceController(context),
                BluetoothLeAudioAllowListPreferenceController(context),
                BluetoothA2dpHwOffloadPreferenceController(context, fragment),
                BluetoothLeAudioHwOffloadPreferenceController(context, fragment),
                BluetoothMaxConnectedAudioDevicesPreferenceController(context),
                BluetoothCodecListPreferenceController(
                    context,
                    lifecycle,
                    a2dpConfigStore,
                    fragment,
                ),
                BluetoothSampleRateDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                BluetoothBitPerSampleDialogPreferenceController(
                    context,
                    lifecycle,
                    a2dpConfigStore,
                ),
                BluetoothQualityDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                BluetoothChannelModeDialogPreferenceController(context, lifecycle, a2dpConfigStore),
                BluetoothHDAudioPreferenceController(context, lifecycle, a2dpConfigStore, fragment),
            )
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.bluetooth_development_settings) {
                override fun isPageSearchEnabled(context: Context) =
                    DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)

                override fun createPreferenceControllers(context: Context) =
                    buildPreferenceControllers(context, null, null, null)
            }
    }
}
