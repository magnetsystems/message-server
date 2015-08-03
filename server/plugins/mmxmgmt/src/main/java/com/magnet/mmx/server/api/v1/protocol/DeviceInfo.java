/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.api.v1.protocol;

import java.util.Date;
import java.util.List;

/**
 * JSON representation of device objects.
 */
public class DeviceInfo {
  private String deviceId;

  private String displayName;

  private String osType;

  private String osVersion;

  private int versionMajor;

  private int versionMinor;

  private String pushType;

  private String phoneNumber;

  private String carrierInfo;

  private List<String> tags;

  private Date dateRegistered;

  private String status;

  private String pushStatus;

  public String getDeviceId() {
    return deviceId;
  }

  public DeviceInfo setDeviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  public String getDisplayName() {
    return displayName;
  }

  public DeviceInfo setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  public String getOsType() {
    return osType;
  }

  public DeviceInfo setOsType(String osType) {
    this.osType = osType;
    return this;
  }

  public String getOsVersion() {
    return osVersion;
  }

  public DeviceInfo setOsVersion(String osVersion) {
    this.osVersion = osVersion;
    return this;
  }

  public int getVersionMajor() {
    return versionMajor;
  }

  public DeviceInfo setVersionMajor(int versionMajor) {
    this.versionMajor = versionMajor;
    return this;
  }

  public int getVersionMinor() {
    return versionMinor;
  }

  public DeviceInfo setVersionMinor(int versionMinor) {
    this.versionMinor = versionMinor;
    return this;
  }

  public String getPushType() {
    return pushType;
  }

  public DeviceInfo setPushType(String pushType) {
    this.pushType = pushType;
    return this;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public DeviceInfo setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public String getCarrierInfo() {
    return carrierInfo;
  }

  public DeviceInfo setCarrierInfo(String carrierInfo) {
    this.carrierInfo = carrierInfo;
    return this;
  }

  public List<String> getTags() {
    return tags;
  }

  public DeviceInfo setTags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public Date getDateRegistered() {
    return dateRegistered;
  }

  public DeviceInfo setDateRegistered(Date dateRegistered) {
    this.dateRegistered = dateRegistered;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public DeviceInfo setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getPushStatus() {
    return pushStatus;
  }

  public DeviceInfo setPushStatus(String pushStatus) {
    this.pushStatus = pushStatus;
    return this;
  }
}
