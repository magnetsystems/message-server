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

package com.magnet.mmx.server.plugin.mmxmgmt.message;


import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import org.jivesoftware.openfire.XMPPServer;
import org.xmpp.packet.Message;
import org.xmpp.packet.Message.Type;

import com.magnet.mmx.protocol.Headers;
import com.magnet.mmx.protocol.MMXError;
import com.magnet.mmx.protocol.Payload;
import com.magnet.mmx.util.GsonData;

/**
 * Error Message Builder.  An error message is a reply to an original message
 * that it has the same message ID, the recipient is the sender (not to the
 * reply-to), the sender is from a pseudo "mmx" service.
 */
public class ErrorMessageBuilder {
  private Message mOrigMsg;
  private Headers mHeaders;
  private Payload mPayload;
  
  /**
   * Constructor with the original message.
   * @param origMsg
   */
  public ErrorMessageBuilder(Message origMsg) {
    mOrigMsg = origMsg;
    mHeaders = new Headers();
  }
  
  /**
   * Copy optional headers to the error message.
   * @param headers
   * @return This object.
   */
  public ErrorMessageBuilder setHeaders(Headers headers) {
    mHeaders.putAll(headers);
    return this;
  }
  
  /**
   * Use MMXError as a JSON payload.
   * @param error
   * @return This object.
   */
  public ErrorMessageBuilder setError(MMXError error) {
    error.setMsgId(mOrigMsg.getID());
    mPayload = new Payload(MMXError.getType(), error.toJson());
    mHeaders.setContentType(GsonData.CONTENT_TYPE_JSON);
    return this;
  }
  
  /**
   * Set a custom error payload.  Caller may use any content other than
   * MMXError as a payload.
   * @param payload A custom payload.
   * @return This object.
   * @see com.magnet.mmx.protocol.MMXError
   */
  public ErrorMessageBuilder setPayload(Payload payload) {
    mPayload = payload;
    return this;
  }
  
  /**
   * Build an XMPP error message with MMX extension.
   * @return
   */
  public Message build() {
    Message errorMsg = new Message();
    String appId = JIDUtil.getAppId(mOrigMsg.getFrom());
    errorMsg.setType(Type.error);
    errorMsg.setTo(mOrigMsg.getFrom());
    errorMsg.setFrom(appId + "%" + appId + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain());
    errorMsg.setID(mOrigMsg.getID());
    errorMsg.addExtension(new MMXPacketExtension(mHeaders, mPayload));
    return errorMsg;
  }
}
