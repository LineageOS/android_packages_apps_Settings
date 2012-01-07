/*
 * Copyright (C) 2010 James Bottomley <James.Bottomley@suse.de>
 *
 *
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

package com.android.settings.vpn;

import com.android.settings.R;

import android.content.Context;
import android.content.Intent;
import android.net.vpn.OpenvpnProfile;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * The class for editing {@link OpenvpnProfile}.
 */
class OpenvpnEditor extends VpnProfileEditor {

    private static final String KEY_PROFILE = "openvpn_profile";

    private static final int REQUEST_ADVANCED = 1;

    private static final String TAG = OpenvpnEditor.class.getSimpleName();

    private int MENU_ID_ADVANCED;

    private KeyStore mKeyStore = KeyStore.getInstance();

    private CheckBoxPreference mUserAuth;

    private ListPreference mCert;

    private ListPreference mCACert;

    public OpenvpnEditor(OpenvpnProfile p) {
        super(p);
    }

    @Override
    protected void loadExtraPreferencesTo(PreferenceGroup subpanel) {
        final Context c = subpanel.getContext();
        final OpenvpnProfile profile = (OpenvpnProfile) getProfile();
        mUserAuth = new CheckBoxPreference(c);
        mUserAuth.setTitle(R.string.vpn_openvpn_userauth);
        mUserAuth.setSummary(R.string.vpn_openvpn_userauth_summary);
        mUserAuth.setChecked(profile.getUserAuth());
        mUserAuth.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                boolean enabled = (Boolean) newValue;
                profile.setUserAuth(enabled);
                mUserAuth.setChecked(enabled);
                return true;
            }
        });
        subpanel.addPreference(mUserAuth);
        mCACert = createList(c, R.string.vpn_ca_certificate_title, profile.getCAName(),
                mKeyStore.saw(Credentials.CA_CERTIFICATE),
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference pref, Object newValue) {
                        String f = (String) newValue;
                        profile.setCAName(f);
                        setSummary(mCACert, R.string.vpn_ca_certificate, profile.getCAName());

                        return true;
                    }
                });
        setSummary(mCACert, R.string.vpn_ca_certificate, profile.getCAName());
        subpanel.addPreference(mCACert);

        mCert = createList(c, R.string.vpn_user_certificate_title, profile.getCertName(),
                mKeyStore.saw(Credentials.USER_CERTIFICATE),
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference pref, Object newValue) {
                        String f = (String) newValue;
                        profile.setCertName(f);
                        setSummary(mCert, R.string.vpn_user_certificate, profile.getCertName());

                        return true;
                    }
                });
        setSummary(mCert, R.string.vpn_user_certificate, profile.getCertName());
        subpanel.addPreference(mCert);
    }

    @Override
    public String validate() {
        String result = super.validate();
        if (result != null)
            return result;

        if (!mUserAuth.isChecked()) {
            result = validate(mCert, R.string.vpn_a_user_certificate);
            if (result != null)
                return result;
        }

        return validate(mCACert, R.string.vpn_a_ca_certificate);
    }

    @Override
    protected void onCreateOptionsMenu(Menu menu, int last_item) {
        MENU_ID_ADVANCED = last_item + 1;

        menu.add(0, MENU_ID_ADVANCED, 0, R.string.wifi_menu_advanced).setIcon(
                android.R.drawable.ic_menu_manage);
    }

    @Override
    protected boolean onOptionsItemSelected(PreferenceActivity p, MenuItem item) {
        if (item.getItemId() == MENU_ID_ADVANCED) {
            Intent intent = new Intent(p, AdvancedSettings.class);
            intent.putExtra(KEY_PROFILE, (Parcelable) getProfile());
            p.startActivityForResult(intent, REQUEST_ADVANCED);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode != REQUEST_ADVANCED)
            return;

        OpenvpnProfile p = (OpenvpnProfile) getProfile();
        OpenvpnProfile newP = data.getParcelableExtra(KEY_PROFILE);
        if (newP == null) {
            Log.e(TAG, "no profile from advanced settings");
            return;
        }
        // manually copy across all advanced settings
        p.setPort(newP.getPort());
        p.setProto(newP.getProto());
        p.setDevice(newP.getDevice());
        p.setUseCompLzo(newP.getUseCompLzo());
        p.setRedirectGateway(newP.getRedirectGateway());
        p.setSupplyAddr(newP.getSupplyAddr());
        p.setLocalAddr(newP.getLocalAddr());
        p.setRemoteAddr(newP.getRemoteAddr());
        p.setCipher(newP.getCipher());
        p.setKeySize(newP.getKeySize());
        p.setExtra(newP.getExtra());
    }

    private ListPreference createList(Context c, int titleResId, String selection, String[] keys,
            Preference.OnPreferenceChangeListener listener) {
        ListPreference pref = new ListPreference(c);
        pref.setTitle(titleResId);
        pref.setDialogTitle(titleResId);
        pref.setPersistent(true);
        pref.setEntries(keys);
        pref.setEntryValues(keys);
        pref.setValue(selection);
        pref.setOnPreferenceChangeListener(listener);
        return pref;
    }

    public static class AdvancedSettings extends PreferenceActivity {
        private static final String KEY_PORT = "set_port";

        private static final String KEY_PROTO = "set_protocol";

        private static final String KEY_DEVICE = "set_device";

        private static final String KEY_COMP_LZO = "set_comp_lzo";

        private static final String KEY_REDIRECT_GATEWAY = "set_redirect_gateway";

        private static final String KEY_SET_ADDR = "set_addr";

        private static final String KEY_LOCAL_ADDR = "set_local_addr";

        private static final String KEY_REMOTE_ADDR = "set_remote_addr";

        private static final String KEY_CIPHER = "set_cipher";

        private static final String KEY_KEYSIZE = "set_keysize";

        private static final String KEY_EXTRA = "set_extra";

        private EditTextPreference mPort;

        private ListPreference mProto;

        private ListPreference mDevice;

        private CheckBoxPreference mCompLzo;

        private CheckBoxPreference mRedirectGateway;

        private CheckBoxPreference mSetAddr;

        private EditTextPreference mLocalAddr;

        private EditTextPreference mRemoteAddr;

        private EditTextPreference mCipher;

        private EditTextPreference mKeySize;

        private EditTextPreference mExtra;

        private OpenvpnProfile profile;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            profile = getIntent().getParcelableExtra(KEY_PROFILE);

            addPreferencesFromResource(R.xml.openvpn_advanced_settings);

            mPort = (EditTextPreference) findPreference(KEY_PORT);
            mProto = (ListPreference) findPreference(KEY_PROTO);
            mDevice = (ListPreference) findPreference(KEY_DEVICE);
            mCompLzo = (CheckBoxPreference) findPreference(KEY_COMP_LZO);
            mRedirectGateway = (CheckBoxPreference) findPreference(KEY_REDIRECT_GATEWAY);
            mSetAddr = (CheckBoxPreference) findPreference(KEY_SET_ADDR);
            mLocalAddr = (EditTextPreference) findPreference(KEY_LOCAL_ADDR);
            mRemoteAddr = (EditTextPreference) findPreference(KEY_REMOTE_ADDR);
            mCipher = (EditTextPreference) findPreference(KEY_CIPHER);
            mKeySize = (EditTextPreference) findPreference(KEY_KEYSIZE);
            mExtra = (EditTextPreference) findPreference(KEY_EXTRA);

            mPort.setSummary(profile.getPort());
            mPort.setText(profile.getPort());
            mPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setPort(name);
                    mPort.setSummary(profile.getPort());

                    return true;
                }
            });

            mProto.setSummary(profile.getProto());
            mProto.setValue(profile.getProto());
            mProto.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setProto(name);
                    mProto.setSummary(profile.getProto());

                    return true;
                }
            });

            mDevice.setSummary(profile.getDevice());
            mDevice.setValue(profile.getDevice());
            mDevice.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setDevice(name);
                    mDevice.setSummary(profile.getDevice());

                    return true;
                }
            });

            mCompLzo.setChecked(profile.getUseCompLzo());
            mCompLzo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    Boolean b = (Boolean) newValue;
                    profile.setUseCompLzo(b);

                    return true;
                }
            });

            mRedirectGateway.setChecked(profile.getRedirectGateway());
            mRedirectGateway
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference pref, Object newValue) {
                            Boolean b = (Boolean) newValue;
                            profile.setRedirectGateway(b);

                            return true;
                        }
                    });

            // This is inverted to cope with the way dependencies work
            mSetAddr.setChecked(!profile.getSupplyAddr());
            mSetAddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    Boolean b = (Boolean) newValue;
                    profile.setSupplyAddr(!b);

                    return true;
                }
            });

            mLocalAddr.setSummary(profile.getLocalAddr());
            mLocalAddr.setText(profile.getLocalAddr());
            mLocalAddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setLocalAddr(name);
                    mLocalAddr.setSummary(profile.getLocalAddr());

                    return true;
                }
            });

            mRemoteAddr.setSummary(profile.getRemoteAddr());
            mRemoteAddr.setText(profile.getRemoteAddr());
            mRemoteAddr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setRemoteAddr(name);
                    mRemoteAddr.setSummary(profile.getRemoteAddr());

                    return true;
                }
            });

            if (profile.getCipher() == null || profile.getCipher().equals(""))
                mCipher.setSummary(R.string.vpn_openvpn_set_cipher_default);
            else
                mCipher.setSummary(profile.getCipher());
            mCipher.setText(profile.getCipher());
            mCipher.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setCipher(name);
                    if (profile.getCipher().equals(""))
                        mCipher.setSummary(R.string.vpn_openvpn_set_cipher_default);
                    else
                        mCipher.setSummary(profile.getCipher());
                    return true;
                }
            });

            if (profile.getKeySize() == null || profile.getKeySize().equals("0")) {
                mKeySize.setSummary(R.string.vpn_openvpn_set_keysize_default);
                mKeySize.setText("");
            } else {
                mKeySize.setSummary(profile.getKeySize());
                mKeySize.setText(profile.getKeySize());
            }
            mKeySize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    if (name.equals(""))
                        name = "0";
                    profile.setKeySize(name);
                    if (profile.getKeySize().equals("0"))
                        mKeySize.setSummary(R.string.vpn_openvpn_set_keysize_default);
                    else
                        mKeySize.setSummary(profile.getKeySize());
                    return true;
                }
            });

            mExtra.setSummary(profile.getExtra());
            mExtra.setText(profile.getExtra());
            mExtra.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    String name = (String) newValue;
                    name.trim();
                    profile.setExtra(name);
                    mExtra.setSummary(profile.getExtra());
                    return true;
                }
            });
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    Intent intent = new Intent(this, VpnEditor.class);
                    intent.putExtra(KEY_PROFILE, (Parcelable) profile);
                    setResult(RESULT_OK, intent);

                    finish();
                    return true;
            }
            return super.onKeyDown(keyCode, event);
        }

    }
}
