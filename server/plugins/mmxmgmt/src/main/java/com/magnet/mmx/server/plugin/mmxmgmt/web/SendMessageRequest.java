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
package com.magnet.mmx.server.plugin.mmxmgmt.web;

import com.magnet.mmx.util.GsonData;

import java.util.HashMap;
import java.util.Map;

/**
 * Object encapsulating SendMessage request.
 */
public class SendMessageRequest {

  private String clientId;
  private String deviceId;
  private String content;
  private String contentType;
  private String type; //normal/chat
  private boolean requestAck;
  private Map<String, String> meta = new HashMap<String, String>();

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isRequestAck() {
    return requestAck;
  }

  public void setRequestAck(boolean requestAck) {
    this.requestAck = requestAck;
  }

  public Map<String, String> getMeta() {
    return meta;
  }

  public void setMeta(Map<String, String> meta) {
    this.meta = meta;
  }

  /**
   * Add meta information.
   * @param key
   * @param value
   */
  public void addMeta (String key, String value) {
    meta.put(key, value);
  }

  /**
   * Build the request object using a JSON string.
   * @param json
   * @return
   */
  public static SendMessageRequest fromJSON (String json) {
    return GsonData.getGson().fromJson(json, SendMessageRequest.class);
  }

  /**
   * Serialize object to JSON.
   * @return
   */
  public String toJSON () {
    String json =  GsonData.getGson().toJson(this);
    return json;
  }
}
