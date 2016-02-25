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
package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;

/**
 * MessageAnnotator implementation that annotates the message to indicate that it has been
 * distributed.
 */
public class MessageDistributedAnnotator implements MessageAnnotator {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDistributedAnnotator.class);

  @Override
  public void annotate(Message message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Annotating a message with id:{}", message.getID());
    }
    Element mmx = message.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
    String currentInternalMetaJSON = internalMeta != null ? internalMeta.getText() : null;

    JsonObject jsonObject = null;

    if (currentInternalMetaJSON != null && !currentInternalMetaJSON.isEmpty()) {
      JsonParser parser = new JsonParser();
      try {
          jsonObject =  parser.parse(currentInternalMetaJSON).getAsJsonObject();
     } catch (JsonSyntaxException e) {
        LOGGER.warn("Failed to parse mmxmeta string:{} as JSON string", currentInternalMetaJSON, e);
        //assume that the mmxmeta was some invalid JSON.
        jsonObject = new JsonObject();
      }
    } else {
      jsonObject = new JsonObject();
    }
    jsonObject.addProperty(MMXServerConstants.DISTRIBUTED_KEY, true);;
    String updatedInternalMetaJSON = jsonObject.toString();
    if (internalMeta == null) {
      internalMeta = mmx.addElement(Constants.MMX_MMXMETA);
    }
    internalMeta.setText(updatedInternalMetaJSON);
  }

  @Override
  public boolean isAnnotated(Message message) {
    boolean rv = false;
    Element mmx = message.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);
    Element internalMeta = mmx.element(Constants.MMX_MMXMETA);
    String currentInternalMetaJSON = internalMeta != null ? internalMeta.getText() : null;
    if (currentInternalMetaJSON != null && !currentInternalMetaJSON.isEmpty()) {
      JsonParser parser = new JsonParser();
      JsonElement jsonElement =  parser.parse(currentInternalMetaJSON).getAsJsonObject();
      JsonObject jsonObject = jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
      if (jsonObject != null) {
        rv = jsonObject.has(MMXServerConstants.DISTRIBUTED_KEY);
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Checking if message ID:{} is annotated: {}", message.getID(), rv);
    }
    return rv;
  }
}
