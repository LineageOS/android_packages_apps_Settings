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
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceActivity.Header;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class SettingsSearchFilterAdapter extends BaseAdapter implements Filterable {
    private Context mContext;
    private List<SearchInfo> mSearchInfo;
    private List<SearchInfo> mFilteredInfo;
    private LayoutInflater mInflater;
    private Resources mResources;
    private Drawable mDefaultIcon;
    private int mMatchHighlightColor;
    private CharSequence mLastConstraint;

    private SparseArray<Drawable> mIconCache = new SparseArray<Drawable>();

    private ResultReceiver mPopulateDoneReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            new LoadSearchInfoTask().execute();
        }
    };

    private Filter mFilter = new Filter() {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            Filter.FilterResults results = new Filter.FilterResults();
            if (mFilteredInfo == null) {
                results.values = null;
                results.count = 1;
            } else {
                ArrayList<SearchInfo> filtered = filterInfos(constraint);
                results.values = filtered;
                results.count = filtered.size();
            }
            mLastConstraint = constraint;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            mFilteredInfo = (List<SearchInfo>) results.values;
            notifyDataSetChanged();
        }
    };

    private class LoadSearchInfoTask extends AsyncTask<Void, Void, List<SearchInfo>> {
        @Override
        protected List<SearchInfo> doInBackground(Void... param) {
            return SearchPopulator.loadSearchData(mContext);
        }

        @Override
        protected void onPostExecute(List<SearchInfo> infos) {
            mSearchInfo = infos;
            if (mLastConstraint == null) {
                mFilteredInfo = new ArrayList<SearchInfo>(infos);
            } else {
                mFilteredInfo = filterInfos(mLastConstraint);
            }
            notifyDataSetChanged();
        }
    };

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

    public SettingsSearchFilterAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResources = mContext.getResources();
        mDefaultIcon = mResources.getDrawable(R.drawable.default_search_icon);
        mMatchHighlightColor = mResources.getColor(R.color.search_match_highlight_foreground);

        Intent i = new Intent(context, SearchPopulator.class);
        i.putExtra(SearchPopulator.EXTRA_NOTIFIER, mPopulateDoneReceiver);
        context.startService(i);
    }

    @Override
    public int getCount() {
        if (mFilteredInfo != null) {
            return mFilteredInfo.size();
        }
        return 1;
    }

    @Override
    public SearchInfo getItem(int position) {
        if (mFilteredInfo == null) {
            return null;
        }
        return mFilteredInfo.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mFilteredInfo == null ? 1 : 0;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (mFilteredInfo == null) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.settings_search_busy_view, null);
            }
            return convertView;
        }

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.settings_search_complete_view, null);
            holder = new ViewHolder();
            holder.imageView  = (ImageView) convertView.findViewById(R.id.autocomplete_image);
            holder.titleView  = (TextView) convertView.findViewById(R.id.autocomplete_title);
            holder.parentView = (TextView) convertView.findViewById(R.id.autocomplete_parent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SearchInfo info = mFilteredInfo.get(position);
        Drawable d = mIconCache.get(info.iconRes);
        if (info.iconRes != 0) {
            d = mResources.getDrawable(info.iconRes);
            mIconCache.put(info.iconRes, d);
        }
        if (d == null) {
            d = mDefaultIcon;
        }
        holder.imageView.setImageDrawable(d);

        if (info.mMatchStart >= 0 && info.mMatchEnd >= 0) {
            SpannableStringBuilder titleSpan = new SpannableStringBuilder(info.title);
            ForegroundColorSpan span = new ForegroundColorSpan(mMatchHighlightColor);
            titleSpan.setSpan(span, info.mMatchStart, info.mMatchEnd,
                    SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE);
            holder.titleView.setText(titleSpan);
        } else {
            holder.titleView.setText(info.title);
        }

        if (info.parentTitle != 0) {
            holder.parentView.setText(info.parentTitle);
            holder.parentView.setVisibility(View.VISIBLE);
        } else {
            holder.parentView.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private ArrayList<SearchInfo> filterInfos(CharSequence constraint) {
        ArrayList<SearchInfo> filteredValues = new ArrayList<SearchInfo>();
        if (constraint == null) {
            return filteredValues;
        }

        String actualConstraint = constraint.toString().trim().toLowerCase();
        if (actualConstraint.isEmpty()) {
            return filteredValues;
        }

        String filteredConstraint = removeNonAlphaNumeric(actualConstraint);

        for (SearchInfo item : mSearchInfo) {
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

                filteredValues.add(item);
            }
        }

        return filteredValues;
    }

    private static String removeNonAlphaNumeric(String s) {
        return s.replaceAll("[^\\p{L}\\p{Nd}]+", "");
    }

    private static class ViewHolder {
        private ImageView imageView;
        private TextView titleView;
        private TextView parentView;
    }
}
