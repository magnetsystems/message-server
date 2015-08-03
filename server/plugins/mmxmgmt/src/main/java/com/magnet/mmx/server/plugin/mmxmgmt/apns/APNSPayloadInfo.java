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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import com.google.gson.stream.JsonReader;
import com.magnet.mmx.protocol.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * APNS Payload info.
 */
public class APNSPayloadInfo {
  private static final Logger LOGGER = LoggerFactory.getLogger(APNSPayloadInfo.class);

  private String messageId;
  private String type;

  private APNSPayloadInfo() {}


  /**
   * Parse payload json string to extract specific properties from the APNS Payload json.
   *
   * @param payloadJSON
   * @return
   */
  public static APNSPayloadInfo parse(String payloadJSON) {
    JsonReader reader = new JsonReader(new StringReader(payloadJSON));
    try {
      APNSPayloadInfo rv = null;
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        if (name.equalsIgnoreCase(Constants.PAYLOAD_MMX_KEY)) {
          rv = readMMXObject(reader);
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      return rv;
    } catch (Throwable t) {
      LOGGER.warn("Exception in parsing payloadJSON:{}", payloadJSON, t);
      return null;
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
      }
    }
  }

  private static APNSPayloadInfo readMMXObject(JsonReader reader) throws IOException {
    APNSPayloadInfo info = new APNSPayloadInfo();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (name.equals(Constants.PAYLOAD_ID_KEY)) {
        info.messageId = reader.nextString();
      } else if (name.equals(Constants.PAYLOAD_TYPE_KEY)) {
        info.type = reader.nextString();
      } else {
        reader.skipValue();
      }
    }
    reader.endObject();
    return info;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getType() {
    return type;
  }


}
