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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.provider.Settings;

/**
 * Per-device automatic connection mode.
 *
 * <p>The mode is stored in {@link Settings.Secure}, keyed by the device's anonymized address, so
 * that it can be read and written both by the Settings app (which exposes the UI) and by the
 * Bluetooth stack (which enforces the behavior) without requiring a framework API change.
 *
 * <p>Modes are ordered: each mode enables everything the previous one does, plus one more
 * behavior:
 * <ul>
 *   <li>{@link #MANUAL_ONLY}: the phone never initiates a connection on its own.
 *   <li>{@link #AFTER_PAIRING}: connect right after pairing completes.
 *   <li>{@link #ON_RANGE}: reconnect when the device comes back in range.
 *   <li>{@link #ALWAYS}: also reconnect when Bluetooth turns on (device boot or BT re-enable).
 * </ul>
 *
 * <p>Manual connections (the user taps the device in Settings) are always allowed regardless of
 * the mode.
 *
 * <p>Keep in sync with {@code com.android.bluetooth.btservice.AutoConnectMode} in
 * packages/modules/Bluetooth.
 */
public final class AutoConnectMode {
    /** Do not connect automatically, only when the user connects manually. */
    public static final int MANUAL_ONLY = 0;
    /** Connect automatically right after the device is paired. */
    public static final int AFTER_PAIRING = 1;
    /** Reconnect automatically when the device comes back in range. */
    public static final int ON_RANGE = 2;
    /** Reconnect automatically when Bluetooth turns on (boot or re-enable). */
    public static final int ALWAYS = 3;

    /** The maximum valid mode value. */
    public static final int MAX_MODE = ALWAYS;

    /** The number of modes, useful to build UI entries. */
    public static final int MODE_COUNT = MAX_MODE + 1;

    private static final String SETTINGS_KEY_PREFIX = "bluetooth_auto_connect_mode_";

    private AutoConnectMode() {}

    /** Returns the current mode for the given device, defaulting to {@link #MANUAL_ONLY}. */
    public static int getMode(Context context, BluetoothDevice device) {
        int mode =
                Settings.Secure.getInt(
                        context.getContentResolver(),
                        SETTINGS_KEY_PREFIX + device.getAnonymizedAddress(),
                        MANUAL_ONLY);
        if (mode < MANUAL_ONLY || mode > MAX_MODE) {
            return MANUAL_ONLY;
        }
        return mode;
    }

    /** Stores the mode for the given device. */
    public static void setMode(Context context, BluetoothDevice device, int mode) {
        Settings.Secure.putInt(
                context.getContentResolver(),
                SETTINGS_KEY_PREFIX + device.getAnonymizedAddress(),
                mode);
    }
}
