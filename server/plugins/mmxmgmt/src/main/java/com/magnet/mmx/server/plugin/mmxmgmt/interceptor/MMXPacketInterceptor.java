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

import com.magnet.mmx.server.plugin.mmxmgmt.context.ContextDispatcherFactory;
import com.magnet.mmx.server.plugin.mmxmgmt.context.GeoEventDispatcher;
import com.magnet.mmx.server.plugin.mmxmgmt.util.IQUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXMessageUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

/**
 * MMX Packet interceptor
 * This is responsible for:
 * 1. Distributing a message targeted to a user to all registered devices for the user
 * 2. Recording the message
 * 3. Queueing wake up notifications for messages targeted to offline resources
 * <p/>
 */
public class MMXPacketInterceptor implements PacketInterceptor {
  private static Logger LOGGER = LoggerFactory.getLogger(MMXPacketInterceptor.class);
  private final String ID = RandomStringUtils.randomAlphanumeric(10);
  private MMXMessageHandlingRule messageHandlingRule;

  public MMXPacketInterceptor(MMXMessageHandlingRule messageHandlingRule){
    this.messageHandlingRule = messageHandlingRule;
  }

  @Override
  public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws
      PacketRejectedException {
    LOGGER.trace("interceptPacket : interceptor id={}", ID);

    IQ geo = IQUtils.isValidGeoIQ(packet);
    if (geo != null) {
      // send it to the geoservice component
      try {
        ContextDispatcherFactory.getInstance().getDispatcher(GeoEventDispatcher.class.getName()).dispatchToExternalService(geo);
      } catch (IllegalAccessException e) {
        LOGGER.error("geoevent dispatch error", e);
      } catch (InstantiationException e) {
        LOGGER.error("geoevent dispatch error", e);
      }
      return;
    }

    if(!MMXMessageUtil.isValidDistributableMessage(packet))
      return;

    LOGGER.debug("interceptPacket : message={}", packet.toString());

    Message mmxMessage = (Message) packet;

    messageHandlingRule.handle(new MMXMsgRuleInput(mmxMessage, session, incoming, processed, MMXMessageUtil.isConfirmationMessage(mmxMessage), (mmxMessage.getTo().getResource() == null)));
  }
}
