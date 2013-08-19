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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settings.R;

import org.cyanogenmod.hardware.ButtonBacklight;

import java.lang.Math;
import java.text.DecimalFormat;

public class ButtonBacklightBrightness extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ButtonBacklight";

    private SeekBar mSeekBar;
    private TextView mValue;
    private TextView mWarning;
    private int mOriginalValue;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;

    public ButtonBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isSupported()) {
            return;
        }

        setDialogLayoutResource(R.layout.button_backlight);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.auto_brightness_reset_button,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(R.id.button_backlight_seekbar);
        mValue = (TextView) view.findViewById(R.id.button_backlight_value);

        Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();

        // Read the current value in case user wants to dismiss his changes
        mOriginalValue = ButtonBacklight.getCurBrightness();

        // Restore percent value from SharedPreferences object
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        int percent = settings.getInt("button_backlight",
                strengthToPercent(ButtonBacklight.getDefaultBrightness()));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(Integer.valueOf(percent));
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setProgress(strengthToPercent(ButtonBacklight.getDefaultBrightness()));
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Store percent value in SharedPreferences object
            SharedPreferences.Editor editor =
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putInt("button_backlight", mSeekBar.getProgress());
            editor.commit();
        } else {
            ButtonBacklight.setBrightness(mOriginalValue);
        }
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

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int strength = percentToStrength(settings.getInt(
                "button_backlight", strengthToPercent(ButtonBacklight.getDefaultBrightness())));

        Log.d(TAG, "Restoring button backlight setting: " + strength);
        ButtonBacklight.setBrightness(strength);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ButtonBacklight.setBrightness(percentToStrength(progress));
        mValue.setText(String.format("%d%%", progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    /**
    * Convert brightness to percent
    */
    public static int strengthToPercent(int strength) {
        double maxValue = ButtonBacklight.getMaxBrightness();
        double minValue = ButtonBacklight.getMinBrightness();
        double percent = (strength - minValue) * (100 / (maxValue - minValue));

        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 0;
        }

        return (int) percent;
    }

    /**
    * Convert percent to brightness
    */
    public static int percentToStrength(int percent) {
        int maxValue = ButtonBacklight.getMaxBrightness();
        int minValue = ButtonBacklight.getMinBrightness();
        int strength = Math.round((((maxValue - minValue) * percent) / 100) + minValue);

        if (strength > maxValue) {
            strength = maxValue;
        } else if (strength < minValue) {
            strength = minValue;
        }

        return strength;
    }
}
