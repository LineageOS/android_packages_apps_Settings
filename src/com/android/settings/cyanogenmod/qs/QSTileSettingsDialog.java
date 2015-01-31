/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cyanogenmod.qs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.internal.util.cm.QSConstants;
import com.android.settings.R;

public class QSTileSettingsDialog extends DialogFragment {

    private final static String TILE_TYPE_KEY = "tile_type";

    public static QSTileSettingsDialog newInstance(String tileType) {
        QSTileSettingsDialog dialog = new QSTileSettingsDialog();

        Bundle args = new Bundle();
        args.putString(TILE_TYPE_KEY, tileType);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.qs_tile_settings, container, false);

        QSTileSettingsFragment fragment = new QSTileSettingsFragment();

        String tileType = getArguments().getString(TILE_TYPE_KEY);
        Bundle args = new Bundle();
        args.putString(TILE_TYPE_KEY, tileType);
        fragment.setArguments(args);

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, fragment);
        transaction.commit();

        Button closeButton = (Button) v.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        return v;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);

        int title = -1;
        String tileType = getArguments().getString(TILE_TYPE_KEY);

        switch (tileType) {
            case QSConstants.TILE_LOCATION:
                title = R.string.qs_tile_location;
                break;
        }

        if (title != -1) {
            d.setTitle(title);
        }

        return d;
    }
}
