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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.Constants;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketExtension;

/**
 */
public class MMXMessageUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXMessageUtil.class);

  public static boolean isValidDistributableMessage(Packet packet) {
    if(!(packet instanceof Message)) {
      LOGGER.debug("isValidDistributableMessage : false packet is not a XMPP Message stanza");
      return false;
    }

    Message mmxMessage = (Message) packet;

    if (isMulticastMessage(mmxMessage)) {
      LOGGER.debug("isValidDistributableMessage :false packet is a Multicast message");
      return false;
    }
    
    if (isGeoEventMessage(mmxMessage)) {
      LOGGER.debug("isValidDistributableMessage :false packet is a GeoEvent message");
      return false;

    }
    if (isPubSubMessage(mmxMessage)) {
      LOGGER.debug("isValidDistributableMessage :false packet is a PubSub message");
      return false;
    }

    if(Strings.isNullOrEmpty(mmxMessage.getID())){
      LOGGER.debug("isValidDistributableMessage : false bad messageId={}", mmxMessage.getID());
      return false;
    }

    if(mmxMessage.getType() == Message.Type.error) {
      LOGGER.debug("isValidDistributableMessage : false message is an error message={}", mmxMessage.getID());
      return false;
    }

    if(mmxMessage.getTo() == null) {
      LOGGER.trace("isValidDistributableMessage : false toJID=null");
      return false;
    }

    return true;
  }

  private static boolean isMulticastMessage(Message message) {
    // XEP-0033 multi-recipients message.
    final String addresses = "addresses";
    final String namespace = "http://jabber.org/protocol/address";
    return (message.getExtension(addresses, namespace) != null);
  }
  
  private static boolean isGeoEventMessage(Message message) {
    return message.getExtension(Constants.MMX_ELEMENT, Constants.MMX_NS_CONTEXT) != null;
  }


  public static boolean isPubSubMessage(Message message) {
    if (message == null) {
      return false;
    }
    final String namespace = "http://jabber.org/protocol/pubsub#event";
    final String event = "event";
    final String items = "items";

    PacketExtension extension = message.getExtension(event, namespace);
    if (extension != null) {
      Element eventElement = extension.getElement();
      if (eventElement != null) {
        Element itemElement = eventElement.element(items);
        if (itemElement != null) {
          return true;
        }
      }
    } else {
      return false;
    }
    return false;
  }

  public static boolean isConfirmationMessage(Message message) {
    return message.getExtension(Constants.XMPP_RECEIVED, Constants.XMPP_NS_RECEIPTS) != null;
  }
}
