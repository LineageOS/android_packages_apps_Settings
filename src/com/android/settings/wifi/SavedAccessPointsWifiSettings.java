/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI to manage saved networks/access points.
 */
public class SavedAccessPointsWifiSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, Indexable {

    private static final String TAG = "SavedAccessPointsWifiSettings";

    private static class DraggableSortListView extends ListView {

        private ImageView mDragView;
        private WindowManager mWindowManager;
        private WindowManager.LayoutParams mWindowParams;
        private int mDragPos;
        private int mFirstDragPos;
        private int mDragPoint;
        private int mCoordOffset;
        private DropListener mDropListener;
        private int mUpperBound;
        private int mLowerBound;
        private int mHeight;
        private Rect mTempRect = new Rect();
        private Bitmap mDragBitmap;
        private final int mTouchSlop;
        private int mItemHeight;

        public DraggableSortListView(Context context) {
            super(context);
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (mDropListener != null && getChildCount() > 1) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        int x = (int) ev.getX();
                        int y = (int) ev.getY();
                        int itemnum = pointToPosition(x, y);
                        if (itemnum == AdapterView.INVALID_POSITION) {
                            break;
                        }
                        ViewGroup item = (ViewGroup) getChildAt(itemnum - getFirstVisiblePosition());
                        mItemHeight = item.getHeight();
                        mDragPoint = y - item.getTop();
                        mCoordOffset = ((int) ev.getRawY()) - y;
                        View dragger = item.findViewById(com.android.internal.R.id.icon);

                        // The dragger icon itself is quite small, so pretend the
                        // touch area is bigger
                        int x1 = item.getLeft() + dragger.getLeft() - (dragger.getWidth() / 2);
                        int x2 = item.getLeft() + dragger.getRight() + (dragger.getWidth() / 2);
                        if (x > x1 && x < x2) {
                            // Fix x position while dragging
                            int[] itemPos = new int[2];
                            item.getLocationOnScreen(itemPos);

                            item.setDrawingCacheEnabled(true);
                            // Create a copy of the drawing cache so that it does
                            // not get recycled
                            // by the framework when the list tries to clean up
                            // memory
                            Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                            startDragging(bitmap, itemPos[0], y);
                            mDragPos = itemnum;
                            mFirstDragPos = mDragPos;
                            mHeight = getHeight();
                            int touchSlop = mTouchSlop;
                            mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                            mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);
                            return false;
                        }
                        stopDragging();
                        break;
                }
            }
            return super.onInterceptTouchEvent(ev);
        }

        /*
         * pointToPosition() doesn't consider invisible views, but we need to, so
         * implement a slightly different version.
         */
        private int myPointToPosition(int x, int y) {

            if (y < 0) {
                // when dragging off the top of the screen, calculate position
                // by going back from a visible item
                int pos = myPointToPosition(x, y + mItemHeight);
                if (pos > 0) {
                    return pos - 1;
                }
            }

            Rect frame = mTempRect;
            final int count = getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return getFirstVisiblePosition() + i;
                }
            }
            return INVALID_POSITION;
        }

        private int getItemForPosition(int y) {
            int adjustedy = y - mDragPoint - (mItemHeight / 2);
            int pos = myPointToPosition(0, adjustedy);
            if (pos >= 0) {
                if (pos <= mFirstDragPos) {
                    pos += 1;
                }
            } else if (adjustedy < 0) {
                // this shouldn't happen anymore now that myPointToPosition deals
                // with this situation
                pos = 0;
            }
            return pos;
        }

        private void adjustScrollBounds(int y) {
            if (y >= mHeight / 3) {
                mUpperBound = mHeight / 3;
            }
            if (y <= mHeight * 2 / 3) {
                mLowerBound = mHeight * 2 / 3;
            }
        }

        /*
         * Restore size and visibility for all listitems
         */
        private void unExpandViews(boolean deletion) {
            for (int i = 0;; i++) {
                View v = getChildAt(i);
                if (v == null) {
                    if (deletion) {
                        // HACK force update of mItemCount
                        int position = getFirstVisiblePosition();
                        int y = getChildAt(0).getTop();
                        setAdapter(getAdapter());
                        setSelectionFromTop(position, y);
                        // end hack
                    }
                    layoutChildren(); // force children to be recreated where needed
                    v = getChildAt(i);
                    if (v == null) {
                        break;
                    }
                }
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = mItemHeight;
                v.setLayoutParams(params);
                v.setVisibility(View.VISIBLE);
                // Reset the drawing cache, the positions might have changed.
                // We don't want the cache to be wrong.
                v.setDrawingCacheEnabled(false);
            }
        }

        /*
         * Adjust visibility and size to make it appear as though an item is being
         * dragged around and other items are making room for it: If dropping the
         * item would result in it still being in the same place, then make the
         * dragged listitem's size normal, but make the item invisible. Otherwise,
         * if the dragged listitem is still on screen, make it as small as possible
         * and expand the item below the insert point. If the dragged item is not on
         * screen, only expand the item below the current insertpoint.
         */
        private void doExpansion() {
            int childnum = mDragPos - getFirstVisiblePosition();
            if (mDragPos > mFirstDragPos) {
                childnum++;
            }

            View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

            for (int i = 0;; i++) {
                View vv = getChildAt(i);
                if (vv == null) {
                    break;
                }
                int height = mItemHeight;
                int visibility = View.VISIBLE;
                if (vv.equals(first)) {
                    // processing the item that is being dragged
                    if (mDragPos == mFirstDragPos) {
                        // hovering over the original location
                        visibility = View.INVISIBLE;
                    } else {
                        // not hovering over it
                        height = 1;
                    }
                } else if (i == childnum) {
                    if (mDragPos < getCount() - 1) {
                        height = mItemHeight * 2;
                    }
                }
                ViewGroup.LayoutParams params = vv.getLayoutParams();
                params.height = height;
                vv.setLayoutParams(params);
                vv.setVisibility(visibility);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (mDropListener != null && mDragView != null) {
                int action = ev.getAction();
                switch (action) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        Rect r = mTempRect;
                        mDragView.getDrawingRect(r);
                        stopDragging();
                        if (mDropListener != null && mDragPos >= 0 && mDragPos < getCount()) {
                            mDropListener.drop(mFirstDragPos, mDragPos);
                        }
                        unExpandViews(false);
                        break;

                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        int x = (int) ev.getX();
                        int y = (int) ev.getY();
                        dragView(x, y);
                        int itemnum = getItemForPosition(y);
                        if (itemnum >= 0) {
                            if (action == MotionEvent.ACTION_DOWN || itemnum != mDragPos) {
                                mDragPos = itemnum;
                                doExpansion();
                            }
                            int speed = 0;
                            adjustScrollBounds(y);
                            if (y > mLowerBound) {
                                // scroll the list up a bit
                                speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
                            } else if (y < mUpperBound) {
                                // scroll the list down a bit
                                speed = y < mUpperBound / 2 ? -16 : -4;
                            }
                            if (speed != 0) {
                                int ref = pointToPosition(0, mHeight / 2);
                                if (ref == AdapterView.INVALID_POSITION) {
                                    // we hit a divider or an invisible view, check
                                    // somewhere else
                                    ref = pointToPosition(0, mHeight / 2 + getDividerHeight() + 64);
                                }
                                View v = getChildAt(ref - getFirstVisiblePosition());
                                if (v != null) {
                                    int pos = v.getTop();
                                    setSelectionFromTop(ref, pos - speed);
                                }
                            }
                        }
                        break;
                }
                return true;
            }
            return super.onTouchEvent(ev);
        }

        private void startDragging(Bitmap bm, int x, int y) {
            stopDragging();

            mWindowParams = new WindowManager.LayoutParams();
            mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
            mWindowParams.x = x;
            mWindowParams.y = y - mDragPoint + mCoordOffset;

            mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            mWindowParams.format = PixelFormat.TRANSLUCENT;
            mWindowParams.windowAnimations = 0;

            Context context = getContext();
            ImageView v = new ImageView(context);
            int backGroundColor = context.getResources().getColor(android.R.color.holo_blue_dark);
            v.setAlpha((float) 0.7);
            v.setBackgroundColor(backGroundColor);
            v.setImageBitmap(bm);
            mDragBitmap = bm;

            mWindowManager = (WindowManager) context.getSystemService("window");
            mWindowManager.addView(v, mWindowParams);
            mDragView = v;
        }

        private void dragView(int x, int y) {
            mWindowParams.y = y - mDragPoint + mCoordOffset;
            mWindowManager.updateViewLayout(mDragView, mWindowParams);
        }

        private void stopDragging() {
            if (mDragView != null) {
                mDragView.setVisibility(GONE);
                WindowManager wm = (WindowManager) getContext().getSystemService("window");
                wm.removeView(mDragView);
                mDragView.setImageDrawable(null);
                mDragView = null;
            }
            if (mDragBitmap != null) {
                mDragBitmap.recycle();
                mDragBitmap = null;
            }
        }

        public void setDropListener(DropListener l) {
            mDropListener = l;
        }

        public interface DropListener {
            void drop(int from, int to);
        }
    }


    private DraggableSortListView.DropListener mDropListener =
            new DraggableSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from == to) return;

            PreferenceScreen preferences = getPreferenceScreen();
            int count = preferences.getPreferenceCount();

            // Sort the new list
            List<AccessPoint> aps = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                aps.add((AccessPoint) preferences.getPreference(i));
            }
            AccessPoint o = aps.remove(from);
            aps.add(to, o);

            // Update the priorities
            for (int i = 0; i < count; i++) {
                AccessPoint ap = aps.get(i);
                WifiConfiguration config = ap.getConfig();
                config.priority = count - i;

                mWifiManager.updateNetwork(config);
            }

            // Now, save all the Wi-Fi configuration with its new priorities
            mWifiManager.saveConfiguration();

            // Send a disconnect to ensure the new wifi priorities are detected
            mWifiManager.disconnect();

            // Redraw the listview
            initPreferences();
        }
    };

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mNetworksListView.setDropListener(isAutoConfigPriorities() ? null : mDropListener);
            getActivity().invalidateOptionsMenu();
        }
    };


    private static final int MENU_ID_AUTO_CONFIG_PRIORITIES = Menu.FIRST;

    private WifiDialog mDialog;
    private WifiManager mWifiManager;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;
    private AccessPoint mSelectedAccessPoint;

    private DraggableSortListView mNetworksListView;

    // Instance state key
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    public SavedAccessPointsWifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();

        mNetworksListView.setDropListener(isAutoConfigPriorities() ? null : mDropListener);
        getActivity().invalidateOptionsMenu();
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION), false, mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onResume();
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mNetworksListView = new DraggableSortListView(getActivity());
        mNetworksListView.setId(android.R.id.list);
        mNetworksListView.setDropListener(isAutoConfigPriorities() ? null : mDropListener);
        return mNetworksListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                    savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }
        }

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the user is not allowed to configure wifi, do not show the menu.
        if (isUiRestricted()) return;

        addOptionsMenuItems(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    void addOptionsMenuItems(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_AUTO_CONFIG_PRIORITIES, 0, R.string.wifi_auto_config_priorities)
                .setCheckable(true)
                .setChecked(isAutoConfigPriorities())
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the user is not allowed to configure wifi, do not handle menu selections.
        if (isUiRestricted()) return false;

        switch (item.getItemId()) {
            case MENU_ID_AUTO_CONFIG_PRIORITIES:
                boolean autoConfig = !item.isChecked();

                // Set the system settings and refresh the listview
                Settings.Global.putInt(getActivity().getContentResolver(),
                        Settings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION, autoConfig ? 1 : 0);
                mNetworksListView.setDropListener(autoConfig ? null : mDropListener);
                item.setChecked(autoConfig);

                if (!autoConfig) {
                    // Reenable all the entries
                    PreferenceScreen preferences = getPreferenceScreen();
                    int count = preferences.getPreferenceCount();
                    for (int i = 0; i < count; i++) {
                        AccessPoint ap = (AccessPoint) preferences.getPreference(i);
                        WifiConfiguration config = ap.getConfig();
                        mWifiManager.enableNetwork(config.networkId, false);
                    }
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final Context context = getActivity();

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final List<AccessPoint> accessPoints = constructSavedAccessPoints(context, mWifiManager);

        preferenceScreen.setOrderingAsAdded(false);
        preferenceScreen.removeAll();

        final int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; ++i){
            final AccessPoint ap = accessPoints.get(i);
            ap.setOrder(i);
            preferenceScreen.addPreference(ap);
        }

        if(getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    private static List<WifiConfiguration> getConfiguredNetworks(WifiManager wifiManager) {
        List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
        if (networks == null) {
            networks = new ArrayList<WifiConfiguration>();
        }
        return networks;
    }

    private static List<AccessPoint> constructSavedAccessPoints(Context context,
            WifiManager wifiManager){
        List<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        Map<String, List<ScanResult>> resultsMap = new HashMap<String, List<ScanResult>>();

        final List<WifiConfiguration> configs = getConfiguredNetworks(wifiManager);
        final List<ScanResult> scanResults = wifiManager.getScanResults();

        if (configs != null) {
            //Construct a Map for quick searching of a wifi network via ssid.
            final int scanResultsSize = scanResults.size();
            for (int i = 0; i < scanResultsSize; ++i){
                final ScanResult result = scanResults.get(i);
                List<ScanResult> res = resultsMap.get(result.SSID);

                if(res == null){
                    res = new ArrayList<ScanResult>();
                    resultsMap.put(result.SSID, res);
                }

                res.add(result);
            }

            final int configsSize = configs.size();
            for (int i = 0; i < configsSize; ++i){
                WifiConfiguration config = configs.get(i);
                if (config.selfAdded && config.numAssociation == 0) {
                    continue;
                }
                AccessPoint accessPoint = new AccessPoint(context, config, true);
                accessPoint.setSortPreference(false);;

                final List<ScanResult> results = resultsMap.get(accessPoint.ssid);
                if(results != null){
                    final int resultsSize = results.size();
                    for (int j = 0; j < resultsSize; ++j){
                        accessPoint.update(results.get(j));
                    }
                }

                accessPoint.setShowSummary(true);
                accessPoints.add(accessPoint);
            }
        }

        // Sort network list by priority (or by network id if the priority is the same)
        Collections.sort(accessPoints, new Comparator<AccessPoint>() {
            @Override
            public int compare(AccessPoint lhs, AccessPoint rhs) {
                WifiConfiguration lwc = lhs.getConfig();
                WifiConfiguration rwc = rhs.getConfig();

                // > priority -- > lower position
                if (lwc.priority < rwc.priority) return 1;
                if (lwc.priority > rwc.priority) return -1;
                // < network id -- > lower position
                if (lhs.networkId < rhs.networkId) return -1;
                if (lhs.networkId > rhs.networkId) return 1;
                return 0;
            }
        });

        return accessPoints;
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(WifiSettings.WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint;

        showDialog(WifiSettings.WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WifiSettings.WIFI_DIALOG_ID:
                if (mDlgAccessPoint == null) { // For re-launch from saved state
                    mDlgAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                    // Reset the saved access point data
                    mAccessPointSavedState = null;
                }
                mSelectedAccessPoint = mDlgAccessPoint;
                mDialog = new WifiDialog(getActivity(), this, mDlgAccessPoint,
                        false /* not editting */, true /* hide the submit button */);
                return mDialog;

        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            mWifiManager.forget(mSelectedAccessPoint.networkId, null);
            getPreferenceScreen().removePreference(mSelectedAccessPoint);
            mSelectedAccessPoint = null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            showDialog((AccessPoint) preference, false);
            return true;
        } else{
            return super.onPreferenceTreeClick(screen, preference);
        }
    }

    private boolean isAutoConfigPriorities() {
        return Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION, 1) != 0;
    }

    /**
     * For search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();
                final String title = res.getString(R.string.wifi_saved_access_points_titlebar);

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = title;
                data.screenTitle = title;
                data.enabled = enabled;
                result.add(data);

                // Add available Wi-Fi access points
                WifiManager wifiManager =
                        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final List<AccessPoint> accessPoints =
                        constructSavedAccessPoints(context, wifiManager);

                final int accessPointsSize = accessPoints.size();
                for (int i = 0; i < accessPointsSize; ++i){
                    data = new SearchIndexableRaw(context);
                    data.title = accessPoints.get(i).getTitle().toString();
                    data.screenTitle = title;
                    data.enabled = enabled;
                    result.add(data);
                }

                return result;
            }
        };
}
