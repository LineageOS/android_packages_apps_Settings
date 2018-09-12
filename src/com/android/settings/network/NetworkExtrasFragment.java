/*
 * Copyright (C) 2018 The LineageOS Project
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
 * limitations under the License.
 */
package com.android.settings.network;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/*
 *  Extras Fragment for additional network settings
 */
public class NetworkExtrasFragment extends DashboardFragment implements
        CaptivePortalWarningDialogHost {

    private static final String TAG = "NetworkExtras";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.TYPE_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.layout.network_extras_view;
    }

    @Override
    public void onCaptivePortalSwitchOffDialogConfirmed() {
        final CaptivePortalModePreferenceController controller =
                getPreferenceController(CaptivePortalModePreferenceController.class);
        controller.onCaptivePortalSwitchOffDialogConfirmed();
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Fragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final CaptivePortalModePreferenceController captiveportalModePreferenceController =
                new CaptivePortalModePreferenceController(context, fragment);
        controllers.add(captiveportalModePreferenceController);
        return controllers;
    }
}
