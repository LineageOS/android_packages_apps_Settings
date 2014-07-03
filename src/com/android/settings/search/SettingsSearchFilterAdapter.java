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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceActivity.Header;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.search.SettingsSearchFilterAdapter.SearchInfo;

import java.util.ArrayList;
import java.util.List;

public class SettingsSearchFilterAdapter extends ArrayAdapter<SearchInfo> implements Filterable {
    private List<SearchInfo> mOriginalValues;
    private List<SearchInfo> mObjects;
    private Filter mFilter;
    private int mResId;
    private Context mContext;
    private Resources mResources;
    private Drawable mDefaultIcon;
    private int mMatchHighlightColor;

    private SparseArray<Drawable> mIconCache = new SparseArray<Drawable>();

    public static class SearchInfo {
        public final Header header;
        public final int level;
        public final String fragment;
        public final String title;
        public final int iconRes;
        public final int parentTitle;
        public final String key;

        private String mNormalizedTitle;
        private int mMatchStart;
        private int mMatchEnd;

        public SearchInfo(Header header, int level, String fragment, String title,
                int iconRes, int parentTitle, String key) {
            this.header = header;
            this.level = level;
            this.fragment = fragment;
            this.title = title;
            this.iconRes = iconRes;
            this.parentTitle = parentTitle;
            this.key = key;

            mNormalizedTitle = removeNonAlphaNumeric(title.toLowerCase());
            mMatchStart = -1;
            mMatchEnd = -1;
        }
    }

    public SettingsSearchFilterAdapter(Context context, int resourceId,
            ArrayList<SearchInfo> infos) {
        super(context, resourceId, infos);
        mOriginalValues = new ArrayList<SearchInfo>(infos);
        mObjects = new ArrayList<SearchInfo>(mOriginalValues);
        mContext = context;
        mResId = resourceId;
        mResources = mContext.getResources();
        mDefaultIcon = mResources.getDrawable(R.drawable.default_search_icon);
        mMatchHighlightColor = mResources.getColor(R.color.search_match_highlight_foreground);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            v = inflater.inflate(mResId, null);
            holder = new ViewHolder();
            holder.imageView  = (ImageView) v.findViewById(R.id.autocomplete_image);
            holder.titleView  = (TextView)  v.findViewById(R.id.autocomplete_title);
            holder.parentView = (TextView)  v.findViewById(R.id.autocomplete_parent);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        SearchInfo info = mObjects.get(position);
        if (info != null) {
            if (holder.imageView != null) {
                Drawable d = mIconCache.get(info.iconRes);
                if (info.iconRes != 0) {
                    d = mResources.getDrawable(info.iconRes);
                    mIconCache.put(info.iconRes, d);
                }
                if (d == null) {
                    d = mDefaultIcon;
                }
                holder.imageView.setImageDrawable(d);
            }
            if (holder.titleView != null) {
                if (info.mMatchStart >= 0 && info.mMatchEnd >= 0) {
                    SpannableStringBuilder titleSpan = new SpannableStringBuilder(info.title);
                    ForegroundColorSpan span = new ForegroundColorSpan(mMatchHighlightColor);
                    titleSpan.setSpan(span, info.mMatchStart, info.mMatchEnd,
                            SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);
                    holder.titleView.setText(titleSpan);
                } else {
                    holder.titleView.setText(info.title);
                }
            }
            if (holder.parentView != null) {
                if (info.parentTitle != 0) {
                    holder.parentView.setText(info.parentTitle);
                    holder.parentView.setVisibility(View.VISIBLE);
                } else {
                    holder.parentView.setVisibility(View.GONE);
                }
            }
        }
        return v;
    }

    @Override
    public int getCount() {
        if (mObjects == null) {
            return 0;
        } else {
            return mObjects.size();
        }
    }

    @Override
    public SearchInfo getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new CustomFilter();
        }
        return mFilter;
    }

    private static String removeNonAlphaNumeric(String s) {
        return s.replaceAll("[^\\p{L}\\p{Nd}]+", "");
    }

    private static class ViewHolder {
        private ImageView imageView;
        private TextView titleView;
        private TextView parentView;
    }

    private class CustomFilter extends Filter {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            Filter.FilterResults results = new Filter.FilterResults();

            if (constraint == null) {
                return results;
            }

            String actualConstraint = constraint.toString().trim().toLowerCase();
            if (actualConstraint.length() > 0) {
                String filteredConstraint = removeNonAlphaNumeric(actualConstraint);
                ArrayList<SearchInfo> newValues = new ArrayList<SearchInfo>();

                for (int i = 0; i < mOriginalValues.size(); i++) {
                    SearchInfo item = mOriginalValues.get(i);
                    String title = item.title.toLowerCase();
                    String filteredTitle = item.mNormalizedTitle;

                    item.mMatchStart = -1;
                    item.mMatchEnd = -1;

                    int pos = filteredTitle.indexOf(filteredConstraint);
                    if (pos != -1) {
                        int unfilteredLen = title.length();
                        int filteredLen = filteredTitle.length();
                        int constraintLen = filteredConstraint.length();
                        for (int ufIndex = pos, fIndex = pos;
                                ufIndex < unfilteredLen && fIndex < filteredLen; ufIndex++) {
                            if (title.charAt(ufIndex) != filteredTitle.charAt(fIndex)) {
                                continue;
                            }
                            if (fIndex == pos) {
                                item.mMatchStart = ufIndex;
                            }
                            if (fIndex == pos + constraintLen - 1) {
                                item.mMatchEnd = ufIndex + 1;
                                break;
                            }
                            fIndex++;
                        }

                        newValues.add(item);
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                Filter.FilterResults results) {
            mObjects = (List<SearchInfo>) results.values;
            notifyDataSetChanged();
        }
    }
}
