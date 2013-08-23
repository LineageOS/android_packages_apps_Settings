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
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;

public class ButtonBacklightBrightness extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {
    private static final int DEFAULT_BUTTON_TIMEOUT = 5;

    private Window mWindow;

    private SeekBar mSeekBar;
    private CheckBox mCheckBox;
    private TextView mValue;

    private ViewGroup mTimeoutContainer;
    private SeekBar mTimeoutBar;
    private TextView mTimeoutValue;

    protected ContentResolver mResolver;
    private boolean mIsSingleValue;

    public ButtonBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs);

        mResolver = context.getContentResolver();

        mIsSingleValue = !context.getResources().getBoolean(
                com.android.internal.R.bool.config_deviceHasVariableButtonBrightness);

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

        mTimeoutContainer = (ViewGroup) view.findViewById(R.id.timeout_container);
        mTimeoutBar = (SeekBar) view.findViewById(R.id.timeout_seekbar);
        mTimeoutValue = (TextView) view.findViewById(R.id.timeout_value);
        mTimeoutBar.setMax(30);
        mTimeoutBar.setOnSeekBarChangeListener(this);
        mTimeoutBar.setProgress(getTimeout());

        if (mIsSingleValue) {
            view.findViewById(R.id.seekbar_container).setVisibility(View.GONE);
            mCheckBox = (CheckBox) view.findViewById(R.id.backlight_switch);
            mCheckBox.setText(getCheckBoxLabelResId());
            mCheckBox.setOnCheckedChangeListener(this);
            mCheckBox.setChecked(getBrightness() != 0);
        } else {
            view.findViewById(R.id.checkbox_container).setVisibility(View.GONE);
            mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
            mValue = (TextView) view.findViewById(R.id.value);

            TextView label = (TextView) view.findViewById(R.id.text);
            label.setText(getSeekBarLabelResId());

            mSeekBar.setMax(255);
            mSeekBar.setOnSeekBarChangeListener(this);
            mSeekBar.setProgress(getBrightness());
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
                mTimeoutBar.setProgress(DEFAULT_BUTTON_TIMEOUT);
                if (mIsSingleValue) {
                    mCheckBox.setChecked(true);
                } else {
                    mSeekBar.setProgress(255);
                }
            }
        });

        if (getDialog() != null) {
            mWindow = getDialog().getWindow();
        }
        updateBrightnessPreview();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            return;
        }

        applyTimeout(mTimeoutBar.getProgress());

        if (mIsSingleValue) {
            applyBrightness(mCheckBox.isChecked() ? 255 : 0);
        } else {
            applyBrightness(mSeekBar.getProgress());
        }
    }

    public static boolean isSupported(Context context) {
        final Resources res = context.getResources();
        boolean hasAnyKey = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) != 0;
        boolean hasBacklight = res.getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault) > 0;

        return hasAnyKey && hasBacklight;
    }

    private int getTimeout() {
        return Settings.System.getInt(mResolver,
                Settings.System.BUTTON_BACKLIGHT_TIMEOUT, DEFAULT_BUTTON_TIMEOUT * 1000) / 1000;
    }

    private void applyTimeout(int timeout) {
        Settings.System.putInt(mResolver,
                Settings.System.BUTTON_BACKLIGHT_TIMEOUT, timeout * 1000);
    }

    protected int getCheckBoxLabelResId() {
        return R.string.button_backlight_enabled;
    }

    protected int getSeekBarLabelResId() {
        return R.string.button_backlight_title;
    }

    protected int getBrightness() {
        return Settings.System.getInt(mResolver, Settings.System.BUTTON_BRIGHTNESS, 255);
    }

    protected void applyBrightness(int value) {
        Settings.System.putInt(mResolver, Settings.System.BUTTON_BRIGHTNESS, value);
    }

    private void updateBrightnessPreview() {
        if (mWindow != null) {
            LayoutParams params = mWindow.getAttributes();
            if (mIsSingleValue) {
                params.buttonBrightness = mCheckBox.isChecked() ? 255 : 0;
            } else {
                params.buttonBrightness = mSeekBar.getProgress();
            }
            mWindow.setAttributes(params);
        }
    }

    private void setTimeoutEnabled(boolean enabled) {
        int count = mTimeoutContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            mTimeoutContainer.getChildAt(i).setEnabled(enabled);
        }
    }

    /* Behaviors when it's a seekbar */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == mTimeoutBar) {
            if (progress == 0) {
                mTimeoutValue.setText(R.string.backlight_timeout_unlimited);
            } else {
                String time = getContext().getResources().getQuantityString(
                        R.plurals.backlight_timeout_time, progress, progress);
                mTimeoutValue.setText(time);
            }
        } else {
            updateBrightnessPreview();
            mValue.setText(String.format("%d%%", (int)((progress * 100) / 255)));
            setTimeoutEnabled(progress != 0);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    /* Behaviors when it's a plain checkbox */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateBrightnessPreview();
        setTimeoutEnabled(isChecked);
    }
}
