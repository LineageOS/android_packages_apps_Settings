package com.android.settings.aim.Preferences;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.util.AttributeSet;

public class SecureSettingSwitchPreference extends SwitchPreference {
    public SecureSettingSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SecureSettingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SecureSettingSwitchPreference(Context context) {
        super(context, null);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                // It's already there, so the same as persisting
                return true;
            }
            Settings.Secure.putInt(getContext().getContentResolver(), getKey(), value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return Settings.Secure.getInt(getContext().getContentResolver(),
                getKey(), defaultReturnValue ? 1 : 0) != 0;
    }

    @Override
    protected boolean isPersisted() {
        // Using getString instead of getInt so we can simply check for null
        // instead of catching an exception. (All values are stored as strings.)
        return Settings.Secure.getString(getContext().getContentResolver(), getKey()) != null;
    }
}
