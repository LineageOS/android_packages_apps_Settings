package com.android.settings.biometrics.face;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.settings.R;

/** Draws the enrollment state without the legacy particle animation. */
public class FaceEnrollProgressView extends View {
    private static final long PROGRESS_ANIMATION_DURATION_MS = 400;
    private static final long COLOR_ANIMATION_DURATION_MS = 300;
    private static final long HELP_STATE_DURATION_MS = 1800;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mMaskPath = new Path();
    private final RectF mBounds = new RectF();
    private float mProgress;
    private float mTargetProgress;
    private int mColor;
    private ValueAnimator mProgressAnimator;
    private ValueAnimator mColorAnimator;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRestoreProgressState = this::animateToProgressColor;

    public FaceEnrollProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 12f);
        mColor = getResources().getColor(R.color.face_enroll_progress, context.getTheme());
        TypedValue background = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorBackground, background, true);
        mMaskPaint.setColor(background.data);
        mMaskPaint.setStyle(Paint.Style.FILL);
    }

    public void setProgress(int steps, int remaining) {
        int total = steps + remaining;
        float target = total > 0 ? Math.max(0f, Math.min(1f, (float) steps / total)) : 0f;
        // Some face HALs briefly report stale progress. Never animate the ring backwards.
        target = Math.max(mTargetProgress, target);
        if (target == mTargetProgress) return;
        mTargetProgress = target;

        if (mProgressAnimator != null) mProgressAnimator.cancel();
        mProgressAnimator = ValueAnimator.ofFloat(mProgress, target);
        mProgressAnimator.setDuration(PROGRESS_ANIMATION_DURATION_MS);
        mProgressAnimator.setInterpolator(new DecelerateInterpolator());
        mProgressAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        mProgressAnimator.start();
    }

    public void setHelpState() {
        mHandler.removeCallbacks(mRestoreProgressState);
        animateToColor(R.color.face_enroll_warning);
        // Progress callbacks may continue during help; only the sweep changes until this expires.
        mHandler.postDelayed(mRestoreProgressState, HELP_STATE_DURATION_MS);
    }

    public void setErrorState() {
        mHandler.removeCallbacks(mRestoreProgressState);
        animateToColor(R.color.face_enroll_error);
    }

    public void setCompleteState() {
        mHandler.removeCallbacks(mRestoreProgressState);
        animateToColor(R.color.face_enroll_success);
    }

    private void animateToProgressColor() {
        animateToColor(R.color.face_enroll_progress);
    }

    private void animateToColor(int colorRes) {
        int target = getResources().getColor(colorRes, getContext().getTheme());
        if (target == mColor) return;
        if (mColorAnimator != null) mColorAnimator.cancel();
        mColorAnimator = ValueAnimator.ofArgb(mColor, target);
        mColorAnimator.setDuration(COLOR_ANIMATION_DURATION_MS);
        mColorAnimator.addUpdateListener(animation -> {
            mColor = (int) animation.getAnimatedValue();
            invalidate();
        });
        mColorAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        mHandler.removeCallbacks(mRestoreProgressState);
        if (mProgressAnimator != null) mProgressAnimator.cancel();
        if (mColorAnimator != null) mColorAnimator.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        // Match the legacy face preview cutout exactly: it used a 20 px inset.
        float inset = 20f;
        mBounds.set(inset, inset, getWidth() - inset, getHeight() - inset);

        mMaskPath.reset();
        mMaskPath.setFillType(Path.FillType.EVEN_ODD);
        mMaskPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        mMaskPath.addCircle(getWidth() / 2f, getHeight() / 2f,
                Math.min(getWidth(), getHeight()) / 2f - inset, Path.Direction.CW);
        canvas.drawPath(mMaskPath, mMaskPaint);

        mPaint.setColor(0x33000000 | (mColor & 0x00ffffff));
        canvas.drawArc(mBounds, -90, 360, false, mPaint);
        mPaint.setColor(mColor);
        canvas.drawArc(mBounds, -90, 360f * mProgress, false, mPaint);
    }
}
