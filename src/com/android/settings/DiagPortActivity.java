/*
    Copyright (c) 2014, The Linux Foundation. All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:
        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials provided
          with the distribution.
        * Neither the name of The Linux Foundation nor the names of its
          contributors may be used to endorse or promote products derived
          from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
    ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
    BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
    BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
    WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
    OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
    IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.android.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class DiagPortActivity extends PreferenceActivity {

    private final String KEY_DIAG_PORT = "diag_port_enable_preference";
    private CheckBoxPreference mDiagport;
    private boolean mIsCancleDilog;
    private UsbManager mUsbManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        addPreferencesFromResource(R.xml.diagport_settings);
        mDiagport = (CheckBoxPreference) findPreference(KEY_DIAG_PORT);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean set = prefs.getBoolean(KEY_DIAG_PORT, false);
        mDiagport.setChecked(set);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDiagport) {
            showPasswordDialog();
        }
        return false;
    }

    private void showPasswordDialog() {
        mIsCancleDilog = false;
        AlertDialog.Builder passworddialog = new AlertDialog.Builder(this);
        View createlayout = LayoutInflater.from(this).inflate(
                R.layout.dialog_edittext, null);
        final EditText edittext = (EditText) createlayout.findViewById(
                R.id.edittext);
        edittext.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passworddialog.setTitle(R.string.crypt_keeper_enter_password);
        passworddialog.setView(createlayout);
        passworddialog.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mIsCancleDilog = true;
                        if ( edittext.getText().toString().equals(getPassword())) {
                            if (mDiagport.isChecked()) {
                                mDiagport.setChecked(true);
                                mUsbManager.setCurrentFunction("diag,serial_smd,rmnet_bam,adb",
                                        true);
                            } else {
                                mDiagport.setChecked(false);
                                mUsbManager.setCurrentFunction("ncm,adb", true);
                            }
                        } else {
                            if (mDiagport.isChecked()) {
                                mDiagport.setChecked(false);
                                Toast.makeText(DiagPortActivity.this,
                                        R.string.credentials_wrong_password,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                mDiagport.setChecked(true);
                                Toast.makeText(DiagPortActivity.this,
                                        R.string.credentials_wrong_password,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        passworddialog.setNegativeButton(android.R.string.no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mIsCancleDilog = true;
                        if (mDiagport.isChecked()) {
                            mDiagport.setChecked(false);
                        } else {
                            mDiagport.setChecked(true);
                        }
                    }
                });
        passworddialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (!mIsCancleDilog) {
                            if (mDiagport.isChecked()) {
                                mDiagport.setChecked(false);
                            } else {
                                mDiagport.setChecked(true);
                            }
                        }
                }
        });
        passworddialog.show();
    }

    private String getPassword() {
        return getResources().getString(R.string.def_custome_carriers_defname)+ Build.MODEL;
    }

    private static String removeFunction(String functions, String function) {
        String[] split = functions.split(",");
        for (int i = 0; i < split.length; i++) {
            if (function.equals(split[i])) {
                split[i] = null;
            }
        }
        if (split.length == 1 && split[0] == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
         for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s != null) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(s);
            }
        }
        return builder.toString();
    }
}
