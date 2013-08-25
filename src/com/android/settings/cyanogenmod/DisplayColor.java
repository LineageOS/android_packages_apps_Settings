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
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import com.android.settings.R;

import org.cyanogenmod.hardware.DisplayColorCalibration;

/**
 * Special preference type that allows configuration of Color settings
 */
public class DisplayColor extends DialogPreference {
    private static final String TAG = "ColorCalibration";

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_ID = new int[] {
        R.id.color_red_seekbar,
        R.id.color_green_seekbar,
        R.id.color_blue_seekbar
    };

    private static final int[] SEEKBAR_VALUE_ID = new int[] {
        R.id.color_red_value,
        R.id.color_green_value,
        R.id.color_blue_value
    };

    private ColorSeekBar[] mSeekBars = new ColorSeekBar[SEEKBAR_ID.length];
    private String[] mCurrentColors;
    private String mOriginalColors;

    public DisplayColor(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isSupported()) {
            return;
        }

        setDialogLayoutResource(R.layout.display_color_calibration);
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

        mOriginalColors = DisplayColorCalibration.getCurColors();
        mCurrentColors = mOriginalColors.split(" ");

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            SeekBar seekBar = (SeekBar) view.findViewById(SEEKBAR_ID[i]);
            TextView value = (TextView) view.findViewById(SEEKBAR_VALUE_ID[i]);
            mSeekBars[i] = new ColorSeekBar(seekBar, value, i, Integer.valueOf(mCurrentColors[i]));
        }
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
                for (ColorSeekBar csb : mSeekBars) {
                    csb.mSeekBar.setProgress(DisplayColorCalibration.getMaxValue());
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Editor editor = getEditor();
            editor.putString("display_color_calibration", DisplayColorCalibration.getCurColors());
            editor.commit();
        } else {
            DisplayColorCalibration.setColors(mOriginalColors);
        }
    }

    public static boolean isSupported() {
        try {
            return DisplayColorCalibration.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework isn't installed
            return false;
        }
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String value = prefs.getString("display_color_calibration", null);

        if (value != null) {
            DisplayColorCalibration.setColors(value);
        }
    }

    private class ColorSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndex;
        private int mOriginal;
        private SeekBar mSeekBar;
        private TextView mValue;

        public ColorSeekBar(SeekBar seekBar, TextView value, int index, int original) {
            mSeekBar = seekBar;
            mValue = value;
            mIndex = index;
            mOriginal = original;

            mSeekBar.setMax(DisplayColorCalibration.getMaxValue());
            mSeekBar.setOnSeekBarChangeListener(this);
            mSeekBar.setProgress(mOriginal);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mCurrentColors[mIndex] = String.valueOf(progress);
            DisplayColorCalibration.setColors(TextUtils.join(" ", mCurrentColors));

            int min = DisplayColorCalibration.getMinValue();
            int max = DisplayColorCalibration.getMaxValue();
            int percent = Math.round(100F * progress / (max - min));
            mValue.setText(String.format("%d%%", percent));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }
    }
}
