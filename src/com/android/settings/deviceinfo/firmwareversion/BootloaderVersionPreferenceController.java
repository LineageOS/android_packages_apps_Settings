package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class BootloaderVersionPreferenceController extends BasePreferenceController {

    static final String BOOTLOADER_PROPERTY = "ro.bootloader";

    public BootloaderVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return SystemProperties.get(BOOTLOADER_PROPERTY,
                mContext.getString(R.string.device_info_default));
    }
}
