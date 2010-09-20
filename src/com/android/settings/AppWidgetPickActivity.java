/*
 * Copyright (C) 2010 Florian Sundermann
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

package com.android.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Window;

public class AppWidgetPickActivity extends Activity {

    private Intent fIntent = null;
    private PackageManager fPManager = null;
    private AppWidgetManager fAppWManager = null;
    private ArrayList<SubItem> fItems;
    private int fAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("");
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        fIntent = getIntent();

        final Intent intent = getIntent();
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            fAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            fPManager = getPackageManager();
            fAppWManager = AppWidgetManager.getInstance(this);

            fItems = new ArrayList<SubItem>();
            AddAppWidgetProviderInfos();
            AddCustomAppWidgets();
            Collections.sort(fItems, new Comparator<SubItem>() {

                @Override
                public int compare(SubItem object1, SubItem object2) {
                    return object1.getName().compareToIgnoreCase(object2.getName());
                }

            });
            for(SubItem itm : fItems) {
                if (itm instanceof Item) {
                    ((Item)itm).sort();
                }
            }
            new PickWidgetDialog(this).showDialog(null);
        } else {
            finish();
        }
    }

    public ArrayList<SubItem> getItems() {
        return fItems;
    }


    public void finishOk(SubItem item) {
        int result;
        if (item.getExtra() != null) {
            // If there are any extras, it's because this entry is custom.
            // Don't try to bind it, just pass it back to the app.
            setResult(RESULT_OK, getIntent(item));
        } else {
            try {
                fAppWManager.bindAppWidgetId(fAppWidgetId, item.getProvider());
                result = RESULT_OK;
            } catch (IllegalArgumentException e) {
                // This is thrown if they're already bound, or otherwise somehow
                // bogus.  Set the result to canceled, and exit.  The app *should*
                // clean up at this point.  We could pass the error along, but
                // it's not clear that that's useful -- the widget will simply not
                // appear.
                result = RESULT_CANCELED;
            }
            setResult(result, fIntent);
        }
        finish();
    }

    /**
     * Build the {@link Intent} described by this item. If this item
     * can't create a valid {@link ComponentName}, it will return
     * {@link Intent#ACTION_CREATE_SHORTCUT} filled with the item label.
     */
    private Intent getIntent(SubItem itm) {
        Intent intent = null;
        Parcelable parcel = fIntent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (parcel instanceof Intent) {
            intent = new Intent((Intent) parcel);
        } else {
            intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        }

        if (itm.getProvider() != null) {
            // Valid package and class, so fill details as normal intent
            intent.setClassName(itm.getProvider().getPackageName(), itm.getProvider().getClassName());
        } else {
            // No valid package or class, so treat as shortcut with label
            intent.setAction(Intent.ACTION_CREATE_SHORTCUT);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, itm.getName());
        }
        if (itm.getExtra() != null) {
            intent.putExtras(itm.getExtra());
        }
        return intent;
    }

    private Item getPackageItem(AppWidgetProviderInfo info) {
        String packName = info.provider.getPackageName();
        for (SubItem itm : fItems) {
            if (itm instanceof Item) {
                Item i = (Item)itm;
                if (i.getPackageName().equals(packName)) {
                    return i;
                }
            }
        }
        try
        {
            android.content.pm.ApplicationInfo appInfo = fPManager.getApplicationInfo(info.provider.getPackageName(), 0);
            Drawable appIcon = fPManager.getApplicationIcon(appInfo);
            CharSequence str = fPManager.getApplicationLabel(appInfo);
            Item newItm = new Item(str.toString(), appIcon);
            newItm.setPackageName(packName);
            fItems.add(newItm);
            return newItm;
        }
        catch(PackageManager.NameNotFoundException expt) {
        }
        return null;
    }

    private void AddAppWidgetProviderInfos() {
        List<AppWidgetProviderInfo> infos = fAppWManager.getInstalledProviders();

        for(AppWidgetProviderInfo info : infos) {
            try
            {
                android.content.pm.ApplicationInfo appInfo = fPManager.getApplicationInfo(info.provider.getPackageName(), 0);
                SubItem itm = new SubItem(info.label,  fPManager.getDrawable(info.provider.getPackageName(), info.icon, appInfo));
                itm.setProvider(info.provider);
                Item mainItm = getPackageItem(info);
                mainItm.getItems().add(itm);
            }
            catch(PackageManager.NameNotFoundException expt) {
            }
        }
    }

    private void AddCustomAppWidgets() {
        final Bundle extras = fIntent.getExtras();

        // get and validate the extras they gave us
        ArrayList<AppWidgetProviderInfo> customInfo = null;
        ArrayList<Bundle> customExtras = null;
        try_custom_items: {
            customInfo = extras.getParcelableArrayList(AppWidgetManager.EXTRA_CUSTOM_INFO);

            if (customInfo == null || customInfo.size() == 0) {
                break try_custom_items;
            }

            int customInfoSize = customInfo.size();
            for (int i=0; i<customInfoSize; i++) {
                Parcelable p = customInfo.get(i);
                if (p == null || !(p instanceof AppWidgetProviderInfo)) {
                    customInfo = null;
                    break try_custom_items;
                }
            }

            customExtras = extras.getParcelableArrayList(AppWidgetManager.EXTRA_CUSTOM_EXTRAS);
            if (customExtras == null) {
                customInfo = null;
                break try_custom_items;
            }
            int customExtrasSize = customExtras.size();
            if (customInfoSize != customExtrasSize) {
                break try_custom_items;
            }
            for (int i=0; i<customExtrasSize; i++) {
                Parcelable p = customExtras.get(i);
                if (p == null || !(p instanceof Bundle)) {
                    customInfo = null;
                    customExtras = null;
                    break try_custom_items;
                }
            }
        }
        putAppWidgetItems(customInfo, customExtras);
    }

    private void putAppWidgetItems(List<AppWidgetProviderInfo> appWidgets, List<Bundle> customExtras) {
        if (appWidgets == null)
            return;
        final int size = appWidgets.size();
        for (int i = 0; i < size; i++) {
            AppWidgetProviderInfo info = appWidgets.get(i);

            String label = info.label.toString();
            Drawable icon = null;

            if (info.icon != 0) {
                icon = fPManager.getDrawable(info.provider.getPackageName(), info.icon, null);
            }

            Item item = new Item(label, icon);
            SubItem subItem = new SubItem(label, icon);
            item.getItems().add(subItem);

            item.setPackageName(info.provider.getPackageName());
            if (customExtras != null) {
                subItem.setExtra(customExtras.get(i));
            }

            fItems.add(item);
        }
    }
}
