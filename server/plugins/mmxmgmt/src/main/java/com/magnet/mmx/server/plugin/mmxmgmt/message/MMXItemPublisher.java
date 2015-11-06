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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.xmpp.packet.JID;

/**
 */

public class MMXItemPublisher {
  String userId;
  String deviceId;

  public MMXItemPublisher(String userId, String deviceId) {
    this.userId = userId;
    this.deviceId = deviceId;
  }

  public MMXItemPublisher(JID publisher) {
    userId = JIDUtil.getUserId(publisher);
    deviceId = publisher.getResource();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXItemPublisher)) return false;

    MMXItemPublisher that = (MMXItemPublisher) o;

    if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null) return false;
    if (!userId.equals(that.userId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = userId.hashCode();
    result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "MMXItemPublisher{" +
            "userId='" + userId + '\'' +
            ", deviceId='" + deviceId + '\'' +
            '}';
  }
}
