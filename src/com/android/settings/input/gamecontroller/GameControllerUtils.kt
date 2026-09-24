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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.android.settings.R
import java.text.Collator

/** Utility class for game controller related settings pages */
@SuppressLint("MissingPermission")
object GameControllerUtils {
    const val TAG = "GameControllerSettings"
    const val EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier"

    /** Default map of Preference key to String resource for preference name */
    val preferenceKeyToNameMap =
        mapOf(
            "controller_button_a" to R.string.game_controller_button_a,
            "controller_button_b" to R.string.game_controller_button_b,
            "controller_button_x" to R.string.game_controller_button_x,
            "controller_button_y" to R.string.game_controller_button_y,
            "controller_button_l1" to R.string.game_controller_button_l1,
            "controller_button_r1" to R.string.game_controller_button_r1,
            "controller_button_l2" to R.string.game_controller_button_l2,
            "controller_button_r2" to R.string.game_controller_button_r2,
            "controller_button_thumbl" to R.string.game_controller_button_thumbl,
            "controller_button_thumbr" to R.string.game_controller_button_thumbr,
            "controller_dpad" to R.string.game_controller_dpad,
            "controller_stick_left" to R.string.game_controller_stick_left,
            "controller_stick_right" to R.string.game_controller_stick_right,
        )

    /** Default map of Preference key to drawable resource for preference name */
    val preferenceKeyToIconMap =
        mapOf(
            "controller_button_a" to R.drawable.ic_controller_button_a,
            "controller_button_b" to R.drawable.ic_controller_button_b,
            "controller_button_x" to R.drawable.ic_controller_button_x,
            "controller_button_y" to R.drawable.ic_controller_button_y,
            "controller_button_l1" to R.drawable.ic_controller_button_l1,
            "controller_button_r1" to R.drawable.ic_controller_button_r1,
            "controller_button_l2" to R.drawable.ic_controller_button_l2,
            "controller_button_r2" to R.drawable.ic_controller_button_r2,
            "controller_button_thumbl" to R.drawable.ic_controller_button_thumbl,
            "controller_button_thumbr" to R.drawable.ic_controller_button_thumbr,
            "controller_dpad" to R.drawable.ic_controller_dpad,
            "controller_stick_left" to R.drawable.ic_controller_left_stick,
            "controller_stick_right" to R.drawable.ic_controller_right_stick,
        )

    // Map XML preference keys to their corresponding controller button constants
    val preferenceKeyToButtonMap =
        mapOf(
            "controller_button_a" to KeyEvent.KEYCODE_BUTTON_A,
            "controller_button_b" to KeyEvent.KEYCODE_BUTTON_B,
            "controller_button_x" to KeyEvent.KEYCODE_BUTTON_X,
            "controller_button_y" to KeyEvent.KEYCODE_BUTTON_Y,
            "controller_button_l1" to KeyEvent.KEYCODE_BUTTON_L1,
            "controller_button_r1" to KeyEvent.KEYCODE_BUTTON_R1,
            "controller_button_l2" to KeyEvent.KEYCODE_BUTTON_L2,
            "controller_button_r2" to KeyEvent.KEYCODE_BUTTON_R2,
            "controller_button_thumbl" to KeyEvent.KEYCODE_BUTTON_THUMBL,
            "controller_button_thumbr" to KeyEvent.KEYCODE_BUTTON_THUMBR,
        )

    // Reverse map of button to XML preference keys..
    val buttonToPreferenceKeyMap = preferenceKeyToButtonMap.entries.associate { (k, v) -> v to k }

    // Map XML preference keys to their corresponding controller axes constants
    val preferenceKeyToAxesMap =
        mapOf(
            "controller_dpad" to arrayOf(MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y),
            "controller_stick_left" to arrayOf(MotionEvent.AXIS_X, MotionEvent.AXIS_Y),
            "controller_stick_right" to arrayOf(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ),
        )

    /**
     * Provides a list of connected game controllers, wrapped in a ControllerDevice class, sorted by
     * name and then by descriptor.
     */
    fun getGameControllers(context: Context): List<ControllerDevice> {
        if (!com.android.hardware.input.Flags.controllerRemapping()) {
            return emptyList()
        }
        val inputManager = context.getSystemService(InputManager::class.java) ?: return emptyList()
        val bluetoothManager =
            context.getSystemService(BluetoothManager::class.java) ?: return emptyList()
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        val hiddenDescriptors = context.resources
            .getStringArray(R.array.config_hidden_game_controller_descriptors)
            .toSet()

        val foundControllers = mutableListOf<ControllerDevice>()

        for (deviceId in inputManager.inputDeviceIds) {
            val device = inputManager.getInputDevice(deviceId)
            if (device == null || !device.isPhysicalDevice) {
                continue
            }

            // Filter out hidden controllers by descriptor
            if (device.identifier.descriptor in hiddenDescriptors) {
                continue
            }

            val sources = device.sources
            val isGameController =
                ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) or
                    ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
            if (isGameController) {
                // Try to find the corresponding BluetoothDevice
                val bluetoothDevice = findBluetoothDevice(device, bluetoothAdapter)
                // Add the new wrapper class to the list
                foundControllers.add(ControllerDevice(device, bluetoothDevice))
            }
        }
        foundControllers.sortedWith(
            compareBy(Collator.getInstance()) { it: ControllerDevice -> it.name }
        )
        return foundControllers
    }

    /**
     * Creates a ControllerDevice object from a given InputDeviceIdentifier. Returns null if the
     * device cannot be found or is not a game controller.
     */
    @SuppressLint("MissingPermission")
    fun getControllerDeviceFromIdentifier(
        context: Context,
        identifier: InputDeviceIdentifier,
    ): ControllerDevice? {
        val inputManager = context.getSystemService(InputManager::class.java) ?: return null
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java) ?: return null
        val device = inputManager.getInputDeviceByDescriptor(identifier.descriptor) ?: return null

        val sources = device.sources
        val isGameController =
            ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) or
                ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
        if (!isGameController) {
            Log.w(TAG, "Device for identifier is not a game controller: ${device.name}")
            return null
        }

        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val bluetoothDevice = findBluetoothDevice(device, bluetoothAdapter)

        return ControllerDevice(device, bluetoothDevice)
    }

    /** Finds the BluetoothDevice corresponding to an InputDevice. */
    @SuppressLint("MissingPermission")
    private fun findBluetoothDevice(
        inputDevice: InputDevice,
        bluetoothAdapter: BluetoothAdapter?,
    ): BluetoothDevice? {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Cannot find BluetoothDevice, adapter is null.")
            return null
        }
        val macAddress = inputDevice.bluetoothAddress ?: return null

        return try {
            bluetoothAdapter.getRemoteDevice(macAddress)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid Bluetooth address: $macAddress", e)
            null
        }
    }

    /**
     * A data class to hold both the InputDevice and its associated BluetoothDevice. The
     * bluetoothDevice is nullable, as a controller might be connected via USB.
     */
    data class ControllerDevice(
        val name: String,
        val inputDeviceId: Int,
        val inputDeviceIdentifier: InputDeviceIdentifier,
        val bluetoothAddress: String?,
        val sources: Int,
        val axes: Set<Int>,
    ) {
        constructor(
            inputDevice: InputDevice,
            bluetoothDevice: BluetoothDevice?,
        ) : this(
            name = bluetoothDevice?.name ?: inputDevice.name,
            inputDeviceId = inputDevice.id,
            inputDeviceIdentifier = inputDevice.identifier,
            bluetoothAddress = inputDevice.bluetoothAddress,
            sources = inputDevice.sources,
            axes =
                inputDevice
                    .motionRanges
                    .filter { it.source and InputDevice.SOURCE_JOYSTICK != 0 }
                    .map { it.axis }
                    .toSet(),
        )
    }
}
