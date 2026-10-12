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

package com.android.settings.display

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.utils.ThreadUtils

// LINT.IfChange
private const val TAG = "ScreenResolutionExt"
private const val SCREEN_RESOLUTION = "user_selected_resolution"
private const val HIGHRESOLUTION_IDX = 0
private const val FULLRESOLUTION_IDX = 1

private fun Context.getDefaultDisplay(): Display? {
    val displayManager = getSystemService(DisplayManager::class.java)
    return displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
}

private fun Context.getSortedResolutionList(): List<Point>? {
    val display = getDefaultDisplay() ?: return null
    val resolutions =
        display.supportedModes.mapTo(mutableSetOf()) { Point(it.physicalWidth, it.physicalHeight) }

    if (resolutions.size != 2) {
        Log.e(TAG, "No support")
        return null
    }

    return resolutions.toList().sortedBy { it.x * it.y }
}

/** Return true if the device contains exactly two resolutions. */
fun Context.checkSupportedResolutions(): Boolean {
    val resolutionList = getSortedResolutionList() ?: return false
    val highWidth = resolutionList[HIGHRESOLUTION_IDX].x
    val fullWidth = resolutionList[FULLRESOLUTION_IDX].x
    return highWidth != 0 && fullWidth != 0
}

/** Return the high resolution width of the device. */
fun Context.getHighResolutionDisplayWidth(): Int = getSortedResolutionList()?.getOrNull(HIGHRESOLUTION_IDX)?.x ?: 0

/** Return the full resolution width of the device. */
fun Context.getFullResolutionDisplayWidth(): Int = getSortedResolutionList()?.getOrNull(FULLRESOLUTION_IDX)?.x ?: 0

/** Return the high resolution height of the device. */
fun Context.getHighResolutionDisplayHeight(): Int = getSortedResolutionList()?.getOrNull(HIGHRESOLUTION_IDX)?.y ?: 0

/** Return the full resolution height of the device. */
fun Context.getFullResolutionDisplayHeight(): Int = getSortedResolutionList()?.getOrNull(FULLRESOLUTION_IDX)?.y ?: 0

/** Return the current display width. */
fun Context.getDisplayWidth(): Int = getDefaultDisplay()?.mode?.physicalWidth ?: 0

/** Get preferred display mode. */
private fun Context.getPreferMode(width: Int): Display.Mode? {
    val display = getDefaultDisplay() ?: return null
    val currentMode = display.mode
    // Find the mode with the matching width, preserving the current refresh rate
    return display.supportedModes.find {
        it.physicalWidth == width && it.refreshRate == currentMode.refreshRate
    } ?: display.supportedModes.find { it.physicalWidth == width } // Fallback to any refresh rate
}

/** Using display manager to set the display mode. */
fun Context.setDisplayModeByWidth(width: Int) {
    if (!ThreadUtils.isMainThread()) {
        ThreadUtils.postOnMainThread { applicationContext.setDisplayModeByWidth(width) }
        return
    }
    val display = getDefaultDisplay() ?: return
    val mode = getPreferMode(width)
    if (mode == null) {
        Log.e(TAG, "No preferred mode found for width: $width")
        return
    }
    if (
        display.mode.physicalWidth == mode.physicalWidth &&
            display.mode.physicalHeight == mode.physicalHeight
    )
        return

    val observer = ScreenResolutionFragment.DisplayObserver(applicationContext)
    try {
        if (!observer.startObserve(width)) return
        /** Apply the resolution change. */
        Log.i(TAG, "setUserPreferredDisplayMode: $mode")
        display.setUserPreferredDisplayMode(mode)
    } catch (e: Exception) {
        observer.stopObserve()
        Log.e(TAG, "setUserPreferredDisplayMode() failed", e)
        return
    }

    /** Send the atom after resolution changed successfully. */
    SettingsStatsLog.write(
        SettingsStatsLog.USER_SELECTED_RESOLUTION,
        display.uniqueId.hashCode(),
        mode.physicalWidth,
        mode.physicalHeight,
    )
}
// LINT.ThenChange(ScreenResolutionController.java)
