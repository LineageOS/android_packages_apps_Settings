/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.android.settings.livedisplay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.IntervalSeekBar;
import com.android.settings.R;

/**
 * Preference for selection of color temperature range for LiveDisplay
 */
public class DisplayTemperature extends DialogPreference {
    private static final String TAG = "DisplayTemperature";

    private final Context mContext;

    private ColorTemperatureSeekBar mDayTemperature;
    private ColorTemperatureSeekBar mNightTemperature;

    private float mOriginalDayTemperature;
    private float mOriginalNightTemperature;

    private final float mDefaultDayTemperature;
    private final float mDefaultNightTemperature;

    public DisplayTemperature(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mDefaultDayTemperature = Float.valueOf(mContext.getResources().getString(
                com.android.internal.R.string.config_dayColorTemperature));
        mDefaultNightTemperature = Float.valueOf(mContext.getResources().getString(
                com.android.internal.R.string.config_nightColorTemperature));

        setDialogLayoutResource(R.layout.display_temperature);
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

        mOriginalDayTemperature = Settings.System.getFloatForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_DAY,
                mDefaultDayTemperature,
                UserHandle.USER_CURRENT);
        mOriginalNightTemperature = Settings.System.getFloatForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_NIGHT,
                mDefaultNightTemperature,
                UserHandle.USER_CURRENT);

        IntervalSeekBar day = (IntervalSeekBar) view.findViewById(R.id.day_temperature_seekbar);
        TextView dayText = (TextView) view.findViewById(R.id.day_temperature_value);
        mDayTemperature = new ColorTemperatureSeekBar(day, dayText);

        IntervalSeekBar night = (IntervalSeekBar) view.findViewById(R.id.night_temperature_seekbar);
        TextView nightText = (TextView) view.findViewById(R.id.night_temperature_value);
        mNightTemperature = new ColorTemperatureSeekBar(night, nightText);

        mDayTemperature.setProgressFloat(mOriginalDayTemperature);
        mNightTemperature.setProgressFloat(mOriginalNightTemperature);
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
                mDayTemperature.setProgressFloat(mDefaultDayTemperature);
                mNightTemperature.setProgressFloat(mDefaultNightTemperature);
                updateTemperature(true);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        updateTemperature(positiveResult);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.originalDayTemperature = mOriginalDayTemperature;
        myState.originalNightTemperature = mOriginalNightTemperature;
        myState.currentDayTemperature = mDayTemperature.getProgressFloat();
        myState.currentNightTemperature = mNightTemperature.getProgressFloat();

        // Restore the old state when the activity or dialog is being paused
        updateTemperature(false);

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
 
        mOriginalDayTemperature = myState.originalDayTemperature;
        mOriginalNightTemperature = myState.originalNightTemperature;
        mDayTemperature.setProgressFloat(myState.currentDayTemperature);
        mNightTemperature.setProgressFloat(myState.currentNightTemperature);;

        updateTemperature(true);
    }

    private static class SavedState extends BaseSavedState {
        float originalDayTemperature;
        float originalNightTemperature;
        float currentDayTemperature;
        float currentNightTemperature;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            originalDayTemperature = source.readFloat();
            originalNightTemperature = source.readFloat();
            currentDayTemperature = source.readFloat();
            currentNightTemperature = source.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloat(originalDayTemperature);
            dest.writeFloat(originalNightTemperature);
            dest.writeFloat(currentDayTemperature);
            dest.writeFloat(currentNightTemperature);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void updateTemperature(boolean accept) {
        float day = accept ? mDayTemperature.getProgressFloat() : mOriginalDayTemperature;
        float night = accept ? mNightTemperature.getProgressFloat() : mOriginalNightTemperature;
        callChangeListener(new Float[] { day, night });

        Settings.System.putFloatForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_DAY, day,
                UserHandle.USER_CURRENT);

        Settings.System.putFloatForUser(mContext.getContentResolver(),
                Settings.System.DISPLAY_TEMPERATURE_NIGHT, night,
                UserHandle.USER_CURRENT);
    }

    private class ColorTemperatureSeekBar implements SeekBar.OnSeekBarChangeListener {
        private final IntervalSeekBar mSeekBar;
        private final TextView mValue;

        public ColorTemperatureSeekBar(IntervalSeekBar seekBar, TextView value) {
            mSeekBar = seekBar;
            mValue = value;

            mSeekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            IntervalSeekBar isb = (IntervalSeekBar)seekBar;
            float fp = isb.getProgressFloat();
            if (fromUser) {
                updateTemperature(true);
            }
            mValue.setText(String.format("%dK", (int)fp));
        }

        public float getProgressFloat() {
            return mSeekBar.getProgressFloat();
        }

        public void setProgressFloat(float progress) {
            mSeekBar.setProgressFloat(progress);
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
