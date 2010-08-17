/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings.activities;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import android.graphics.BlurMaskFilter.Blur;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Paint.Style;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.os.SystemClock;
import android.util.StateSet;


public class ColorPickerDialog extends Dialog {

    public interface OnColorChangedListener {
        void colorChanged(int color);
	/** @hide */
        void colorUpdate(int color);
    }

    private OnColorChangedListener mListener;
    private int mInitialColor;

    private class ColorPickerView extends View {
        private Paint mPaint;
        private Paint mCenterPaint;
        private final int[] mColors;
        private OnColorChangedListener mListener;

        ColorPickerView(Context c, OnColorChangedListener l, int color) {
            super(c);
            mListener = l;
            mColors = new int[] {
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFFFFFF, 0xFF808080, 0xFF000000, 0xFFFF0000
            };
            Shader s = new SweepGradient(0, 0, mColors, null);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setShader(s);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(32);

            mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCenterPaint.setColor(color);
            mCenterPaint.setStrokeWidth(5);
        }

        private boolean mTrackingCenter;
        private boolean mHighlightCenter;

        @Override
        protected void onDraw(Canvas canvas) {
            float r = CENTER_X - mPaint.getStrokeWidth()*0.5f;

            canvas.translate(CENTER_X, CENTER_X);

            canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
            canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

            if (mTrackingCenter) {
                int c = mCenterPaint.getColor();
                mCenterPaint.setStyle(Paint.Style.STROKE);

                if (mHighlightCenter) {
                    mCenterPaint.setAlpha(0xFF);
                } else {
                    mCenterPaint.setAlpha(0x80);
                }
                canvas.drawCircle(0, 0,
                                  CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
                                  mCenterPaint);

                mCenterPaint.setStyle(Paint.Style.FILL);
                mCenterPaint.setColor(c);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(CENTER_X*2, CENTER_Y*2);
        }

        private static final int CENTER_X = 100;
        private static final int CENTER_Y = 100;
        private static final int CENTER_RADIUS = 32;

        private int floatToByte(float x) {
            int n = java.lang.Math.round(x);
            return n;
        }
        private int pinToByte(int n) {
            if (n < 0) {
                n = 0;
            } else if (n > 255) {
                n = 255;
            }
            return n;
        }

        public void setCenterColor(int color) {
            mCenterPaint.setColor(color);
            invalidate();
        }

        public void setTransparency(int alpha) {
            int color = mCenterPaint.getColor();
            int newColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
            mCenterPaint.setColor(newColor);
            mEditText.setText(convertToARGB(newColor));
            invalidate();
        }

        private int ave(int s, int d, float p) {
            return s + java.lang.Math.round(p * (d - s));
        }

        private int interpColor(int colors[], float unit) {
            if (unit <= 0) {
                return colors[0];
            }
            if (unit >= 1) {
                return colors[colors.length - 1];
            }

            float p = unit * (colors.length - 1);
            int i = (int)p;
            p -= i;

            // now p is just the fractional part [0...1) and i is the index
            int c0 = colors[i];
            int c1 = colors[i+1];
            int a = ave(Color.alpha(c0), Color.alpha(c1), p);
            int r = ave(Color.red(c0), Color.red(c1), p);
            int g = ave(Color.green(c0), Color.green(c1), p);
            int b = ave(Color.blue(c0), Color.blue(c1), p);

            return Color.argb(a, r, g, b);
        }

        private int rotateColor(int color, float rad) {
            float deg = rad * 180 / 3.1415927f;
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            ColorMatrix cm = new ColorMatrix();
            ColorMatrix tmp = new ColorMatrix();

            cm.setRGB2YUV();
            tmp.setRotate(0, deg);
            cm.postConcat(tmp);
            tmp.setYUV2RGB();
            cm.postConcat(tmp);

            final float[] a = cm.getArray();

            int ir = floatToByte(a[0] * r +  a[1] * g +  a[2] * b);
            int ig = floatToByte(a[5] * r +  a[6] * g +  a[7] * b);
            int ib = floatToByte(a[10] * r + a[11] * g + a[12] * b);

            return Color.argb(Color.alpha(color), pinToByte(ir),
                              pinToByte(ig), pinToByte(ib));
        }

        private static final float PI = 3.1415926f;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX() - CENTER_X;
            float y = event.getY() - CENTER_Y;
            boolean inCenter = java.lang.Math.sqrt(x*x + y*y) <= CENTER_RADIUS;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTrackingCenter = inCenter;
                    if (inCenter) {
                        mHighlightCenter = true;
                        invalidate();
                        break;
                    }
                case MotionEvent.ACTION_MOVE:
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter;
                            invalidate();
                        }
                    } else {
                        float angle = (float)java.lang.Math.atan2(y, x);
                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle/(2*PI);
                        if (unit < 0) {
                            unit += 1;
                        }
                        int color = interpColor(mColors, unit);
                        mCenterPaint.setColor(color);
                        mEditText.setText(convertToARGB(color));
                        mListener.colorUpdate(color);
			invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mTrackingCenter) {
                        if (inCenter) {
                            mListener.colorChanged(mCenterPaint.getColor());
                        }
                        mTrackingCenter = false;    // so we draw w/o halo
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    }

    private String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + green + blue;
    }

    private int convertToColorInt(String argb) throws NumberFormatException {

        int alpha = -1, red = -1, green = -1, blue = -1;

        if (argb.length() == 8) {
            alpha = Integer.parseInt(argb.substring(0, 2), 16);
            red = Integer.parseInt(argb.substring(2, 4), 16);
            green = Integer.parseInt(argb.substring(4, 6), 16);
            blue = Integer.parseInt(argb.substring(6, 8), 16);
        }
        else if (argb.length() == 6) {
            alpha = 255;
            red = Integer.parseInt(argb.substring(0, 2), 16);
            green = Integer.parseInt(argb.substring(2, 4), 16);
            blue = Integer.parseInt(argb.substring(4, 6), 16);
        }

        return Color.argb(alpha, red, green, blue);
    }

    private Context mContext;
    private EditText mEditText;
    private ColorPickerView mColorPickerView;
    private SeekBar mTransparencyBar;

    public ColorPickerDialog(Context context, OnColorChangedListener listener, int initialColor) {
        super(context);
        mContext = context;
        mListener = listener;
        mInitialColor = initialColor;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                                    LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 0, 10, 5);

        TextView tv = new TextView(mContext);
        tv.setText(com.android.settings.R.string.lockpattern_settings_custom_msg_color);
        layout.addView(tv, layoutParams);

        mColorPickerView = new ColorPickerView(getContext(), onColorChangedListener, mInitialColor);
        layout.addView(mColorPickerView, layoutParams);

        mTransparencyBar = new SeekBar(mContext);
        mTransparencyBar.setMax(255);
		mTransparencyBar.setProgressDrawable(new TextSeekBarDrawable(mContext.getResources(), "alpha", true));
		mTransparencyBar.setProgress(Color.alpha(mInitialColor));
		mTransparencyBar.setOnSeekBarChangeListener(onTransparencyChangedListener);
		layout.addView(mTransparencyBar, layoutParams);

        mEditText = new EditText(mContext);
        mEditText.addTextChangedListener(mEditTextListener);
        mEditText.setText(convertToARGB(mInitialColor));
        layout.addView(mEditText, layoutParams);

        setContentView(layout);
        setTitle(com.android.settings.R.string.lockpattern_settings_custom_msg_color);
    }

    private OnColorChangedListener onColorChangedListener = new OnColorChangedListener() {
        public void colorChanged(int color) {
           mListener.colorChanged(color);
           dismiss();
        }
        public void colorUpdate(int color) {
	  mListener.colorUpdate(color);
        }
    };

    private SeekBar.OnSeekBarChangeListener onTransparencyChangedListener = new SeekBar.OnSeekBarChangeListener() {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mColorPickerView.setTransparency(progress);
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private TextWatcher mEditTextListener = new TextWatcher() {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                String s2 = (s.toString()).replace("#", "");
                if (s2.length() == 6 || s2.length() == 8) {
                    int color = convertToColorInt(s2);
                    mColorPickerView.setCenterColor(color);
                    mTransparencyBar.setProgress(Color.alpha(color));
                }
            }
            catch (NumberFormatException e) {
            }
        }
    };

    //Source: http://www.anddev.org/announce_color_picker_dialog-t10771.html
	static final int[] STATE_FOCUSED = {android.R.attr.state_focused};
	static final int[] STATE_PRESSED = {android.R.attr.state_pressed};

    static class TextSeekBarDrawable extends Drawable implements Runnable {

		private static final String TAG = "TextSeekBarDrawable";
		private static final long DELAY = 50;
		private String mText;
		private Drawable mProgress;
		private Paint mPaint;
		private Paint mOutlinePaint;
		private float mTextWidth;
		private boolean mActive;
		private float mTextXScale;
		private int mDelta;
		private ScrollAnimation mAnimation;

		public TextSeekBarDrawable(Resources res, String label, boolean labelOnRight) {
			mText = label;
			mProgress = res.getDrawable(android.R.drawable.progress_horizontal);
			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setTypeface(Typeface.DEFAULT_BOLD);
			mPaint.setTextSize(16);
			mPaint.setColor(0xff000000);
			mOutlinePaint = new Paint(mPaint);
			mOutlinePaint.setStyle(Style.STROKE);
			mOutlinePaint.setStrokeWidth(3);
			mOutlinePaint.setColor(0xbbffc300);
			mOutlinePaint.setMaskFilter(new BlurMaskFilter(1, Blur.NORMAL));
			mTextWidth = mOutlinePaint.measureText(mText);
			mTextXScale = labelOnRight? 1 : 0;
			mAnimation = new ScrollAnimation();
		}

		@Override
		protected void onBoundsChange(Rect bounds) {
			mProgress.setBounds(bounds);
		}

		@Override
		protected boolean onStateChange(int[] state) {
			mActive = StateSet.stateSetMatches(STATE_FOCUSED, state) | StateSet.stateSetMatches(STATE_PRESSED, state);
			invalidateSelf();
			return false;
		}

		@Override
		public boolean isStateful() {
			return true;
		}

		@Override
		protected boolean onLevelChange(int level) {
//			Log.d(TAG, "onLevelChange " + level);
			if (level < 4000 && mDelta <= 0) {
//				Log.d(TAG, "onLevelChange scheduleSelf ++");
				mDelta = 1;
				mAnimation.startScrolling(mTextXScale, 1);
				scheduleSelf(this, SystemClock.uptimeMillis() + DELAY);
			} else
			if (level > 6000 && mDelta >= 0) {
//				Log.d(TAG, "onLevelChange scheduleSelf --");
				mDelta = -1;
				mAnimation.startScrolling(mTextXScale, 0);
				scheduleSelf(this, SystemClock.uptimeMillis() + DELAY);
			}
			return mProgress.setLevel(level);
		}

		@Override
		public void draw(Canvas canvas) {
			mProgress.draw(canvas);

			if (mAnimation.hasStarted() && !mAnimation.hasEnded()) {
				// pending animation
				mAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), null);
				mTextXScale = mAnimation.getCurrent();
//				Log.d(TAG, "draw " + mTextX + " " + SystemClock.uptimeMillis());
			}

			Rect bounds = getBounds();
			float x = 6 + mTextXScale * (bounds.width() - mTextWidth - 6 - 6);
			float y = (bounds.height() + mPaint.getTextSize()) / 2;
			mOutlinePaint.setAlpha(mActive? 255 : 255 / 2);
			mPaint.setAlpha(mActive? 255 : 255 / 2);
			canvas.drawText(mText, x, y, mOutlinePaint);
			canvas.drawText(mText, x, y, mPaint);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

		public void run() {
			mAnimation.getTransformation(AnimationUtils.currentAnimationTimeMillis(), null);
			// close interpolation of mTextX
			mTextXScale = mAnimation.getCurrent();
			if (!mAnimation.hasEnded()) {
				scheduleSelf(this, SystemClock.uptimeMillis() + DELAY);
			}
			invalidateSelf();
//			Log.d(TAG, "run " + mTextX + " " + SystemClock.uptimeMillis());
		}
	}

	static class ScrollAnimation extends Animation {
		private static final String TAG = "ScrollAnimation";
		private static final long DURATION = 750;
		private float mFrom;
		private float mTo;
		private float mCurrent;

		public ScrollAnimation() {
			setDuration(DURATION);
			setInterpolator(new DecelerateInterpolator());
		}

		public void startScrolling(float from, float to) {
			mFrom = from;
			mTo = to;
			startNow();
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			mCurrent = mFrom + (mTo - mFrom) * interpolatedTime;
//			Log.d(TAG, "applyTransformation " + mCurrent);
		}

		public float getCurrent() {
			return mCurrent;
		}
	}
}
