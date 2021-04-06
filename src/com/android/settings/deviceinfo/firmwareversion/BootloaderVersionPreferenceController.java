package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.Build;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class BootloaderVersionPreferenceController extends BasePreferenceController {

    public BootloaderVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return Build.BOOTLOADER;
    }
}
