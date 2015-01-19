package com.android.settings.cyanogenmod;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.animation.AnticipateOvershootInterpolator;

public class GlowBackground extends Drawable implements ValueAnimator.AnimatorUpdateListener {

    private static final int MAX_CIRCLE_SIZE = 150;

    private final Paint mPaint;
    private Animator mAnimator;
    private float mCircleSize;

    public GlowBackground(int color) {
        mPaint = new Paint();
        mPaint.setColor(color);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final boolean changed = super.onStateChange(stateSet);

        boolean pressed = false;
        boolean focused = false;

        for (int state : stateSet) {
            if (state == android.R.attr.state_focused) {
                focused = true;
            }
            if (state == android.R.attr.state_pressed) {
                pressed = true;
            }
        }

        if (focused || pressed) {
            startAnimation(false);
        }

        return changed;
    }

    private void startAnimation(boolean reset) {
        if (!reset && mAnimator != null && mAnimator.isRunning()) {
            return;
        }
        if (reset && mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = getAnimator(reset);
        mAnimator.start();
    }

    private Animator getAnimator(boolean reset) {
        float start = mCircleSize;
        float end = 0f;
        if (!reset) {
            start = mCircleSize;
            end = mCircleSize == MAX_CIRCLE_SIZE ? 0f : MAX_CIRCLE_SIZE;
        }
        ValueAnimator animator = ObjectAnimator.ofFloat(start, end);
        animator.setInterpolator(new AnticipateOvershootInterpolator());
        animator.setDuration(400);
        animator.addUpdateListener(this);
        return animator;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(getBounds().width() / 2, getBounds().height() / 2, mCircleSize, mPaint);
    }

    @Override
    public void setAlpha(int i) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        mCircleSize = (Float) valueAnimator.getAnimatedValue();
        invalidateSelf();
    }

    public void reset() {
        startAnimation(true);
    }
}

