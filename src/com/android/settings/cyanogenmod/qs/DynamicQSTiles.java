/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package com.android.settings.cyanogenmod.qs;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.util.cm.DynamicQSUtils;

import java.util.List;

public class DynamicQSTiles extends QSTiles {

    protected String getDefaultOrder() {
        return DynamicQSUtils.getDefaultTilesAsString(getActivity());
    }

    protected List<String> getAvailableTiles() {
        return DynamicQSUtils.getAvailableTiles(getActivity());
    }

    protected boolean hasLargeFirstRow() {
        return false;
    }

    protected String getTilesUri() {
        return Settings.Secure.QS_DYNAMIC_TILES;
    }

    public static int determineTileCount(Context context) {
        String order = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.QS_DYNAMIC_TILES);
        if (order == null) {
            order = DynamicQSUtils.getDefaultTilesAsString(context);
        }
        if (TextUtils.isEmpty(order)) {
            return 0;
        }
        return order.split(",").length;
    }
}
