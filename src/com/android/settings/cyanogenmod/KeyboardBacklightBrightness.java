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
import android.util.AttributeSet;

import com.android.settings.R;
import org.cyanogenmod.hardware.KeyboardBacklight;

public class KeyboardBacklightBrightness extends HWValueSliderPreference {
    private static final HardwareInterface HW_INTERFACE = new HardwareInterface() {
        @Override
        public int getMinValue() {
            return KeyboardBacklight.getMinBrightness();
        }
        @Override
        public int getMaxValue() {
            return KeyboardBacklight.getMaxBrightness();
        }
        @Override
        public int getCurrentValue() {
            return KeyboardBacklight.getCurBrightness();
        }
        @Override
        public int getDefaultValue() {
            return KeyboardBacklight.getDefaultBrightness();
        }
        @Override
        public int getWarningThreshold() {
            return -1;
        }
        @Override
        public boolean setValue(int value) {
            return KeyboardBacklight.setBrightness(value);
        }
        @Override
        public String getPreferenceName() {
            return "keyboard_backlight";
        }
    };

    public KeyboardBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs, HW_INTERFACE);

        if (!isSupported()) {
            return;
        }

        setDialogLayoutResource(R.layout.keyboard_backlight);
    }

    public static boolean isSupported() {
        try {
            return KeyboardBacklight.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework isn't installed
            return false;
        }
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }
        HWValueSliderPreference.restore(context, HW_INTERFACE);
    }
}
