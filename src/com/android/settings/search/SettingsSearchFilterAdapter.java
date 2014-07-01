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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

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

    public static class SearchInfo {
        public Header header;
        public int level;
        public String fragment;
        public String title;
        public int iconRes;
        public String parentTitle;
    }

    public SettingsSearchFilterAdapter(Context context, int resourceId, ArrayList<SearchInfo> infos) {
        super(context, resourceId, infos);
        mOriginalValues = new ArrayList<SearchInfo>(infos);
        mObjects = new ArrayList<SearchInfo>(mOriginalValues);
        mContext = context;
        mResId = resourceId;
        mResources = mContext.getResources();
        mDefaultIcon = mResources.getDrawable(R.drawable.default_search_icon);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(mResId, null);
            holder = new ViewHolder();
            holder.imageView = (ImageView) v.findViewById(R.id.autocomplete_image);
            holder.titleView = (TextView)  v.findViewById(R.id.autocomplete_title);
            v.setTag(holder);
        }else {
            holder = (ViewHolder) v.getTag();
        }

        SearchInfo info = mObjects.get(position);
        if (info != null) {
            if (holder.imageView != null) {
                Drawable d = null;
                if (info.iconRes != 0) {
                    d = mResources.getDrawable(info.iconRes);
                }
                if (d == null) {
                    d = mDefaultIcon;
                }
                holder.imageView.setImageDrawable(d);
            }
            if (holder.titleView != null) {
                holder.titleView.setText(info.title);
            }
        }
        return v;
    }

    @Override
    public int getCount() {
        if(mObjects == null)
            return 0;
        else
            return mObjects.size();
    }

    @Override
    public SearchInfo getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new CustomFilter();
        }
        return mFilter;
    }

    private static class ViewHolder {
        private ImageView imageView;
        private TextView titleView;
    }

    private String removeNonAlphaNumeric(String s) {
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    private class CustomFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint == null) {
                return results;
            }

            constraint = constraint.toString().trim();

            if (constraint.length() == 0) {
                return results;
            } else {
                String filteredConstraint = removeNonAlphaNumeric(constraint.toString());
                ArrayList<SearchInfo> newValues = new ArrayList<SearchInfo>();
                for (int i = 0; i < mOriginalValues.size(); i++) {
                    SearchInfo item = mOriginalValues.get(i);
                    String filteredTitle = removeNonAlphaNumeric(item.title.toString());
                    if (item.title.toString().toLowerCase().contains(
                            constraint.toString().toLowerCase()) ||
                            filteredTitle.toLowerCase().contains(
                                    filteredConstraint.toLowerCase())) {
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
                                      FilterResults results) {
            mObjects = (List<SearchInfo>) results.values;
            notifyDataSetChanged();
        }
    }
}
