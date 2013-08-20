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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;
import org.cyanogenmod.hardware.DisplayGammaCalibration;

/**
 * Special preference type that allows configuration of Gamma settings
 */
public class DisplayGamma extends DialogPreference {
    private static final String TAG = "GammaCalibration";

    private static final int[] BAR_COLORS = new int[] {
        R.string.color_red_title,
        R.string.color_green_title,
        R.string.color_blue_title
    };

    private GammaSeekBar[][] mSeekBars;

    private String[][] mCurrentColors;
    private String[] mOriginalColors;
    private int mNumberOfControls;

    public DisplayGamma(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isSupported()) {
            return;
        }

        mNumberOfControls = DisplayGammaCalibration.getNumberOfControls();

        mSeekBars = new GammaSeekBar[mNumberOfControls][BAR_COLORS.length];

        mOriginalColors = new String[mNumberOfControls];
        mCurrentColors = new String[mNumberOfControls][];

        setDialogLayoutResource(R.layout.display_gamma_calibration);
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

        final ViewGroup container = (ViewGroup) view.findViewById(R.id.gamma_container);
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final SharedPreferences prefs = getSharedPreferences();

        // Create multiple sets of seekbars, depending on the
        // number of controls the device has
        for (int index = 0; index < mNumberOfControls; index++) {
            mOriginalColors[index] = DisplayGammaCalibration.getCurGamma(index);
            mCurrentColors[index] = mOriginalColors[index].split(" ");

            final String defaultKey = "display_gamma_default_" + index;
            if (!prefs.contains(defaultKey)) {
                prefs.edit().putString(defaultKey, mOriginalColors[index]).commit();
            }

            for (int color = 0; color < BAR_COLORS.length; color++) {
                ViewGroup item = (ViewGroup) inflater.inflate(
                        R.layout.display_gamma_calibration_item, container, false);

                GammaSeekBar bar = new GammaSeekBar(index, color, item);
                bar.setGamma(Integer.valueOf(mCurrentColors[index][color]));
                mSeekBars[index][color] = bar;
                // make sure to add the seekbar group to the container _after_
                // creating GammaSeekBar, so that GammaSeekBar has a chance to
                // get the correct subviews without getting confused by duplicate IDs
                container.addView(item);
            }
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int index = 0; index < mSeekBars.length; index++) {
                    final GammaSeekBar[] bars = mSeekBars[index];
                    final SharedPreferences prefs = getSharedPreferences();
                    final String defaultKey = "display_gamma_default_" + index;
                    // this key is guaranteed to be present, as we have
                    // created it in onBindDialogView()
                    final String[] defaultColors = prefs.getString(defaultKey, null).split(" ");

                    for (int color = 0; color < BAR_COLORS.length; color++) {
                        mSeekBars[index][color].setGamma(Integer.valueOf(defaultColors[color]));
                    }
                }
            }
       });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Editor editor = getEditor();
            for (int i = 0; i < mNumberOfControls; i++) {
                editor.putString("display_gamma_" + i, DisplayGammaCalibration.getCurGamma(i));
            }
            editor.commit();
        } else {
            for (int i = 0; i < mNumberOfControls; i++) {
                DisplayGammaCalibration.setGamma(i, mOriginalColors[i]);
            }
        }
    }

    public static boolean isSupported() {
        try {
            return DisplayGammaCalibration.isSupported();
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
        for (int i = 0; i < DisplayGammaCalibration.getNumberOfControls(); i++) {
            final String values = prefs.getString("display_gamma_" + i, null);
            if (values != null) {
                DisplayGammaCalibration.setGamma(i, values);
            }
        }
    }

    private class GammaSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mControlIndex;
        private int mColorIndex;
        private int mOriginal;
        private SeekBar mSeekBar;
        private TextView mValue;

        public GammaSeekBar(int controlIndex, int colorIndex, ViewGroup container) {
            mControlIndex = controlIndex;
            mColorIndex = colorIndex;

            mValue = (TextView) container.findViewById(R.id.color_value);
            mSeekBar = (SeekBar) container.findViewById(R.id.color_seekbar);

            TextView label = (TextView) container.findViewById(R.id.color_text);
            CharSequence color = container.getContext().getString(BAR_COLORS[colorIndex]);
            if (mNumberOfControls == 1) {
                label.setText(color);
            } else {
                label.setText(color + " " + (controlIndex + 1));
            }

            int min = DisplayGammaCalibration.getMinValue();
            mSeekBar.setMax(DisplayGammaCalibration.getMaxValue());
            mSeekBar.setProgress(min);
            mValue.setText(String.valueOf(min));

            // this must be done last, we don't want to apply our initial value to the hardware
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        public void setGamma(int gamma) {
            mSeekBar.setProgress(gamma);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mCurrentColors[mControlIndex][mColorIndex] = String.valueOf(progress);
            DisplayGammaCalibration.setGamma(mControlIndex,
                    TextUtils.join(" ", mCurrentColors[mControlIndex]));
            mValue.setText(String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }
    }
}
