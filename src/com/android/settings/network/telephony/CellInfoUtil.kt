/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.telephony.AccessNetworkConstants.AccessNetworkType
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.ServiceState
import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import com.android.internal.telephony.OperatorInfo

/**
 * Add static Utility functions to get information from the CellInfo object.
 * TODO: Modify [CellInfo] for simplify those functions
 */
object CellInfoUtil {

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * By the following order,
     * 1. Long Name if not null/empty
     * 2. Short Name if not null/empty
     * 3. OperatorNumeric (MCCMNC) string
     */
    @JvmStatic
    fun CellIdentity.getNetworkTitle(): String? {
        operatorAlphaLong?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        operatorAlphaShort?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        val operatorNumeric = getOperatorNumeric() ?: return null
        val bidiFormatter = BidiFormatter.getInstance()
        return bidiFormatter.unicodeWrap(operatorNumeric, TextDirectionHeuristics.LTR)
    }

    /**
     * Convert an [OperatorInfo] from the legacy network query to a [CellInfo].
     *
     * Some RILs append the radio technology to the numeric, e.g. "26006+14".
     */
    @JvmStatic
    fun OperatorInfo.toCellInfo(): CellInfo? {
        val numericWithRat = operatorNumeric ?: return null
        val numeric = numericWithRat.substringBefore('+')
        if (!numeric.matches(Regex("^[0-9]{5,6}$"))) return null
        val mcc = numeric.substring(0, 3)
        val mnc = numeric.substring(3)
        val alphaLong = operatorAlphaLong
        val alphaShort = operatorAlphaShort
        val ran = numericWithRat.substringAfter('+', "").toIntOrNull()
            ?.let { ServiceState.rilRadioTechnologyToAccessNetworkType(it) }
            ?: this.ran
        val cellInfo = when (ran) {
            AccessNetworkType.EUTRAN -> CellInfoLte().apply {
                setCellIdentity(CellIdentityLte(
                    CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE,
                    CellInfo.UNAVAILABLE, intArrayOf(), CellInfo.UNAVAILABLE,
                    mcc, mnc, alphaLong, alphaShort, emptyList(), null))
            }
            AccessNetworkType.UTRAN -> CellInfoWcdma().apply {
                setCellIdentity(CellIdentityWcdma(
                    CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE,
                    CellInfo.UNAVAILABLE, mcc, mnc, alphaLong, alphaShort, emptyList(), null))
            }
            else -> CellInfoGsm().apply {
                setCellIdentity(CellIdentityGsm(
                    CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE,
                    CellInfo.UNAVAILABLE, mcc, mnc, alphaLong, alphaShort, emptyList()))
            }
        }
        cellInfo.isRegistered = state == OperatorInfo.State.CURRENT
        return cellInfo
    }

    /**
     * Convert a list of cellInfos to readable string without sensitive info.
     */
    @JvmStatic
    fun cellInfoListToString(cellInfos: List<CellInfo>): String =
        cellInfos.joinToString(System.lineSeparator()) { cellInfo -> cellInfo.readableString() }

    /**
     * Convert [CellInfo] to a readable string without sensitive info.
     */
    private fun CellInfo.readableString(): String = buildString {
        append("{CellType = ${this@readableString::class.simpleName}, ")
        append("isRegistered = $isRegistered, ")
        append(cellIdentity.readableString())
        append("}")
    }

    private fun CellIdentity.readableString(): String = buildString {
        append("mcc = $mccString, ")
        append("mnc = $mncString, ")
        append("alphaL = $operatorAlphaLong, ")
        append("alphaS = $operatorAlphaShort")
    }

    /**
     * Returns the MccMnc.
     */
    @JvmStatic
    fun CellIdentity.getOperatorNumeric(): String? {
        val mcc = mccString
        val mnc = mncString
        return if (mcc == null || mnc == null) null else mcc + mnc
    }
}
