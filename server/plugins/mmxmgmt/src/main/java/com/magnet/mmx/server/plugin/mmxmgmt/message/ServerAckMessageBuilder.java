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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.bot.MMXMetaBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.JSONifiable;
import com.magnet.mmx.util.Utils;
import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder that builds the server ack message.
 */
public class ServerAckMessageBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerAckMessageBuilder.class);
  private Message originalMessage;
  private String appId;

  /**
   * Constructor
   * @param originalMessage
   * @param appId
   */
  public ServerAckMessageBuilder(Message originalMessage, String appId) {
    this.originalMessage = originalMessage;
    this.appId = appId;
  }

  /**
   * Build the ServerAckMessage
   * @return Message
   */
  public Message build() {

    JID sender = originalMessage.getFrom();
    String senderUserId = JIDUtil.getUserId(sender);
    String senderDeviceId = sender.getResource();

    JID receiver = originalMessage.getTo();
    String receiverUserId = JIDUtil.getUserId(receiver);
    String receiverDeviceId = receiver.getResource();

    Message ackMessage = new Message();
    ackMessage.setType(Message.Type.chat);
    ackMessage.setFrom(appId + "%" + appId + "@" + XMPPServer.getInstance().getServerInfo().getXMPPDomain());
    ackMessage.setTo(sender);
    ackMessage.setID(new MessageIdGeneratorImpl().generate(sender.toString(), appId, senderDeviceId));
    Element mmxElement = ackMessage.addChildElement(Constants.MMX, Constants.MMX_NS_MSG_SIGNAL);
    Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);
    Map<String, ServerAckMmxMeta> mmxMetaMap = new HashMap<String, ServerAckMmxMeta>();
    ServerAckMmxMeta meta = new ServerAckMmxMeta();
    meta.setAckForMsgId(originalMessage.getID());
    meta.setReceiver(receiverUserId, receiverDeviceId);
    meta.setSender(senderUserId, senderDeviceId);
    mmxMetaMap.put(MMXServerConstants.SERVER_ACK_KEY, meta);

    String mmxMetaJSON = GsonData.getGson().toJson(mmxMetaMap);
    mmxMetaElement.setText(mmxMetaJSON);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);

    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date());
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = ".";
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));
    ackMessage.setBody(MMXServerConstants.MESSAGE_BODY_DOT);
    return ackMessage;
  }

  /**
   * Class for modeling the server ack mmx meta data.
   */
  static class ServerAckMmxMeta extends JSONifiable {
    private String ackForMsgId;
    private MMXMetaBuilder.MetaToEntry sender;
    private MMXMetaBuilder.MetaToEntry receiver;

    public String getAckForMsgId() {
      return ackForMsgId;
    }

    public MMXMetaBuilder.MetaToEntry getReceiver() {
      return receiver;
    }

    public MMXMetaBuilder.MetaToEntry getSender() {
      return sender;
    }

    public void setAckForMsgId(String ackForMsgId) {
      this.ackForMsgId = ackForMsgId;
    }

    public void setSender(String userId, String devId) {
      MMXMetaBuilder.MetaToEntry entry = new MMXMetaBuilder.MetaToEntry();
      entry.setUserId(userId);
      entry.setDevId(devId);
      sender = entry;
    }

    public void setReceiver(String userId, String devId) {
      MMXMetaBuilder.MetaToEntry entry = new MMXMetaBuilder.MetaToEntry();
      entry.setUserId(userId);
      entry.setDevId(devId);
      receiver = entry;
    }
  }
}
