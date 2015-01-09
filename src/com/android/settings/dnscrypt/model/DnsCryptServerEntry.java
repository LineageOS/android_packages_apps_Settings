/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.dnscrypt.model;

/**
 * DnsCryptServerEntry
 * <pre>
 *    Model object for representing a dns crypt server entry
 * </pre>
 */
public class DnsCryptServerEntry {

    // Members
    private String mName;
    private String mFullName;
    private String mDescription;
    private String mLocation;
    private String mCoords;
    private String mUrl;
    private String mVersion;
    private String mDnsSecValidation;
    private String mNoLogs;
    private String mNamecoin;
    private String mResolverAddress;
    private String mProviderName;
    private String mProviderPublicKey;
    private String mProviderPublicKeyTextRecord;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String fullName) {
        mFullName = fullName;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getCoords() {
        return mCoords;
    }

    public void setCoords(String coords) {
        mCoords = coords;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getDnsSecValidation() {
        return mDnsSecValidation;
    }

    public void setDnsSecValidation(String dnsSecValidation) {
        mDnsSecValidation = dnsSecValidation;
    }

    public String getNoLogs() {
        return mNoLogs;
    }

    public void setNoLogs(String noLogs) {
        mNoLogs = noLogs;
    }

    public String getNamecoin() {
        return mNamecoin;
    }

    public void setNamecoin(String namecoin) {
        mNamecoin = namecoin;
    }

    public String getResolverAddress() {
        return mResolverAddress;
    }

    public void setResolverAddress(String resolverAddress) {
        mResolverAddress = resolverAddress;
    }

    public String getProviderName() {
        return mProviderName;
    }

    public void setProviderName(String providerName) {
        mProviderName = providerName;
    }

    public String getProviderPublicKey() {
        return mProviderPublicKey;
    }

    public void setProviderPublicKey(String providerPublicKey) {
        mProviderPublicKey = providerPublicKey;
    }

    public String getProviderPublicKeyTextRecord() {
        return mProviderPublicKeyTextRecord;
    }

    public void setProviderPublicKeyTextRecord(String providerPublicKeyTextRecord) {
        mProviderPublicKeyTextRecord = providerPublicKeyTextRecord;
    }

}
