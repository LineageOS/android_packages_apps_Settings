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
import org.cyanogenmod.hardware.ButtonBacklight;

public class ButtonBacklightBrightness extends HWValueSliderPreference {
    private static final HardwareInterface HW_INTERFACE = new HardwareInterface() {
        @Override
        public int getMinValue() {
            return ButtonBacklight.getMinBrightness();
        }
        @Override
        public int getMaxValue() {
            return ButtonBacklight.getMaxBrightness();
        }
        @Override
        public int getCurrentValue() {
            return ButtonBacklight.getCurBrightness();
        }
        @Override
        public int getDefaultValue() {
            return ButtonBacklight.getDefaultBrightness();
        }
        @Override
        public int getWarningThreshold() {
            return -1;
        }
        @Override
        public boolean setValue(int value) {
            return ButtonBacklight.setBrightness(value);
        }
        @Override
        public String getPreferenceName() {
            return "button_backlight";
        }
    };

    public ButtonBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs, HW_INTERFACE);

        if (!isSupported()) {
            return;
        }

        setDialogLayoutResource(R.layout.button_backlight);
    }

    public static boolean isSupported() {
        try {
            return ButtonBacklight.isSupported();
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
