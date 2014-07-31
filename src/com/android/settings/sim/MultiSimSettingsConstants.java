/**
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.android.settings.sim;

public class MultiSimSettingsConstants {
    /**
     * Event for sim name has been changed.
     */
    public static final int EVENT_MULTI_SIM_NAME_CHANGED = 1;

    /**
     * Event for preferred subscription change
     */
    public static final int EVENT_PREFERRED_SUBSCRIPTION_CHANGED = 2;

    /**
     * Type that refers to sounds that are used for the phone ringer
     *
     */
    public static final int TYPE_RINGTONE_2 = 8;

    /**
     * Indicates target package
     */
    public static final String TARGET_PACKAGE = "PACKAGE";

    /**
     * Indicates target class
     */
    public static final String TARGET_CLASS = "TARGET_CLASS";

    /**
     * Indicates multi sim network related setting package
     */
    public static final String NETWORK_PACKAGE = "com.android.phone";

    /**
     * Indicates multi sim network related setting class
     */
    public static final String NETWORK_CLASS = "com.android.phone.MSimMobileNetworkSubSettings";

    /**
     * Indicates multi sim call related setting package
     */
    public static final String CALL_PACKAGE = "com.android.phone";

    /**
     * Indicates multi sim call related setting class
     */
    public static final String CALL_CLASS = "com.android.phone.MSimCallFeaturesSubSetting";

    /**
     * Indicates multi sim config related settings package
     */
    public static final String CONFIG_PACKAGE = "com.android.multisimsettings";

    /**
     * Indicates multi sim config related settings class
     */
    public static final String CONFIG_CLASS =
            "com.android.multisimsettings.MultiSimConfiguration";

    /**
     * Indicates multi sim sound related setting package
     */
    public static final String SOUND_PACKAGE = "com.android.multisimsettings";

    /**
     * Indicates multi sim sound related setting class
     */
    public static final String SOUND_CLASS =
            "com.android.multisimsettings.MultiSimSoundSettings";

    /**
     * Indicates multi sim name changed
     */
    public static final String SUBNAME_CHANGED = "com.android.multisimsettings.SUBNAME_CHANGED";

    /**
     * Preferred subscription icon index. 0 = @drawable/ic_sim_icon_1
     *                                    1 = @drawable/ic_sim_icon_2
     *                                    2 = @drawable/ic_sim_icon_c
     *                                    3 = @drawable/ic_sim_icon_g
     *                                    4 = @drawable/ic_sim_icon_w
     * The default value is "0,1". And 0 for SUB1, 1 for SUB2.
     */
    public static final String PREFERRED_SIM_ICON_INDEX = "preferred_sim_icon_index";

    /**
      * Channel name for subcription one and two i.e. channele name 1,
      * channel name 2
      *
      */
    public static final String MULTI_SIM_NAME = "perferred_name_sub";
}
