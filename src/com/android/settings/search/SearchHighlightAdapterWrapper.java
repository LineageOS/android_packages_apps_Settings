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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOverlay;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

/**
 * Adapter wrapper which highlights a search result at a certain position
 */
public class SearchHighlightAdapterWrapper implements WrapperListAdapter {
    private ListAdapter mAdapter;
    private int mHighlightedPosition = -1;
    private boolean mHighlightDone;
    private int mInitialDelay;
    private int mDuration;
    private int mHighlightColor;
    private View mHighlightedView;

    private static final int MSG_HIGHLIGHT = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIGHLIGHT:
                    addHighlight((View) msg.obj);
                    mHighlightDone = true;
                    break;
            }
        }
    };

    public SearchHighlightAdapterWrapper(ListAdapter adapter,
            int initialDelay, int duration, int highlightColor) {
        mAdapter = adapter;
        mInitialDelay = initialDelay;
        mDuration = duration;
        mHighlightColor = highlightColor;
    }

    public void setHighlightedPosition(int highlightedPosition) {
        if (mHighlightedPosition != highlightedPosition) {
            mHighlightedPosition = highlightedPosition;
            removeHighlight();
            mHighlightDone = false;
        }
    }

    @Override
    public ListAdapter getWrappedAdapter() {
        return mAdapter;
    }

    @Override
    public int getCount() {
        return mAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mAdapter.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mAdapter.getItemViewType(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null && convertView == mHighlightedView) {
            removeHighlight();
        }
        final View v = mAdapter.getView(position, convertView, parent);
        if (position == mHighlightedPosition && !mHighlightDone) {
            Message msg = mHandler.obtainMessage(MSG_HIGHLIGHT, v);
            mHighlightedView = v;
            mHandler.sendMessageDelayed(msg, mInitialDelay);
        }
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return mAdapter.getViewTypeCount();
    }

    @Override
    public boolean hasStableIds() {
        return mAdapter.hasStableIds();
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mAdapter.unregisterDataSetObserver(observer);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mAdapter.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return mAdapter.isEnabled(position);
    }

    private void addHighlight(View view) {
        final ColorDrawable d = new ColorDrawable(mHighlightColor);
        d.setBounds(0, 0, view.getWidth(), view.getHeight());
        view.getOverlay().add(d);
        view.setTag(d);

        ObjectAnimator bgAnim = ObjectAnimator.ofInt(d, "alpha", 0, 65, 0);
        bgAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                removeHighlight();
            }
        });
        bgAnim.setDuration(mDuration);
        bgAnim.start();
    }

    private void removeHighlight() {
        if (mHighlightedView == null) {
            return;
        }
        mHandler.removeMessages(MSG_HIGHLIGHT);
        ColorDrawable d = (ColorDrawable) mHighlightedView.getTag();
        if (d != null) {
            mHighlightedView.getOverlay().remove(d);
            mHighlightedView.setTag(null);
        }
        mHighlightedView = null;
    }
}
