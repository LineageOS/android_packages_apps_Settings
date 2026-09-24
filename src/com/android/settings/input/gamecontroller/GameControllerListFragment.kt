/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.input.gamecontroller

import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.input.InputDeviceIdentifier
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.input.gamecontroller.GameControllerUtils.EXTRA_INPUT_DEVICE_IDENTIFIER
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

/** Fragment showing all currently connected game controllers. */
@SearchIndexable
class GameControllerListFragment : DashboardFragment() {

    private lateinit var viewModel: GameControllerListViewModel
    private var autoInputDeviceIdentifier: InputDeviceIdentifier? = null

    override fun getLogTag(): String = TAG

    override fun getPreferenceScreenResId(): Int = R.xml.game_controller_settings

    override fun getMetricsCategory(): Int = SettingsEnums.SETTINGS_GAME_CONTROLLER

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this)[GameControllerListViewModel::class.java]
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        super.onCreatePreferences(bundle, s)

        val inputDeviceIdentifier =
            requireActivity()
                .intent
                .getParcelableExtra(
                    EXTRA_INPUT_DEVICE_IDENTIFIER,
                    InputDeviceIdentifier::class.java,
                )
        autoInputDeviceIdentifier = inputDeviceIdentifier

        // Don't repeat the autoselection.
        if (isAutoSelection(bundle, inputDeviceIdentifier)) {
            showGameControllerFragment(inputDeviceIdentifier!!)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.controllers.observe(viewLifecycleOwner) { controllers ->
            val hiddenDescriptors = resources
                .getStringArray(R.array.config_hidden_game_controller_descriptors)
                .toSet()

            val visibleControllers = controllers.filter { controller ->
                controller.inputDeviceIdentifier.descriptor !in hiddenDescriptors
            }

            if (visibleControllers.isEmpty()) {
                // No controllers are connected, so there's nothing to show.
                activity?.finish()
            } else {
                updatePreferenceList(visibleControllers)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_AUTO_SELECTION, autoInputDeviceIdentifier)
        super.onSaveInstanceState(outState)
    }

    private fun updatePreferenceList(controllers: List<GameControllerUtils.ControllerDevice>) {
        val prefScreen = preferenceScreen ?: return
        prefScreen.removeAll()

        val category =
            PreferenceCategory(prefContext).apply {
                title = getString(R.string.virtual_device_connected)
                order = 0
            }
        prefScreen.addPreference(category)

        for (controller in controllers) {
            val pref =
                Preference(prefContext).apply {
                    title = controller.name
                    setIcon(R.drawable.ic_default_controller)
                    setOnPreferenceClickListener {
                        showGameControllerFragment(controller.inputDeviceIdentifier)
                        true
                    }
                }
            category.addPreference(pref)
        }
    }

    private fun showGameControllerFragment(inputDeviceIdentifier: InputDeviceIdentifier) {
        val args =
            Bundle().apply { putParcelable(EXTRA_INPUT_DEVICE_IDENTIFIER, inputDeviceIdentifier) }
        SubSettingLauncher(context)
            .setSourceMetricsCategory(metricsCategory)
            .setDestination(GameControllerFragment::class.java.name)
            .setArguments(args)
            .launch()
    }

    companion object {
        private val TAG = GameControllerListFragment::class.java.simpleName
        private const val EXTRA_AUTO_SELECTION = "auto_selection"

        private fun isAutoSelection(bundle: Bundle?, identifier: InputDeviceIdentifier?): Boolean {
            if (
                bundle?.getParcelable(EXTRA_AUTO_SELECTION, InputDeviceIdentifier::class.java) !=
                    null
            ) {
                return false
            }
            return identifier != null
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.game_controller_settings) {
                override fun isPageSearchEnabled(context: Context): Boolean {
                    return GameControllerUtils.getGameControllers(context).isNotEmpty()
                }
            }
    }
}
