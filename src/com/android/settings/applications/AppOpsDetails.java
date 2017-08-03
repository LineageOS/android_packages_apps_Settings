/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;

import java.util.List;

public class AppOpsDetails extends PreferenceFragment {
    static final String TAG = "AppOpsDetails";

    public static final String ARG_PACKAGE_NAME = "package";

    private AppOpsState mState;
    private PackageManager mPm;
    private AppOpsManager mAppOps;
    private PackageInfo mPackageInfo;
    private LayoutInflater mInflater;
    private View mRootView;
    private PreferenceScreen mPreferenceScreen;

    private final int MODE_ALLOWED = 0;
    private final int MODE_IGNORED = 1;
    private final int MODE_ASK     = 2;

    private final String[] MODE_ENTRIES = {
            String.valueOf(MODE_ALLOWED),
            String.valueOf(MODE_IGNORED),
            String.valueOf(MODE_ASK)
    };

    private int modeToPosition (int mode) {
        switch(mode) {
        case AppOpsManager.MODE_ALLOWED:
            return MODE_ALLOWED;
        case AppOpsManager.MODE_IGNORED:
            return MODE_IGNORED;
        case AppOpsManager.MODE_ASK:
            return MODE_ASK;
        };

        return MODE_IGNORED;
    }

    private int positionToMode (int position) {
        switch(position) {
        case MODE_ALLOWED:
            return AppOpsManager.MODE_ALLOWED;
        case MODE_IGNORED:
            return AppOpsManager.MODE_IGNORED;
        case MODE_ASK:
            return AppOpsManager.MODE_ASK;
        };

        return AppOpsManager.MODE_IGNORED;
    }

    private boolean isPlatformSigned() {
        final int match = mPm.checkSignatures("android", mPackageInfo.packageName);
        return match >= PackageManager.SIGNATURE_MATCH;
    }

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        final View appSnippet = mRootView.findViewById(R.id.app_snippet);
        CharSequence label = mPm.getApplicationLabel(pkgInfo.applicationInfo);
        Drawable icon = mPm.getApplicationIcon(pkgInfo.applicationInfo);
        InstalledAppDetails.setupAppSnippet(appSnippet, label, icon,
                pkgInfo != null ? pkgInfo.versionName : null, null);
    }

    private String retrieveAppEntry() {
        final Bundle args = getArguments();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (packageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }
        try {
            mPackageInfo = mPm.getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + packageName, e);
            mPackageInfo = null;
        }

        return packageName;
    }

    private boolean refreshUi() {
        if (mPackageInfo == null) {
            return false;
        }

//        setAppLabelAndIcon(mPackageInfo);

        final Resources res = getActivity().getResources();

        mPreferenceScreen.removeAll();
        String lastPermGroup = "";
        boolean isPlatformSigned = isPlatformSigned();
        for (AppOpsState.OpsTemplate tpl : AppOpsState.ALL_TEMPLATES) {
            /* If we are platform signed, only show the root switch, this
             * one is safe to toggle while other permission-based ones could
             * certainly cause system-wide problems
             */
            if (isPlatformSigned && tpl != AppOpsState.SU_TEMPLATE) {
                 continue;
            }
            List<AppOpsState.AppOpEntry> entries = mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName);
            for (final AppOpsState.AppOpEntry entry : entries) {
                final AppOpsManager.OpEntry firstOp = entry.getOpEntry(0);
                String perm = AppOpsManager.opToPermission(firstOp.getOp());
                Drawable icon = null;
                if (perm != null) {
                    try {
                        PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                                icon = pgi.loadIcon(mPm);
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                }

                final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());
                int mode = mAppOps.checkOpNoThrow(switchOp, entry.getPackageOps().getUid(),
                        entry.getPackageOps().getPackageName());

                // ListPreference
                if (AppOpsManager.isStrictOp(switchOp)) {
                    ListPreference listPref = new ListPreference(getActivity());
                    if (icon != null) {
                        listPref.setIcon(icon);
                    }
                    listPref.setKey(entry.getSwitchText(mState).toString());
                    listPref.setTitle(entry.getSwitchText(mState));
                    listPref.setEntries(R.array.app_ops_permissions);
                    listPref.setEntryValues(MODE_ENTRIES);
                    listPref.setValue(String.valueOf(modeToPosition(mode)));
                    String summary = getSummary(new CharSequence[] {listPref.getEntry(),
                            entry.getCountsText(res), entry.getTimeText(res, true)});
                    listPref.setSummary(summary);
                    listPref.setEnabled(true);
                    listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            ListPreference listPref = (ListPreference) preference;
                            String value = newValue.toString();
                            mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
                                    entry.getPackageOps().getPackageName(),
                                    positionToMode(Integer.parseInt(value)));
                            String summary = getSummary(new CharSequence[] {
                                    listPref.getEntries()[listPref.findIndexOfValue(value)],
                                    entry.getCountsText(res), entry.getTimeText(res, true)});
                            listPref.setSummary(summary);
                            return true;
                        }
                    });
                    mPreferenceScreen.addPreference(listPref);
                } else {
                    SwitchPreference switchPref = new SwitchPreference(getActivity());
                    if (icon != null) {
                        switchPref.setIcon(icon);
                    }
                    switchPref.setTitle(entry.getSwitchText(mState));
                    String summary = getSummary(new CharSequence[] {entry.getCountsText(res),
                            entry.getTimeText(res, true)});
                    switchPref.setSummary(summary);
                    switchPref.setChecked(mode == AppOpsManager.MODE_ALLOWED);
                    switchPref.setEnabled(true);
                    switchPref.setOnPreferenceChangeListener(
                            new Preference.OnPreferenceChangeListener() {
                                @Override
                                public boolean onPreferenceChange(Preference preference,
                                                                  Object newValue) {
                                    Boolean isChecked = (Boolean) newValue;
                                    mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
                                            entry.getPackageOps().getPackageName(),
                                            isChecked ? AppOpsManager.MODE_ALLOWED
                                                    : AppOpsManager.MODE_IGNORED);
                                    return true;
                                }
                            });
                    mPreferenceScreen.addPreference(switchPref);
                }
            }
        }

        return true;
    }

    private String getSummary(CharSequence[] lines) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence line: lines) {
            if (line != null) {
                sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString().replaceFirst("\n", "");
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        SettingsActivity sa = (SettingsActivity)getActivity();
        sa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mState = new AppOpsState(getActivity());
        mPm = getActivity().getPackageManager();
        mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);

        mPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        retrieveAppEntry();

        setPreferenceScreen(mPreferenceScreen);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) { }
}
