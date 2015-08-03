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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

/**
 */
public class MMXDeviceCountResult {
  long devicesPerApp;
  long devicesPerUser;
  String appId;
  String ownerJid;

  public MMXDeviceCountResult(long devicesPerApp, long devicesPerUser, String appId, String ownerJid) {
    this.devicesPerApp = devicesPerApp;
    this.devicesPerUser = devicesPerUser;
    this.appId = appId;
    this.ownerJid = ownerJid;
  }

  public long getDevicesPerApp() {
    return devicesPerApp;
  }

  public long getDevicesPerUser() {
    return devicesPerUser;
  }

  public String getAppId() {
    return appId;
  }

  public String getOwnerJid() {
    return ownerJid;
  }

  @Override
  public String toString() {
    return "MMXDeviceCountResult{" +
            "devicesPerApp=" + devicesPerApp +
            ", devicesPerUser=" + devicesPerUser +
            ", appId='" + appId + '\'' +
            ", ownerJid='" + ownerJid + '\'' +
            '}';
  }
}
