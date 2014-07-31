/**
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.android.settings.sim;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;

import java.util.HashMap;

public class ImageListPreference extends DialogPreference {

    private static final String LOG_TAG = "ImageListPreference";

    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private TypedArray mIconArray;
    private String mValue;
    private NetTypeAdapter mAdapter;
    private int mCheckedItemEntryIndex;
    private boolean[] mTagCheckedEntryIndices;

    private int mOtherSlotCheckedIndex;
    private boolean[] mOtherSlotTagCheckedIndices;

    private ImageView mImageView;
    private ListView mListView;
    private TextView mTextView;

    private Drawable mResDrawable;

    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference);
        mEntries = array
                .getTextArray(R.styleable.ImageListPreference_entries);
        mEntryValues = array
                .getTextArray(R.styleable.ImageListPreference_entryValues);
        array.recycle();

        if (mEntries == null || mEntryValues == null || mEntries.length != mEntryValues.length) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array " +
                            "which are both the same length");
        }

        // Initialize the array of boolean to the same size as number of entries
        mTagCheckedEntryIndices = new boolean[mEntries.length];
        mOtherSlotTagCheckedIndices = new boolean[mEntries.length];
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(
                R.layout.image_preference, parent, false);
        return view;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mTextView = (TextView) view.findViewById(R.id.textview);
        mImageView = (ImageView) view.findViewById(R.id.imageview);
        if (mTextView != null) {
            mTextView.setText(getTitle());
        }
        if (mImageView != null && mResDrawable != null) {
            mImageView.setImageDrawable(mResDrawable);
        }
    }

    public void setSimIcon(Drawable resDrawable) {
        mResDrawable = resDrawable;
        notifyChanged();
    }

    public void setIconEntries(TypedArray icons) {
        mIconArray = icons;
    }

    @Override
    protected View onCreateDialogView() {
        mCheckedItemEntryIndex = getValueIndex();
        setTagCheckedEntryIndices();

        mAdapter = new NetTypeAdapter(getContext());

        mListView = new ListView(getContext());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCheckedItemEntryIndex = position;
                setTagCheckedEntryIndices();
                mAdapter.notifyDataSetChanged();

            }
        });
        return mListView;
    }

    private void setTagCheckedEntryIndices() {
        for (int i = 0; i < mTagCheckedEntryIndices.length; i++) {
            mTagCheckedEntryIndices[i] = mCheckedItemEntryIndex == i ? true : false;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult && mCheckedItemEntryIndex >= 0
                && mEntryValues != null) {
            String value = mEntryValues[mCheckedItemEntryIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

    /**
     * set the default value
     *
     * @param defaultValue: the index of checked item. need Integer.
     */
    @Override
    public void setDefaultValue(Object defaultValue) {
        super.setDefaultValue(defaultValue);
        mCheckedItemEntryIndex = (Integer) defaultValue;
        if (mCheckedItemEntryIndex < mEntryValues.length && mCheckedItemEntryIndex >= 0) {
            String value = mEntryValues[mCheckedItemEntryIndex].toString();
            setValue(value);
        } else {
            Log.e(LOG_TAG, "The range of index is error!");
        }
    }

    public void setOtherSlotValue(Object otherSlotValue) {
        mOtherSlotCheckedIndex = (Integer) otherSlotValue;
        for (int i = 0; i < mOtherSlotTagCheckedIndices.length; i++) {
            mOtherSlotTagCheckedIndices[i] = mOtherSlotCheckedIndex == i ? true : false;
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        setValue(restorePersistedValue ? getPersistedString(mValue) : (String) defaultValue);
    }

    public void setValue(String value) {
        mValue = value;
        persistString(value);
    }

    public String getValue() {
        return mValue;
    }

    private int getValueIndex() {
        return findIndexOfValue(mValue);
    }

    public int findIndexOfValue(String value) {
        if (value != null && mEntryValues != null) {
            for (int i = mEntryValues.length - 1; i >= 0; i--) {
                if (mEntryValues[i].equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public class NetTypeAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private HashMap<String, Object> mMap = new HashMap<String, Object>();

        class SimNetType {
            Drawable _icon;
            CharSequence _name;

            public SimNetType (Drawable icon, CharSequence name) {
                _icon = icon;
                _name = name;
            }
            public Drawable getIcon() {
                return _icon;
            };

            public CharSequence getName() {
                return _name;
            };
        }

        private void initResource () {
            for (int i = 0; i < mEntries.length; i++) {
                mMap.put(Integer.toString(i), new SimNetType(mIconArray.getDrawable(i),
                        mEntries[i]));
            }
        }

        private SimNetType getSimNetType(int index) {
            return (SimNetType) mMap.get(Integer.toString(index));
        }

        public NetTypeAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            initResource();
        }

        public int getCount() {
            return mEntries.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public final class ViewHolder {
            public ImageView netTypeIcon;
            public CheckedTextView netTypeName;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.image_preference_item, null);
                holder.netTypeIcon = (ImageView) convertView
                        .findViewById(R.id.item_imageview);
                holder.netTypeName = (CheckedTextView) convertView
                        .findViewById(R.id.item_checkedtextview);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.netTypeIcon.setImageDrawable(getSimNetType(position).getIcon());
            holder.netTypeName.setText(getSimNetType(position).getName());

            holder.netTypeName.setChecked(mTagCheckedEntryIndices[position]);
            holder.netTypeName.setFocusable(mOtherSlotTagCheckedIndices[position]);
            holder.netTypeName.setEnabled(!mOtherSlotTagCheckedIndices[position]);
            if (mOtherSlotTagCheckedIndices[position]) {
                holder.netTypeName.setChecked(true);
            }
            return convertView;
        }

    }

}
