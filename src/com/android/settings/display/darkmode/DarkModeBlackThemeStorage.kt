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
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceChangeReason
import lineageos.providers.LineageSettings

@Suppress("UNCHECKED_CAST")
class DarkModeBlackThemeStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {

    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                notifyChange(LineageSettings.Secure.BERRY_BLACK_THEME, PreferenceChangeReason.VALUE)
            }
        }

    override fun contains(key: String) = key == LineageSettings.Secure.BERRY_BLACK_THEME

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val value = LineageSettings.Secure.getInt(context.contentResolver, key, 0) != 0
        return value as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val boolValue = value as? Boolean ?: false
        LineageSettings.Secure.putInt(
            context.contentResolver,
            LineageSettings.Secure.BERRY_BLACK_THEME,
            if (boolValue) 1 else 0,
        )
        // Notify immediately for UI responsiveness
        notifyChange(key, PreferenceChangeReason.VALUE)
    }

    override fun onFirstObserverAdded() {
        context.contentResolver.registerContentObserver(
            LineageSettings.Secure.getUriFor(LineageSettings.Secure.BERRY_BLACK_THEME),
            false,
            settingsObserver,
        )
    }

    override fun onLastObserverRemoved() {
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    companion object {
        fun getReadPermissions() = Permissions.EMPTY

        fun getWritePermissions() = Permissions.allOf("lineageos.permission.WRITE_SECURE_SETTINGS")
    }
}
