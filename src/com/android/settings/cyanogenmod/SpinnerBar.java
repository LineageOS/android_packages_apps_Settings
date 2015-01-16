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


package com.android.settings.cyanogenmod;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;

/**
 * Based off of {@link com.android.settings.widget.SwitchBar}
 * Creates a generic toolbar with a state textview
 * Created by Adnan on 1/15/15.
 */
public class SpinnerBar extends LinearLayout implements OnItemSelectedListener {

    public static interface OnSpinnerItemSelectedListener {
        /**
         * Called when the spinner state has changed.
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        void onSpinnerItemSelected(AdapterView<?> parent, View view, int position, long id);

        void onSpinnerNothingSelected(AdapterView<?> parent);
    }

    private Spinner mSpinner;
    private TextView mTextView;

    private ArrayList<OnSpinnerItemSelectedListener> mOnItemSelectedListeners =
            new ArrayList<OnSpinnerItemSelectedListener>();

    private static int[] MARGIN_ATTRIBUTES = {
            R.attr.switchBarMarginStart, R.attr.switchBarMarginEnd};

    public SpinnerBar(Context context) {
        this(context, null);
    }

    public SpinnerBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinnerBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SpinnerBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.spinner_bar, this);

        final TypedArray a = context.obtainStyledAttributes(attrs, MARGIN_ATTRIBUTES);
        int spinnerBarMarginStart = (int) a.getDimension(0, 0);
        int spinnerBarMarginEnd = (int) a.getDimension(1, 0);
        a.recycle();

        mTextView = (TextView) findViewById(R.id.spinner_text);
        mTextView.setText(R.string.spinner_inflated_text);
        ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) mTextView.getLayoutParams();
        lp.setMarginStart(spinnerBarMarginStart);

        mSpinner = (Spinner) findViewById(R.id.spinner_widget);
        // Prevent onSaveInstanceState() to be called as we are managing the state of the Spinner
        // on our own
        mSpinner.setSaveEnabled(false);
        lp = (MarginLayoutParams) mSpinner.getLayoutParams();
        lp.setMarginEnd(spinnerBarMarginEnd);

        addOnItemSelectedListener(new OnSpinnerItemSelectedListener() {
            @Override
            public void onSpinnerItemSelected(AdapterView<?> parent, View view,
                     int position, long id) {
                // don't care
            }

            @Override
            public void onSpinnerNothingSelected(AdapterView<?> parent) {
                // don't care
            }
        });

        // Default is hide
        setVisibility(View.GONE);
    }

    /**
     * Force the {@link android.widget.ArrayAdapter} type
     * @param arrayAdapter
     */
    public void setAdapter(ArrayAdapter<CharSequence> arrayAdapter) {
        mSpinner.setAdapter(arrayAdapter);
    }

    public void show() {
        if (!isShowing()) {
            setVisibility(View.VISIBLE);
            mSpinner.setOnItemSelectedListener(this);
        }
    }

    public void hide() {
        if (isShowing()) {
            setVisibility(View.GONE);
            mSpinner.setOnItemSelectedListener(null);
        }
    }

    public void addOnItemSelectedListener(OnSpinnerItemSelectedListener listener) {
        if (mOnItemSelectedListeners.contains(listener)) {
            throw new IllegalStateException("Cannot add twice the same OnItemSelectedListener");
        }
        mOnItemSelectedListeners.add(listener);
    }

    public void removeOnItemSelectedListener(OnSpinnerItemSelectedListener listener) {
        if (!mOnItemSelectedListeners.contains(listener)) {
            throw new IllegalStateException("Cannot remove OnItemSelectedListener");
        }
        mOnItemSelectedListeners.remove(listener);
    }

    public void setSpinnerPosition(int i) {
        mSpinner.setSelection(i);
    }

    public void setTextViewLabel(CharSequence textViewLabel) {
        mTextView.setText(textViewLabel);
    }

    public CharSequence getTextViewLabel() {
        return mTextView.getText();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        propagateSelected(adapterView, view, i, l);
    }

    public void propagateSelected(AdapterView<?> adapterView, View view, int i, long l) {
        final int count = mOnItemSelectedListeners.size();
        for (int n = 0; n < count; n++) {
            mOnItemSelectedListeners.get(n).onSpinnerItemSelected(adapterView, view, i, l);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        propagateNotSelected(adapterView);
    }

    public void propagateNotSelected(AdapterView<?> adapterView) {
        final int count = mOnItemSelectedListeners.size();
        for (int n = 0; n < count; n++) {
            mOnItemSelectedListeners.get(n).onSpinnerNothingSelected(adapterView);
        }
    }

    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE);
    }

    static class SavedState extends BaseSavedState {
        int state;
        boolean visible;

        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            state = (Integer)in.readValue(null);
            visible = (Boolean)in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(state);
            out.writeValue(visible);
        }

        @Override
        public String toString() {
            return "SpinnerBar.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " selected=" + state
                    + " visible=" + visible + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.state = mSpinner.getSelectedItemPosition();
        ss.visible = isShowing();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        mSpinner.setSelection(ss.state);
        setVisibility(ss.visible ? View.VISIBLE : View.GONE);
        mSpinner.setOnItemSelectedListener(ss.visible ? this : null);

        requestLayout();
    }
}
