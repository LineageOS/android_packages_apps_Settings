/*
 * Copyright (C) The LineageOS Project
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

package com.android.settings.display.darkmode

import android.content.Context
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import lineageos.providers.LineageSettings

class DarkModeBlackThemePreference(context: Context, private val darkModeStorage: DarkModeStorage) :
    PreferenceMetadata, BooleanValuePreference, PreferenceBinding {

    private val blackThemeStorage = DarkModeBlackThemeStorage(context)

    override val key: String = LineageSettings.Secure.BERRY_BLACK_THEME

    override val purpose: Int = R.string.berry_black_theme_purpose

    override val title: Int = R.string.berry_black_theme_title

    override val summary: Int = R.string.berry_black_theme_summary

    override val supportsWrite = true

    override fun storage(context: Context) = blackThemeStorage

    override fun isEnabled(context: Context): Boolean {
        return darkModeStorage.getBoolean(DarkModeMainSwitchPreference.KEY) ?: false
    }

    override fun createWidget(context: Context) =
        SwitchPreferenceCompat(context).apply { isPersistent = false }

    fun isAvailable(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                "org.lineageos.overlay.customization.blacktheme",
                0,
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
