/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AppRestrictionsFragmentTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private AppRestrictionsFragment mFragment;
    private AppRestrictionsFragment.RestrictionsResultReceiver mReceiver;

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        mFragment = new AppRestrictionsFragment();
        ReflectionHelpers.setField(mFragment, "mPackageManager", mPackageManager);

        mReceiver = mFragment.new RestrictionsResultReceiver("com.example.app", null, false);
    }

    @Test
    public void assertSafeToStartCustomActivity_stripsSensitiveData() {
        Intent intent = new Intent("com.example.action");
        intent.setPackage("com.example.app");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.setData(Uri.parse("content://com.android.settings.files/secret"));
        intent.setClipData(android.content.ClipData.newRawUri("secret",
                Uri.parse("content://com.android.settings.files/secret")));

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = "com.example.app";
        resolveInfo.activityInfo.name = "com.example.app.CustomActivity";

        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(resolveInfo);

        Intent vettedIntent = ReflectionHelpers.callInstanceMethod(mReceiver,
                "assertSafeToStartCustomActivity",
                ReflectionHelpers.ClassParameter.from(Intent.class, intent));

        assertThat(vettedIntent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION).isEqualTo(0);
        assertThat(vettedIntent.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION).isEqualTo(0);
        assertThat(vettedIntent.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .isEqualTo(0);
        assertThat(vettedIntent.getFlags() & Intent.FLAG_GRANT_PREFIX_URI_PERMISSION).isEqualTo(0);
        assertThat(vettedIntent.getData()).isNull();
        assertThat(vettedIntent.getClipData()).isNull();
    }
}
