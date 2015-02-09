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

package com.android.settings.hardware;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.CmHardwareManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settings.R;

public class VibratorIntensity extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private static final String PREF_NAME = "vibrator_intensity";
    private SeekBar mSeekBar;
    private TextView mValue;
    private TextView mWarning;
    private int[] mOriginalValue;
    private CmHardwareManager mCmHardwareManager;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    public VibratorIntensity(Context context, AttributeSet attrs) {
        super(context, attrs);

        mCmHardwareManager = (CmHardwareManager) context.getSystemService(Context.CMHW_SERVICE);

        if (!mCmHardwareManager.isSupported(CmHardwareManager.FEATURE_VIBRATOR)) {
            return;
        }

        setDialogLayoutResource(R.layout.vibrator_intensity);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.settings_reset_button,
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

        // Read the current value in case user wants to dismiss his changes
        mOriginalValue = mCmHardwareManager.getVibratorIntensity();

        int warningThreshold = mOriginalValue[CmHardwareManager.VIBRATOR_WARNING_INDEX];
        int min = mOriginalValue[CmHardwareManager.VIBRATOR_MIN_INDEX];
        int max = mOriginalValue[CmHardwareManager.VIBRATOR_MIN_INDEX];
        if (warningThreshold > 0) {
            String message = getContext().getResources().getString(
                    R.string.vibrator_warning, intensityToPercent(min, max, warningThreshold));
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

        // Restore percent value from SharedPreferences object
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int defaultValue = intensityToPercent(min, max, mOriginalValue[CmHardwareManager.VIBRATOR_DEFAULT_INDEX]);
        int percent = prefs.getInt(PREF_NAME, defaultValue);

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
                mSeekBar.setProgress(intensityToPercent(mOriginalValue[CmHardwareManager.VIBRATOR_MIN_INDEX],
                    mOriginalValue[CmHardwareManager.VIBRATOR_MAX_INDEX],
                    mOriginalValue[CmHardwareManager.VIBRATOR_DEFAULT_INDEX]));
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Store percent value in SharedPreferences object
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.edit().putInt(PREF_NAME, mSeekBar.getProgress()).commit();
        } else {
            mCmHardwareManager.setVibratorIntensity(mOriginalValue[CmHardwareManager.VIBRATOR_INTENSITY_INDEX]);
        }
    }

    public static void restore(Context context) {
        CmHardwareManager cmHardwareManager = (CmHardwareManager) context.getSystemService(Context.CMHW_SERVICE);
        if (!cmHardwareManager.isSupported(CmHardwareManager.FEATURE_VIBRATOR)) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int[] vibrator = cmHardwareManager.getVibratorIntensity();
        int defaultValue = intensityToPercent(vibrator[CmHardwareManager.VIBRATOR_MIN_INDEX],
            vibrator[CmHardwareManager.VIBRATOR_MAX_INDEX], vibrator[CmHardwareManager.VIBRATOR_DEFAULT_INDEX]);
        int percent = prefs.getInt(PREF_NAME, defaultValue);

        cmHardwareManager.setVibratorIntensity(percentToIntensity(vibrator[CmHardwareManager.VIBRATOR_MIN_INDEX],
            vibrator[CmHardwareManager.VIBRATOR_MAX_INDEX], percent));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int warningThreshold = mOriginalValue[CmHardwareManager.VIBRATOR_WARNING_INDEX];
        int min = mOriginalValue[CmHardwareManager.VIBRATOR_MIN_INDEX];
        int max = mOriginalValue[CmHardwareManager.VIBRATOR_MAX_INDEX];
        boolean shouldWarn =
                warningThreshold > 0 && progress >= intensityToPercent(min, max, warningThreshold);

        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }

        mCmHardwareManager.setVibratorIntensity(percentToIntensity(mOriginalValue[CmHardwareManager.VIBRATOR_MIN_INDEX],
            mOriginalValue[CmHardwareManager.VIBRATOR_MAX_INDEX], progress));
        mValue.setText(String.format("%d%%", progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    private static int intensityToPercent(double minValue, double maxValue, int value) {
        double percent = (value - minValue) * (100 / (maxValue - minValue));

        if (percent > 100) {
            percent = 100;
        } else if (percent < 0) {
            percent = 0;
        }

        return (int) percent;
    }

    private static int percentToIntensity(int minValue, int maxValue, int percent) {
        int value = Math.round((((maxValue - minValue) * percent) / 100) + minValue);

        if (value > maxValue) {
            value = maxValue;
        } else if (value < minValue) {
            value = minValue;
        }

        return value;
    }
}
