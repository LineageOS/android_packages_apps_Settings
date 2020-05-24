/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.gestures;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import lineageos.providers.LineageSettings;

/**
 * Dialog to set the back gesture's sensitivity in Gesture navigation mode.
 */
public class GestureNavigationBackSensitivityDialog extends InstrumentedDialogFragment {
    private static final String TAG = "GestureNavigationBackSensitivityDialog";
    private static final String KEY_BACK_SENSITIVITY = "back_sensitivity";

    public static void show(SystemNavigationGestureSettings parent, int sensitivity) {
        if (!parent.isAdded()) {
            return;
        }

        final GestureNavigationBackSensitivityDialog dialog =
                new GestureNavigationBackSensitivityDialog();
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_BACK_SENSITIVITY, sensitivity);
        dialog.setArguments(bundle);
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_GESTURE_NAV_BACK_SENSITIVITY_DLG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(
                R.layout.dialog_back_gesture_sensitivity, null);
        final SeekBar sensitivitySeekBar = view.findViewById(R.id.back_sensitivity_seekbar);
        sensitivitySeekBar.setProgress(getArguments().getInt(KEY_BACK_SENSITIVITY));

        final ContentResolver cr = getContext().getContentResolver();
        final int excludedPercentage = LineageSettings.Secure.getInt(cr,
                LineageSettings.Secure.GESTURE_BACK_EXCLUDE_TOP, 0);
        final SeekBar excludedTopSeekBar = view.findViewById(R.id.back_excluded_top_seekbar);
        excludedTopSeekBar.setProgress(excludedPercentage);

        final boolean isShowHintEnabled = LineageSettings.System.getInt(cr,
                LineageSettings.System.NAVIGATION_BAR_HINT, 1) == 1;
        final Switch hintSwitch = view.findViewById(R.id.show_navbar_hint);
        hintSwitch.setChecked(isShowHintEnabled);

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.edge_to_edge_navigation_title)
                .setView(view)
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    int sensitivity = sensitivitySeekBar.getProgress();
                    getArguments().putInt(KEY_BACK_SENSITIVITY, sensitivity);
                    SystemNavigationGestureSettings.setBackSensitivity(getActivity(),
                            getOverlayManager(), sensitivity);

                    int excludedTopPercentage = excludedTopSeekBar.getProgress();
                    LineageSettings.Secure.putInt(cr,
                            LineageSettings.Secure.GESTURE_BACK_EXCLUDE_TOP, excludedTopPercentage);

                    int showHint = hintSwitch.isChecked() ? 1 : 0;
                    LineageSettings.System.putInt(cr,
                            LineageSettings.System.NAVIGATION_BAR_HINT, showHint);
                })
                .create();
    }

    private IOverlayManager getOverlayManager() {
        return IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
    }
}