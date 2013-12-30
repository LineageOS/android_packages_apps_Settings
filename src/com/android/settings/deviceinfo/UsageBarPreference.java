/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.android.settings.R;
import com.google.android.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * Creates a percentage bar chart inside a preference.
 */
public class UsageBarPreference extends Preference {

    public static class RefreshAnimationView extends ImageView {

        public class RefreshAnimationDrawable extends Drawable {

            private static final int ROTATE_ANIMATION_DURATION = 1700;
            private static final float SWEEP_MAX_DELTA = .25f;
            private static final int ARROW_OUT_ANIMATION_DURATION = 550;
            private static final int ARROW_IN_ANIMATION_DURATION = 350;
            private static final int TO_ORIGINAL_ANIMATION_DURATION = 350;

            private static final float SCALE = 24.0f;
            private static final float STROKE_WIDTH = 2.8f;

            private static final float DEFAULT_START_ANGLE = 315f;
            private static final float MAX_SWEEP_ANGLE = 285f;
            private static final float MIN_SWEEP_ANGLE = 15f;
            private static final float TOTAL_SWEEP_ANGLE = MAX_SWEEP_ANGLE - MIN_SWEEP_ANGLE;

            private final Paint mPaint;
            private final Paint mArrowPaint;
            private final Path mArrowPath = new Path();
            private RectF mIntrinsicBounds = null;

            private AnimatorSet mFromStop;
            private AnimatorSet mToStop;

            private float mStartAngle = 0f;
            private float mExtraStartAngle = 0f;
            private float mSweepAngle = DEFAULT_START_ANGLE;
            private float mLastStartAngle = 0f;
            private float mLastSweepAngle = 0f;
            private int mLastState;
            private float mCurrentArrowDelta = 1f;

            private RectF mArrowRect;
            private float mCenterX;
            private float mCenterY;
            private float mStrokeWidth;

            private boolean mRunning;
            private boolean mIndeterminate;

            public RefreshAnimationDrawable() {
                TypedArray ta = getContext().obtainStyledAttributes(
                        new int[]{android.R.attr.colorControlActivated});
                int color = ta.getColor(0, Color.WHITE);
                ta.recycle();

                mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPaint.setStrokeCap(Paint.Cap.BUTT);
                mPaint.setColor(color);
                mPaint.setStyle(Paint.Style.STROKE);

                mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mArrowPaint.setColor(color);
                mArrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

                createAnimators();
            }

            private void createAnimators() {
                // Indeterminate
                TimeAnimator indeterminateAnimator = new TimeAnimator();
                indeterminateAnimator.setDuration(ROTATE_ANIMATION_DURATION);
                indeterminateAnimator.setRepeatMode(ValueAnimator.RESTART);
                indeterminateAnimator.setRepeatCount(ValueAnimator.INFINITE);
                indeterminateAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                    @Override
                    public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                        updateArcAngle(totalTime);
                    }
                });

                // From arrow
                ValueAnimator fromArrowAnimator = ValueAnimator.ofFloat(1f, 0f);
                fromArrowAnimator.setDuration(ARROW_OUT_ANIMATION_DURATION);
                fromArrowAnimator.setInterpolator(new AccelerateInterpolator());
                fromArrowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mCurrentArrowDelta = (float) animation.getAnimatedValue();
                        createArrowPath(mCurrentArrowDelta);
                        invalidateSelf();
                    }
                });

                // From arrow
                ValueAnimator toArrowAnimator = ValueAnimator.ofFloat(0f, 1f);
                toArrowAnimator.setDuration(ARROW_IN_ANIMATION_DURATION);
                toArrowAnimator.setInterpolator(new AccelerateInterpolator());
                toArrowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mCurrentArrowDelta = (float) animation.getAnimatedValue();
                        createArrowPath(mCurrentArrowDelta);
                        invalidateSelf();
                    }
                });

                // To original position
                ValueAnimator toOriginalAnimator = ValueAnimator.ofFloat(0f, 1f);
                toOriginalAnimator.setDuration(TO_ORIGINAL_ANIMATION_DURATION);
                toOriginalAnimator.setInterpolator(new AccelerateInterpolator());
                toOriginalAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float delta = (float) animation.getAnimatedValue();
                        mStartAngle = mLastStartAngle - (mLastStartAngle * delta);
                        mSweepAngle = mLastSweepAngle +
                                ((DEFAULT_START_ANGLE - mLastSweepAngle) * delta);
                        invalidateSelf();
                    }
                });

                // Animators
                mFromStop = new AnimatorSet();
                mFromStop.playSequentially(fromArrowAnimator, indeterminateAnimator);
                mFromStop.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mRunning = true;
                        mIndeterminate = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mExtraStartAngle = 0;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                mToStop = new AnimatorSet();
                mToStop.playSequentially(toOriginalAnimator, toArrowAnimator);
                mToStop.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mRunning = true;
                        mIndeterminate = false;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mRunning = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            }

            private void updateArcAngle(long totalTime) {
                if (totalTime > 0) {
                    long deltaTime = totalTime % ROTATE_ANIMATION_DURATION;
                    float delta = deltaTime / (float) ROTATE_ANIMATION_DURATION;

                    int state = 0;
                    if (delta > 0.3f && delta < 0.55f) {
                        // close sweep angle
                        if (mLastState == -1) {
                            float sweepAngleDelta = (SWEEP_MAX_DELTA - (0.55f - delta)) / SWEEP_MAX_DELTA;
                            mExtraStartAngle = (mLastStartAngle +
                                    (TOTAL_SWEEP_ANGLE * sweepAngleDelta)) % 360;
                            mSweepAngle = MAX_SWEEP_ANGLE - (TOTAL_SWEEP_ANGLE * sweepAngleDelta);
                        }
                        state = -1;
                    } else if (delta > 0.75f) {
                        // open sweep angle
                        float sweepAngleDelta = (SWEEP_MAX_DELTA - (1f - delta)) / SWEEP_MAX_DELTA;
                        mSweepAngle = MIN_SWEEP_ANGLE + (TOTAL_SWEEP_ANGLE * sweepAngleDelta);
                        state = 1;
                    }

                    float rotation = ((totalTime % ROTATE_ANIMATION_DURATION) * 360);
                    mStartAngle = ((rotation / ROTATE_ANIMATION_DURATION) + mExtraStartAngle) % 360;

                    if (mLastState != state || state != -1) {
                        mLastStartAngle = mStartAngle;
                        mLastSweepAngle = mSweepAngle;
                        mLastState = state;
                    }
                    invalidateSelf();
                }
            }

            @Override
            protected void onBoundsChange(Rect bounds) {
                super.onBoundsChange(bounds);

                float scale = bounds.width() / SCALE;
                mStrokeWidth = scale * STROKE_WIDTH;
                mIntrinsicBounds = new RectF(getBounds());
                mIntrinsicBounds.inset(mStrokeWidth / 2, mStrokeWidth / 2);

                // Create the arrow triangle
                final float offset = (mStrokeWidth / 2) - 1;
                float radius = mIntrinsicBounds.width() / 2;
                mCenterX = (float)(radius * Math.cos(DEFAULT_START_ANGLE * Math.PI / 180f))
                        + (mIntrinsicBounds.width() / 2) + offset;
                mCenterY = (float) (radius * Math.sin(DEFAULT_START_ANGLE * Math.PI / 180f))
                        + (mIntrinsicBounds.height() / 2) + offset;

                mArrowRect = new RectF(mCenterX - mStrokeWidth, mCenterY - mStrokeWidth,
                        mCenterX + mStrokeWidth, mCenterY + mStrokeWidth);
                createArrowPath(mCurrentArrowDelta);

                mPaint.setStrokeWidth(mStrokeWidth);
            }

            private void createArrowPath(float delta) {
                float maxDistance = mStrokeWidth - (mStrokeWidth / 2f);
                float deltaDistance = (mStrokeWidth / 1.5f) + (maxDistance * delta);
                float inverseDeltaDistance = (mStrokeWidth / 2f) - (maxDistance * delta);
                float height = mStrokeWidth * 1.5f * delta;

                mArrowPath.reset();
                mArrowPath.moveTo(mCenterX - deltaDistance, mCenterY);
                mArrowPath.lineTo(mCenterX + deltaDistance, mCenterY);
                mArrowPath.lineTo(mCenterX + inverseDeltaDistance, mCenterY + height);
                mArrowPath.lineTo(mCenterX - inverseDeltaDistance, mCenterY + height);
            }

            @Override
            public void draw(Canvas canvas) {
                canvas.drawArc(mIntrinsicBounds, mStartAngle, mSweepAngle, false, mPaint);

                // Draw the arrow if necessary
                if (mCurrentArrowDelta > 0f) {
                    canvas.save();
                    canvas.translate(mArrowRect.centerX(), mArrowRect.centerY());
                    canvas.rotate(-45);
                    canvas.translate(-1 * mArrowRect.centerX(), -1 * mArrowRect.centerY());
                    canvas.drawPath(mArrowPath, mArrowPaint);
                    canvas.restore();
                }
            }

            @Override
            public void setAlpha(int alpha) {
                mPaint.setAlpha(alpha);
                mArrowPaint.setAlpha(alpha);
                invalidateSelf();
            }

            @Override
            public void setColorFilter(ColorFilter cf) {
                mPaint.setColorFilter(cf);
                mArrowPaint.setColorFilter(cf);
                invalidateSelf();
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        }

        private RefreshAnimationDrawable mDrawable;

        public RefreshAnimationView(Context context) {
            this(context, null, 0, 0);
        }

        public RefreshAnimationView(Context context, AttributeSet attrs) {
            this(context, attrs, 0, 0);
        }

        public RefreshAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        public RefreshAnimationView(Context context, AttributeSet attrs,
                int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init();
        }

        private void init() {
            mDrawable = new RefreshAnimationDrawable();
            super.setImageDrawable(mDrawable);
        }

        @Override
        protected void onVisibilityChanged(View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (mDrawable.mRunning) {
                if (visibility == GONE || visibility == INVISIBLE) {
                    mDrawable.mFromStop.pause();
                    mDrawable.mToStop.pause();
                } else {
                    if (mDrawable.mIndeterminate) {
                        mDrawable.mFromStop.resume();
                    } else {
                        mDrawable.mToStop.resume();
                    }
                }
            }
        }

        public void startProgress() {
            // Stop currents
            if (mDrawable.mToStop.isStarted() || mDrawable.mToStop.isRunning()) {
                mDrawable.mToStop.end();
            }

            // Start progress
            if (!mDrawable.mFromStop.isStarted() && !mDrawable.mFromStop.isRunning()) {
                mDrawable.mFromStop.start();
            }
        }

        public void stopProgress() {
            // Stop currents
            if (mDrawable.mFromStop.isStarted() || mDrawable.mFromStop.isRunning()) {
                mDrawable.mFromStop.end();
            }

            // Stop progress
            if (!mDrawable.mToStop.isStarted() && !mDrawable.mToStop.isRunning()) {
                mDrawable.mToStop.start();
            }
        }

        public void setColor(int color) {
            mDrawable.mPaint.setColor(color);
            mDrawable.mArrowPaint.setColor(color);
        }

        @Override
        public final void setImageDrawable(Drawable drawable) {
            // Ignore
        }

        @Override
        public final void setImageResource(int resId) {
            // Ignore
        }

        @Override
        public final void setImageURI(Uri uri) {
            // Ignore
        }

        @Override
        public final void setImageBitmap(Bitmap bm) {
            // Ignore
        }

    }


    public interface OnRequestMediaRescanListener {
        void onRequestMediaRescan();
    }

    private RefreshAnimationView mRescanMedia = null;
    private PercentageBarChart mChart = null;

    private boolean mAllowMediaScan;
    private boolean mRescanMediaStarted = false;

    private OnRequestMediaRescanListener mOnRequestMediaRescanListener;

    private final List<PercentageBarChart.Entry> mEntries = Lists.newArrayList();

    private Handler mHandler;

    public UsageBarPreference(Context context) {
        this(context, null);
    }

    public UsageBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UsageBarPreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public UsageBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_memoryusage);
        mHandler = new Handler();
        mAllowMediaScan = false;
    }

    public void addEntry(int order, float percentage, int color) {
        mEntries.add(PercentageBarChart.createEntry(order, percentage, color));
        Collections.sort(mEntries);
    }

    protected void setOnRequestMediaRescanListener(OnRequestMediaRescanListener listener) {
        mOnRequestMediaRescanListener = listener;
    }

    protected void setAllowMediaScan(boolean allow) {
        mAllowMediaScan = allow;
        notifyScanCompleted();
    }

    protected void notifyScanCompleted() {
        if (mRescanMedia != null) {
            mRescanMedia.setVisibility(mAllowMediaScan ? View.VISIBLE : View.GONE);
            if (mRescanMediaStarted) {
                mRescanMedia.stopProgress();
                mRescanMediaStarted = true;
            }
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mChart = (PercentageBarChart) view.findViewById(R.id.percentage_bar_chart);
        mChart.setEntries(mEntries);

        mRescanMedia = (RefreshAnimationView) view.findViewById(R.id.memory_usage_rescan_media);
        mRescanMedia.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnRequestMediaRescanListener != null) {
                    mRescanMediaStarted = true;
                    mRescanMedia.startProgress();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mOnRequestMediaRescanListener.onRequestMediaRescan();
                        }
                    });
                }
            }
        });
    }

    public void commit() {
        if (mChart != null) {
            mChart.invalidate();
        }
    }

    public void clear() {
        mEntries.clear();
    }
}
