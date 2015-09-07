package com.android.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.Toast;

public class CopyOnItemLongClickListener implements OnItemLongClickListener {

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ListAdapter listAdapter = (ListAdapter) parent.getAdapter();
        Preference pref = (Preference) listAdapter.getItem(position);

        CharSequence summary = pref.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            ClipboardManager cm = (ClipboardManager)
                    parent.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText(pref.getTitle(), summary));
            Toast.makeText(
                    parent.getContext(),
                    com.android.internal.R.string.text_copied,
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
