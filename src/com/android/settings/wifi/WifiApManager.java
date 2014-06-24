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

package com.android.settings.wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class WifiApManager {
    private static final String TAG = "WifiApManager";
    private final WifiManager mWifiManager;

    public WifiApManager(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Gets a list of the clients connected to the Hotspot, reachable timeout is 1000
     *
     * @param onlyReachables {@code false} if the list should contain unreachable
    (probably disconnected) clients, {@code true} otherwise
     * @return ArrayList of {@link ClientScanResult}
     */
    public ArrayList<ClientScanResult> getClientList(boolean onlyReachables) {
        getClientList getClientList1 = new getClientList(onlyReachables, 1000);
        try {
            return getClientList1.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    class getClientList extends AsyncTask<String, Integer, ArrayList<ClientScanResult>> {

        BufferedReader br = null;
        ArrayList<ClientScanResult> result = null;
        boolean onlyReachables;
        int reachableTimeout;

        public getClientList(boolean onlyReachablesO, int reachableTimeoutO) {
            onlyReachables = onlyReachablesO;
            reachableTimeout = reachableTimeoutO;
        }

        protected ArrayList<ClientScanResult> doInBackground(String... sUrl) {
            try {
                result = new ArrayList<ClientScanResult>();
                br = new BufferedReader(new FileReader("/proc/net/arp"));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] splitted = line.split(" +");

                    if (splitted.length >= 4) {
                        // Basic sanity check
                        String mac = splitted[3];

                        if (mac.matches("..:..:..:..:..:..")) {
                            boolean isReachable = InetAddress.getByName(splitted[0])
                                    .isReachable(reachableTimeout);

                            if (!onlyReachables || isReachable) {
                                result.add(new ClientScanResult(splitted[0],
                                        splitted[3], splitted[5], isReachable));
                            }
                        }
                    }
                }
            } catch (UnknownHostException e) {
                e.getStackTrace();
                Log.d(TAG, "catch UnknownHostException hit in run", e);
            } catch (FileNotFoundException e) {
                e.getStackTrace();
                Log.d(TAG, "catch FileNotFoundException hit in run", e);
            } catch (IOException e) {
                e.getStackTrace();
                Log.d(TAG, "catch IOException hit in run", e);
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            return result;
        }
    }
}
