package com.android.settings.notification;

import android.content.Context;
import android.os.RemoteException;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.NoSuchElementException;

import vendor.lineage.vibrator.V1_0.IVibrator;

public class VibratorIntensityController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_VIBRATOR_INTENSITY = "vibrator_intensity";

    public VibratorIntensityController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        try {
            IVibrator server = IVibrator.getService("default");
            return server != null;
        } catch (NoSuchElementException | RemoteException ignored) {
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIBRATOR_INTENSITY;
    }
} 