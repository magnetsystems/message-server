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

import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Message;

/**
 */
public class MMXMsgRuleInput {
  private final Message message;
  private final Session session;
  private final boolean incoming;
  private final boolean processed;
  private final boolean isReceipt;
  private final boolean isBareJID;

  public MMXMsgRuleInput(Message message, Session session, boolean incoming, boolean processed, boolean isReceipt, boolean isBareJID) {
    this.message = message;
    this.session = session;
    this.incoming = incoming;
    this.processed = processed;
    this.isReceipt = isReceipt;
    this.isBareJID = isBareJID;
  }

  public Message getMessage() {
    return message;
  }

  public Session getSession() {
    return session;
  }

  public boolean isIncoming() {
    return incoming;
  }

  public boolean isProcessed() {
    return processed;
  }

  public boolean isReceipt() {
    return isReceipt;
  }

  public boolean isBareJID() {
    return isBareJID;
  }

  @Override
  public String toString() {
    return "MMXMsgRuleInput{" +
            "message=" + (message != null ? message.getID() : "") +
            ", session=" + (session != null ? session.getAddress() : "") +
            ", incoming=" + incoming +
            ", processed=" + processed +
            ", isReceipt=" + isReceipt +
            ", isBareJID=" + isBareJID +
            '}';
  }
}
