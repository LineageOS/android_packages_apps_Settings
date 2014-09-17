/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.android.settings.profiles;

import android.app.AirplaneModeSettings;
import android.app.AlertDialog;
import android.app.ConnectionSettings;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.Profile;
import android.app.ProfileManager;
import android.app.RingModeSettings;
import android.app.StreamSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.wimax.WimaxHelper;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.profiles.actions.ItemListAdapter;
import com.android.settings.profiles.actions.item.AirplaneModeItem;
import com.android.settings.profiles.actions.item.ConnectionOverrideItem;
import com.android.settings.profiles.actions.item.ExpandedDesktopItem;
import com.android.settings.profiles.actions.item.Header;
import com.android.settings.profiles.actions.item.Item;
import com.android.settings.profiles.actions.item.LockModeItem;
import com.android.settings.profiles.actions.item.ProfileNameItem;
import com.android.settings.profiles.actions.item.RingModeItem;
import com.android.settings.profiles.actions.item.VolumeStreamItem;

import java.util.ArrayList;
import java.util.List;

import static android.app.ConnectionSettings.PROFILE_CONNECTION_2G3G;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_GPS;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_NFC;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_SYNC;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_WIFI;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_WIFIAP;
import static android.app.ConnectionSettings.PROFILE_CONNECTION_WIMAX;
import static com.android.internal.util.cm.QSUtils.deviceSupportsBluetooth;
import static com.android.internal.util.cm.QSUtils.deviceSupportsMobileData;
import static com.android.internal.util.cm.QSUtils.deviceSupportsNfc;


public class SetupActionsFragment extends ListFragment {

    private static final int RINGTONE_REQUEST_CODE = 1000;

    Profile mProfile;
    ItemListAdapter mAdapter;
    ProfileManager mProfileManager;

    boolean mNewProfileMode;

    public static SetupActionsFragment newInstance(Profile profile, boolean newProfile) {
        SetupActionsFragment fragment = new SetupActionsFragment();
        Bundle args = new Bundle();
        args.putParcelable("profile", profile);
        args.putBoolean(ProfileActivity.EXTRA_NEW_PROFILE, newProfile);

        fragment.setArguments(args);
        return fragment;
    }

    public SetupActionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable("profile");
            mNewProfileMode = getArguments().getBoolean(ProfileActivity.EXTRA_NEW_PROFILE, true);
        }
        mProfileManager = (ProfileManager) getActivity().getSystemService(Context.PROFILE_SERVICE);
        List<Item> items = new ArrayList<Item>();
        // general prefs
        items.add(new Header(getString(R.string.profile_name_title)));
        items.add(new ProfileNameItem(mProfile));

        // connection overrides
        items.add(new Header(getString(R.string.profile_connectionoverrides_title)));
        if (deviceSupportsBluetooth()) {
            items.add(new ConnectionOverrideItem(PROFILE_CONNECTION_BLUETOOTH,
                    mProfile.getSettingsForConnection(PROFILE_CONNECTION_BLUETOOTH)));
        }
        items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_GPS));
        items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIFI));
        items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_SYNC));
        if (deviceSupportsMobileData(getActivity())) {
            items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_MOBILEDATA));
            items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIFIAP));

            final TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_2G3G));
            }
        }
        if (WimaxHelper.isWimaxSupported(getActivity())) {
            items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_WIMAX));
        }
        if (deviceSupportsNfc(getActivity())) {
            items.add(generateConnectionOverrideItem(PROFILE_CONNECTION_NFC));
        }

        // add volume streams
        items.add(new Header(getString(R.string.profile_volumeoverrides_title)));
        items.add(generateVolumeStreamItem(AudioManager.STREAM_ALARM));
        items.add(generateVolumeStreamItem(AudioManager.STREAM_MUSIC));
        items.add(generateVolumeStreamItem(AudioManager.STREAM_RING));
        items.add(generateVolumeStreamItem(AudioManager.STREAM_NOTIFICATION));

        // system settings
        items.add(new Header(getString(R.string.profile_system_settings_title)));
        items.add(new RingModeItem(mProfile.getRingMode()));
        items.add(new AirplaneModeItem(mProfile.getAirplaneMode()));
        items.add(new LockModeItem(mProfile));
        items.add(new ExpandedDesktopItem(mProfile));


        mAdapter = new ItemListAdapter(getActivity(), items);
    }

    private ConnectionOverrideItem generateConnectionOverrideItem(int connectionId) {
        ConnectionSettings settings = mProfile.getSettingsForConnection(connectionId);
        if (settings == null) {
            settings = new ConnectionSettings(connectionId);
            mProfile.setConnectionSettings(settings);
        }
        return new ConnectionOverrideItem(connectionId, settings);
    }

    private VolumeStreamItem generateVolumeStreamItem(int stream) {
        StreamSettings settings = mProfile.getSettingsForStream(stream);
        if (settings == null) {
            settings = new StreamSettings(stream);
            mProfile.setStreamSettings(settings);
        }
        return new VolumeStreamItem(stream, settings);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView desc = new TextView(getActivity());
        int descPadding = getResources().getDimensionPixelSize(R.dimen.profile_instruction_padding);
        desc.setPadding(descPadding, descPadding, descPadding, descPadding);
        desc.setText(R.string.profile_setup_actions_description);
        getListView().addHeaderView(desc, null, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(mAdapter);
        getActivity().getActionBar().setTitle(mNewProfileMode
                ? R.string.profile_setup_actions_title
                : R.string.profile_setup_actions_title_config);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        final Item itemAtPosition = (Item) l.getItemAtPosition(position);
        final int viewType = itemAtPosition.getViewType();
        if (viewType == ItemListAdapter.RowType.AIRPLANEMODE_ITEM.ordinal()) {
            requestAirplaneModeDialog(((AirplaneModeItem) (itemAtPosition)).getSettings());
        } else if (viewType == ItemListAdapter.RowType.EXPANDEDDESKTOP_ITEM.ordinal()) {
            requestExpandedDesktopDialog();
        } else if (viewType == ItemListAdapter.RowType.LOCKSCREENMODE_ITEM.ordinal()) {
            requestLockscreenModeDialog();
        } else if (viewType == ItemListAdapter.RowType.RINGMODE_ITEM.ordinal()) {
            requestRingModeDialog(((RingModeItem) (itemAtPosition)).getSettings());
        } else if (viewType == ItemListAdapter.RowType.CONNECTION_ITEM.ordinal()) {
            requestConnectionOverrideDialog(((ConnectionOverrideItem) (itemAtPosition)).getSettings());
        } else if (viewType == ItemListAdapter.RowType.VOLUME_STREAM_ITEM.ordinal()) {
            ((VolumeStreamItem) (itemAtPosition)).requestVolumeDialog(getActivity(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mAdapter.notifyDataSetChanged();
                }
            });
        } else if (viewType == ItemListAdapter.RowType.NAME_ITEM.ordinal()) {
            requestProfileName();
        }
    }

    private void requestLockscreenModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] lockValues = getResources().getStringArray(R.array.profile_action_generic_connection_values);
        final String[] lockEntries = getResources().getStringArray(R.array.profile_lockmode_entries);

        int defaultIndex = 0; // no action
        switch (mProfile.getScreenLockMode()) {
            case Profile.LockMode.DEFAULT:
                defaultIndex = 0;
                break;
            case Profile.LockMode.INSECURE:
                defaultIndex = 1;
                break;
            case Profile.LockMode.DISABLE:
                defaultIndex = 2;
                break;
        }

        builder.setTitle(R.string.profile_lockmode_title);
        builder.setSingleChoiceItems(lockEntries,
                defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        int newMode = Profile.LockMode.DEFAULT;
                        switch (item) {
                            case 0:
                                break;
                            case 1:
                                newMode = Profile.LockMode.INSECURE;
                                break;
                            case 2:
                                newMode = Profile.LockMode.DISABLE;
                                break;
                        }
                        mProfile.setScreenLockMode(newMode);
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void requestExpandedDesktopDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] ConnectionValues = getResources().getStringArray(R.array.profile_expanded_desktop_values);
        final String[] connectionNames = getResources().getStringArray(R.array.profile_expanded_desktop_entries);

        int defaultIndex = 0; // no action
        switch (mProfile.getExpandedDesktopMode()) {
            case Profile.ExpandedDesktopMode.DEFAULT:
                defaultIndex = 0;
                break;
            case Profile.ExpandedDesktopMode.DISABLE:
                defaultIndex = 2;
                break;
            case Profile.ExpandedDesktopMode.ENABLE:
                defaultIndex = 1;
                break;
        }

        builder.setTitle(R.string.power_menu_expanded_desktop);
        builder.setSingleChoiceItems(connectionNames,
                defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        int newMode = Profile.ExpandedDesktopMode.DEFAULT;
                        switch (item) {
                            case 0:
                                break;
                            case 1:
                                newMode = Profile.ExpandedDesktopMode.ENABLE;
                                break;
                            case 2:
                                newMode = Profile.ExpandedDesktopMode.DISABLE;
                                break;
                        }
                        mProfile.setExpandedDesktopMode(newMode);
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void requestAirplaneModeDialog(final AirplaneModeSettings setting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] ConnectionValues = getResources().getStringArray(R.array.profile_action_generic_connection_values);
        final String[] connectionNames = getResources().getStringArray(R.array.profile_action_generic_connection_entries);

        int defaultIndex = 0; // no action
        if (setting.isOverride()) {
            if (setting.getValue() == 1) {
                defaultIndex = 2; // enabled
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(R.string.profile_airplanemode_title);
        builder.setSingleChoiceItems(connectionNames,
                defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // disable override
                                setting.setOverride(false);
                                break;
                            case 1: // enable override, disable
                                setting.setOverride(true);
                                setting.setValue(0);
                                break;
                            case 2: // enable override, enable
                                setting.setOverride(true);
                                setting.setValue(1);
                                break;
                        }
                        mProfile.setAirplaneMode(setting);
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void requestProfileRingMode() {
        // Launch the ringtone picker
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
//        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
//                onRestoreRingtone());

        boolean showDefault = false;
        boolean showSilent = true;
        int ringToneType = RingtoneManager.TYPE_RINGTONE;
        int subscriptionId = 0;
        int dialogStyle = 0;
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, showDefault);
        if (showDefault) {
            if (ringToneType == RingtoneManager.TYPE_RINGTONE) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultRingtoneUriBySubId(subscriptionId));
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(ringToneType));
            }
        }
        if (dialogStyle != 0) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DIALOG_THEME,
                    dialogStyle);
        }

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, showSilent);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringToneType);
//        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());
        Fragment owningFragment = this;
        if (owningFragment != null) {
            owningFragment.startActivityForResult(intent, RINGTONE_REQUEST_CODE);
        } else {
            getActivity().startActivityForResult(intent, RINGTONE_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void requestRingModeDialog(final RingModeSettings setting) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] ConnectionValues = getResources().getStringArray(R.array.ring_mode_values);
        final String[] connectionNames = getResources().getStringArray(R.array.ring_mode_entries);

        int defaultIndex = 0; // normal by default
        if (setting.isOverride()) {
            if (setting.getValue().equals(ConnectionValues[1] /* vibrate */)) {
                defaultIndex = 1; // enabled
            } else if (setting.getValue().equals(ConnectionValues[2] /* mute */)) {
                defaultIndex = 2; // mute
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(R.string.ring_mode_title);
        builder.setSingleChoiceItems(connectionNames,
                defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // disable override
                                setting.setOverride(false);
                                break;
                            case 1: // enable override, disable
                                setting.setOverride(true);
                                setting.setValue(ConnectionValues[1]);
                                break;
                            case 2: // enable override, enable
                                setting.setOverride(true);
                                setting.setValue(ConnectionValues[2]);
                                break;
                        }
                        mProfile.setRingMode(setting);
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void requestConnectionOverrideDialog(final ConnectionSettings setting) {
        if (setting == null) {
            throw new UnsupportedOperationException("connection setting  cannot be null yo");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] ConnectionValues = getResources().getStringArray(R.array.profile_action_generic_connection_values);
        final String[] connectionNames = getResources().getStringArray(R.array.profile_action_generic_connection_entries);

        int defaultIndex = 0; // no action
        if (setting.isOverride()) {
            if (setting.getValue() == 1) {
                defaultIndex = 2; // enabled
            } else {
                defaultIndex = 1; // disabled
            }
        }

        builder.setTitle(ConnectionOverrideItem.getConnectionTitle(setting.getConnectionId()));
        builder.setSingleChoiceItems(connectionNames,
                defaultIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0: // disable override
                                setting.setOverride(false);
                                break;
                            case 1: // enable override, disable
                                setting.setOverride(true);
                                setting.setValue(0);
                                break;
                            case 2: // enable override, enable
                                setting.setOverride(true);
                                setting.setValue(1);
                                break;
                        }
                        mProfile.setConnectionSettings(setting);
                        mAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void requestProfileName() {
        Context context = getActivity();
        if (context != null) {
            final EditText entry = new EditText(context);
            entry.setSingleLine();
            entry.setText(mProfile.getName());

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.rename_dialog_title);
            builder.setMessage(R.string.rename_dialog_message);
            builder.setView(entry, 34, 16, 34, 16);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String value = entry.getText().toString();
                            mProfile.setName(value);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            ((TextView) dialog.findViewById(android.R.id.message)).setTextAppearance(context,
                    android.R.style.TextAppearance_DeviceDefault_Small);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_actions, container, false);

        view.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProfileManager.addProfile(mProfile);
                getActivity().finish();
            }
        });

        return view;
    }
}
