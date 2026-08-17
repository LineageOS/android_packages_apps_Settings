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

import static android.bluetooth.BluetoothDevice.BOND_NONE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.ui.model.FragmentTypeModel;
import com.android.settings.connecteddevice.stylus.StylusDevicesController;
import com.android.settings.inputmethod.KeyboardSettingsPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.collect.ImmutableList;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BluetoothDeviceDetailsFragment extends BluetoothDetailsConfigurableFragment {
    private static final String TAG = "BTDeviceDetailsFrg";
    private static final int METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25;

    /**
     * An interface to let tests override the normal mechanism for looking up the
     * CachedBluetoothDevice and LocalBluetoothManager, and substitute their own mocks instead.
     * This is only needed in situations where you instantiate the fragment indirectly (eg via an
     * intent) and can't use something like spying on an instance you construct directly via
     * newInstance.
     */
    @VisibleForTesting
    interface TestDataFactory {
        CachedBluetoothDevice getDevice(String deviceAddress);

        LocalBluetoothManager getManager(Context context);

        UserManager getUserManager();
    }

    @VisibleForTesting
    static TestDataFactory sTestDataFactory;

    BluetoothAdapter mBluetoothAdapter;
    boolean mIsKeyMissingDevice = false;

    @Nullable
    InputDevice mInputDevice;

    private UserManager mUserManager;

    private final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onBluetoothStateChanged(int bluetoothState) {
                    if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                        Log.i(TAG, "Bluetooth is off, exit activity.");
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.finish();
                        }
                    }
                }

                @Override
                public void onDeviceBondStateChanged(
                        @NonNull CachedBluetoothDevice device, int bondState) {
                    if (device.equals(cachedDevice)) {
                        finishFragmentIfNecessary();
                    }
                }
            };

    public BluetoothDeviceDetailsFragment() {
        super();
    }

    @VisibleForTesting
    LocalBluetoothManager getLocalBluetoothManager(Context context) {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getManager(context);
        }
        return Utils.getLocalBtManager(context);
    }

    @VisibleForTesting
    UserManager getUserManager() {
        if (sTestDataFactory != null) {
            return sTestDataFactory.getUserManager();
        }

        return getSystemService(UserManager.class);
    }

    public static BluetoothDeviceDetailsFragment newInstance(String deviceAddress) {
        Bundle args = new Bundle(1);
        args.putString(KEY_DEVICE_ADDRESS, deviceAddress);
        BluetoothDeviceDetailsFragment fragment = new BluetoothDeviceDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        String callingAppPackageName =
                ((SettingsActivity) getActivity()).getInitialCallingPackage();
        logPageEntrypoint(context, callingAppPackageName, getIntent());
        localBluetoothManager = getLocalBluetoothManager(context);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mUserManager = getUserManager();

        if (FeatureFlagUtils.isEnabled(context,
                FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES)) {
            mInputDevice = BluetoothUtils.getInputDevice(context, deviceAddress);
        }

        if (cachedDevice == null) {
            return;
        }
        Integer keyMissingCount = BluetoothUtils.getKeyMissingCount(cachedDevice.getDevice());
        mIsKeyMissingDevice = keyMissingCount != null && keyMissingCount > 0;

        getController(
                AdvancedBluetoothDetailsHeaderController.class,
                controller -> controller.init(cachedDevice, this));
        getController(
                LeAudioBluetoothDetailsHeaderController.class,
                controller -> controller.init(cachedDevice, localBluetoothManager, this));
        getController(
                KeyboardSettingsPreferenceController.class,
                controller -> controller.init(cachedDevice));

        final BluetoothFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getBluetoothFeatureProvider();

        getController(
                BlockingPrefWithSliceController.class,
                controller ->
                        controller.setSliceUri(
                                featureProvider.getBluetoothDeviceSettingsUri(
                                        cachedDevice.getDevice())));

        localBluetoothManager.getEventManager().registerCallback(mBluetoothCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        localBluetoothManager.getEventManager().unregisterCallback(mBluetoothCallback);
    }

    protected <T extends AbstractPreferenceController> void getController(Class<T> clazz,
            Consumer<T> action) {
        T controller = use(clazz);
        if (controller != null) {
            action.accept(controller);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleForInputDevice();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        getListView().setItemViewCacheSize(100);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mIsKeyMissingDevice) {
            requestUpdateLayout(generatePreferenceKeysForBondingLoss());
        } else {
            requestUpdateLayout(generatePreferenceKeysForLoading());
            requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment.INSTANCE);
        }
        setDivider(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitleForInputDevice();
        finishFragmentIfNecessary();
    }

    @VisibleForTesting
    void finishFragmentIfNecessary() {
        if (cachedDevice.getBondState() == BOND_NONE) {
            finish();
            return;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_device_details_fragment;
    }

    private List<String> generatePreferenceKeysForBondingLoss() {
        ImmutableList.Builder<String> visibleKeys = new ImmutableList.Builder<>();
        visibleKeys
                .add(use(BluetoothDetailsBannerController.class).getPreferenceKey())
                .add(use(AdvancedBluetoothDetailsHeaderController.class).getPreferenceKey())
                .add(use(LeAudioBluetoothDetailsHeaderController.class).getPreferenceKey())
                .add(use(BluetoothDetailsButtonsController.class).getPreferenceKey());
        if (!BluetoothUtils.isHeadset(cachedDevice.getDevice())) {
            visibleKeys.add(use(BluetoothDetailsMacAddressController.class).getPreferenceKey());
        }
        return visibleKeys.build();
    }

    private List<String> generatePreferenceKeysForLoading() {
        ImmutableList.Builder<String> visibleKeys = new ImmutableList.Builder<>();
        visibleKeys
                .add(use(BluetoothDetailsBannerController.class).getPreferenceKey())
                .add(use(AdvancedBluetoothDetailsHeaderController.class).getPreferenceKey())
                .add(use(LeAudioBluetoothDetailsHeaderController.class).getPreferenceKey())
                .add(use(BluetoothDetailsButtonsController.class).getPreferenceKey())
                .add(LOADING_PREF);
        return visibleKeys.build();
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();
        if (isCachedDeviceInitialized()) {
            Lifecycle lifecycle = getSettingsLifecycle();
            controllers.add(
                    new BluetoothDetailsBannerController(
                            context, this, cachedDevice, lifecycle));
            controllers.add(
                    new GeneralBluetoothDetailsHeaderController(
                            context, this, cachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsButtonsController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(
                    new BluetoothDetailsAudioSharingController(
                            context, this, localBluetoothManager, cachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsCompanionAppsController(context, this,
                    cachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsAudioDeviceTypeController(context, this,
                    localBluetoothManager,
                    cachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsSpatialAudioController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsProfilesController(context, this,
                    localBluetoothManager,
                    cachedDevice, lifecycle));
            controllers.add(new BluetoothDetailsAutoConnectController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsMacAddressController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(new StylusDevicesController(context, mInputDevice, cachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsRelatedToolsController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsPairOtherController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsDataSyncController(context, this, cachedDevice,
                    lifecycle));
            controllers.add(new BluetoothDetailsExtraOptionsController(context, this, cachedDevice,
                    lifecycle));
            BluetoothDetailsHearingDeviceController hearingDeviceController =
                    new BluetoothDetailsHearingDeviceController(context, this,
                            localBluetoothManager,
                            cachedDevice, lifecycle);
            controllers.add(hearingDeviceController);
            hearingDeviceController.initSubControllers(isLaunchFromHearingDevicePage());
            controllers.addAll(hearingDeviceController.getSubControllers());
        }
        return controllers;
    }

    private int getPaddingSize() {
        TypedArray resolvedAttributes =
                getContext().obtainStyledAttributes(
                        new int[]{
                                android.R.attr.listPreferredItemPaddingStart,
                                android.R.attr.listPreferredItemPaddingEnd
                        });
        int width = resolvedAttributes.getDimensionPixelSize(0, 0)
                + resolvedAttributes.getDimensionPixelSize(1, 0);
        resolvedAttributes.recycle();
        return width;
    }

    private boolean isLaunchFromHearingDevicePage() {
        final Intent intent = getIntent();
        if (intent == null) {
            return false;
        }

        return intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                SettingsEnums.PAGE_UNKNOWN) == SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS;
    }

    @VisibleForTesting
    void setTitleForInputDevice() {
        if (BluetoothUtils.isDeviceStylus(mInputDevice, cachedDevice)) {
            // This will override the default R.string.device_details_title "Device Details"
            // that will show on non-stylus bluetooth devices.
            // That title is set via the manifest and also from BluetoothDeviceUpdater.
            getActivity().setTitle(getContext().getString(R.string.stylus_device_details_title));
        }
    }

    private void logPageEntrypoint(
            @NonNull Context context,
            @Nullable String callingAppPackageName,
            @Nullable Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        JSONObject formattedLogging = new JSONObject();
        try {
            formattedLogging.put("calling_package", callingAppPackageName);
            formattedLogging.put("intent_action", action);
            mMetricsFeatureProvider.action(
                    context,
                    SettingsEnums.ACTION_OPEN_SETTINGS_DEVICE_DETAILS,
                    formattedLogging.toString());
        } catch (JSONException e) {
            Log.w(TAG, "Error happened when logging entrypoint");
        }
    }
}
