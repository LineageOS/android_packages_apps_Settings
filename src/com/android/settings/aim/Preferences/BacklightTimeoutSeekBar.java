package com.android.settings.aim.Preferences;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class BacklightTimeoutSeekBar extends SeekBar {
    private int mMax;
    private int mGap;
    private boolean mUpdatingThumb;

    public BacklightTimeoutSeekBar(Context context) {
        super(context);
    }

    public BacklightTimeoutSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BacklightTimeoutSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mUpdatingThumb = true;
        super.onSizeChanged(w, h, oldw, oldh);
        mUpdatingThumb = false;
    }

    @Override
    public void setThumb(Drawable thumb) {
        mUpdatingThumb = true;
        super.setThumb(thumb);
        mUpdatingThumb = false;
    }

    @Override
    public void setMax(int max) {
        mMax = max;
        mGap = max / 10;
        super.setMax(max + 2 * mGap - 1);
    }

    @Override
    protected int updateTouchProgress(int lastProgress, int newProgress) {
        if (newProgress < mMax) {
            return newProgress;
        }
        if (newProgress < mMax + mGap) {
            return mMax - 1;
        }
        return getMax();
    }
}
