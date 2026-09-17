/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility.textreading.ui

import android.Manifest
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.DisplayInfo
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TooltipSliderPreference
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_BUTTON_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_SLIDER_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.MIN_COMMIT_DELAY
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.display.DisplayDensityUtils
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import com.google.android.material.slider.Slider
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DisplaySizePreference(context: Context, @EntryPoint private val entryPoint: Int) :
    IntRangeValuePreference,
    SliderPreferenceBinding,
    Slider.OnSliderTouchListener,
    Slider.OnChangeListener,
    PreferenceLifecycleProvider {

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(Manifest.permission.WRITE_SECURE_SETTINGS)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int {
        return ReadWritePermit.ALLOW
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    private val displaySizeDataStore by lazy {
        if (context.resources.getBoolean(R.bool.config_independent_display_dpi_setting)) {
            DisplaySizeDataStore(
                context = context,
                entryPoint = entryPoint,
                displayDensityUtils = DisplayDensityUtils(context)
                    { info: DisplayInfo -> info.displayId == Display.DEFAULT_DISPLAY }
            )
        } else {
            DisplaySizeDataStore(context = context, entryPoint = entryPoint)
        }
    }

    private val displaySizes by lazy { displaySizeDataStore.displaySizeData.value.values }
    private var isDraggingSlider = false

    private val _displaySizePreview by lazy {
        MutableStateFlow(displaySizeDataStore.displaySizeData.value)
    }

    /**
     * [displaySizePreview] is the temporary display size while the user is dragging and haven't
     * commit the change. This is useful when trying to display preview of the size changes.
     */
    val displaySizePreview by lazy { _displaySizePreview.asStateFlow() }

    private val debounceCommitController by lazy {
        DebounceConfigurationChangeCommitController(minCommitDelay = MIN_COMMIT_DELAY)
    }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.screen_zoom_title

    override val summary: Int
        get() = R.string.screen_zoom_short_summary

    override val keywords: Int
        get() = R.string.keywords_display_size

    override fun createWidget(context: Context): SliderPreference {
        val widget =
            if (context.isInSetupWizard()) {
                // Use TooltipSliderPreference in setup wizard for the temp fix for b/421323125.
                // TODO(b/407080818): Remove the setup wizard branch once we decouple SUW and
                // Settings, since the slider doesn't need to show a quick settings tooltip
                TooltipSliderPreference(context)
            } else {
                SliderPreference(context)
            }
        widget.apply {
            setIconStart(R.drawable.ic_remove_24dp)
            setIconStartContentDescription(R.string.screen_zoom_make_smaller_desc)
            setIconEnd(R.drawable.ic_add_24dp)
            setIconEndContentDescription(R.string.screen_zoom_make_larger_desc)
            setTickVisible(true)
            setDefaultValue(_displaySizePreview.value.currentIndex)
            setExtraChangeListener(this@DisplaySizePreference)
            setExtraTouchListener(this@DisplaySizePreference)
        }
        return widget
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // The PreferenceMetadata is persistent to allow GET/SET api to access the storage.
        // Set the preference widget to non-persistent to prevent it trying to save the value to
        // datastore while the user is dragging, or when we want to have some delay to show the
        // preview before committing the changes.
        preference as SliderPreference
        preference.isPersistent = false
        preference.value = _displaySizePreview.value.currentIndex
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        // This is needed to prevent slider value gets overwrites by [View#onRestoreInstanceState].
        // When the display size changed, it triggers the configuration changes. The
        // SliderPreference widget is not a persistent preference, hence when the data is
        // changed outside of Settings app while the display size slider is visible, the Slider
        // widget won't save the correct index when
        // [View#onSaveInstanceState] is called.
        val preference = context.findPreference<SliderPreference>(KEY)
        if (context.resources.getBoolean(R.bool.config_independent_display_dpi_setting)) {
            preference?.min = getMinValue(context)
            preference?.max = getMaxValue(context)
        }
        preference?.value = _displaySizePreview.value.currentIndex
    }

    override fun getIncrementStep(context: Context): Int {
        return 1
    }

    override fun getMinValue(context: Context): Int {
        return 0
    }

    override fun getMaxValue(context: Context): Int {
        return displaySizes.size - 1
    }

    override fun storage(context: Context): KeyValueStore {
        return displaySizeDataStore
    }

    override fun onStartTrackingTouch(slider: Slider) {
        isDraggingSlider = true
    }

    override fun onStopTrackingTouch(slider: Slider) {
        isDraggingSlider = false
        // call data store to save the value
        commitChange(CHANGE_BY_SLIDER_DELAY, slider.value.toInt())
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        _displaySizePreview.value = _displaySizePreview.value.copy(currentIndex = value.toInt())

        if (!isDraggingSlider) {
            // if not dragging call datastore to save the value
            commitChange(CHANGE_BY_BUTTON_DELAY, value.toInt())
        }
    }

    @VisibleForTesting
    internal fun commitChange(delay: Duration, index: Int) {
        debounceCommitController.commitDelayed(delay) { displaySizeDataStore.setInt(KEY, index) }
    }

    companion object {
        const val KEY = "display_size"
    }
}
