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
package com.android.settings.profiles.triggers;

import android.app.Fragment;
import android.app.PendingIntent;
import android.app.Profile;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.profiles.NFCProfileUtils;
import com.android.settings.profiles.ProfileActivity;


public class NfcTriggerFragment extends Fragment {

    Profile mProfile;

    private NfcAdapter mNfcAdapter;
    private IntentFilter[] mWriteTagFilters;


    public static NfcTriggerFragment newInstance(Profile profile) {
        NfcTriggerFragment fragment = new NfcTriggerFragment();

        Bundle extras = new Bundle();
        extras.putParcelable("profile", profile);

        fragment.setArguments(extras);
        return fragment;
    }

    public NfcTriggerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable("profile");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProfile != null) {
            enableTagWriteMode();

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableTagWriteMode();
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getActivity(getActivity(), 0,
                new Intent(getActivity(), ProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    private void disableTagWriteMode() {
        mNfcAdapter.disableForegroundDispatch(getActivity());
    }

    private void enableTagWriteMode() {
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mWriteTagFilters = new IntentFilter[] {
                tagDetected
        };
        mNfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), mWriteTagFilters, null);
    }

    public void onNfcIntent(Intent intent) {
        if (mProfile == null) {
            Toast.makeText(getActivity(), "no associated profile!!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (NFCProfileUtils.writeTag(NFCProfileUtils.getProfileAsNdef(mProfile), detectedTag)) {
                Toast.makeText(getActivity(), R.string.profile_write_success, Toast.LENGTH_LONG).show();
                NFCProfileUtils.vibrate(getActivity());

            } else {
                Toast.makeText(getActivity(), R.string.profile_write_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.nfc_writer, container, false);



        return root;

    }


}
