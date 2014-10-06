package com.android.settings.profiles;

import android.nfc.Tag;

public interface NFCProfileTagCallback {
    public void onTagRead(Tag tag);
}
