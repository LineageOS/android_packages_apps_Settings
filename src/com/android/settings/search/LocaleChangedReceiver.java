package com.android.settings.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class LocaleChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    context.getPackageName(), Context.MODE_PRIVATE);
            sharedPreferences.edit().remove(SearchPopulator.LAST_PACKAGE_HASH).apply();
        }
    }

}
