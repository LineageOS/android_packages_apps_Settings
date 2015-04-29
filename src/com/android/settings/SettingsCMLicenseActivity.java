/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.DialogInterface;

/**
 * The "dialog" that shows from "CyanogenMod Legal" in the Settings app.
 */
public class SettingsCMLicenseActivity extends Activity
        implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private static final String PROPERTY_CMLICENSE_URL = "ro.cmlegal.url";

    private WebView mWebView;

    private AlertDialog mErrorDialog = null;
    private AlertDialog mDialog;
    private ProgressDialog mSpinnerDlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence title = getText(R.string.settings_cmlicense_activity_title);
        CharSequence msg = getText(R.string.settings_license_activity_loading);

        ProgressDialog pd = ProgressDialog.show(this, title, msg, true, false);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mSpinnerDlg = pd;

        String userCMLicenseUrl = SystemProperties.get(PROPERTY_CMLICENSE_URL);

        final Configuration configuration = getResources().getConfiguration();
        final String language = configuration.locale.getLanguage();
        final String country = configuration.locale.getCountry();

        //String loc = String.format("locale=%s-%s", language, country);

        userCMLicenseUrl = String.format(userCMLicenseUrl);

        mWebView = new WebView(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_cmlicense_activity_title);
        builder.setView(mWebView);
        builder.setOnCancelListener(this);
        mDialog = builder.create();

        // Begin accessing
        if (savedInstanceState == null) {
            mWebView.loadUrl(userCMLicenseUrl);
        } else {
            mWebView.restoreState(savedInstanceState);
        }
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mSpinnerDlg.dismiss();
                mDialog.show();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                showErrorAndFinish(failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });
    }

    private void showErrorAndFinish(String url) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (mSpinnerDlg != null) {
            mSpinnerDlg.dismiss();
        }
        if (mErrorDialog == null) {
            mErrorDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_cmlicense_activity_title)
                    .setPositiveButton(android.R.string.ok, this)
                    .setOnCancelListener(this)
                    .setCancelable(true)
                    .create();
        } else {
            if (mErrorDialog.isShowing()) {
                mErrorDialog.dismiss();
            }
        }
        mErrorDialog.setMessage(getResources()
                .getString(R.string.settings_cmlicense_activity_unreachable, url));
        mErrorDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        finish();
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        mWebView.saveState(icicle);
        super.onSaveInstanceState(icicle);
    }
}
