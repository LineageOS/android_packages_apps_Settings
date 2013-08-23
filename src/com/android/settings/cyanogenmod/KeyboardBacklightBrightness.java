/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;

import com.android.settings.R;

public class KeyboardBacklightBrightness extends ButtonBacklightBrightness {
    public KeyboardBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int getCheckBoxLabelResId() {
        return R.string.keyboard_backlight_enabled;
    }

    @Override
    protected int getSeekBarLabelResId() {
        return R.string.keyboard_backlight_title;
    }

    @Override
    protected int getBrightness() {
        return Settings.System.getInt(mResolver, Settings.System.KEYBOARD_BRIGHTNESS, 255);
    }

    protected void applyBrightness(int value) {
        Settings.System.putInt(mResolver, Settings.System.KEYBOARD_BRIGHTNESS, value);
    }

    public static boolean isSupported(Context context) {
        return context.getResources().getInteger(
                com.android.internal.R.integer.config_keyboardBrightnessSettingDefault) > 0;
    }
}
