/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 */
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.util.Log;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Bundle;

public class WarnedPreference extends Preference implements RadioGroup.OnCheckedChangeListener
        , RadioButton.OnClickListener {
    private Context mContext;
    private AlertDialog.Builder mBuilder;
    private AlertDialog mDialogFontChoose;
    private View mFontDialogView;
    private SharedPreferences mSharedPreferenceFontChoose;
    private SharedPreferences.Editor mSpFontEditor;
    private OnPreferenceValueChangeListener mPreferenceValueChangeListener;
    private OnPreferenceClickListener mOnPreferenceClickListener;
    private static final String FILE_FONT_CHOOSE = "fontsize_choose";
    private static final String KEY_FONT_CHOOSE_ID = "radioButton_checkedId";
    private static final String KEY_FONT_CHOOSE_PRE_ID = "radioButton_checked_pre_Id";

    public void setPreferenceValueChangeListener(OnPreferenceValueChangeListener
                                                         preferenceValueChangeListener) {
        this.mPreferenceValueChangeListener = preferenceValueChangeListener;
    }

    public void setOnPreferenceClickListener(OnPreferenceClickListener
                                                         OnPreferenceClickListener) {
        this.mOnPreferenceClickListener = OnPreferenceClickListener;
    }

    @Override
    public void onClick(View v) {
        if (v != null)
            mDialogFontChoose.dismiss();
    }

    public interface OnPreferenceValueChangeListener {
        void onPreferenceValueChange(Preference preference, Object newValue);
    }

    public interface OnPreferenceClickListener {
        void onPreferenceClick(Preference preference);
    }

    public WarnedPreference(Context context) {
        this(context, null);
    }

    public WarnedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mFontDialogView = inflater.inflate(R.layout.font_dialog, null);
        mSharedPreferenceFontChoose = mContext.getSharedPreferences(FILE_FONT_CHOOSE,
                Activity.MODE_PRIVATE);
    }

    @Override
    protected void onClick() {
        if (mOnPreferenceClickListener != null) {
            mOnPreferenceClickListener.onPreferenceClick(this);
        }
        showDialog(null);
    }


    protected void showDialog(Bundle state) {

        RadioGroup radioGroup = (RadioGroup) mFontDialogView.findViewById(R.id.font_choose);
        radioGroup.setOnCheckedChangeListener(this);

        mBuilder = new AlertDialog.Builder(mContext);
        int rbCheckedId = mSharedPreferenceFontChoose.getInt(KEY_FONT_CHOOSE_ID, 0);
        if (rbCheckedId != 0) {
            RadioButton rbChecked = (RadioButton) mFontDialogView.findViewById(rbCheckedId);
            rbChecked.setChecked(true);
            rbChecked.setOnClickListener(this);
        } else {
            RadioButton radioButton = (RadioButton) mFontDialogView.findViewById(R.id.rb_medium);
            radioButton.setChecked(true);
            radioButton.setOnClickListener(this);
        }
        mBuilder.setView(mFontDialogView);
        mBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if (mDialogFontChoose == null) {
            mDialogFontChoose = mBuilder.create();
        }

        if (state != null) {
            mDialogFontChoose.onRestoreInstanceState(state);
            mDialogFontChoose.show();
        }
    }


    public void click() {
        super.onClick();
    }

    public AlertDialog getDialog() {
        return mDialogFontChoose;
    }

    public String getWarnedPreferenceSummary() {
        RadioButton rbSave = (RadioButton) mFontDialogView.
                findViewById(mSharedPreferenceFontChoose.getInt(KEY_FONT_CHOOSE_ID, 0));
        if (rbSave != null) {
            return rbSave.getText().toString();
        } else {
            final Resources res = mContext.getResources();
            String fontSizeNames = res.getString(R.string.choose_font_Medium);
            return fontSizeNames;
        }


    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        int radioButtonId = group.getCheckedRadioButtonId();
        RadioButton rb = (RadioButton) mFontDialogView.findViewById(checkedId);
        mSpFontEditor = mSharedPreferenceFontChoose.edit();

        boolean isChecked = mContext.getSharedPreferences(DisplaySettings.FILE_FONT_WARING,
                Activity.MODE_PRIVATE).getBoolean(DisplaySettings.KEY_IS_CHECKED, false);
        if (!rb.getText().equals(mContext.getResources().getString(R.string.choose_font_VeryLarge))
                || isChecked) {
            mSpFontEditor.putInt(KEY_FONT_CHOOSE_ID, radioButtonId);
        } else {
            mSpFontEditor.putInt(KEY_FONT_CHOOSE_PRE_ID, radioButtonId);
        }

        mSpFontEditor.commit();

        if (mPreferenceValueChangeListener != null) {
            mPreferenceValueChangeListener.onPreferenceValueChange(this, rb.getText());
        }

        if (mDialogFontChoose != null) {
            mDialogFontChoose.dismiss();
        }
    }

    public void waringDialogOk() {
        if (mSpFontEditor == null) {
            mSpFontEditor = mSharedPreferenceFontChoose.edit();
        }
        mSpFontEditor.putInt(KEY_FONT_CHOOSE_ID,
                mSharedPreferenceFontChoose.getInt(KEY_FONT_CHOOSE_PRE_ID, 0));
        mSpFontEditor.commit();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialogFontChoose == null || !mDialogFontChoose.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mDialogFontChoose.onSaveInstanceState();
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
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }


    private static class SavedState extends android.view.View.BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
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
}
