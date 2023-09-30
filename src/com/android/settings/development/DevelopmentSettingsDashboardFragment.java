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

package com.android.settings.development;

import static android.app.Activity.RESULT_OK;
import static android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED;
import static android.view.flags.Flags.sensitiveContentAppProtectionApi;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.biometrics.IdentityCheckBiometricErrorDialog;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.development.autofill.AutofillCategoryController;
import com.android.settings.development.autofill.AutofillLoggingLevelPreferenceController;
import com.android.settings.development.autofill.AutofillResetOptionsPreferenceController;
import com.android.settings.development.desktopexperience.DesktopExperiencePreferenceController;
import com.android.settings.development.desktopexperience.DesktopModePreferenceController;
import com.android.settings.development.desktopexperience.DesktopModeSecondaryDisplayPreferenceController;
import com.android.settings.development.desktopexperience.FreeformWindowsPreferenceController;
import com.android.settings.development.graphicsdriver.GraphicsDriverEnableAngleAsSystemDriverController;
import com.android.settings.development.linuxterminal.LinuxTerminalPreferenceController;
import com.android.settings.development.storage.SharedDataPreferenceController;
import com.android.settings.development.window.NonResizableMultiWindowPreferenceController;
import com.android.settings.development.window.ResizableActivityPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.development.SystemPropPoker;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DevelopmentSettingsDashboardFragment extends RestrictedDashboardFragment
        implements OnCheckedChangeListener, OemUnlockDialogHost, AdbDialogHost,
        AdbClearKeysDialogHost, LogPersistDialogHost,
        NfcRebootDialog.OnNfcRebootDialogConfirmedListener {

    private static final String TAG = "DevSettingsDashboard";
    @VisibleForTesting static final int REQUEST_BIOMETRIC_PROMPT = 100;

    private boolean mIsAvailable = true;
    private boolean mIsBiometricsAuthenticated;
    private SettingsMainSwitchBar mSwitchBar;
    private DevelopmentSwitchBarController mSwitchBarController;
    private List<AbstractPreferenceController> mPreferenceControllers = new ArrayList<>();

    private final BroadcastReceiver mEnableAdbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (AbstractPreferenceController controller : mPreferenceControllers) {
                if (controller instanceof AdbOnChangeListener) {
                    ((AdbOnChangeListener) controller).onAdbSettingChanged();
                }
            }
        }
    };


    private final Runnable mSystemPropertiesChanged = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        updatePreferenceStates();
                    });
                }
            }
        }
    };

    private final Uri mDevelopEnabled = Settings.Global.getUriFor(DEVELOPMENT_SETTINGS_ENABLED);
    private final ContentObserver mDeveloperSettingsObserver = new ContentObserver(new Handler(
            Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            final boolean developmentEnabledState =
                    DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(activity);
            final boolean switchState = mSwitchBar.isChecked();

            // when developer options is enabled, but it is disabled by other privilege apps like:
            // adb command, we should disable all items and finish the activity.
            if (developmentEnabledState != switchState) {
                if (developmentEnabledState) {
                    return;
                }
                disableDeveloperOptions();
                activity.runOnUiThread(() -> finishFragment());
            }
        }
    };

    public DevelopmentSettingsDashboardFragment() {
        super(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public void onStart() {
        super.onStart();
        final ContentResolver cr = getContext().getContentResolver();
        mIsBiometricsAuthenticated = false;
        cr.registerContentObserver(mDevelopEnabled, false, mDeveloperSettingsObserver);

        // Restore UI state based on whether developer options is enabled
        if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            enableDeveloperOptions();
        } else {
            disableDeveloperOptions();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        final ContentResolver cr = getContext().getContentResolver();
        cr.unregisterContentObserver(mDeveloperSettingsObserver);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SearchMenuController.init(this);
        if (Utils.isMonkeyRunning()) {
            getActivity().finish();
            return;
        }
        Context context = requireContext();
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);

        if (!um.isAdminUser()) {
            Toast.makeText(context, R.string.dev_settings_available_to_admin_only_warning,
                            Toast.LENGTH_SHORT)
                    .show();
            finish();
        } else if (!DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)) {
            Toast.makeText(context, R.string.dev_settings_disabled_warning, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);
        // Apply page-level restrictions
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted() || !WizardManagerHelper.isDeviceProvisioned(getActivity())) {
            // Block access to developer options if the user is not the owner, if user policy
            // restricts it, or if the device has not been provisioned
            mIsAvailable = false;
            // Show error message
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(
                        com.android.settingslib.R.string.development_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        // Set up primary switch
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.setTitle(getContext().getString(R.string.developer_options_main_switch_title));
        mSwitchBar.show();
        mSwitchBarController = new DevelopmentSwitchBarController(
                this /* DevelopmentSettings */, mSwitchBar, mIsAvailable,
                getSettingsLifecycle());
    }

    @Override
    protected boolean shouldSkipForInitialSUW() {
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        registerReceivers();
        SystemProperties.addChangeCallback(mSystemPropertiesChanged);
        View root = super.onCreateView(inflater, container, savedInstanceState);
        // Mark the view sensitive to black out the screen during screen share.
        if (sensitiveContentAppProtectionApi()) {
            root.setContentSensitivity(View.CONTENT_SENSITIVITY_SENSITIVE);
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterReceivers();

        SystemProperties.removeChangeCallback(mSystemPropertiesChanged);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVELOPMENT;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean developmentEnabledState =
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext());
        if (isChecked != developmentEnabledState) {
            if (isChecked) {
                final int userId = getContext().getUserId();

                final Utils.BiometricStatus biometricAuthStatus =
                        Utils.requestBiometricAuthenticationForMandatoryBiometrics(
                                getContext(),
                                mIsBiometricsAuthenticated,
                                userId);
                if (biometricAuthStatus != Utils.BiometricStatus.NOT_ACTIVE) {
                    mSwitchBar.setChecked(false);
                    Utils.launchBiometricPromptForMandatoryBiometrics(this,
                            REQUEST_BIOMETRIC_PROMPT, userId, false /* hideBackground */);
                    return;
                } else {
                    //Reset biometrics once enable dialog is shown
                    mIsBiometricsAuthenticated = false;
                    EnableDevelopmentSettingWarningDialog.show(this /* host */);
                }
            } else {
                final BluetoothA2dpHwOffloadPreferenceController a2dpController =
                        getDevelopmentOptionsController(
                                BluetoothA2dpHwOffloadPreferenceController.class);
                final BluetoothLeAudioHwOffloadPreferenceController leAudioController =
                        getDevelopmentOptionsController(
                                BluetoothLeAudioHwOffloadPreferenceController.class);
                final NfcSnoopLogPreferenceController nfcSnoopLogController =
                        getDevelopmentOptionsController(
                                NfcSnoopLogPreferenceController.class);
                final NfcVerboseVendorLogPreferenceController nfcVerboseLogController =
                        getDevelopmentOptionsController(
                                NfcVerboseVendorLogPreferenceController.class);
                // If hardware offload isn't default value, we must reboot after disable
                // developer options. Show a dialog for the user to confirm.
                if ((a2dpController == null || a2dpController.isDefaultValue())
                        && (leAudioController == null || leAudioController.isDefaultValue())
                        && (nfcSnoopLogController == null || nfcSnoopLogController.isDefaultValue())
                        && (nfcVerboseLogController == null
                                || nfcVerboseLogController.isDefaultValue())) {
                    disableDeveloperOptions();
                } else {
                    // Disabling developer options in page-agnostic mode isn't supported as device
                    // isn't in production state
                    if (Enable16kUtils.isPageAgnosticModeOn(getContext())) {
                        Enable16kUtils.showPageAgnosticWarning(getContext());
                        onDisableDevelopmentOptionsRejected();
                        return;
                    }
                    DisableDevSettingsDialogFragment.show(this /* host */);
                }
            }
            FeatureFactory.getFeatureFactory().getSearchFeatureProvider()
                    .sendPreIndexIntent(getContext());
        }
    }

    @Override
    public void onOemUnlockDialogConfirmed() {
        final OemUnlockPreferenceController controller = getDevelopmentOptionsController(
                OemUnlockPreferenceController.class);
        controller.onOemUnlockConfirmed();
    }

    @Override
    public void onOemUnlockDialogDismissed() {
        final OemUnlockPreferenceController controller = getDevelopmentOptionsController(
                OemUnlockPreferenceController.class);
        controller.onOemUnlockDismissed();
    }

    @Override
    public void onEnableAdbDialogConfirmed() {
        final AdbPreferenceController controller = getDevelopmentOptionsController(
                AdbPreferenceController.class);
        controller.onAdbDialogConfirmed();

    }

    @Override
    public void onEnableAdbDialogDismissed() {
        final AdbPreferenceController controller = getDevelopmentOptionsController(
                AdbPreferenceController.class);
        controller.onAdbDialogDismissed();
    }

    @Override
    public void onAdbClearKeysDialogConfirmed() {
        final ClearAdbKeysPreferenceController controller = getDevelopmentOptionsController(
                ClearAdbKeysPreferenceController.class);
        controller.onClearAdbKeysConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogConfirmed() {
        final LogPersistPreferenceController controller = getDevelopmentOptionsController(
                LogPersistPreferenceController.class);
        controller.onDisableLogPersistDialogConfirmed();
    }

    @Override
    public void onDisableLogPersistDialogRejected() {
        final LogPersistPreferenceController controller = getDevelopmentOptionsController(
                LogPersistPreferenceController.class);
        controller.onDisableLogPersistDialogRejected();
    }

    @Override
    public void onNfcRebootDialogConfirmed() {
        final NfcSnoopLogPreferenceController controller =
                getDevelopmentOptionsController(NfcSnoopLogPreferenceController.class);
        controller.onNfcRebootDialogConfirmed();

        final NfcVerboseVendorLogPreferenceController vendorLogController =
                getDevelopmentOptionsController(NfcVerboseVendorLogPreferenceController.class);
        vendorLogController.onNfcRebootDialogConfirmed();
    }

    @Override
    public void onNfcRebootDialogCanceled() {
        final NfcSnoopLogPreferenceController controller =
                getDevelopmentOptionsController(NfcSnoopLogPreferenceController.class);
        controller.onNfcRebootDialogCanceled();

        final NfcVerboseVendorLogPreferenceController vendorLogController =
                getDevelopmentOptionsController(NfcVerboseVendorLogPreferenceController.class);
        vendorLogController.onNfcRebootDialogCanceled();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handledResult = false;
        if (requestCode == REQUEST_BIOMETRIC_PROMPT) {
            if (resultCode == RESULT_OK) {
                mIsBiometricsAuthenticated = true;
                mSwitchBar.setChecked(true);
            } else if (resultCode
                    == ConfirmDeviceCredentialActivity.BIOMETRIC_LOCKOUT_ERROR_RESULT) {
                IdentityCheckBiometricErrorDialog.showBiometricErrorDialog(getActivity(),
                        Utils.BiometricStatus.LOCKOUT, false /* twoFactorAuthentication */);
            }
        }
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof OnActivityResultListener) {
                // We do not break early because it is possible for multiple controllers to
                // handle the same result code.
                handledResult |=
                        ((OnActivityResultListener) controller).onActivityResult(
                                requestCode, resultCode, data);
            }
        }
        if (!handledResult) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return Utils.isMonkeyRunning() ? R.xml.placeholder_prefs : R.xml.development_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        if (Utils.isMonkeyRunning()) {
            mPreferenceControllers = new ArrayList<>();
            return null;
        }
        mPreferenceControllers = buildPreferenceControllers(context, getActivity(),
                getSettingsLifecycle(), this /* devOptionsDashboardFragment */);
        return mPreferenceControllers;
    }

    private void registerReceivers() {
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(mEnableAdbReceiver, new IntentFilter(
                        AdbPreferenceController.ACTION_ENABLE_ADB_STATE_CHANGED));
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mEnableAdbReceiver);
    }

    private void enableDeveloperOptions() {
        if (Utils.isMonkeyRunning()) {
            return;
        }
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), true);
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof DeveloperOptionsPreferenceController) {
                ((DeveloperOptionsPreferenceController) controller).onDeveloperOptionsEnabled();
            }
        }
    }

    private void disableDeveloperOptions() {
        if (Utils.isMonkeyRunning()) {
            return;
        }

        // Disabling developer options in page-agnostic mode isn't supported as device isn't in
        // production state
        if (Enable16kUtils.isPageAgnosticModeOn(getContext())) {
            Enable16kUtils.showPageAgnosticWarning(getContext());
            onDisableDevelopmentOptionsRejected();
            return;
        }

        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(getContext(), false);
        final SystemPropPoker poker = SystemPropPoker.getInstance();
        poker.blockPokes();
        for (AbstractPreferenceController controller : mPreferenceControllers) {
            if (controller instanceof DeveloperOptionsPreferenceController) {
                ((DeveloperOptionsPreferenceController) controller)
                        .onDeveloperOptionsDisabled();
            }
        }
        poker.unblockPokes();
        poker.poke();
    }

    void onEnableDevelopmentOptionsConfirmed() {
        enableDeveloperOptions();
    }

    void onEnableDevelopmentOptionsRejected() {
        // Reset the toggle
        mSwitchBar.setChecked(false);
    }

    void onDisableDevelopmentOptionsConfirmed() {
        disableDeveloperOptions();
    }

    void onDisableDevelopmentOptionsRejected() {
        // Reset the toggle
        mSwitchBar.setChecked(true);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            @Nullable Activity activity, @Nullable Lifecycle lifecycle,
            @Nullable DevelopmentSettingsDashboardFragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new MemoryUsagePreferenceController(context));
        controllers.add(new EnableWebAppMinterPreferenceController(context));
        controllers.add(new BugReportPreferenceController(context));
        controllers.add(new BugReportHandlerPreferenceController(context));
        controllers.add(new SystemServerHeapDumpPreferenceController(context));
        controllers.add(new DevelopmentMemtagPagePreferenceController(context));
        controllers.add(new LocalBackupPasswordPreferenceController(context));
        controllers.add(new StayAwakePreferenceController(context, lifecycle));
        controllers.add(new HdcpCheckingPreferenceController(context));
        controllers.add(new DefaultLaunchPreferenceController(context,
                "bluetooth_development_settings"));
        controllers.add(new OemUnlockPreferenceController(context, activity, fragment));
        controllers.add(new Enable16kPagesPreferenceController(context, fragment));
        controllers.add(new PictureColorModePreferenceController(context, lifecycle));
        controllers.add(new WebViewAppPreferenceController(context));
        controllers.add(new WebViewDevUiPreferenceController(context));
        controllers.add(new CoolColorTemperaturePreferenceController(context));
        controllers.add(new DisableAutomaticUpdatesPreferenceController(context));
        controllers.add(new SelectDSUPreferenceController(context));
        controllers.add(new AdbPreferenceController(context, fragment));
        controllers.add(new ClearAdbKeysPreferenceController(context, fragment));
        controllers.add(new AdbWirelessDebuggingPreferenceController(context, lifecycle));
        controllers.add(new AdbRootPreferenceController(context, fragment));
        controllers.add(new AdbAuthorizationTimeoutPreferenceController(context));
        controllers.add(new LocalTerminalPreferenceController(context));
        controllers.add(new LinuxTerminalPreferenceController(context));
        controllers.add(new BugReportInPowerPreferenceController(context));
        controllers.add(new AutomaticSystemServerHeapDumpPreferenceController(context));
        controllers.add(new MockLocationAppPreferenceController(context, fragment));
        controllers.add(new MockModemPreferenceController(context));
        controllers.add(new DebugViewAttributesPreferenceController(context));
        controllers.add(new SelectDebugAppPreferenceController(context, fragment));
        controllers.add(new WaitForDebuggerPreferenceController(context));
        controllers.add(new EnableGpuDebugLayersPreferenceController(context));
        controllers.add(new GraphicsDriverEnableAngleAsSystemDriverController(context, fragment));
        controllers.add(new ForcePeakRefreshRatePreferenceController(context));
        controllers.add(new EnableVerboseVendorLoggingPreferenceController(context));
        controllers.add(new VerifyAppsOverUsbPreferenceController(context));
        controllers.add(new ArtVerifierPreferenceController(context));
        controllers.add(new LogdSizePreferenceController(context));
        controllers.add(new LogPersistPreferenceController(context, fragment, lifecycle));
        controllers.add(new CameraLaserSensorPreferenceController(context));
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            controllers.add(new WifiDisplayCertificationPreferenceController(context));
            controllers.add(new WifiVerboseLoggingPreferenceController(context));
            controllers.add(new WifiScanThrottlingPreferenceController(context));
            controllers.add(new WifiNonPersistentMacRandomizationPreferenceController(context));
        }
        controllers.add(new MobileDataAlwaysOnPreferenceController(context));
        controllers.add(new TetheringHardwareAccelPreferenceController(context));
        controllers.add(new NfcSnoopLogPreferenceController(context, fragment));
        controllers.add(new NfcVerboseVendorLogPreferenceController(context, fragment));
        controllers.add(new ShowTapsPreferenceController(context));
        controllers.add(new PointerLocationPreferenceController(context));
        controllers.add(new ShowKeyPressesPreferenceController(context));
        controllers.add(new TouchpadVisualizerPreferenceController(context));
        controllers.add(new ShowSurfaceUpdatesPreferenceController(context));
        controllers.add(new ShowLayoutBoundsPreferenceController(context));
        controllers.add(new ShowHdrSdrRatioPreferenceController(context));
        controllers.add(new ShowRefreshRatePreferenceController(context));
        controllers.add(new RtlLayoutPreferenceController(context));
        controllers.add(new WindowAnimationScalePreferenceController(context));
        controllers.add(new EmulateDisplayCutoutPreferenceController(context));
        controllers.add(new TransparentNavigationBarPreferenceController(context));
        controllers.add(new TransitionAnimationScalePreferenceController(context));
        controllers.add(new AnimatorDurationScalePreferenceController(context));
        controllers.add(new SecondaryDisplayPreferenceController(context));
        controllers.add(new GpuViewUpdatesPreferenceController(context));
        controllers.add(new HardwareLayersUpdatesPreferenceController(context));
        controllers.add(new DebugGpuOverdrawPreferenceController(context));
        controllers.add(new DebugNonRectClipOperationsPreferenceController(context));
        controllers.add(new GameDefaultFrameRatePreferenceController(context));
        controllers.add(new ForceDarkPreferenceController(context));
        controllers.add(new ForceMSAAPreferenceController(context));
        controllers.add(new HardwareOverlaysPreferenceController(context));
        controllers.add(new SimulateColorSpacePreferenceController(context));
        controllers.add(new UsbAudioRoutingPreferenceController(context));
        controllers.add(new StrictModePreferenceController(context));
        controllers.add(new ProfileGpuRenderingPreferenceController(context));
        controllers.add(new KeepActivitiesPreferenceController(context));
        controllers.add(new BackgroundProcessLimitPreferenceController(context));
        controllers.add(new CachedAppsFreezerPreferenceController(context));
        controllers.add(new ShowFirstCrashDialogPreferenceController(context));
        controllers.add(new AppsNotRespondingPreferenceController(context));
        controllers.add(new NotificationChannelWarningsPreferenceController(context));
        controllers.add(new AllowAppsOnExternalPreferenceController(context));
        controllers.add(new ResizableActivityPreferenceController(context));
        controllers.add(new FreeformWindowsPreferenceController(context, fragment));
        controllers.add(new DesktopModePreferenceController(context, fragment));
        controllers.add(new DesktopModeSecondaryDisplayPreferenceController(context, fragment));
        controllers.add(new DesktopExperiencePreferenceController(context, fragment));
        controllers.add(new NonResizableMultiWindowPreferenceController(context));
        controllers.add(new ShortcutManagerThrottlingPreferenceController(context));
        controllers.add(new EnableGnssRawMeasFullTrackingPreferenceController(context));
        controllers.add(new DefaultLaunchPreferenceController(context, "running_apps"));
        controllers.add(new DefaultLaunchPreferenceController(context, "demo_mode"));
        controllers.add(new DefaultLaunchPreferenceController(context, "quick_settings_tiles"));
        controllers.add(new DefaultLaunchPreferenceController(context, "feature_flags_dashboard"));
        controllers.add(new DefaultUsbConfigurationPreferenceController(context));
        controllers.add(new DefaultLaunchPreferenceController(context, "density"));
        controllers.add(new DefaultLaunchPreferenceController(context, "background_check"));
        controllers.add(new DefaultLaunchPreferenceController(context, "inactive_apps"));
        controllers.add(new AutofillCategoryController(context, lifecycle));
        controllers.add(new AutofillLoggingLevelPreferenceController(context, lifecycle));
        controllers.add(new AutofillResetOptionsPreferenceController(context));
        controllers.add(new PrintVerboseLoggingController(context));
        controllers.add(new SharedDataPreferenceController(context));
        controllers.add(new OverlaySettingsPreferenceController(context));
        controllers.add(new StylusHandwritingPreferenceController(context));
        controllers.add(new IngressRateLimitPreferenceController((context)));
        controllers.add(new PhantomProcessPreferenceController(context));
        controllers.add(new ForceEnableNotesRolePreferenceController(context));
        controllers.add(new GrammaticalGenderPreferenceController(context));
        controllers.add(new SensitiveContentProtectionPreferenceController(context));
        controllers.add(new ShadeDisplayAwarenessPreferenceController(context));
        controllers.add(new TextCursorBlinkRatePreferenceController(context));

        return controllers;
    }

    @VisibleForTesting
    <T extends AbstractPreferenceController> T getDevelopmentOptionsController(Class<T> clazz) {
        return use(clazz);
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.development_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* activity */,
                            null /* lifecycle */, null /* devOptionsDashboardFragment */);
                }
            };
}
