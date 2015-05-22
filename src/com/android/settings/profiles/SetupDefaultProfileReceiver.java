package com.android.settings.profiles;

import android.app.Profile;
import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import java.util.UUID;

public class SetupDefaultProfileReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Settings.System.getInt(context.getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1) {
            ProfileManager profileManager = (ProfileManager) context
                    .getSystemService(Context.PROFILE_SERVICE);
            Profile defaultProfile = profileManager.getProfile(
                    UUID.fromString("d393035d-ea71-4f2a-bdbd-b65f6bf298f1"));
            if (defaultProfile != null) {
                SetupActionsFragment.fillProfileWithCurrentSettings(context, defaultProfile);
                profileManager.updateProfile(defaultProfile);
            }
        }


    }
}
