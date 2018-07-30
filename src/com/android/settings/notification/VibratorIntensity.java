/**
 * Copyright (C) 2013-2016 The CyanogenMod Project
 * Copyright (C) 2018 The LineageOS Project
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settingslib.CustomDialogPreference;
import com.android.settings.R;

import lineageos.providers.LineageSettings;
import vendor.lineage.vibrator.V1_0.IVibrator;

import java.util.NoSuchElementException;

public class VibratorIntensity extends CustomDialogPreference
        implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VibratorIntensity";

    private int mOriginalValue;
    private int mDefaultValue;

    private SeekBar mSeekBar;
    private TextView mValueText;
    private TextView mWarningText;

    private Drawable mProgressDrawable;
    private Drawable mProgressThumb;
    private LightingColorFilter mRedFilter;

    private IVibrator mServer;

    public VibratorIntensity(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.vibrator_intensity);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder,
            final DialogInterface.OnClickListener listener) {
        builder.setNeutralButton(R.string.vibrator_intensity_dialog_reset, null);
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);

        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mValueText = (TextView) view.findViewById(R.id.value);
        mWarningText = (TextView) view.findViewById(R.id.warning_text);

        try {
            mServer = IVibrator.getService();

            // Read the current value in case user wants to dismiss his changes
            mDefaultValue = mServer.getDefaultIntensity();
            mOriginalValue = LineageSettings.Secure.getInt(getContext().getContentResolver(),
                    LineageSettings.Secure.VIBRATOR_INTENSITY, mDefaultValue);

        } catch (NoSuchElementException | RemoteException e) {
            throw new RuntimeException("Service not found!");
        }

        if (mDefaultValue > 0 && mDefaultValue < 255) {
            final String message = getContext().getResources().getString(
                    R.string.vibrator_intensity_dialog_warning,
                    intensityToPercent(mDefaultValue));
            mWarningText.setText(message);
        } else {
            mWarningText.setVisibility(View.GONE);
        }

        final Drawable progressDrawable = mSeekBar.getProgressDrawable();
        if (progressDrawable instanceof LayerDrawable) {
            LayerDrawable ld = (LayerDrawable) progressDrawable;
            mProgressDrawable = ld.findDrawableByLayerId(android.R.id.progress);
        }
        mProgressThumb = mSeekBar.getThumb();
        mRedFilter = new LightingColorFilter(Color.BLACK,
                getContext().getResources().getColor(android.R.color.holo_red_light));

        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(255);
        mSeekBar.setProgress(mOriginalValue);
    }

    @Override
    protected boolean onDismissDialog(final DialogInterface dialog, final int which) {
        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            mSeekBar.setProgress(mDefaultValue);
            testVibrationIntensity(mDefaultValue);
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
        } else {
            setVibratorIntensity(mOriginalValue);
        }
    }

    @Override
    public void onProgressChanged(
                final SeekBar seekBar, final int progress, final boolean fromUser) {
        final int intensity = progress;
        final boolean shouldWarn = mDefaultValue > 0 && mDefaultValue < 255
                && intensity >= mDefaultValue;

        if (mProgressDrawable != null) {
            mProgressDrawable.setColorFilter(shouldWarn ? mRedFilter : null);
        }
        if (mProgressThumb != null) {
            mProgressThumb.setColorFilter(shouldWarn ? mRedFilter : null);
        }

        mValueText.setText(
                String.format("%d%%", intensityToPercent(intensity)));
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        // Do nothing
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        testVibrationIntensity(seekBar.getProgress());
    }

    private void setVibratorIntensity(final int intensity) {
        try {
            mServer.setIntensity((byte) intensity);
        } catch (RemoteException ignored) {
        }
    }

    private void testVibrationIntensity(final int intensity) {
        setVibratorIntensity(intensity);
        final Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        vib.vibrate(200);
    }

    private static int intensityToPercent(int value) {
        if (value > 255) {
            value = 255;
        } else if (value < 0) {
            value = 0;
        }

        return Math.round(value * 100.f / 255);
    }
}
