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

import android.content.Context
import com.android.settings.R
import com.android.settings.inputmethod.InputDeviceSettingsController

/** Preference controller for the entry preference for Game controller */
class GameControllerSettingsController(context: Context, key: String) :
    InputDeviceSettingsController(context, key) {

    override fun getAvailabilityStatus(): Int {
        if (!com.android.hardware.input.Flags.controllerRemapping()) {
            return UNSUPPORTED_ON_DEVICE
        }

        val hiddenDescriptors = mContext.resources
            .getStringArray(R.array.config_hidden_game_controller_descriptors)
            .toSet()

        val visibleControllers = GameControllerUtils.getGameControllers(mContext)
            .filter { controller ->
                controller.inputDeviceIdentifier.descriptor !in hiddenDescriptors
            }

        return if (visibleControllers.isEmpty()) {
            CONDITIONALLY_UNAVAILABLE
        } else {
            AVAILABLE
        }
    }
}
