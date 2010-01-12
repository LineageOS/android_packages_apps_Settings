/*
 * Copyright (C) 2010 James Bottomley <James.Bottomley@suse.de>
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

import android.app.Dialog;
import android.content.Context;
import android.net.vpn.OpenvpnProfile;
import android.net.vpn.VpnProfile;

/**
 * A {@link VpnProfileActor} that provides an authentication view for users to
 * input username and password before connecting to the VPN server.
 */
public class OpenvpnAuthenticationActor extends AuthenticationActor {

    OpenvpnAuthenticationActor(Context c, VpnProfile p) {
        super(c, p);
    }

    // @Override
    public boolean isConnectDialogNeeded() {
        OpenvpnProfile p = (OpenvpnProfile) getProfile();
        return p.getUserAuth();
    }

    public void connect(Dialog d) {
        if (d == null)
            // null d means we don't need an authentication dialogue
            // so skip it and pass in null user and password
            connect((String) null, (String) null);
        else
            super.connect(d);
    }
}
