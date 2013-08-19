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
import android.graphics.Color;
import android.graphics.LightingColorFilter;
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

public abstract class HWValueSliderPreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private TextView mValue;
    private TextView mWarning;
    private int mOriginalValue;

    private HardwareInterface mHw;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    protected interface HardwareInterface {
        int getMinValue();
        int getMaxValue();
        int getCurrentValue();
        int getDefaultValue();
        int getWarningThreshold();
        boolean setValue(int value);
        String getPreferenceName();
    }

    public HWValueSliderPreference(Context context, AttributeSet attrs, HardwareInterface hw) {
        super(context, attrs);
        mHw = hw;
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

        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mValue = (TextView) view.findViewById(R.id.value);
        mWarning = (TextView) view.findViewById(R.id.warning_text);

        int warningThreshold = mHw.getWarningThreshold();
        if (warningThreshold > 0) {
            String message = getContext().getResources().getString(
                    R.string.vibrator_warning, hwValueToPercent(warningThreshold, mHw));
            mWarning.setText(message);
        } else if (mWarning != null) {
            mWarning.setVisibility(View.GONE);
        }

        Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        // Read the current value in case user wants to dismiss his changes
        mOriginalValue = mHw.getCurrentValue();

        // Restore percent value from SharedPreferences object
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        int percent = settings.getInt(mHw.getPreferenceName(),
                hwValueToPercent(mHw.getDefaultValue(), mHw));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(percent);
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
                mSeekBar.setProgress(hwValueToPercent(mHw.getDefaultValue(), mHw));
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
            editor.putInt(mHw.getPreferenceName(), mSeekBar.getProgress());
            editor.commit();
        } else {
            mHw.setValue(mOriginalValue);
        }
    }

    protected static void restore(Context context, HardwareInterface hw) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int defaultValue = hwValueToPercent(hw.getDefaultValue(), hw);
        int percent = prefs.getInt(hw.getPreferenceName(), defaultValue);

        hw.setValue(percentToHwValue(percent, hw));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int warningThreshold = mHw.getWarningThreshold();
        boolean shouldWarn =
                warningThreshold > 0 && progress >= hwValueToPercent(warningThreshold, mHw);

        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }

        mHw.setValue(percentToHwValue(progress, mHw));
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

    private static int hwValueToPercent(int value, HardwareInterface hw) {
        double maxValue = hw.getMaxValue();
        double minValue = hw.getMinValue();
        double percent = (value - minValue) * (100 / (maxValue - minValue));

        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 0;
        }

        return (int) percent;
    }

    private static int percentToHwValue(int percent, HardwareInterface hw) {
        int maxValue = hw.getMaxValue();
        int minValue = hw.getMinValue();
        int value = Math.round((((maxValue - minValue) * percent) / 100) + minValue);

        if (value > maxValue) {
            value = maxValue;
        } else if (value < minValue) {
            value = minValue;
        }

        return value;
    }
}
