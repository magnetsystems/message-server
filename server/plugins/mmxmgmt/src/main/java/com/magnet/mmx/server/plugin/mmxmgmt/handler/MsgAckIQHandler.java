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
package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MsgAck;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.IQUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXOfflineStorageUtil;
import com.magnet.mmx.util.GsonData;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;

import java.util.concurrent.TimeUnit;

/**
 * IQHandler that processes the MsgAck Payload.
 */
public class MsgAckIQHandler extends IQHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(MsgAckIQHandler.class);


  public MsgAckIQHandler(String moduleName) {
    super(moduleName);
  }

  @Override
  public IQ handleIQ(IQ packet) throws UnauthorizedException {
    long start = System.nanoTime();
    try {
      Element element = packet.getChildElement();
      String payload = element.getText();
      MsgAck acknowledgement = GsonData.getGson().fromJson(payload, MsgAck.class);
      String from = acknowledgement.getFrom();
      String to = acknowledgement.getTo();
      String messageId = acknowledgement.getMsgId();

      MMXOfflineStorageUtil.removeMessage(to, messageId);

      String appId = JIDUtil.getAppId(to);
      String deviceId = JIDUtil.getResource(to);
      MessageDAO messageDAO = getMessageDAO();
      int count = messageDAO.messageDelivered(appId, deviceId, messageId);
      if (count == 0) {
        LOGGER.warn(String.format("No message updated for appId:%s deviceId:%s messageId:%s", appId, deviceId, messageId));
      }
    } catch (Throwable t) {
      LOGGER.warn("Throwable in handleIQ", t);
    }
    /**
     * based on discussion with login2 we are returning 200 even if an exception is encountered when processing
     * these ack. The reason is the client can't deal with an error response.
     */
    MMXStatus response = new MMXStatus();
    response.setCode(MsgAckOperationStatusCode.DELIVERY_ACK_PROCESSED.getCode());
    IQ result = IQUtils.createResultIQ(packet, response.toJson());
    long end = System.nanoTime();
    long delta = end - start;
    String template = "Processed msgack in [%d] milliseconds";
    LOGGER.info(String.format(template, TimeUnit.MILLISECONDS.convert(delta, TimeUnit.NANOSECONDS)));
    return result;
  }

  protected MessageDAO getMessageDAO() {
    return new MessageDAOImpl(new OpenFireDBConnectionProvider());
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX, Constants.MMX_NS_MSG_ACK);
  }

  /**
   * Enum for the status codes
   */
  public static enum MsgAckOperationStatusCode {
    DELIVERY_ACK_PROCESSED (200, "Delivery ack processed"),
    ;

    private int code;
    private String message;
    MsgAckOperationStatusCode(int c, String m) {
      code = c;
      message = m;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }
}
