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
package com.magnet.mmx.server.plugin.mmxmgmt.bot;

import org.xmpp.packet.Packet;

/**
 * Interface that defines API for an auto response processor.
 * Code in this class is inspired by:
 * https://community.igniterealtime.org/docs/DOC-1130
 */
public interface AutoResponseProcessor {

  /**
   * Initialize the auto response processor. AutoResponseConnection representing the
   * auto response user is passed in as argument.
   * @param connection not null auto response connection object.
   */
  void initialize(AutoRespondingConnection connection);

  /**
   * Called by {@link AutoRespondingConnection} when a packet is received for the auto response connection user.
   * API provides implementations a chance to execute business logic when a packet is received.
   *
   * @param packet
   *            The XMPP packet received by the connection.
   */
  void processIncoming(Packet packet);

  /**
   * Called by {@link AutoRespondingConnection} whenever a raw text arrives for the connection.
   *
   * @param rawText
   *            The raw text received by the bot.
   */
  void processIncomingRaw(String rawText);

  /**
   * Called by {@link AutoRespondingConnection} whenever the auto response user's (virtual) connection
   * is closed so that the class implementor will have the chance to perform
   * necessary cleanup when terminated.
   */
  void terminate();
}
