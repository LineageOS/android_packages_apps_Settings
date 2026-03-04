/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.supervision

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.app.supervision.SupervisionManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.UserInfo
import android.os.UserManager
import android.os.UserManager.USER_TYPE_FULL_SECONDARY
import android.os.UserManager.USER_TYPE_FULL_SYSTEM
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowRoleManager

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class DisableSupervisionActivityTest {
    private val mockSupervisionManager = mock<SupervisionManager>()
    private val mockUserManager = mock<UserManager>()
    private val mockRoleManager = mock<RoleManager>()
    private val mockDevicePolicyManager = mock<DevicePolicyManager>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val currentUser = context.user
    private lateinit var mActivity: DisableSupervisionActivity
    private lateinit var mActivityController: ActivityController<DisableSupervisionActivity>

    private lateinit var shadowActivity: ShadowActivity

    @Before
    fun setUp() {
        ShadowRoleManager.reset()

        // Note, we have to use ActivityController (instead of ActivityScenario) in order to access
        // the activity before it is created, so we can set up various mocked responses before they
        // are referenced in onCreate.
        mActivityController = Robolectric.buildActivity(DisableSupervisionActivity::class.java)
        mActivity = mActivityController.get()

        shadowActivity = shadowOf(mActivity)
        shadowActivity.setCallingPackage(CALLING_PACKAGE)
        Shadow.extract<ShadowContextImpl>(mActivity.baseContext).apply {
            setSystemService(Context.SUPERVISION_SERVICE, mockSupervisionManager)
            setSystemService(Context.USER_SERVICE, mockUserManager)
            setSystemService(Context.DEVICE_POLICY_SERVICE, mockDevicePolicyManager)
            setSystemService(Context.ROLE_SERVICE, mockRoleManager)
        }

        mockUserManager.stub {
            on { users } doReturn listOf(MAIN_USER, SECONDARY_USER, SUPERVISING_PROFILE)
            on { removeUserEvenWhenDisallowed(SUPERVISING_USER_ID) } doReturn true
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn false
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_systemSupervision,
            null,
        )
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent,
            null,
        )
    }

    @Test
    fun onCreate_callerWithoutValidRole_doesNotDisableSupervision() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn listOf("com.other.package")
        }
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf("com.other.package")
        }

        mActivityController.create()

        verifyNoInteractions(mockSupervisionManager) // Supervision is not disabled
        verifyRemoveSupervisionRole(/* times= */ 0) // Role removal is not called
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerNull_doesNotDisableSupervision() {
        shadowActivity.setCallingPackage(null)
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                emptyList()
        }

        mActivityController.create()

        verifyNoInteractions(mockSupervisionManager) // Supervision is not disabled
        verifyRemoveSupervisionRole(/* times= */ 0) // Role removal is not called
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerHasSystemSupervisionRole_disablesSupervisionAndDeletesData() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(CALLING_PACKAGE)
        }

        mActivityController.create()

        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
        verifyRemoveSupervisionRole(/* times= */ 0) // Role removal is not called
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerHasSupervisionRole_removesRoleAndDisablesSupervisionAndDeletesData() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn listOf(CALLING_PACKAGE)
        }

        mActivityController.create()

        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
        verifyRemoveSupervisionRole(/* times= */ 1).firstValue.accept(true) // Role removal succeeds
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_multipleSupervisionApps_keepsSupervisionAndRevokesRole() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn
                listOf(CALLING_PACKAGE, "com.other.package")
        }

        mActivityController.create()

        verify(mockSupervisionManager, never())
            .setSupervisionEnabled(any()) // Supervision is not disabled
        verifyRemoveSupervisionRole(/* times= */ 1).firstValue.accept(true) // Role removal succeeds
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_multipleSupervisedUsers_disablesSupervisionAndKeepsData() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn listOf(CALLING_PACKAGE)
        }
        mockSupervisionManager.stub {
            on { isSupervisionEnabledForUser(MAIN_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SECONDARY_USER_ID) } doReturn true
            on { isSupervisionEnabledForUser(SUPERVISING_USER_ID) } doReturn false
        }

        mActivityController.create()

        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
        verify(mockUserManager, never()).removeUserEvenWhenDisallowed(any<Int>())
        verifyRemoveSupervisionRole(/* times= */ 1).firstValue.accept(true) // Role removal succeeds
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerIsDefaultProfileOwner_disablesSupervisionAndDeletesData() {
        // Mock RoleManager to indicate caller does NOT have the role
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn emptyList()
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn emptyList()
        }
        // Mock DevicePolicyManager to indicate caller is the profile owner
        mockDevicePolicyManager.stub {
            on { getProfileOwner() } doReturn CALLER_PROFILE_OWNER_COMPONENT.toComponentName()
        }
        // The caller also holds the default profile owner component.
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent,
            CALLER_PROFILE_OWNER_COMPONENT,
        )

        mActivityController.create()

        // Verify supervision is disabled and data deleted
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        // Verify the supervised user is removed
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
        // Verify role removal is NOT called (caller didn't have the role)
        verifyRemoveSupervisionRole(/* times= */ 0)
        // Verify activity finishes successfully
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerIsSystemSupervisionProfileOwner_disablesSupervisionAndDeletesData() {
        // Mock RoleManager to indicate caller does NOT have the role
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn emptyList()
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn emptyList()
        }
        // Mock DevicePolicyManager to indicate caller is the profile owner
        mockDevicePolicyManager.stub {
            on { getProfileOwner() } doReturn CALLER_PROFILE_OWNER_COMPONENT.toComponentName()
        }
        // The caller is also the system supervision app.
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_systemSupervision,
            CALLING_PACKAGE,
        )

        mActivityController.create()

        // Verify supervision is disabled and data deleted
        verify(mockSupervisionManager).setSupervisionEnabled(false)
        verify(mockSupervisionManager).setSupervisionRecoveryInfo(null)
        // Verify the supervised user is removed
        verify(mockUserManager).removeUserEvenWhenDisallowed(eq(SUPERVISING_USER_ID))
        // Verify role removal is NOT called (caller didn't have the role)
        verifyRemoveSupervisionRole(/* times= */ 0)
        // Verify activity finishes successfully
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_otherProfileOwner_doesNotDisableSupervision() {
        // Mock RoleManager to indicate caller does NOT have the role
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn emptyList()
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn emptyList()
        }
        // There's a profile owner, but it's not the caller
        mockDevicePolicyManager.stub {
            on { getProfileOwner() } doReturn OTHER_APP_PROFILE_OWNER_COMPONENT.toComponentName()
        }
        // The caller would hold the default profile owner component, if it was active.
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent,
            CALLER_PROFILE_OWNER_COMPONENT,
        )
        mActivityController.create()

        // Verify supervision is not disabled
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
        verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
        verify(mockUserManager, never()).removeUserEvenWhenDisallowed(any<Int>())
        // Verify role removal is NOT called (caller didn't have the role)
        verifyRemoveSupervisionRole(/* times= */ 0)
        // Verify activity finishes successfully
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_callerIsProfileOwnerButNotTheDefault_doesNotDisableSupervision() {
        // Mock RoleManager to indicate caller does NOT have the role
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn emptyList()
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn emptyList()
        }
        // Mock DevicePolicyManager to indicate caller is the profile owner
        mockDevicePolicyManager.stub {
            on { getProfileOwner() } doReturn CALLER_PROFILE_OWNER_COMPONENT.toComponentName()
        }
        // But the caller is not the default profile owner component.
        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent,
            OTHER_APP_PROFILE_OWNER_COMPONENT,
        )

        mActivityController.create()

        // Verify supervision is not disabled
        verify(mockSupervisionManager, never()).setSupervisionEnabled(any())
        verify(mockSupervisionManager, never()).setSupervisionRecoveryInfo(any())
        verify(mockUserManager, never()).removeUserEvenWhenDisallowed(any<Int>())
        // Verify role removal is NOT called (caller didn't have the role)
        verifyRemoveSupervisionRole(/* times= */ 0)
        // Verify activity finishes successfully
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_roleRemovalFails_cancelsActivity() {
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SUPERVISION) } doReturn listOf(CALLING_PACKAGE)
        }

        mActivityController.create()

        verifyRemoveSupervisionRole(/* times= */ 1).firstValue.accept(false) // Role removal fails
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(mActivity.isFinishing).isTrue()
    }

    /**
     * Verifies that `removeRoleHolderAsUser` was called on `mockRoleManager` and returns the
     * argument captor for the callback.
     */
    private fun verifyRemoveSupervisionRole(times: Int): KArgumentCaptor<Consumer<Boolean>> {
        val captor = argumentCaptor<Consumer<Boolean>>()
        verify(mockRoleManager, times(times))
            .removeRoleHolderAsUser(
                eq(RoleManager.ROLE_SUPERVISION),
                eq(CALLING_PACKAGE),
                any(),
                eq(currentUser),
                any(),
                captor.capture(),
            )

        return captor
    }

    fun String.toComponentName(): ComponentName {
        return this.let(ComponentName::unflattenFromString)!!
    }

    companion object {
        private const val CALLING_PACKAGE = "com.fake.caller"
        private const val CALLER_PROFILE_OWNER_COMPONENT =
            "com.fake.caller/.fake.account.receiver.ProfileOwnerReceiver"
        private const val OTHER_APP_PROFILE_OWNER_COMPONENT =
            "com.other.app/.fake.account.receiver.ProfileOwnerReceiver"
        private const val MAIN_USER_ID = 0
        private const val SECONDARY_USER_ID = 1
        private const val SUPERVISING_USER_ID = 10
        private val MAIN_USER = UserInfo(MAIN_USER_ID, "Main", null, 0, USER_TYPE_FULL_SYSTEM)
        private val SECONDARY_USER =
            UserInfo(SECONDARY_USER_ID, "Secondary", null, 0, USER_TYPE_FULL_SECONDARY)
        private val SUPERVISING_PROFILE =
            UserInfo(SUPERVISING_USER_ID, "Supervising", null, 0, USER_TYPE_PROFILE_SUPERVISING)
    }
}
