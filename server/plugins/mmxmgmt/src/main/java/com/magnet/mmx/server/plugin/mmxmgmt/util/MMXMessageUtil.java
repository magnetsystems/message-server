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

import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;
import org.xmpp.packet.Packet;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.Constants;

/**
 */
public class MMXMessageUtil {
  private static final boolean ENABLE_TRACE = false;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(MMXMessageUtil.class);

  public static boolean isValidDistributableMessage(Packet packet) {
    if(!(packet instanceof Message)) {
      if (ENABLE_TRACE) {
        LOGGER.trace(
          "isValidDistributableMessage(): false; packet is "+packet.getClass().getSimpleName());
      }
      return false;
    }

    Message mmxMessage = (Message) packet;

    if (isMulticastMessage(mmxMessage)) {
      if (ENABLE_TRACE) {
        LOGGER.trace(
          "isValidDistributableMessage(): false; packet is a Multicast message");
      }
      return false;
    }

    if (isGeoEventMessage(mmxMessage)) {
      if (ENABLE_TRACE) {
        LOGGER.trace(
          "isValidDistributableMessage(): false; packet is a GeoEvent message");
      }
      return false;
    }

//    if (isPubSubMessage(mmxMessage)) {
//      if (ENABLE_TRACE) {
//        LOGGER.trace("isValidDistributableMessage(): false; packet is a PubSub message");
//      }
//      return false;
//    }

    if(Strings.isNullOrEmpty(mmxMessage.getID())){
      if (ENABLE_TRACE) {
        LOGGER.trace("isValidDistributableMessage(): false; bad messageId={}",
          mmxMessage.getID());
      }
      return false;
    }

    // For the fire-and-forget
    if(mmxMessage.getType() == Message.Type.headline) {
      if (ENABLE_TRACE) {
        LOGGER.trace(
          "isValidDistributableMessage(): false; packet is a headline message");
      }
      return false;
    }

    if(mmxMessage.getType() == Message.Type.error) {
      if (ENABLE_TRACE) {
        LOGGER.trace(
          "isValidDistributableMessage(): false; message is an error message={}",
          mmxMessage.getID());
      }
      return false;
    }

    if(mmxMessage.getTo() == null) {
      if (ENABLE_TRACE) {
        LOGGER.trace("isValidDistributableMessage(): false; toJID=null");
      }
      return false;
    }

    if (isSignalMessage(mmxMessage)) {
      if (ENABLE_TRACE) {
        LOGGER.trace(
            "isValidDistributableMessage():false; packet is a MMX signal message");
      }
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
    return message.getExtension(Constants.MMX_ELEMENT,
        Constants.MMX_NS_CONTEXT) != null;
  }

  public static boolean isPubSubMessage(Message message) {
    if (message == null) {
      return false;
    }
    return message.getFrom().toString()
        .equals(XMPPServer.getInstance().getPubSubModule().getServiceDomain());
  }

  public static boolean isConfirmationMessage(Message message) {
    return message.getExtension(Constants.XMPP_RECEIVED, Constants.XMPP_NS_RECEIPTS) != null;
  }

  public static boolean isMMXMulticastMessage(Message message) {
    // MMX Multicast Message
    return Constants.MMX_MULTICAST.equalsIgnoreCase(JIDUtil.getUserId(message.getTo()));
  }

  /**
   * Check if the message is a signal message.
   * @param message
   * @return
   */
  public static boolean isSignalMessage(Message message) {
    return message.getChildElement(Constants.MMX, Constants.MMX_NS_MSG_SIGNAL) != null;
  }
}
