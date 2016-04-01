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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 */
public class MMXOfflineStorageUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXOfflineStorageUtil.class);
  private static Executor service = Executors.newFixedThreadPool(20);

  public static void storeMessage(final Message message) {
    LOGGER.debug("storeMessage : sender={}, receiver={}, messageId={}", new Object[]{message.getTo(), message.getFrom(), message.getID()});
    LOGGER.trace("storeMessage : message={}", message.toString().replaceAll("[\\r\\n]+", ""));

    // Cannot run this asynchronously; otherwise, an offline message may not be
    // deleted upon an ack and the message will be delivered again.
    LOGGER.trace("storeMessage(): from={}, to={}, packetId={}",
        new Object[]{message.getFrom(), message.getTo(), message.getID()});
    XMPPServer.getInstance().getOfflineMessageStore().addMessage(message);
  }

  public static void removeMessage(final String username, final String messageId) {
    service.execute(new Runnable() {
      @Override
      public void run() {
        LOGGER.trace("removeMessage(): username={}, packetId={}",
            username, messageId);
        XMPPServer.getInstance().getOfflineMessageStore().deleteMessage(
            username, messageId);
      }
    });
  }
}
