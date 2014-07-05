/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.settings.search;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;

import com.android.settings.R;

public class SettingsAutoCompleteTextView extends AutoCompleteTextView
        implements View.OnTouchListener {

    public Drawable mClearButton;

    public SettingsAutoCompleteTextView(Context context) {
        super(context);
        create();
    }

    public SettingsAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        create();
    }

    public SettingsAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    private void create() {
        mClearButton = getResources().getDrawable(R.drawable.ic_action_content_remove);

        setCompoundDrawablesWithIntrinsicBounds(null, null, mClearButton, null);
        setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_SEARCH);

        // set touch listener
        setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {
        case MeasureSpec.AT_MOST:
            width = Math.min(getPreferredWidth(), width);
            break;
        case MeasureSpec.UNSPECIFIED:
            width = getPreferredWidth();
            break;
        }
        widthMode = MeasureSpec.EXACTLY;
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
    }

    private int getPreferredWidth() {
        return getContext().getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.search_view_preferred_width);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
            return false;
        }

        int clearButtonStart = getWidth() - getPaddingRight()
                - mClearButton.getIntrinsicWidth();
        if (motionEvent.getX() > clearButtonStart) {
            // clear text
            setText("");
        }
        return false;
    }
}
