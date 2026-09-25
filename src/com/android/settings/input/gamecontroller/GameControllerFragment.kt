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

import android.annotation.SuppressLint
import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.input.InputDeviceIdentifier
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.input.gamecontroller.GameControllerRemappingDialogFragment.Companion.ARGS_FROM_PREFERENCE_KEY
import com.android.settings.input.gamecontroller.GameControllerRemappingDialogFragment.Companion.ARGS_TO_PREFERENCE_KEY
import com.android.settingslib.widget.ButtonPreference
import kotlinx.coroutines.launch

/** Fragment that manages device specific game controller settings. */
@SuppressLint("MissingPermission")
class GameControllerFragment
@VisibleForTesting
internal constructor(private var viewModelFactory: ViewModelProvider.Factory? = null) :
    DashboardFragment() {

    private lateinit var viewModel: GameControllerViewModel

    override fun onAttach(context: Context) {
        val identifier =
            requireArguments()
                .getParcelable(
                    GameControllerUtils.EXTRA_INPUT_DEVICE_IDENTIFIER,
                    InputDeviceIdentifier::class.java,
                )!!
        // If a factory wasn't injected for testing, create the default one.
        if (viewModelFactory == null) {
            viewModelFactory = viewModelFactory {
                initializer { GameControllerViewModel(requireActivity().application, identifier) }
            }
        }
        viewModel = ViewModelProvider(this, viewModelFactory!!)[GameControllerViewModel::class.java]

        // createPreferenceControllers gets called here so need to create viewModel before calling
        // super.onAttach()
        super.onAttach(context)

        // Accept a custom title when launched externally
        activity?.title = activity?.intent?.getStringExtra(":settings:show_fragment_title")
            ?.takeIf { it.isNotEmpty() } ?: viewModel.controllerDevice.name
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the remapping state to automatically refresh the screen
        viewModel.buttonRemapping.observe(viewLifecycleOwner) { newMap -> updatePreferenceStates() }
        viewModel.axisRemapping.observe(viewLifecycleOwner) { newMap -> updatePreferenceStates() }

        findPreference<ButtonPreference>(RESET_BUTTON_PREFERENCE_KEY)?.setOnClickListener {
            val dialog = GameControllerResetDialogFragment(viewModel)
            dialog.show(parentFragmentManager, RESET_DIALOG_TAG)
            true
        }

        // Observe requests to show the remapping dialog
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showRemappingDialog.collect { request ->
                    val dialog = GameControllerRemappingDialogFragment(viewModel)
                    dialog.arguments =
                        bundleOf(
                            ARGS_FROM_PREFERENCE_KEY to request.fromPreferenceKey,
                            ARGS_TO_PREFERENCE_KEY to request.toPreferenceKey,
                        )
                    dialog.show(
                        parentFragmentManager,
                        GameControllerRemappingDialogFragment::class.simpleName,
                    )
                }
            }
        }

        // On device disconnected, finish this activity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onDeviceDisconnected.collect { activity?.finish() }
            }
        }

        parentFragmentManager.setFragmentResultListener(
            GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
            viewLifecycleOwner,
        ) { requestKey, bundle ->
            val fromPreferenceKey =
                bundle.getString(GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY)!!
            val toPreferenceKey =
                bundle.getString(GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY)!!
            viewModel.applyRemapping(fromPreferenceKey, toPreferenceKey)
        }
    }

    override fun createPreferenceControllers(context: Context): List<BasePreferenceController> {
        return listOf(GameControllerRemappingPreferenceController(context, viewModel))
    }

    override fun getPreferenceScreenResId(): Int = R.xml.game_controller_customization

    override fun getLogTag(): String = TAG

    override fun getMetricsCategory(): Int = SettingsEnums.SETTINGS_GAME_CONTROLLER

    companion object {
        private val TAG = GameControllerFragment::class.java.simpleName

        @VisibleForTesting val RESET_BUTTON_PREFERENCE_KEY = "reset_to_default"

        @VisibleForTesting
        val RESET_DIALOG_TAG = GameControllerResetDialogFragment::class.java.simpleName
    }
}
