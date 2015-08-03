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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

/**
 * Request describing a push message.
 */
public class PushRequest {

  private String appId;
  private String deviceId;
  private String pushType;
  private String text;
  private MMXPayload payload;

  public PushRequest(MMXPayload payload) {
    this.payload = payload;
  }

  public PushRequest(String appId, String deviceId, MMXPayload payload) {
    this.appId = appId;
    this.deviceId = deviceId;
    this.payload = payload;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getPushType() {
    return pushType;
  }

  public void setPushType(String pushType) {
    this.pushType = pushType;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
