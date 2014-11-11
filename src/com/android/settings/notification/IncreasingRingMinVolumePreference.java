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

package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.preference.SeekBarPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.android.settings.R;

/** A slider preference that directly controls an audio stream volume (no dialog) **/
public class IncreasingRingMinVolumePreference extends SeekBarPreference implements
        PreferenceManager.OnActivityStopListener, Handler.Callback {
    private static final String TAG = "IncreasingRingMinVolumePreference";

    public interface Callback {
        void onChangeStarting();
        void onChangeStopped();
    }

    private SeekBar mSeekBar;
    private AudioManager mAudioManager;
    private Handler mHandler;
    private Ringtone mRingtone;
    private Callback mCallback;
    private int mMaxStreamVolume;
    private int mOriginalStreamVolume;

    private static final int MSG_SET_STREAM_VOLUME = 0;
    private static final int MSG_START_SAMPLE = 1;
    private static final int MSG_STOP_SAMPLE = 2;
    private static final int MSG_INIT_SAMPLE = 3;
    private static final int CHECK_RINGTONE_PLAYBACK_DELAY_MS = 1000;

    public IncreasingRingMinVolumePreference(Context context) {
        this(context, null);
    }

    public IncreasingRingMinVolumePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IncreasingRingMinVolumePreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IncreasingRingMinVolumePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // TODO: init sample

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mMaxStreamVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mOriginalStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);

        HandlerThread thread = new HandlerThread(TAG + ".CallbackHandler");
        thread.start();

        mHandler = new Handler(thread.getLooper(), this);
        mHandler.sendEmptyMessage(MSG_INIT_SAMPLE);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onActivityStop() {
        postStopSample();
        mHandler.getLooper().quitSafely();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SET_STREAM_VOLUME:
                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, msg.arg1, 0);
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.INCREASING_RING_MIN_VOLUME, msg.arg1);
                break;
            case MSG_START_SAMPLE:
                onStartSample();
                break;
            case MSG_STOP_SAMPLE:
                onStopSample();
                break;
            case MSG_INIT_SAMPLE:
                onInitSample();
                break;
        }
        return true;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        getPreferenceManager().registerOnActivityStopListener(this);

        final SeekBar seekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        if (seekBar == mSeekBar) return;
        mSeekBar = seekBar;

        int currentVolume = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.INCREASING_RING_MIN_VOLUME, 1);

        mSeekBar.setMax(mMaxStreamVolume);
        mSeekBar.setProgress(currentVolume);
        mSeekBar.setSecondaryProgress(mOriginalStreamVolume);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mCallback != null) {
            mCallback.onChangeStarting();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        postStartSample();
        if (mCallback != null) {
            mCallback.onChangeStopped();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        if (fromTouch) {
            postSetVolume(progress);
        }
    }

    private void onInitSample() {
        mRingtone = RingtoneManager.getRingtone(getContext(),
                Settings.System.DEFAULT_RINGTONE_URI);
        if (mRingtone != null) {
            mRingtone.setStreamType(AudioManager.STREAM_RING);
        }
    }

    private void postSetVolume(int progress) {
        // Do the volume changing separately to give responsive UI
        mHandler.removeMessages(MSG_SET_STREAM_VOLUME);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_STREAM_VOLUME, progress, 0, null));
    }

    private void postStartSample() {
        mHandler.removeMessages(MSG_START_SAMPLE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_START_SAMPLE),
                isSamplePlaying() ? CHECK_RINGTONE_PLAYBACK_DELAY_MS : 0);
    }

    private void onStartSample() {
        if (!isSamplePlaying() && mRingtone != null) {
            try {
                mRingtone.play();
            } catch (Throwable e) {
                Log.w(TAG, "Error playing ringtone", e);
            }
            mHandler.removeMessages(MSG_STOP_SAMPLE);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_SAMPLE), 2000);
        }
    }

    private boolean isSamplePlaying() {
        return mRingtone != null && mRingtone.isPlaying();
    }

    public void stopSample() {
        postStopSample();
    }

    private void postStopSample() {
        // remove pending delayed start messages
        mHandler.removeMessages(MSG_START_SAMPLE);
        mHandler.removeMessages(MSG_STOP_SAMPLE);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_SAMPLE));
    }

    private void onStopSample() {
        if (mRingtone != null) {
            mRingtone.stop();
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mOriginalStreamVolume, 0);
    }
}
