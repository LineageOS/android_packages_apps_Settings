package com.android.settings.dashboard;
/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
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


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;

import java.util.Locale;


public class PagerSlidingTabStrip extends HorizontalScrollView {

    public interface IconTabProvider {
        public int getPageIconResId(int position);
    }

    private static final int[] ATTRS = new int[] {
            android.R.attr.textSize,
            android.R.attr.textColor
    };

    private LinearLayout.LayoutParams mDefaultTabLayoutParams;
    private LinearLayout.LayoutParams mExpandedTabLayoutParams;

    private final PageListener mPageListener = new PageListener();
    public OnPageChangeListener mDelegatePageListener;

    private LinearLayout mTabsContainer;
    private ViewPager mPager;

    private int mTabCount;

    private int mCurrentPosition = 0;
    private float mCurrentPositionOffset = 0f;

    private Paint mIndicatorPaint;
    private Paint mUnderlinePaint;
    private Paint mDividerPaint;

    private int mIndicatorColor = 0xFF666666;
    private int mUnderlineColor = 0x1A000000;
    private int mDividerColor = 0x1A000000;

    private boolean mShouldExpand = false;
    private boolean mTextAllCaps = true;

    private int mScrollOffset = 52;
    private int mIndicatorHeight = 8;
    private int mUnderlineHeight = 2;
    private int mDividerPadding = 12;
    private int mTabPadding = 24;
    private int mDividerWidth = 1;

    private int mTabTextSize = 10;
    private int mTabTextColor = 0xFF666666;
    private Typeface mTabTypeface = null;
    private int mTabTypefaceStyle = Typeface.NORMAL;

    private int mLastScrollX = 0;

    private int mTabBackgroundResId = R.drawable.dashboard_tile_background;

    private Locale mLocale;

    public PagerSlidingTabStrip(Context context) {
        this(context, null);
    }

    public PagerSlidingTabStrip(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public PagerSlidingTabStrip(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setFillViewport(true);
        setWillNotDraw(false);

        mTabsContainer = new LinearLayout(context);
        mTabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        mTabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mTabsContainer);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        mScrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mScrollOffset, dm);
        mIndicatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mIndicatorHeight, dm);
        mUnderlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mUnderlineHeight, dm);
        mDividerPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mDividerPadding, dm);
        mTabPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mTabPadding, dm);
        mDividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mDividerWidth, dm);
        mTabTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTabTextSize, dm);

        // get system attrs (android:textSize and android:textColor)

        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);

        mTabTextSize = a.getDimensionPixelSize(0, mTabTextSize);
        mTabTextColor = a.getColor(1, mTabTextColor);

        a.recycle();

        // get custom attrs

        a = context.obtainStyledAttributes(attrs, R.styleable.PagerSlidingTabStrip);

        mIndicatorColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsIndicatorColor, mIndicatorColor);
        mUnderlineColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsUnderlineColor, mUnderlineColor);
        mDividerColor = a.getColor(R.styleable.PagerSlidingTabStrip_pstsDividerColor, mDividerColor);
        mIndicatorHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsIndicatorHeight, mIndicatorHeight);
        mUnderlineHeight = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsUnderlineHeight, mUnderlineHeight);
        mDividerPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsDividerPadding, mDividerPadding);
        mTabPadding = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsTabPaddingLeftRight, mTabPadding);
        mTabBackgroundResId = a.getResourceId(R.styleable.PagerSlidingTabStrip_pstsTabBackground, mTabBackgroundResId);
        mShouldExpand = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsShouldExpand, mShouldExpand);
        mScrollOffset = a.getDimensionPixelSize(R.styleable.PagerSlidingTabStrip_pstsScrollOffset, mScrollOffset);
        mTextAllCaps = a.getBoolean(R.styleable.PagerSlidingTabStrip_pstsTextAllCaps, mTextAllCaps);

        a.recycle();

        mIndicatorPaint = new Paint();
        mIndicatorPaint.setAntiAlias(true);
        mIndicatorPaint.setStyle(Style.FILL);
        mIndicatorPaint.setColor(mIndicatorColor);

        mUnderlinePaint = new Paint();
        mUnderlinePaint.setAntiAlias(true);
        mUnderlinePaint.setStyle(Style.FILL);
        mUnderlinePaint.setColor(mUnderlineColor);

        mDividerPaint = new Paint();
        mDividerPaint.setAntiAlias(true);
        mDividerPaint.setStrokeWidth(mDividerWidth);
        mDividerPaint.setColor(mDividerColor);

        mDefaultTabLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mExpandedTabLayoutParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);

        if (mLocale == null) {
            mLocale = getResources().getConfiguration().locale;
        }
    }

    public void setViewPager(ViewPager pager) {
        this.mPager = pager;

        if (pager.getAdapter() == null) {
            throw new IllegalStateException("ViewPager does not have adapter instance.");
        }

        pager.setOnPageChangeListener(mPageListener);

        notifyDataSetChanged();
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.mDelegatePageListener = listener;
    }

    public void notifyDataSetChanged() {
        mTabsContainer.removeAllViews();
        mTabCount = mPager.getAdapter().getCount();
        for (int i = 0; i < mTabCount; i++) {
            if (mPager.getAdapter() instanceof IconTabProvider) {
                addIconTab(i, ((IconTabProvider) mPager.getAdapter()).getPageIconResId(i),
                        mPager.getAdapter().getPageTitle(i));
            } else {
                addTextTab(i, mPager.getAdapter().getPageTitle(i).toString());
            }
        }

        updateTabStyles();
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @SuppressWarnings("deprecation")
            @SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                mCurrentPosition = mPager.getCurrentItem();
                scrollToChild(mCurrentPosition, 0);
            }
        });

    }

    private void addTextTab(final int position, String title) {

        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine();

        addTab(position, tab);
    }

    private void addIconTab(final int position, int resId, CharSequence text) {

        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(getContext());
        textView.setId(android.R.id.text1);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        textView.setSingleLine();
        textView.setPadding(0, 0, 0, 10);

        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(resId);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setPadding(0, 0, 0, 0);

        ll.addView(imageView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.f));
        ll.addView(textView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        addTab(position, ll);
    }

    private void addTab(final int position, View tab) {
        tab.setFocusable(true);
        tab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(position);
            }
        });

        tab.setPadding(mTabPadding, 0, mTabPadding, 0);
        mTabsContainer.addView(tab, position, mShouldExpand ? mExpandedTabLayoutParams : mDefaultTabLayoutParams);
    }

    private void updateTabStyles() {

        for (int i = 0; i < mTabCount; i++) {
            View v = mTabsContainer.getChildAt(i);
            v.setBackgroundResource(mTabBackgroundResId);
            TextView tab = null;
            if (v instanceof TextView) {
                tab = (TextView) v;
            } else if (v instanceof ViewGroup) {
                tab = (TextView) ((ViewGroup) v).findViewById(android.R.id.text1);
            }
            if (tab != null) {
                tab.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabTextSize);
                tab.setTypeface(mTabTypeface, mTabTypefaceStyle);
                tab.setTextColor(mTabTextColor);

                // setAllCaps() is only available from API 14, so the upper case is made manually if we are on a
                // pre-ICS-build
                if (mTextAllCaps) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        tab.setAllCaps(true);
                    } else {
                        tab.setText(tab.getText().toString().toUpperCase(mLocale));
                    }
                }
            }
        }

    }

    private void scrollToChild(int position, int offset) {

        if (mTabCount == 0) {
            return;
        }

        int newScrollX = mTabsContainer.getChildAt(position).getLeft() + offset;

        if (position > 0 || offset > 0) {
            newScrollX -= mScrollOffset;
        }

        if (newScrollX != mLastScrollX) {
            mLastScrollX = newScrollX;
            scrollTo(newScrollX, 0);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode() || mTabCount == 0) {
            return;
        }

        final int height = getHeight();

        // draw indicator line
        View currentTab = mTabsContainer.getChildAt(mCurrentPosition);
        float lineLeft = currentTab.getLeft();
        float lineRight = currentTab.getRight();

        // if there is an offset, start interpolating left and right coordinates between current and next tab
        if (mCurrentPositionOffset > 0f && mCurrentPosition < mTabCount - 1) {

            View nextTab = mTabsContainer.getChildAt(mCurrentPosition + 1);
            final float nextTabLeft = nextTab.getLeft();
            final float nextTabRight = nextTab.getRight();

            lineLeft = (mCurrentPositionOffset * nextTabLeft + (1f - mCurrentPositionOffset) * lineLeft);
            lineRight = (mCurrentPositionOffset * nextTabRight + (1f - mCurrentPositionOffset) * lineRight);
        }
        canvas.drawRect(lineLeft, 0, lineRight, mIndicatorHeight, mIndicatorPaint);

        // draw underline
        canvas.drawRect(0, height - mUnderlineHeight, mTabsContainer.getWidth(), height, mUnderlinePaint);

        // draw divider
        for (int i = 0; i < mTabCount - 1; i++) {
            View tab = mTabsContainer.getChildAt(i);
            canvas.drawLine(tab.getRight(), mDividerPadding, tab.getRight(), height - mDividerPadding - mUnderlineHeight, mDividerPaint);
        }
    }

    private class PageListener implements OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            mCurrentPosition = position;
            mCurrentPositionOffset = positionOffset;

            scrollToChild(position, (int) (positionOffset * mTabsContainer.getChildAt(position).getWidth()));

            invalidate();

            if (mDelegatePageListener != null) {
                mDelegatePageListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                scrollToChild(mPager.getCurrentItem(), 0);
            }

            if (mDelegatePageListener != null) {
                mDelegatePageListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mDelegatePageListener != null) {
                mDelegatePageListener.onPageSelected(position);
            }
            for (int i = 0; i < mTabCount; i++) {
                View tab = mTabsContainer.getChildAt(i);
                tab.setSelected(position == i);
            }
        }

    }

    public void setIndicatorColor(int indicatorColor) {
        this.mIndicatorColor = indicatorColor;
        mIndicatorPaint.setColor(this.mIndicatorColor);
        invalidate();
    }

    public void setIndicatorColorResource(int resId) {
        this.mIndicatorColor = getResources().getColor(resId);
        mIndicatorPaint.setColor(mIndicatorColor);
        invalidate();
    }

    public int getIndicatorColor() {
        return this.mIndicatorColor;
    }

    public void setIndicatorHeight(int indicatorLineHeightPx) {
        this.mIndicatorHeight = indicatorLineHeightPx;
        invalidate();
    }

    public int getIndicatorHeight() {
        return mIndicatorHeight;
    }

    public void setUnderlineColor(int underlineColor) {
        this.mUnderlineColor = underlineColor;
        mUnderlinePaint.setColor(underlineColor);
        invalidate();
    }

    public void setUnderlineColorResource(int resId) {
        this.mUnderlineColor = getResources().getColor(resId);
        mUnderlinePaint.setColor(mUnderlineColor);
        invalidate();
    }

    public int getUnderlineColor() {
        return mUnderlineColor;
    }

    public void setDividerColor(int dividerColor) {
        this.mDividerColor = dividerColor;
        mDividerPaint.setColor(dividerColor);
        invalidate();
    }

    public void setDividerColorResource(int resId) {
        this.mDividerColor = getResources().getColor(resId);
        mDividerPaint.setColor(mDividerColor);
        invalidate();
    }

    public int getDividerColor() {
        return mDividerColor;
    }

    public void setUnderlineHeight(int underlineHeightPx) {
        this.mUnderlineHeight = underlineHeightPx;
        invalidate();
    }

    public int getUnderlineHeight() {
        return mUnderlineHeight;
    }

    public void setDividerPadding(int dividerPaddingPx) {
        this.mDividerPadding = dividerPaddingPx;
        invalidate();
    }

    public int getDividerPadding() {
        return mDividerPadding;
    }

    public void setScrollOffset(int scrollOffsetPx) {
        this.mScrollOffset = scrollOffsetPx;
        invalidate();
    }

    public int getScrollOffset() {
        return mScrollOffset;
    }

    public void setShouldExpand(boolean shouldExpand) {
        this.mShouldExpand = shouldExpand;
        requestLayout();
    }

    public boolean getShouldExpand() {
        return mShouldExpand;
    }

    public boolean isTextAllCaps() {
        return mTextAllCaps;
    }

    public void setAllCaps(boolean textAllCaps) {
        this.mTextAllCaps = textAllCaps;
    }

    public void setTextSize(int textSizePx) {
        this.mTabTextSize = textSizePx;
        updateTabStyles();
    }

    public int getTextSize() {
        return mTabTextSize;
    }

    public void setTextColor(int textColor) {
        this.mTabTextColor = textColor;
        updateTabStyles();
    }

    public void setTextColorResource(int resId) {
        this.mTabTextColor = getResources().getColor(resId);
        updateTabStyles();
    }

    public int getTextColor() {
        return mTabTextColor;
    }

    public void setTypeface(Typeface typeface, int style) {
        this.mTabTypeface = typeface;
        this.mTabTypefaceStyle = style;
        updateTabStyles();
    }

    public void setTabBackground(int resId) {
        this.mTabBackgroundResId = resId;
    }

    public int getTabBackground() {
        return mTabBackgroundResId;
    }

    public void setTabPaddingLeftRight(int paddingPx) {
        this.mTabPadding = paddingPx;
        updateTabStyles();
    }

    public int getTabPaddingLeftRight() {
        return mTabPadding;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mCurrentPosition = savedState.currentPosition;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPosition = mCurrentPosition;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPosition;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPosition);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
