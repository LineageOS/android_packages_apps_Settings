/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOverlay;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ListView;

import com.android.settings.search.SearchPopulator;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public class SettingsPreferenceFragment extends PreferenceFragment implements
        DialogCreatable, ViewTreeObserver.OnPreDrawListener {

    private static final String TAG = "SettingsPreferenceFragment";

    private static final int MENU_HELP = Menu.FIRST + 100;

    private SettingsDialogFragment mDialogFragment;

    private String mHelpUrl;

    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Prepare help url and enable menu if necessary
        int helpResource = getHelpResource();
        if (helpResource != 0) {
            mHelpUrl = getResources().getString(helpResource);
        }
    }

    private int getPreferenceListViewPosition(String key) {
        Preference pref = findPreference(key);
        if (pref == null) {
            return -1;
        }
        return countPreferencesInGroup(getPreferenceScreen(), pref);
    }

    private int countPreferencesInGroup(PreferenceGroup group, Preference stopAt) {
        int result = 0, count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = group.getPreference(i);
            // if this is our preference, don't check any other
            // condition, break and return
            if (p == stopAt) {
                break;
            }
            // otherwise, if we've hit another group, restart
            // our search
            if (p != stopAt && p instanceof PreferenceGroup) {
                result += countPreferencesInGroup((PreferenceGroup) p, stopAt);
            }
            // keep searching within the preference group
            result++;
        }
        return result;
    }

    private String getPreferenceKey() {
        Bundle args = getArguments();
        if (args != null && args.containsKey(SearchPopulator.EXTRA_PREF_KEY)) {
            return args.getString(SearchPopulator.EXTRA_PREF_KEY);
        }
        return getActivity().getIntent().getStringExtra(SearchPopulator.EXTRA_PREF_KEY);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!TextUtils.isEmpty(mHelpUrl)) {
            setHasOptionsMenu(true);
        }
        String prefKey = getPreferenceKey();
        if (!TextUtils.isEmpty(prefKey)) {
            int position = getPreferenceListViewPosition(prefKey);
            if (position != -1) {
                getListView().setSelection(position);
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!TextUtils.isEmpty(getPreferenceKey())) {
            getListView().getViewTreeObserver().addOnPreDrawListener(this);
        }
    }

    @Override
    public boolean onPreDraw() {
        final ListView list = getListView();
        final int highlightedPosition = getPreferenceListViewPosition(getPreferenceKey());
        // will return null if highlightedPosition is -1
        final View child = list.getChildAt(highlightedPosition - list.getFirstVisiblePosition());

        if (child != null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final ViewOverlay overlay = child.getOverlay();
                    final ColorDrawable d = new ColorDrawable(
                            getResources().getColor(R.color.search_pref_highlight_background));
                    d.setBounds(0, 0, child.getWidth(), child.getHeight());
                    overlay.add(d);

                    ObjectAnimator bgAnim = ObjectAnimator.ofInt(d, "alpha", 0, 100, 0);
                    bgAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            overlay.remove(d);
                        }
                    });
                    bgAnim.setDuration(1000);
                    bgAnim.start();
                }
            }, 300);
        }

        list.getViewTreeObserver().removeOnPreDrawListener(this);
        return true;
    }

    protected void removePreference(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * Override this if you want to show a help item in the menu, by returning the resource id.
     * @return the resource id for the help url
     */
    protected int getHelpResource() {
        return 0;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mHelpUrl != null && getActivity() != null) {
            MenuItem helpItem = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), helpItem, mHelpUrl);
        }
    }

    /*
     * The name is intentionally made different from Activity#finish(), so that
     * users won't misunderstand its meaning.
     */
    public final void finishFragment() {
        getActivity().onBackPressed();
    }

    // Some helpers for functions used by the settings fragments when they were activities

    /**
     * Returns the ContentResolver from the owning Activity.
     */
    protected ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected Object getSystemService(final String name) {
        return getActivity().getSystemService(name);
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    @Override
    public void onDetach() {
        if (isRemoving()) {
            if (mDialogFragment != null) {
                mDialogFragment.dismiss();
                mDialogFragment = null;
            }
        }
        super.onDetach();
    }

    // Dialog management

    protected void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    protected void removeDialog(int dialogId) {
        // mDialogFragment may not be visible yet in parent fragment's onResume().
        // To be able to dismiss dialog at that time, don't check
        // mDialogFragment.isVisible().
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
            mDialogFragment.dismiss();
        }
        mDialogFragment = null;
    }

    /**
     * Sets the OnCancelListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnCancelListener = listener;
        }
    }

    /**
     * Sets the OnDismissListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnDismissListener = listener;
        }
    }

    public void onDialogShowing() {
        // override in subclass to attach a dismiss listener, for instance
    }

    public static class SettingsDialogFragment extends DialogFragment {
        private static final String KEY_DIALOG_ID = "key_dialog_id";
        private static final String KEY_PARENT_FRAGMENT_ID = "key_parent_fragment_id";

        private int mDialogId;

        private Fragment mParentFragment;

        private DialogInterface.OnCancelListener mOnCancelListener;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public SettingsDialogFragment() {
            /* do nothing */
        }

        public SettingsDialogFragment(DialogCreatable fragment, int dialogId) {
            mDialogId = dialogId;
            if (!(fragment instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of "
                        + Fragment.class.getName());
            }
            mParentFragment = (Fragment) fragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (mParentFragment != null) {
                outState.putInt(KEY_DIALOG_ID, mDialogId);
                outState.putInt(KEY_PARENT_FRAGMENT_ID, mParentFragment.getId());
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            if (mParentFragment != null && mParentFragment instanceof SettingsPreferenceFragment) {
                ((SettingsPreferenceFragment) mParentFragment).onDialogShowing();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mDialogId = savedInstanceState.getInt(KEY_DIALOG_ID, 0);
                int mParentFragmentId = savedInstanceState.getInt(KEY_PARENT_FRAGMENT_ID, -1);
                if (mParentFragmentId > -1) {
                    mParentFragment = getFragmentManager().findFragmentById(mParentFragmentId);
                    if (!(mParentFragment instanceof DialogCreatable)) {
                        throw new IllegalArgumentException(
                                (mParentFragment != null
                                        ? mParentFragment.getClass().getName()
                                        : mParentFragmentId)
                                + " must implement "
                                + DialogCreatable.class.getName());
                    }
                }
                // This dialog fragment could be created from non-SettingsPreferenceFragment
                if (mParentFragment instanceof SettingsPreferenceFragment) {
                    // restore mDialogFragment in mParentFragment
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = this;
                }
            }
            return ((DialogCreatable) mParentFragment).onCreateDialog(mDialogId);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (mOnCancelListener != null) {
                mOnCancelListener.onCancel(dialog);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }
        }

        public int getDialogId() {
            return mDialogId;
        }

        @Override
        public void onDetach() {
            super.onDetach();

            // This dialog fragment could be created from non-SettingsPreferenceFragment
            if (mParentFragment instanceof SettingsPreferenceFragment) {
                // in case the dialog is not explicitly removed by removeDialog()
                if (((SettingsPreferenceFragment) mParentFragment).mDialogFragment == this) {
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = null;
                }
            }
        }
    }

    protected boolean hasNextButton() {
        return ((ButtonBarHandler)getActivity()).hasNextButton();
    }

    protected Button getNextButton() {
        return ((ButtonBarHandler)getActivity()).getNextButton();
    }

    public void finish() {
        getActivity().onBackPressed();
    }

    public boolean startFragment(
            Fragment caller, String fragmentClass, int requestCode, Bundle extras) {
        if (getActivity() instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity)getActivity();
            preferenceActivity.startPreferencePanel(fragmentClass, extras,
                    R.string.lock_settings_picker_title, null, caller, requestCode);
            return true;
        } else {
            Log.w(TAG, "Parent isn't PreferenceActivity, thus there's no way to launch the "
                    + "given Fragment (name: " + fragmentClass + ", requestCode: " + requestCode
                    + ")");
            return false;
        }
    }
}
