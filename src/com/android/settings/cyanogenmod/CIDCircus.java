/*);
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

package com.android.settings.cyanogenmod;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Random;

import com.android.settings.R;

public class CIDCircus extends Activity {
    final static boolean DEBUG = false;

    private static class Board extends FrameLayout
    {
        static Random sRNG = new Random();

        static float lerp(float a, float b, float f) {
            return (b-a)*f + a;
        }

        static float randfrange(float a, float b) {
            return lerp(a, b, sRNG.nextFloat());
        }

        static int randsign() {
            return sRNG.nextBoolean() ? 1 : -1;
        }

        static boolean flip() {
            return sRNG.nextBoolean();
        }

        static float mag(float x, float y) {
            return (float) Math.sqrt(x*x+y*y);
        }

        static float clamp(float x, float a, float b) {
            return ((x<a)?a:((x>b)?b:x));
        }

        static int pickInt(int[] array) {
            if (array.length == 0) return 0;
            return array[sRNG.nextInt(array.length)];
        }

        static int NUM_CIDS = 40;
        static float MIN_SCALE = 0.2f;
        static float MAX_SCALE = 1f;

        static float LUCKY = 0.001f;

        static int MAX_RADIUS = (int)(576 * MAX_SCALE);

        static int CIDS[] = {
          R.drawable.cid_angry,
          R.drawable.cid_angry,
          R.drawable.cid_angry,
          R.drawable.cid_angry,
          R.drawable.cid_normal,
          R.drawable.cid_normal,
          R.drawable.cid_confused,
        };

        static int COLORS[] = {
            0xFF0099CC,
            0xFF33B5E5,
            0xFF669900,
            0xFF99CC00,
            0xFFCC0000,
            0xFFFF8800,
            0xFFFFBB33,
            0xFF9933CC,
            0xFFAA66CC,
        };

        public class CID extends ImageView {
            public float x, y, a;

            public float va;
            public float vx, vy;

            public float r;

            public float z;

            public int h,w;

            public boolean grabbed;
            public float grabx, graby;
            private float grabx_offset, graby_offset;

            public CID(Context context, AttributeSet as) {
                super(context, as);
            }

            public String toString() {
                return String.format("<cid (%.1f, %.1f) (%d x %d)>",
                    getX(), getY(), getWidth(), getHeight());
            }

            private void pickCID() {
                int cidId = pickInt(CIDS);
                if (randfrange(0,1) <= LUCKY) {
                    cidId = R.drawable.jandycane;
                }
                BitmapDrawable cid = (BitmapDrawable) getContext().getResources().getDrawable(cidId);
                Bitmap cidBits = cid.getBitmap();
                h=cidBits.getHeight();
                w=cidBits.getWidth();

                if (DEBUG) {
                    cid.setAlpha(0x80);
                }
                this.setImageDrawable(cid);

                Paint pt = new Paint();
                final int color = pickInt(COLORS);
                ColorMatrix CM = new ColorMatrix();
                float[] M = CM.getArray();
                // we assume the color information is in the red channel
                /* R */ M[0]  = (float)((color & 0x00FF0000) >> 16) / 0xFF;
                /* G */ M[5]  = (float)((color & 0x0000FF00) >> 8)  / 0xFF;
                /* B */ M[10] = (float)((color & 0x000000FF))       / 0xFF;
                pt.setColorFilter(new ColorMatrixColorFilter(M));
                setLayerType(View.LAYER_TYPE_HARDWARE, (cidId == R.drawable.jandycane) ? null : pt);
            }

            public void reset() {
                pickCID();

                final float scale = lerp(MIN_SCALE,MAX_SCALE,z);
                setScaleX(scale); setScaleY(scale);

                r = 0.3f*Math.max(h,w)*scale;

                a=(randfrange(0,360));
                va = randfrange(-30,30);

                vx = randfrange(-40,40) * z;
                vy = randfrange(-40,40) * z;
                final float boardh = boardHeight;
                final float boardw = boardWidth;
                if (flip()) {
                    x=(vx < 0 ? boardw+2*r : -r*4f);
                    y=(randfrange(0, boardh-3*r)*0.5f + ((vy < 0)?boardh*0.5f:0));
                } else {
                    y=(vy < 0 ? boardh+2*r : -r*4f);
                    x=(randfrange(0, boardw-3*r)*0.5f + ((vx < 0)?boardw*0.5f:0));
                }
            }

            public void update(float dt) {
                if (grabbed) {
                    vx = (vx * 0.75f) + ((grabx - x) / dt) * 0.25f;
                    x = grabx;
                    vy = (vy * 0.75f) + ((graby - y) / dt) * 0.25f;;
                    y = graby;
                } else {
                    x = (x + vx * dt);
                    y = (y + vy * dt);
                    a = (a + va * dt);
                }
            }

            public float overlap(CID other) {
                final float dx = (x - other.x);
                final float dy = (y - other.y);
                return mag(dx, dy) - r - other.r;
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        grabbed = true;
                        grabx_offset = e.getRawX() - x;
                        graby_offset = e.getRawY() - y;
                        va = 0;
                        // fall
                    case MotionEvent.ACTION_MOVE:
                        grabx = e.getRawX() - grabx_offset;
                        graby = e.getRawY() - graby_offset;
                        e.getEventTime();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        grabbed = false;
                        float a = randsign() * clamp(mag(vx, vy) * 0.33f, 0, 1080f);
                        va = randfrange(a*0.5f, a);
                        break;
                }
                return true;
            }
        }

        TimeAnimator mAnim;
        private int boardWidth;
        private int boardHeight;

        public Board(Context context, AttributeSet as) {
            super(context, as);

            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

            setWillNotDraw(!DEBUG);
        }

        private void reset() {
            removeAllViews();

            final ViewGroup.LayoutParams wrap = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

            for(int i=0; i<NUM_CIDS; i++) {
                CID nv = new CID(getContext(), null);
                addView(nv, wrap);
                nv.z = ((float)i/NUM_CIDS);
                nv.z *= nv.z;
                nv.reset();
                nv.x = (randfrange(0, boardWidth));
                nv.y = (randfrange(0, boardHeight));
            }

            if (mAnim != null) {
                mAnim.cancel();
            }
            mAnim = new TimeAnimator();
            mAnim.setTimeListener(new TimeAnimator.TimeListener() {
                public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                    if (DEBUG) {
                        for (int i=0; i<getChildCount(); i++) {
                            android.util.Log.d("CIDCircus", "cid " + i + ": " + getChildAt(i));
                        }
                    }

                    for (int i=0; i<getChildCount(); i++) {
                        View v = getChildAt(i);
                        if (!(v instanceof CID)) continue;
                        CID nv = (CID) v;
                        nv.update(deltaTime / 1000f);

                        for (int j=i+1; j<getChildCount(); j++) {
                            View v2 = getChildAt(j);
                            if (!(v2 instanceof CID)) continue;
                            CID nv2 = (CID) v2;
                            nv.overlap(nv2);
                        }

                        nv.setRotation(nv.a);
                        nv.setX(nv.x-nv.getPivotX());
                        nv.setY(nv.y-nv.getPivotY());

                        if (   nv.x < - MAX_RADIUS
                            || nv.x > boardWidth + MAX_RADIUS
                            || nv.y < -MAX_RADIUS
                            || nv.y > boardHeight + MAX_RADIUS)
                        {
                            nv.reset();
                        }
                    }

                    if (DEBUG) invalidate();
                }
            });
        }

        @Override
        protected void onSizeChanged (int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w,h,oldw,oldh);
            boardWidth = w;
            boardHeight = h;
        }

        public void startAnimation() {
            stopAnimation();
            if (mAnim == null) {
                post(new Runnable() { public void run() {
                    reset();
                    startAnimation();
                } });
            } else {
                mAnim.start();
            }
        }

        public void stopAnimation() {
            if (mAnim != null) mAnim.cancel();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            stopAnimation();
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        public void onDraw(Canvas c) {
            if (DEBUG) {
                //android.util.Log.d("BeanBag", "onDraw");
                Paint pt = new Paint();
                pt.setAntiAlias(true);
                pt.setStyle(Paint.Style.STROKE);
                pt.setColor(0xFFFF0000);
                pt.setStrokeWidth(4.0f);
                c.drawRect(0, 0, getWidth(), getHeight(), pt);
                pt.setColor(0xFFFFCC00);
                pt.setStrokeWidth(1.0f);
                for (int i=0; i<getChildCount(); i++) {
                    CID b = (CID) getChildAt(i);
                    final float a = (360-b.a)/180f*3.14159f;
                    final float tx = b.getTranslationX();
                    final float ty = b.getTranslationY();
                    c.drawCircle(b.x, b.y, b.r, pt);
                    c.drawCircle(tx, ty, 4, pt);
                    c.drawLine(b.x, b.y, (float)(b.x+b.r*Math.sin(a)), (float)(b.y+b.r*Math.cos(a)), pt);
                }
            }
        }
    }

    private Board mBoard;

    @Override
    public void onStart() {
        super.onStart();

        getWindow().addFlags(
                  WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                );
        mBoard = new Board(this, null);
        setContentView(mBoard);
    }

    @Override
    public void onPause() {
        super.onPause();
        mBoard.stopAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
        mBoard.startAnimation();
    }
}
