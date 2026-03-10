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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.os.Bundle;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowAlertDialogCompat.class,
})
public class ForgetDeviceDialogFragmentTest {

    private static final String DEVICE_NAME = "Nightshade";
    private static final String PACKAGE_NAME = "com.android.test";
    private static final CharSequence APP_NAME = "test";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private CompanionDeviceManager mCompanionDeviceManager;
    @Mock
    private PackageManager mPackageManager;

    private ForgetDeviceDialogFragment mFragment;
    private FragmentActivity mActivity;
    private AlertDialog mDialog;
    private Context mContext;
    private List<AssociationInfo> mAssociations;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        String deviceAddress = "55:66:77:88:99:AA";
        mAssociations = new ArrayList<>();
        mFragment = spy(ForgetDeviceDialogFragment.newInstance(deviceAddress));
        mContext = spy(RuntimeEnvironment.application);
        mFragment.mCompanionDeviceManager = mCompanionDeviceManager;
        mFragment.mPackageManager = mPackageManager;
        mFragment.mDevice = mCachedDevice;
        mActivity = Robolectric.setupActivity(FragmentActivity.class);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mCachedDevice.getAddress()).thenReturn(deviceAddress);
        when(mCachedDevice.getIdentityAddress()).thenReturn(deviceAddress);
        when(mCachedDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getName()).thenReturn(DEVICE_NAME);
        when(mCompanionDeviceManager.getAllAssociations()).thenReturn(mAssociations);
        doReturn(mCachedDevice).when(mFragment).getDevice(any());
    }

    @Ignore("b/253386225")
    @Test
    public void cancelDialog() {
        initDialog();

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        verify(mCachedDevice, never()).unpair();
        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Ignore("b/253386225")
    @Test
    public void confirmDialog() {
        initDialog();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        verify(mCachedDevice).unpair();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Ignore("b/253386225")
    @Test
    public void createDialog_normalDevice_showNormalMessage() {
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET))
                .thenReturn("false".getBytes());

        FragmentController.setupFragment(mFragment, FragmentActivity.class,
                0 /* containerViewId */, null /* bundle */);
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.bluetooth_unpair_dialog_body, DEVICE_NAME));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_REMOVE_ASSOCIATION_BT_UNPAIR)
    public void cancelDialog_with_association() {
        addAssociation();
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();

        verify(mCachedDevice, never()).unpair();
        verify(mCompanionDeviceManager, never()).disassociate(1);
        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_REMOVE_ASSOCIATION_BT_UNPAIR)
    public void confirmDialog_with_association() {
        addAssociation();
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        verify(mCachedDevice).unpair();
        verify(mCompanionDeviceManager).disassociate(1);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_REMOVE_ASSOCIATION_BT_UNPAIR)
    public void createDialog_showMessage_with_association() {
        addAssociation();
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();
        ShadowLooper.idleMainLooper();

        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString()).isEqualTo(
                mContext.getString(
                        R.string.bluetooth_unpair_dialog_with_associations_body,
                        DEVICE_NAME, APP_NAME)
        );
    }

    private void initDialog() {
        mActivity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commit();
        mDialog = (AlertDialog) ShadowDialog.getLatestDialog();
    }

    private void addAssociation() {
        setupLabelAndInfo(PACKAGE_NAME, APP_NAME);
        final AssociationInfo association =
                new AssociationInfo.Builder(1, /* userId */ 0, PACKAGE_NAME)
                        .setDeviceMacAddress(MacAddress.fromString(mCachedDevice.getAddress()))
                        .setNotifyOnDeviceNearby(true)
                        .build();

        mAssociations.add(association);
    }

    private void setupLabelAndInfo(String packageName, CharSequence appName) {
        ApplicationInfo appInfo = mock(ApplicationInfo.class);
        try {
            when(mPackageManager.getApplicationInfo(packageName, 0)).thenReturn(appInfo);
            when(appInfo.loadSafeLabel(
                    mPackageManager,
                    PackageItemInfo.MAX_SAFE_LABEL_LENGTH,
                    TextUtils.SAFE_STRING_FLAG_TRIM | TextUtils.SAFE_STRING_FLAG_FIRST_LINE))
                    .thenReturn(appName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
