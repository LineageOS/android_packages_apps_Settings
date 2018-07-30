/*
 * Copyright (C) 2013-2016 The CyanogenMod Project
 * Copyright (C) 2018-2019 The LineageOS Project
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

package com.android.settings.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settingslib.CustomDialogPreference;
import com.android.settings.R;

import lineageos.providers.LineageSettings;

public class VibratorIntensity extends CustomDialogPreference
        implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VibratorIntensity";

    private static final int MAX_VIBRATION_INTENSITY = 255;

    private int mOriginalValue;

    private SeekBar mSeekBar;
    private TextView mValueText;

    public VibratorIntensity(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.vibrator_intensity);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder,
            final DialogInterface.OnClickListener listener) {
        builder.setNeutralButton(R.string.vibrator_intensity_dialog_reset, null);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mValueText = (TextView) view.findViewById(R.id.value);

        mOriginalValue = LineageSettings.Secure.getInt(getContext().getContentResolver(),
                LineageSettings.Secure.VIBRATOR_INTENSITY, MAX_VIBRATION_INTENSITY);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(MAX_VIBRATION_INTENSITY);
        mSeekBar.setProgress(mOriginalValue);
    }

    @Override
    protected boolean onDismissDialog(final DialogInterface dialog, final int which) {
        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            mSeekBar.setProgress(MAX_VIBRATION_INTENSITY);
            testVibratorIntensity(MAX_VIBRATION_INTENSITY);
            return false;
        }
        return true;
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            final int intensity = mSeekBar.getProgress();
            LineageSettings.Secure.putInt(getContext().getContentResolver(),
                    LineageSettings.Secure.VIBRATOR_INTENSITY, intensity);
        }
    }

    @Override
    public void onProgressChanged(
                final SeekBar seekBar, final int progress, final boolean fromUser) {
        mValueText.setText(
                String.format("%d%%", intensityToPercent(progress)));
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        // Do nothing
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        testVibratorIntensity(seekBar.getProgress());
    }

    private void testVibratorIntensity(final int intensity) {
        final Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    private static int intensityToPercent(int value) {
        if (value > MAX_VIBRATION_INTENSITY) {
            value = MAX_VIBRATION_INTENSITY;
        } else if (value < 0) {
            value = 0;
        }

        return Math.round(value * 100.f / MAX_VIBRATION_INTENSITY);
    }
}
