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
import com.magnet.mmx.protocol.MessageNotification;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXPushManager;
import com.magnet.mmx.util.GsonData;

import java.util.HashMap;

/**
 * Builder that helps with building the Push Payload for Android devices.  It
 * can be used by the console and IQHandler.
 */
public class MMXPushGCMPayloadBuilder {
  private final HashMap<String, ? super Object> mmxDictionary;
  private final GCMPayload payload;
  private final MMXPushHeader header;

  public MMXPushGCMPayloadBuilder(PushMessage.Action action) {
    payload = new GCMPayload();
    mmxDictionary = new HashMap<String, Object>(10);
    this.header = new MMXPushHeader(Constants.MMX, action.getCode());

  }
  public MMXPushGCMPayloadBuilder(PushMessage.Action action, String type) {
    payload = new GCMPayload();
    mmxDictionary = new HashMap<String, Object>(10);
    header = new MMXPushHeader(Constants.MMX, action.getCode(), type);
  }

  public MMXPushGCMPayloadBuilder(MMXPushHeader header) {
    payload = new GCMPayload();
    mmxDictionary = new HashMap<String, Object>(10);
    this.header = header;
  }

  public MMXPushGCMPayloadBuilder setGcm(MMXPushManager.GcmPayload gcm) {
    setTitle(gcm.mTitle);
    setBody(gcm.mBody);
    setIcon(gcm.mIcon);
    setSound(gcm.mSound);
    setBadge(gcm.mBadge);
    return this;
  }

  public MMXPushGCMPayloadBuilder setTitle(String title) {
    if (title != null) {
      payload.setTitle(title);
    }
    return this;
  }

  public MMXPushGCMPayloadBuilder setBody(String body) {
    if (body != null) {
      payload.setBody(body);
    }
    return this;
  }

  public MMXPushGCMPayloadBuilder setSound(String sound) {
    if (sound != null) {
      payload.setSound(sound);
    }
    return this;
  }

  public MMXPushGCMPayloadBuilder setIcon(String icon) {
    if (icon != null) {
      payload.setIcon(icon);
    }
    return this;
  }

  public MMXPushGCMPayloadBuilder setBadge(Integer badge) {
    if (badge != null) {
      payload.setBadge(badge);
    }
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

  public MMXPushGCMPayloadBuilder setCustomType(String customType) {
    mmxDictionary.put(Constants.PAYLOAD_TYPE_KEY, customType);
    return this;
  }

  public MMXPushGCMPayloadBuilder setCustom(Object customObject) {
    mmxDictionary.put(Constants.PAYLOAD_CUSTOM_KEY, customObject);
    return this;
  }

  /**
   * Build the complete header and JSON payload
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
   * Build the JSON payload without the header.
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
   * Build and return a wakeup payload for retrieve (no custom payload.)
   * @return
   */
  public static String wakeupPayload() {
    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder(
        PushMessage.Action.WAKEUP, Constants.PingPongCommand.retrieve.name());
    builder.setCustomType(null);
    return builder.build();
  }

  /**
   * Build a custom payload for message notification.
   * @param type
   * @param custom
   * @return
   */
  public static String customPayload(PushMessage.Action type,
                                     MessageNotification custom) {
    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder(type,
        MessageNotification.getType());
    builder.setCustom(custom);
    return builder.build();
  }
}
