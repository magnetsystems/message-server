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
package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

/**
 */
public class PushIdTuple {

  private String deviceId;
  private String pushId;

  public PushIdTuple(String deviceId, String pushId) {
    this.deviceId = deviceId;
    this.pushId = pushId;
  }

  public PushIdTuple() {
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getPushId() {
    return pushId;
  }

  public void setPushId(String pushId) {
    this.pushId = pushId;
  }
}
