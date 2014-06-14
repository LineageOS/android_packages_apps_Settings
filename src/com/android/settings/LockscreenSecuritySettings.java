package com.android.settings;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.*;
import android.provider.Settings;
import com.android.internal.widget.LockPatternUtils;

import java.util.List;

public class LockscreenSecuritySettings extends RestrictedSettingsFragment
        implements Preference.OnPreferenceChangeListener {
    static final String TAG = "LockscreenSecuritySettings";

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
            "biometric_weak_improve_matching";
    private static final String KEY_BIOMETRIC_WEAK_LIVELINESS = "biometric_weak_liveliness";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_VISIBLE_ERROR_PATTERN = "visible_error_pattern";
    private static final String KEY_VISIBLE_DOTS = "visibledots";
    private static final String KEY_SECURITY_CATEGORY = "security_category";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;

    // CM Attributes
    private static final String SLIDE_LOCK_CATEGORY = "delay_and_timeout";
    private static final String SLIDE_LOCK_TIMEOUT_DELAY = "slide_lock_timeout_delay";
    private static final String SLIDE_LOCK_SCREENOFF_DELAY = "slide_lock_screenoff_delay";

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;

    private CheckBoxPreference mBiometricWeakLiveliness;
    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mVisibleErrorPattern;
    private CheckBoxPreference mVisibleDots;

    private boolean mIsSlideLockDelay = false;

    private ListPreference mSlideLockTimeoutDelay;
    private ListPreference mSlideLockScreenOffDelay;

    public LockscreenSecuritySettings() {
        super(null /* Don't ask for restrictions pin on creation. */);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();
        PreferenceGroup category;

        final ContentResolver resolver = getContentResolver();

        // Add options for lock/unlock screen
        int resid = 0;
        if (!mLockPatternUtils.isSecure()) {
            // if there are multiple users, disable "None" setting
            UserManager mUm = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = mUm.getUsers(true);
            final boolean singleUser = users.size() == 1;

            if (singleUser && mLockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_chooser_disabled_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (mLockPatternUtils.usingBiometricWeak() &&
                mLockPatternUtils.isBiometricWeakInstalled()) {
            resid = R.xml.security_settings_biometric_weak;
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        addPreferencesFromResource(resid);

        mIsSlideLockDelay = (resid == R.xml.security_settings_chooser);

        if (!mLockPatternUtils.isLockScreenDisabled()) {
            addPreferencesFromResource(R.xml.security_settings_slide_delay_cyanogenmod);
            category = (PreferenceCategory) root.findPreference(SLIDE_LOCK_CATEGORY);

            mSlideLockTimeoutDelay = (ListPreference) root
                    .findPreference(SLIDE_LOCK_TIMEOUT_DELAY);
            final int slideTimeoutDelay;
            if (mIsSlideLockDelay) {
                slideTimeoutDelay = Settings.System.getInt(resolver,
                        Settings.System.SCREEN_LOCK_SLIDE_TIMEOUT_DELAY, 5000);
            } else {
                slideTimeoutDelay = Settings.Secure.getInt(resolver,
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
            }
            mSlideLockTimeoutDelay.setValue(String.valueOf(slideTimeoutDelay));
            updateSlideAfterTimeoutSummary();
            mSlideLockTimeoutDelay.setOnPreferenceChangeListener(this);

            mSlideLockScreenOffDelay = (ListPreference) root
                    .findPreference(SLIDE_LOCK_SCREENOFF_DELAY);
            if (mSlideLockScreenOffDelay != null) {
                if (!mIsSlideLockDelay && category != null) {
                    category.removePreference(mSlideLockScreenOffDelay);
                } else {
                    final int slideScreenOffDelay = Settings.System.getInt(resolver,
                            Settings.System.SCREEN_LOCK_SLIDE_SCREENOFF_DELAY, 0);
                    mSlideLockScreenOffDelay.setValue(String.valueOf(slideScreenOffDelay));
                    updateSlideAfterScreenOffSummary();
                    mSlideLockScreenOffDelay.setOnPreferenceChangeListener(this);
                }
            }
        }

        // biometric weak liveliness
        mBiometricWeakLiveliness =
                (CheckBoxPreference) root.findPreference(KEY_BIOMETRIC_WEAK_LIVELINESS);

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // visible error pattern
        mVisibleErrorPattern = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_ERROR_PATTERN);

        // visible dots
        mVisibleDots = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_DOTS);

        // don't display visible pattern if biometric and backup is not pattern
        if (resid == R.xml.security_settings_biometric_weak &&
                mLockPatternUtils.getKeyguardStoredPasswordQuality() !=
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            category = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (category != null) {
                if (mVisiblePattern != null) {
                    category.removePreference(mVisiblePattern);
                }
                if (mVisibleErrorPattern != null) {
                    category.removePreference(mVisibleErrorPattern);
                }
                if (mVisibleDots != null) {
                    category.removePreference(mVisibleDots);
                }
            }
        }
        return root;
    }

    private void updateSlideAfterTimeoutSummary() {
        // Update summary message with current value
        long currentTimeout = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.SCREEN_LOCK_SLIDE_TIMEOUT_DELAY, 5000);
        final CharSequence[] entries = mSlideLockTimeoutDelay.getEntries();
        final CharSequence[] values = mSlideLockTimeoutDelay.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mSlideLockTimeoutDelay.setSummary(entries[best]);
    }

    private void updateSlideAfterScreenOffSummary() {
        // Update summary message with current value
        long currentTimeout = android.provider.Settings.System.getInt(getContentResolver(),
                Settings.System.SCREEN_LOCK_SLIDE_SCREENOFF_DELAY, 0);
        final CharSequence[] entries = mSlideLockScreenOffDelay.getEntries();
        final CharSequence[] values = mSlideLockScreenOffDelay.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mSlideLockScreenOffDelay.setSummary(entries[best]);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mBiometricWeakLiveliness != null) {
            mBiometricWeakLiveliness.setChecked(
                    lockPatternUtils.isBiometricWeakLivelinessEnabled());
        }
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mVisibleErrorPattern != null) {
            mVisibleErrorPattern.setChecked(lockPatternUtils.isShowErrorPath());
        }
        if (mVisibleDots != null) {
            mVisibleDots.setChecked(lockPatternUtils.isVisibleDotsEnabled());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (ensurePinRestrictedPreference(preference)) {
            return true;
        }
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST, null, null)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and start improve.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                startBiometricWeakImprove();
            }
        } else if (KEY_BIOMETRIC_WEAK_LIVELINESS.equals(key)) {
            if (isToggled(preference)) {
                lockPatternUtils.setBiometricWeakLivelinessEnabled(true);
            } else {
                // In this case the user has just unchecked the checkbox, but this action requires
                // them to confirm their password.  We need to re-check the checkbox until
                // they've confirmed their password
                mBiometricWeakLiveliness.setChecked(true);
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(
                        CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, null)) {
                    // If this returns false, it means no password confirmation is required, so
                    // go ahead and uncheck it here.
                    // Note: currently a backup is required for biometric_weak so this code path
                    // can't be reached, but is here in case things change in the future
                    lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
                    mBiometricWeakLiveliness.setChecked(false);
                }
            }
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_ERROR_PATTERN.equals(key)) {
            lockPatternUtils.setShowErrorPath(isToggled(preference));
        } else if (KEY_VISIBLE_DOTS.equals(key)) {
            lockPatternUtils.setVisibleDotsEnabled(isToggled(preference));
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            // Setting the mBiometricWeakLiveliness checked value to false is handled when onResume
            // is called by grabbing the value from lockPatternUtils.  We can't set it here
            // because mBiometricWeakLiveliness could be null
            return;
        }
        createPreferenceHierarchy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mSlideLockTimeoutDelay) {
            int slideTimeoutDelay = Integer.valueOf((String) value);
            if (mIsSlideLockDelay) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_LOCK_SLIDE_TIMEOUT_DELAY, slideTimeoutDelay);
            } else {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, slideTimeoutDelay);
            }
            updateSlideAfterTimeoutSummary();
        } else if (preference == mSlideLockScreenOffDelay) {
            int slideScreenOffDelay = Integer.valueOf((String) value);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_LOCK_SLIDE_SCREENOFF_DELAY, slideScreenOffDelay);
            updateSlideAfterScreenOffSummary();
        }
        return true;
    }

    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }
}
