/*
 * Copyright (C) 2022 Project Kaleidoscope
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

package com.android.settings.media;

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import static com.android.settings.slices.CustomSliceRegistry.APP_VOLUME_SLICE_URI;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AppVolume;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

public class AppVolumeSlice implements CustomSliceable {

    private static final String TAG = "AppVolumeSlice";
    private static final String PACKAGE_NAME = "package_name";
    private static final String ACTION_LAUNCH_DIALOG = "action_launch_dialog";

    private final Context mContext;

    private final AudioManager mAudioManager;

    public AppVolumeSlice(Context context) {
        mContext = context;
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final int newPosition = intent.getIntExtra(EXTRA_RANGE_VALUE, -1);
        final String packageName = intent.getStringExtra(PACKAGE_NAME);
        if (!TextUtils.isEmpty(packageName)) {
            mAudioManager.setAppVolume(packageName, newPosition / 100.0f);
            return;
        }
    }

    @Override
    public Slice getSlice() {
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED);

        // Only displaying active tracks
        final List<AppVolume> appVols = new ArrayList<>();
        for (AppVolume vol : mAudioManager.listAppVolumes()) {
            if (vol.isActive()) {
                appVols.add(vol);
            }
        }
        if (appVols.isEmpty()) {
            Log.d(TAG, "No active tracks");
            return listBuilder.build();
        }

        for (AppVolume vol : appVols) {
            final CharSequence appName = Utils.getApplicationLabel(
                    mContext, vol.getPackageName());
            IconCompat icon = getApplicationIcon(vol.getPackageName());
            final SliceAction primarySliceAction = SliceAction.create(
                    getBroadcastIntent(mContext), icon, ListBuilder.ICON_IMAGE, appName);
            listBuilder.addInputRange(new InputRangeBuilder()
                    .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                    .setTitle(appName)
                    .setInputAction(getSliderInputAction(vol.getPackageName()))
                    .setMax(100)
                    .setValue((int)(vol.getVolume() * 100))
                    .setPrimaryAction(primarySliceAction));
               }
        return listBuilder.build();
    }

    private IconCompat getApplicationIcon(String packageName) {
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            Resources resources = pm.getResourcesForApplication(ai);
            IconCompat icon = IconCompat.createWithResource(resources, packageName, ai.icon);
            return icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get icon of " + packageName, e);
        }

        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        return IconCompat.createWithBitmap(bitmap);
    }


    private PendingIntent getSliderInputAction(String packageName) {
        final int requestCode = packageName.hashCode();
        final Intent intent = new Intent(getUri().toString())
                .setData(getUri())
                .putExtra(PACKAGE_NAME, packageName)
                .setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, requestCode, intent,
                PendingIntent.FLAG_MUTABLE);
    }

    @Override
    public Uri getUri() {
        return APP_VOLUME_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_sound;
    }
}
