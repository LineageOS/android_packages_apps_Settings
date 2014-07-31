/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cyanogenmod;

import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.internal.util.cm.SpamFilter;
import com.android.internal.util.cm.SpamFilter.SpamContract;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Settings.NotificationStationActivity;

import java.util.ArrayList;
import java.util.List;

public class SpamList extends ListFragment {

    private static final int MENU_NOTIFICATIONS = Menu.FIRST;
    private static final Uri PACKAGES_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SpamFilter.AUTHORITY)
            .encodedPath("packages")
            .build();
    private static final Uri PACKAGES_NOTIFICATION_URI = PACKAGES_URI.buildUpon()
            .encodedPath("message")
            .build();

    private SpamAdapter mAdapter;
    private FetchFilterTask mTask;

    private ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            if (mTask != null) {
                mTask.cancel(true);
            }
            mTask = new FetchFilterTask();
            mTask.execute();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(com.android.internal.R.layout.preference_list_fragment,
                container, false);
        TextView emptyView = (TextView) v.findViewById(android.R.id.empty);
        emptyView.setText(R.string.no_filters_title);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTask = new FetchFilterTask();
        mTask.execute();
        getListView().setDividerHeight(0);
        setHasOptionsMenu(true);
        getActivity().getContentResolver().registerContentObserver(
                SpamFilter.NOTIFICATION_URI, true, mObserver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_NOTIFICATIONS, 0, R.string.quiet_hours_mute)
                .setIcon(R.drawable.ic_settings_notifications)
                .setAlphabeticShortcut('n')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NOTIFICATIONS:
                Intent i = new Intent(getActivity(), NotificationStationActivity.class);
                startActivity(i);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    private static class ItemInfo {
        int id;
    }

    private static final class PackageInfo extends ItemInfo {
        String packageName;
        CharSequence applicationLabel;
    }

    private static final class NotificationInfo extends ItemInfo {
        String messageText;
        CharSequence appLabel;
        long date;
        int count;
    }

    private class FetchFilterTask extends AsyncTask<Void, Void, List<ItemInfo>> {

        private void addNotificationsForPackage(PackageInfo pInfo, List<ItemInfo> items) {
            Uri notificationUri = Uri.withAppendedPath(PACKAGES_NOTIFICATION_URI,
                    String.valueOf(pInfo.id));
            Cursor c = getActivity().getContentResolver().query(notificationUri,
                    null, null, null, null);
            if (c != null) {
                int notificationIdIndex = c.getColumnIndex(SpamContract.NotificationTable.ID);
                int notificationMessageIndex =
                        c.getColumnIndex(SpamContract.NotificationTable.MESSAGE_TEXT);
                int notificationBlockedIndex =
                        c.getColumnIndex(SpamContract.NotificationTable.LAST_BLOCKED);
                int notificationCountIndex =
                        c.getColumnIndex(SpamContract.NotificationTable.COUNT);

                while (c.moveToNext()) {
                    NotificationInfo nInfo = new NotificationInfo();
                    nInfo.messageText = c.getString(notificationMessageIndex);
                    nInfo.id = c.getInt(notificationIdIndex);
                    nInfo.date = c.getLong(notificationBlockedIndex);
                    nInfo.count = c.getInt(notificationCountIndex);
                    nInfo.appLabel = pInfo.applicationLabel;
                    items.add(nInfo);
                }
                c.close();
            }
        }

        @Override
        protected List<ItemInfo> doInBackground(Void... params) {
            List<ItemInfo> items = new ArrayList<ItemInfo>();
            Cursor c = getActivity().getContentResolver().query(PACKAGES_URI,
                    null, null, null, null);
            if (c != null) {
                int packageIdIndex = c.getColumnIndex(SpamContract.PackageTable.ID);
                int packageNameIndex = c.getColumnIndex(SpamContract.PackageTable.PACKAGE_NAME);
                while (c.moveToNext()) {
                    PackageInfo pInfo = new PackageInfo();
                    pInfo.id = c.getInt(packageIdIndex);
                    pInfo.packageName = c.getString(packageNameIndex);
                    pInfo.applicationLabel = fetchAppLabel(pInfo.packageName);
                    items.add(pInfo);
                    addNotificationsForPackage(pInfo, items);
                }
                c.close();
            }
            return items;
        }

        private CharSequence fetchAppLabel(String packageName) {
            PackageManager pm = getActivity().getPackageManager();
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                return appInfo.loadLabel(pm);
            } catch (PackageManager.NameNotFoundException e) {
                return packageName;
            }
        }

        @Override
        protected void onPostExecute(List<ItemInfo> result) {
            mAdapter = new SpamAdapter(getActivity(), result);
            setListAdapter(mAdapter);
            mTask = null;
        }
    }

    private static class SpamAdapter extends BaseAdapter implements View.OnClickListener {
        private static final int HEADER_TYPE = 0;
        private static final int ENTRY_TYPE = 1;
        private List<ItemInfo> mItems;
        private Context mContext;

        SpamAdapter(Context context, List<ItemInfo> items) {
            mContext = context;
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public ItemInfo getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof PackageInfo ? HEADER_TYPE : ENTRY_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == ENTRY_TYPE;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            ItemInfo info = getItem(position);

            if (viewType == HEADER_TYPE) {
                if (convertView == null) {
                    convertView = new TextView(mContext, null,
                            android.R.attr.listSeparatorTextViewStyle);
                }
                TextView titleView = (TextView) convertView;
                titleView.setText(((PackageInfo) info).applicationLabel);
            } else {
                final ViewHolder holder;
                final NotificationInfo nInfo = (NotificationInfo) info;

                if (convertView == null) {
                    convertView = View.inflate(mContext, R.layout.spam_item_row, null);
                    holder = new ViewHolder();
                    holder.message = (TextView) convertView.findViewById(R.id.label);
                    holder.dateAndCount = (TextView) convertView.findViewById(R.id.date_and_count);
                    holder.deleteButton = convertView.findViewById(R.id.spam_item_remove);
                    holder.deleteButton.setOnClickListener(this);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.message.setText(nInfo.messageText);

                CharSequence dateString = DateUtils.getRelativeTimeSpanString(nInfo.date);
                if (nInfo.count == 0) {
                    holder.dateAndCount.setText(mContext.getString(R.string.spam_added_title,
                            dateString));
                } else {
                    String countString = mContext.getResources().getQuantityString(
                            R.plurals.app_ops_count, nInfo.count, nInfo.count);
                    holder.dateAndCount.setText(mContext.getString(R.string.spam_last_blocked_title,
                            dateString, countString));
                }

                holder.deleteButton.setTag(nInfo);
            }

            return convertView;
        }

        @Override
        public void onClick(View v) {
            NotificationInfo item = (NotificationInfo) v.getTag();
            Uri uri = Uri.withAppendedPath(PACKAGES_NOTIFICATION_URI, String.valueOf(item.id));
            mContext.getContentResolver().delete(uri, null, null);
            notifyDataSetChanged();
        }

        private static class ViewHolder {
            TextView message;
            TextView dateAndCount;
            View deleteButton;
        }
    }
}
