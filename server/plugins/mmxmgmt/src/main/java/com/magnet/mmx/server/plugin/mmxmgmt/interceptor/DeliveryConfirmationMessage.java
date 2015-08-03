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

import com.magnet.mmx.protocol.Constants;
import org.dom4j.Element;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketExtension;

/**
 * Wrapper class for Message objects that include delivery confirmations
 */
public class DeliveryConfirmationMessage {
  private Message wrapped;
  /**
   * id of the message whose delivery we are confirming
   */
  private String messageId;
  /**
   * resource that is confirming receipt
   */
  private String confirmingDeviceId;
  /**
   * Private no args constructor
   */
  private DeliveryConfirmationMessage() {
  }

  public Message getWrapped() {
    return wrapped;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getConfirmingDeviceId() {
    return confirmingDeviceId;
  }

  /**
   * Build a DeliveryConfirmationMessage using the source Message
   * @param source
   * @return DeliveryConfirmationMessage if the source message represents a DeliveryConfirmation
   * null other wise.
   */
  public static DeliveryConfirmationMessage build (Message source) {
    DeliveryConfirmationMessage dc = null;
    if (source != null) {
      PacketExtension confirmation = source.getExtension(Constants.XMPP_RECEIVED, Constants.XMPP_NS_RECEIPTS);
      if (confirmation != null) {
        //make sure we have the id attribute which identifies the id of the message
        //for which this message represents a delivery confirmation
        Element extensionElement = confirmation.getElement();
        String idVal = extensionElement.attributeValue(Constants.XMPP_ATTR_ID);
        if (idVal != null || !idVal.isEmpty()) {
          dc = new DeliveryConfirmationMessage();
          dc.messageId = idVal;
          dc.wrapped = source;

          JID from = source.getFrom();
          if (from != null && from.getResource() != null) {
            dc.confirmingDeviceId = from.getResource();
          }
        }
      }
    }
    return dc;
  }

  @Override
  public String toString() {
    return "DeliveryConfirmationMessage{" +
            "wrapped=" + wrapped +
            ", messageId='" + messageId + '\'' +
            ", confirmingDeviceId='" + confirmingDeviceId + '\'' +
            '}';
  }
}
