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
package com.android.settings.profiles.actions.item;

import android.app.AlertDialog;
import android.app.StreamSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;

public class VolumeStreamItem implements Item {

    private int mStreamId;
    private StreamSettings mStreamSettings;

    public VolumeStreamItem(int streamId, StreamSettings streamSettings) {
        this.mStreamId = streamId;
        mStreamSettings = streamSettings;
    }

    @Override
    public int getViewType() {
        return ItemListAdapter.RowType.VOLUME_STREAM_ITEM.ordinal();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void requestVolumeDialog(Context context, final DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(VolumeStreamItem.getNameForStream(mStreamId));

        final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SeekBar seekBar = new SeekBar(context);
        seekBar.setPaddingRelative(32, 16, 32, 16); // TODO: confirm appropriate padding
        seekBar.setMax(am.getStreamMaxVolume(mStreamId));
        seekBar.setProgress(am.getDevicesForStream(mStreamId));
        builder.setView(seekBar);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onClick(dialog, which);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onClick(dialog, which);
            }
        });
        builder.create().show();
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView) {
        View view;
        if (convertView == null) {
            view = (View) inflater.inflate(R.layout.list_two_line_item, null);
            // Do some initialization
        } else {
            view = convertView;
        }

        Context context = inflater.getContext();
        final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        TextView text = (TextView) view.findViewById(R.id.title);
        text.setText(getNameForStream(mStreamId));

        TextView desc = (TextView) view.findViewById(R.id.summary);
        int denominator = am.getDevicesForStream(mStreamId);
        int numerator = am.getStreamMaxVolume(mStreamId);
        desc.setText(context.getResources().getString(R.string.volume_override_summary,
                denominator, numerator));

        return view;
    }

    public static int getNameForStream(int stream) {
        switch (stream) {
            case AudioManager.STREAM_ALARM:
                return R.string.alarm_volume_title;
            case AudioManager.STREAM_MUSIC:
                return R.string.media_volume_title;
            case AudioManager.STREAM_RING:
                return R.string.incoming_call_volume_title;
            case AudioManager.STREAM_NOTIFICATION:
                return R.string.notification_volume_title;
            default: return 0;
        }
    }

    public int getStreamType() {
        return mStreamId;
    }
}