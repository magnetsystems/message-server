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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXPushManager;
import com.notnoop.apns.APNS;
import com.notnoop.apns.PayloadBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder that helps with building the Push Payload for iOS devices
 */
public class MMXPushAPNSPayloadBuilder {
  private PayloadBuilder builder = null;
  private final HashMap<String, ? super Object> mmxDictionary;

  public MMXPushAPNSPayloadBuilder(PushMessage.Action action) {
    builder = APNS.newPayload();
    mmxDictionary = new HashMap<String, Object>(10);
    if (action == PushMessage.Action.WAKEUP) {
      builder.instantDeliveryOrSilentNotification();
    }
  }

  public MMXPushAPNSPayloadBuilder(PushMessage.Action action, String type) {
    builder = APNS.newPayload();
    mmxDictionary = new HashMap<String, Object>(10);
    if (action == PushMessage.Action.WAKEUP) {
      builder.instantDeliveryOrSilentNotification();
    }
    if (type != null) {
      setCustomType(type);
    }
  }

  public MMXPushAPNSPayloadBuilder() {
    builder = APNS.newPayload();
    mmxDictionary = new HashMap<String, Object>(10);
  }

  /**
   * Convert a "aps" payload object into this payload builder.
   * @param aps An APNS POJO.
   * @return
   */
  public MMXPushAPNSPayloadBuilder setAps(MMXPushManager.ApsPayload aps) {
    if (aps.mAlert != null) {
      if (aps.mAlert instanceof String) {
        builder.alertBody((String) aps.mAlert);
      } else if (aps.mAlert instanceof MMXPushManager.ApsAlert) {
        MMXPushManager.ApsAlert alert = (MMXPushManager.ApsAlert) aps.mAlert;
        if (alert.mTitle != null) {
          builder.alertTitle(alert.mTitle);
        }
        if (alert.mBody != null) {
          builder.alertBody(alert.mBody);
        }
      } else if (aps.mAlert instanceof Map) {
        String value;
        Map<String, Object> alert = (Map<String, Object>) aps.mAlert;
        if ((value = (String) alert.get("title")) != null) {
          builder.alertTitle(value);
        }
        if ((value = (String) alert.get("body")) != null) {
          builder.alertBody(value);
        }
      }
    }
    if (aps.mBadge != null) {
      builder.badge(aps.mBadge);
    }
    if (aps.mSound != null) {
      builder.sound(aps.mSound);
    }
    if (aps.mCategory != null) {
      builder.category(aps.mCategory);
    }
    return this;
  }

  public MMXPushAPNSPayloadBuilder setTitle(String title) {
    if (title != null && !title.isEmpty()) {
      builder.alertTitle(title);
    }
    return this;
  }

  public MMXPushAPNSPayloadBuilder setBody(String body) {
    if (body != null && !body.isEmpty()) {
      builder.alertBody(body);
    }
    return this;
  }

  public MMXPushAPNSPayloadBuilder setBadge(Integer badge) {
    if (badge != null && badge.intValue() != 0) {
      if (badge.intValue() < 1) {
        throw new IllegalArgumentException("Negative badge values are not permitted");
      }
      builder.badge(badge.intValue());
    }
    return this;
  }

  public MMXPushAPNSPayloadBuilder setSound(String sound) {
    if (sound != null && !sound.isEmpty()) {
      builder.sound(sound);
    }
    return this;
  }

  public MMXPushAPNSPayloadBuilder silent() {
    builder.instantDeliveryOrSilentNotification();
    return this;
  }

  public MMXPushAPNSPayloadBuilder setCustomType(String type) {
    mmxDictionary.put(Constants.PAYLOAD_TYPE_KEY, type);
    return this;
  }

  public MMXPushAPNSPayloadBuilder setId(String id) {
    mmxDictionary.put(Constants.PAYLOAD_ID_KEY, id);
    return this;
  }

  public MMXPushAPNSPayloadBuilder setCallBackURL(String callBackURL) {
    mmxDictionary.put(Constants.PAYLOAD_CALLBACK_URL_KEY, callBackURL);
    return this;
  }

  public MMXPushAPNSPayloadBuilder setCustomDictionary(Map<String, Object> dictionary) {
    mmxDictionary.put(Constants.PAYLOAD_CUSTOM_KEY, dictionary);
    return this;
  }

  public MMXPushAPNSPayloadBuilder setCategory(String category) {
    if (category != null && !category.isEmpty()) {
      builder.category(category);
    }
    return this;
  }

  public String build() throws PayloadSizeException {
    if (this.mmxDictionary.isEmpty()) {
      throw new IllegalStateException("mmx dictionary can't be empty");
    }
    builder.customField(Constants.PAYLOAD_MMX_KEY, mmxDictionary);
    if (builder.isTooLong()) {
      throw new PayloadSizeException("Payload size exceeds allowed limit");
    }
    String payload = builder.build();
    return payload;
  }

  /**
   * Convenience method.
   * Build and return a wakeup payload
   * @return
   */
  public static String wakeupPayload() {
    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder(
        PushMessage.Action.WAKEUP, Constants.PingPongCommand.retrieve.name());
    builder.silent();
    return builder.build();
  }

}
