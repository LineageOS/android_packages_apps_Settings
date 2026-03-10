/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.android.internal.util.CollectionUtils.filter;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.icu.text.ListFormatter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.flags.Flags;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.common.base.Objects;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Implements an AlertDialog for confirming that a user wishes to unpair or "forget" a paired
 *  device*/
public class ForgetDeviceDialogFragment extends InstrumentedDialogFragment {
    public static final String TAG = "ForgetBluetoothDevice";
    private static final String KEY_DEVICE_ADDRESS = "device_address";

    @VisibleForTesting
    CachedBluetoothDevice mDevice;
    @VisibleForTesting
    CompanionDeviceManager mCompanionDeviceManager;
    @VisibleForTesting
    PackageManager mPackageManager;
    public static ForgetDeviceDialogFragment newInstance(String deviceAddress) {
        Bundle args = new Bundle(1);
        args.putString(KEY_DEVICE_ADDRESS, deviceAddress);
        ForgetDeviceDialogFragment dialog = new ForgetDeviceDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @VisibleForTesting
    CachedBluetoothDevice getDevice(Context context) {
        String deviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        BluetoothDevice device = manager.getBluetoothAdapter().getRemoteDevice(deviceAddress);
        return manager.getCachedDeviceManager().findDevice(device);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_BLUETOOTH_PAIRED_DEVICE_FORGET;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCompanionDeviceManager = context.getSystemService(CompanionDeviceManager.class);
        mPackageManager = context.getPackageManager();
        mDevice = getDevice(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle inState) {
        if (mDevice == null) {
            throw new IllegalStateException("Device must not be null when creating dialog.");
        }
        List<AssociationInfo> associationInfos = getAssociations(mDevice.getAddress());
        Set<String> packageNames = new HashSet<>();
        if (Flags.enableRemoveAssociationBtUnpair()) {
            for (AssociationInfo ai : associationInfos) {
                CharSequence appLabel = getAppLabel(ai.getPackageName());
                if (!TextUtils.isEmpty(appLabel)) {
                    packageNames.add(appLabel.toString());
                }
            }
        }

        DialogInterface.OnClickListener onConfirm = (dialog, which) -> {
            // 1. Unpair the device.
            mDevice.unpair();
            // 2. Remove the associations if any.
            if (Flags.enableRemoveAssociationBtUnpair()) {
                for (AssociationInfo ai : associationInfos) {
                    mCompanionDeviceManager.disassociate(ai.getId());
                }
            }

            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.bluetooth_unpair_dialog_forget_confirm_button,
                        onConfirm)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setTitle(R.string.bluetooth_unpair_dialog_title);
        String message = buildUnpairMessage(
                getActivity(), mDevice, associationInfos, packageNames.stream().toList());
        dialog.setMessage(message);

        return dialog;
    }

    private List<AssociationInfo> getAssociations(String address) {
        return filter(
                mCompanionDeviceManager.getAllAssociations(),
                a -> Objects.equal(address, a.getDeviceMacAddressAsString()));
    }

    private String buildUnpairMessage(Context context, CachedBluetoothDevice device,
            List<AssociationInfo> associationInfos, List<String> packageNames) {
        if (Flags.enableRemoveAssociationBtUnpair() && !associationInfos.isEmpty()) {
            String appNamesString = getAppNamesString(packageNames.stream().toList());
            return context.getString(R.string.bluetooth_unpair_dialog_with_associations_body,
                    device.getName(), appNamesString);
        } else {
            return context.getString(R.string.bluetooth_unpair_dialog_body, device.getName());
        }
    }

    private String getAppNamesString(List<String> appNames) {
        if (appNames == null || appNames.isEmpty()) {
            return "";
        }

        ListFormatter formatter = ListFormatter.getInstance(Locale.getDefault());
        return formatter.format(appNames);
    }

    private CharSequence getAppLabel(String packageName) {
        try {
            return mPackageManager.getApplicationInfo(packageName, 0).loadSafeLabel(
                    mPackageManager,
                    PackageItemInfo.MAX_SAFE_LABEL_LENGTH,
                    TextUtils.SAFE_STRING_FLAG_TRIM
                            | TextUtils.SAFE_STRING_FLAG_FIRST_LINE);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package Not Found", e);
            return "";
        }
    }
}
