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

import com.google.gson.Gson;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.GCMPayload;
import com.magnet.mmx.util.GsonData;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder that helps with building the Push Payload for Android devices
 */
public class MMXPushGCMPayloadBuilder {
  private HashMap<String, ? super Object> mmxDictionary;
  private GCMPayload payload;
  private MMXPushHeader header;

  public MMXPushGCMPayloadBuilder() {
    payload = new GCMPayload();
    mmxDictionary = new HashMap<String, Object>(10);
  }


  public MMXPushGCMPayloadBuilder setTitle(String title) {
    if (title == null) {
      return this;
    }
    payload.setTitle(title);
    return this;
  }

  public MMXPushGCMPayloadBuilder setBody(String body) {
    if (body == null) {
      return this;
    }
    payload.setBody(body);
    return this;
  }


  public MMXPushGCMPayloadBuilder setSound(String sound) {
    if (sound == null) {
      return this;
    }
    payload.setSound(sound);
    return this;
  }

  public MMXPushGCMPayloadBuilder setIcon(String icon) {
    if (icon == null) {
      return this;
    }
    payload.setIcon(icon);
    return this;
  }

  public MMXPushGCMPayloadBuilder setType(MMXPushHeader header) {
    mmxDictionary.put(Constants.PAYLOAD_TYPE_KEY, header.toString(false));
    this.header = header;
    return this;
  }


  public MMXPushGCMPayloadBuilder setId(String id) {
    mmxDictionary.put(Constants.PAYLOAD_ID_KEY, id);
    return this;
  }

  public MMXPushGCMPayloadBuilder setCallBackURL(String callBackURL) {
    mmxDictionary.put(Constants.PAYLOAD_CALLBACK_URL_KEY, callBackURL);
    return this;
  }

  public MMXPushGCMPayloadBuilder setCustomDictionary(Map<String, String> dictionary) {
    mmxDictionary.put(Constants.PAYLOAD_CUSTOM_KEY, dictionary);
    return this;
  }

  /**
   * Build the complete payload
   * @return
   */
  public String build() {
    if (this.mmxDictionary.isEmpty()) {
      throw new IllegalStateException("mmx dictionary can't be empty");
    }
    if (header == null) {
      throw new IllegalStateException("invalid message type");
    }
    payload.setMmx(mmxDictionary);
    Gson gson = GsonData.getGson();
    String json = gson.toJson(payload);
    StringBuilder builder = new StringBuilder();
    builder.append(header.toString(true)).append(json);
    return builder.toString();
  }

  /**
   * Build the JSON part of the payload.
   * @return
   */
  public String buildJSON() {
    if (this.mmxDictionary.isEmpty()) {
      throw new IllegalStateException("mmx dictionary can't be empty");
    }
    payload.setMmx(mmxDictionary);
    Gson gson = GsonData.getGson();
    String json = gson.toJson(payload);
    return json;
  }

  /**
   * Convenience method.
   * Build and return a wakeup payload
   * @return
   */
  public static String wakeupPayload() {
    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder();
    builder.setType(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_WAKEUP, Constants.PingPongCommand.retrieve.name()));
    return builder.build();
  }

}
