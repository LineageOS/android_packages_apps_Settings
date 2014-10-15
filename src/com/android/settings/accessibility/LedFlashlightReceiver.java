/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials provided
          with the distribution.
        * Neither the name of The Linux Foundation nor the names of its
          contributors may be used to endorse or promote products derived
          from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings.accessibility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LedFlashlightReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "LedFlashlightReceiver";
    public static final String LED_SWITCH = "LedSwitch";
    // LED light on/off values.
    private static final byte[] LIGHTE_ON = {
            '1', '2', '7'
    };
    private static final byte[] LIGHTE_OFF = {
            '0'
    };
    private static final byte[] LIGHT_TORCH = {
            '1'
    };
    private static final byte[] LIGHT_DEFAULT = {
            '0'
    };
    // LED node used in different chipsets
    public final static String MSM8226_FLASHLIGHT_BRIGHTNESS =
            "/sys/class/leds/torch-light/brightness";
    public final static String COMMON_FLASHLIGHT_BRIGHTNESS =
            "/sys/class/leds/flashlight/brightness";
    public final static String COMMON_FLASHLIGHT_MODE =
            "/sys/class/leds/flashlight/mode";
    private static boolean mLightsOn = false;
    private static PowerManager.WakeLock mWakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        mLightsOn = intent.getBooleanExtra(LED_SWITCH, false);
        setLEDStatus(mLightsOn);
    }

    private void setLEDStatus(boolean status) {
        Log.d(LOG_TAG, "setLEDStatus(" + status + ")");
        if (mWakeLock != null && status) {
            mWakeLock.acquire();
        } else if (mWakeLock != null && !status) {
            mWakeLock.release();
        }
        // for MSM8x26, BSP add MSM8226_TORCH_NODE for control torch brightness
        if (isFileExists(MSM8226_FLASHLIGHT_BRIGHTNESS)) {
            changeLEDFlashBrightness(status, MSM8226_FLASHLIGHT_BRIGHTNESS);
        } else {
            changeLEDFlashMode(status, COMMON_FLASHLIGHT_MODE);
            changeLEDFlashBrightness(status, COMMON_FLASHLIGHT_BRIGHTNESS);
        }
    }

    private void changeLEDFlashMode(boolean status, String node) {
        try {
            byte[] ledMode = status ? LIGHT_TORCH : LIGHT_DEFAULT;
            FileOutputStream mode = new FileOutputStream(node);
            mode.write(ledMode);
            mode.close();
        } catch (FileNotFoundException e) {
            Log.d(LOG_TAG, e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void changeLEDFlashBrightness(boolean status, String node) {
        try {
            byte[] ledData = status ? LIGHTE_ON : LIGHTE_OFF;
            FileOutputStream brightness = new FileOutputStream(node);
            brightness.write(ledData);
            brightness.close();
        } catch (FileNotFoundException e) {
            Log.d(LOG_TAG, e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
}
