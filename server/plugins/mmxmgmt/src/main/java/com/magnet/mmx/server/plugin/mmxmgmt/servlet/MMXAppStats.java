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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 */
@XmlRootElement
@XmlType(propOrder={"appId", "inAppMessagesStats", "pushMessageStats", "deviceStats"})
public class MMXAppStats {
  private String appId;
  private MMXInAppMessageStats inAppMessagesStats;
  private MMXPushMessageStats pushMessageStats;
  private MMXDeviceStats deviceStats;

  public MMXAppStats(String appId, MMXInAppMessageStats inAppMessagesStats, MMXPushMessageStats pushMessageStats, MMXDeviceStats deviceStats) {
    this.appId = appId;
    this.inAppMessagesStats = inAppMessagesStats;
    this.pushMessageStats = pushMessageStats;
    this.deviceStats = deviceStats;
  }

  public String getAppId() {
    return appId;
  }

  public MMXInAppMessageStats getInAppMessagesStats() {
    return inAppMessagesStats;
  }

  public MMXPushMessageStats getPushMessageStats() {
    return pushMessageStats;
  }

  public MMXDeviceStats getDeviceStats() {
    return deviceStats;
  }
}
