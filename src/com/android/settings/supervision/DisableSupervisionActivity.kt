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

import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SUPERVISION
import android.app.supervision.SupervisionManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.settingslib.supervision.SupervisionLog.TAG
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

/**
 * Activity for disabling device supervision.
 *
 * This activity is only available to the system supervision role holder. It disables device
 * supervision and finishes the activity with `Activity.RESULT_OK`.
 */
class DisableSupervisionActivity : FragmentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate for DisableSupervisionActivity")

        val supervisionApps = supervisionRoleHolders
        if (!isCallingPackageAllowed(supervisionApps)) {
            Log.w(TAG, "Caller does not have the proper permissions. Finishing activity.")
            setResultAndFinish(RESULT_CANCELED)
            return
        }

        val supervisionManager = getSystemService(SupervisionManager::class.java)
        if (supervisionManager == null) {
            Log.e(TAG, "SupervisionManager is null. Finishing activity.")
            setResultAndFinish(RESULT_CANCELED)
            return
        }

        val otherSupervisionApps = supervisionApps.filter { it != callingPackage }
        // If there are no other supervision apps, we can disable supervision.
        if (otherSupervisionApps.isEmpty()) {
            // Delete supervision data if possible (i.e single supervised user).
            if (!deleteSupervisionData()) {
                // Only disable supervision, in case we can't delete data.
                supervisionManager.setSupervisionEnabled(false)
            }
        }

        // Finally, revoke the supervision role for the caller when applicable.
        if (supervisionApps.contains(callingPackage)) {
            lifecycleScope.launch {
                if (revokeSupervisionRole()) {
                    setResultAndFinish(RESULT_OK)
                } else {
                    Log.w(TAG, "Caller cannot revoke supervision role. Finishing activity.")
                    setResultAndFinish(RESULT_CANCELED)
                }
            }
        } else {
            // If the caller does not have the supervision role, simply finish the activity.
            setResultAndFinish(RESULT_OK)
        }
    }

    private fun isCallingPackageAllowed(supervisionApps: List<String>): Boolean {
        if (callingPackage == null) {
            return false
        }
        if (callingPackage == systemSupervisionPackageName ||
            supervisionApps.contains(callingPackage)) {
            return true
        }

        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        return isCallingPackageSupervisionProfileOwner(devicePolicyManager)
    }

    private suspend fun revokeSupervisionRole(): Boolean {
        val packageName = callingPackage
        val userHandle = user
        val roleManager = getSystemService(RoleManager::class.java)

        if (packageName == null || userHandle == null || roleManager == null) {
            Log.w(
                TAG,
                "Calling package, user handle, or role manager are null. Finishing activity.",
            )
            return false
        }

        val executor = ContextCompat.getMainExecutor(this)
        return suspendCoroutine { continuation ->
            roleManager.removeRoleHolderAsUser(
                ROLE_SUPERVISION,
                packageName,
                RoleManager.MANAGE_HOLDERS_FLAG_DONT_KILL_APP,
                userHandle,
                executor,
            ) { isSuccessful ->
                continuation.resumeWith(Result.success(isSuccessful))
            }
        }
    }

    /**
     * Checks if the calling package is the supervision profile owner for the current user.
     *
     * This involves checking if the calling package is the profile owner for the current user and
     * if the profile owner is a supervision package.
     */
    private fun isCallingPackageSupervisionProfileOwner(dpm: DevicePolicyManager?): Boolean {
        val profileOwnerComponent = dpm?.getProfileOwner()
        return profileOwnerComponent != null &&
            profileOwnerComponent.packageName == callingPackage &&
            isSupervisionPackageProfileOwner()
    }

    private fun setResultAndFinish(resultCode: Int) {
        setResult(resultCode)
        finish()
    }
}
