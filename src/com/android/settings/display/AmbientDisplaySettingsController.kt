/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Settings.Secure.DOZE_ENABLED
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

class AmbientDisplaySettingsController(
    context: Context,
    key: String
) : TogglePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int =
        if (isExternallyManaged(mContext)) AVAILABLE else UNSUPPORTED_ON_DEVICE

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_display

    override fun isChecked(): Boolean =
        Settings.Secure.getInt(mContext.contentResolver, DOZE_ENABLED, 1) != 0

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.Secure.putInt(mContext.contentResolver, DOZE_ENABLED, if (isChecked) 1 else 0)

    companion object {
        private val DOZE_SETTINGS = "org.lineageos.settings.device.DOZE_SETTINGS"

        @JvmStatic
        fun isExternallyManaged(context: Context): Boolean =
            context.packageManager.queryIntentActivities(
                Intent(DOZE_SETTINGS),
                PackageManager.ResolveInfoFlags.of(0)
            )?.isNotEmpty() ?: false
    }
}
