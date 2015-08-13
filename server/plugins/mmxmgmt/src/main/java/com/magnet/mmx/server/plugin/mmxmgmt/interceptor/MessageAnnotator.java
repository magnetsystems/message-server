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

import org.xmpp.packet.Message;

public interface MessageAnnotator {

  /**
   * Annotate message to indicate processing that has been done for the message.
   * Implementations are expected to annotate the message by adding/modifying mmxmeta content.
   *
   * @param message not null message object.
   */
  void annotate (Message message);

  /**
   * Check if the input message is annotated by this annotator implementation.
   * @param message Message
   * @return true if it is annotated. false other wise.
   */
  boolean isAnnotated(Message message);

}
