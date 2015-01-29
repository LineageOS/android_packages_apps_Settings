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
    public static final String LED_SWITCH = "LedSwitch";
    public static final String LED_SWITCH_ACTION = "android.intent.action.ACTION_SHUTDOWN";
    private static final String TAG = "LedFlashlightReceiver";
    // LED light on/off values.
    private static final byte[] LIGHTE_OFF = {
            '0'
    };
    private static final byte[] LIGHTE_ON = {
            '1', '2', '7'
    };
    private static final byte[] LIGHT_MODE_TORCH = {
            '1'
    };
    private static final byte[] LIGHT_MODE_DEFAULT = {
            '0'
    };

    // LED node used in different chipsets
    public final static String MSM8226_FLASHLIGHT_BRIGHTNESS =
            "/sys/class/leds/torch-light/brightness";
    public final static String COMMON_FLASHLIGHT_BRIGHTNESS =
            "/sys/class/leds/flashlight/brightness";
    public final static String COMMON_FLASHLIGHT_MODE =
            "/sys/class/leds/flashlight/mode";
    private static PowerManager.WakeLock mWakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null &&
                LED_SWITCH_ACTION.equals(intent.getAction())) {
            boolean isLightsOn = intent.getBooleanExtra(LED_SWITCH, false);
            setLEDWakeLock(context, isLightsOn);
            setLEDStatus(isLightsOn);
        }
    }

    private void setLEDStatus(boolean status) {
        byte[] data = status ? LIGHTE_ON : LIGHTE_OFF;
        // for MSM8x26, BSP add MSM8226_TORCH_NODE for control torch brightness
        if (isFileExists(MSM8226_FLASHLIGHT_BRIGHTNESS)) {
            setLEDFlashDataToDriver(MSM8226_FLASHLIGHT_BRIGHTNESS, data);
        } else {
            byte[] mode = status ? LIGHT_MODE_TORCH : LIGHT_MODE_DEFAULT;
            setLEDFlashDataToDriver(COMMON_FLASHLIGHT_MODE, mode);
            setLEDFlashDataToDriver(COMMON_FLASHLIGHT_BRIGHTNESS, data);
        }
    }

    private void setLEDFlashDataToDriver(String node, byte[] data) {
        FileOutputStream driver = null;
        try {
            driver = new FileOutputStream(node);
            driver.write(data);
        } catch (FileNotFoundException e) {
            Log.d(TAG, e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (driver != null) {
                    driver.flush();
                    driver.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing file: " + e.toString());
            }
        }
    }

    private boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private void acquireLEDWakeLock(Context context) {
        if (mWakeLock == null && context != null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.setReferenceCounted(false);
            mWakeLock.acquire();
        }
    }

    private void releaseLEDWakeLock() {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            mWakeLock = null;
        }
    }

    private void setLEDWakeLock(Context context, boolean isLightsOn) {
        if (isLightsOn) {
            acquireLEDWakeLock(context);
        } else {
            releaseLEDWakeLock();
        }
    }
}
