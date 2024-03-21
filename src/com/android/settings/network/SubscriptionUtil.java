/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network;

import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import static com.android.internal.util.CollectionUtils.emptyIfNull;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionUtil {
    private static final String TAG = "SubscriptionUtil";
    private static List<SubscriptionInfo> sAvailableResultsForTesting;
    private static List<SubscriptionInfo> sActiveResultsForTesting;

    @VisibleForTesting
    public static void setAvailableSubscriptionsForTesting(List<SubscriptionInfo> results) {
        sAvailableResultsForTesting = results;
    }

    @VisibleForTesting
    public static void setActiveSubscriptionsForTesting(List<SubscriptionInfo> results) {
        sActiveResultsForTesting = results;
    }

    public static List<SubscriptionInfo> getActiveSubscriptions(SubscriptionManager manager) {
        if (sActiveResultsForTesting != null) {
            return sActiveResultsForTesting;
        }
        final List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList(true);
        if (subscriptions == null) {
            return new ArrayList<>();
        }
        return subscriptions;
    }

    @VisibleForTesting
    static boolean isInactiveInsertedPSim(UiccSlotInfo slotInfo) {
        if (slotInfo == null)  {
            return false;
        }
        return !slotInfo.getIsEuicc() && !slotInfo.getIsActive() &&
                slotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT;
    }

    public static List<SubscriptionInfo> getAvailableSubscriptions(Context context) {
        if (sAvailableResultsForTesting != null) {
            return sAvailableResultsForTesting;
        }
        final SubscriptionManager subMgr = context.getSystemService(SubscriptionManager.class);
        final TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);

        List<SubscriptionInfo> subscriptions =
                new ArrayList<>(emptyIfNull(subMgr.getSelectableSubscriptionInfoList()));

        // Look for inactive but present physical SIMs that are missing from the selectable list.
        final List<UiccSlotInfo> missing = new ArrayList<>();
        UiccSlotInfo[] slotsInfo =  telMgr.getUiccSlotsInfo();
        for (int i = 0; slotsInfo != null && i < slotsInfo.length; i++) {
            final UiccSlotInfo slotInfo = slotsInfo[i];
            if (isInactiveInsertedPSim(slotInfo)) {
                final int index = slotInfo.getLogicalSlotIdx();
                final String cardId = slotInfo.getCardId();

                final boolean found = subscriptions.stream().anyMatch(info ->
                        index == info.getSimSlotIndex() && cardId.equals(info.getCardString()));
                if (!found) {
                    missing.add(slotInfo);
                }
            }
        }
        if (!missing.isEmpty()) {
            for (SubscriptionInfo info : subMgr.getAllSubscriptionInfoList()) {
                for (UiccSlotInfo slotInfo : missing) {
                    if (info.getSimSlotIndex() == slotInfo.getLogicalSlotIdx() &&
                    info.getCardString().equals(slotInfo.getCardId())) {
                        subscriptions.add(info);
                        break;
                    }
                }
            }
        }
<<<<<<< HEAD   (af73dd Automatic translation import)
        return subscriptions;
=======

        // find any physical SIM which is currently inserted within logical slot
        // and which is our target subscription
        if (slotsInfo != null && !physicalSubInfoList.isEmpty()) {
            final SubscriptionInfo subInfo = searchForSubscriptionId(physicalSubInfoList, subId);
            if (subInfo == null) {
                return false;
            }
            // verify if subscription is inserted within slot
            for (UiccSlotInfo slotInfo : slotsInfo) {
                if ((slotInfo != null) && (!slotInfo.getIsEuicc())
                        && (slotInfo.getLogicalSlotIdx() == subInfo.getSimSlotIndex())) {
                    return true;
                }
            }
            return false;
        }

        // When all of the eSIM profiles are opprtunistic and no physical SIM,
        // first opportunistic subscriptions with same group UUID can be primary.
        if (nonOpportunisticSubInfoList.isEmpty()) {
            if (!physicalSubInfoList.isEmpty()) {
                return false;
            }
            if (!activeSlotSubInfoList.isEmpty()) {
                return (activeSlotSubInfoList.get(0).getSubscriptionId() == subId);
            }
            return (inactiveSlotSubInfoList.get(0).getSubscriptionId() == subId);
        }

        // Allow non-opportunistic + active eSIM subscription as primary
        int numberOfActiveNonOpportunisticSubs = 0;
        boolean isTargetNonOpportunistic = false;
        for (SubscriptionInfo subInfo : nonOpportunisticSubInfoList) {
            final boolean isTargetSubInfo = (subInfo.getSubscriptionId() == subId);
            if (subInfo.getSimSlotIndex() != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                if (isTargetSubInfo) {
                    return true;
                }
                numberOfActiveNonOpportunisticSubs++;
            } else {
                isTargetNonOpportunistic |= isTargetSubInfo;
            }
        }
        if (numberOfActiveNonOpportunisticSubs > 0) {
            return false;
        }
        return isTargetNonOpportunistic;
    }

    private static SubscriptionInfo searchForSubscriptionId(List<SubscriptionInfo> subInfoList,
            int subscriptionId) {
        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.getSubscriptionId() == subscriptionId) {
                return subInfo;
            }
        }
        return null;
>>>>>>> CHANGE (402bd7 Optimize code and change network restriction preferences)
    }

    public static String getDisplayName(SubscriptionInfo info) {
        final CharSequence name = info.getDisplayName();
        if (name != null) {
            return name.toString();
        }
        return "";
    }
}
