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

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXid;
import com.magnet.mmx.protocol.MmxHeaders;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;

/**
 * Builder that builds the Message object
 */
public class MessageBuilder {
  private static Logger LOGGER = LoggerFactory.getLogger(MessageBuilder.class);

  private String appId;
  private DeviceEntity deviceEntity;  // @deprecated
  private long utcTime;
  private String domain;
  private String messageId;
  private String userId;              // @deprecated
  private String msgType;
  private String contentType;
  private MmxHeaders mmxMeta = new MmxHeaders();
  private Map<String, String> metadata = new HashMap<String, String>();
  private boolean receipt;
  private static final String EMPTY = "";

  public MessageBuilder setAppId(String appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Set the end-point of the recipient.
   * @param deviceEntity
   * @return
   * @deprecated {@link #setRecipientIds(MMXid[])}
   */
  public MessageBuilder setDeviceEntity(DeviceEntity deviceEntity) {
    this.deviceEntity = deviceEntity;
    return this;
  }

  public MessageBuilder setUtcTime(long utcTime) {
    this.utcTime = utcTime;
    return this;
  }

  public MessageBuilder setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public MessageBuilder setId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  /**
   * Set the user ID of the sender.
   * @param senderId
   * @return
   * @deprecated {@link #setSenderId(MMXid)}
   */
  public MessageBuilder setSenderId(String senderId) {
    setSenderId(new MMXid(senderId, null));
    return this;
  }

  /**
   * Set the user ID of the recipient.
   * @param userId
   * @return
   * @deprecated {@link #setRecipientIds(MMXid[])}
   */
  public MessageBuilder setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * This feature should not be used.
   * @param replyTo The user ID to be used when replying to.
   * @return
   */
  public MessageBuilder setReplyTo(String replyTo) {
    if (replyTo != null) {
      metadata.put(MMXServerConstants.REPLY_TO, (new MMXid(replyTo, null)).toJson());
    }
    return this;
  }

  public MessageBuilder setMetadata(Map<String, String> metadata) {
    if (metadata != null) {
      this.metadata.putAll(metadata);
    }
    return this;
  }

  public MessageBuilder setReceipt(boolean receipt) {
    this.receipt = receipt;
    return this;
  }

  public MessageBuilder setMsgType(String msgType) {
    this.msgType = msgType;
    return this;
  }

  public MessageBuilder setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public MessageBuilder setSenderId(MMXid senderId) {
    this.mmxMeta.setFrom(senderId);
    return this;
  }

  public MessageBuilder setRecipientId(MMXid recipientId) {
    this.mmxMeta.setTo(new MMXid[] { recipientId });
    return this;
  }

  public MessageBuilder setRecipientIds(MMXid[] recipientIds) {
    this.mmxMeta.setTo(recipientIds);
    return this;
  }

  public Message build() {
    Message message = new Message();
    message.setID(messageId);
    Element mmxElement = message.addChildElement(Constants.MMX,
        Constants.MMX_NS_MSG_PAYLOAD);

    // construct the "mmxmeta" stanza with To with the old userID/deviceEntity.
    if (userId != null) {
      mmxMeta.setTo(new MMXid[] { new MMXid(userId, (deviceEntity == null) ?
          null : deviceEntity.getDeviceId()) });
    } else if (deviceEntity != null) {
      mmxMeta.setTo(new MMXid[] { new MMXid(deviceEntity.getOwnerId(),
          deviceEntity.getDeviceId()) });
    }
    if (!mmxMeta.isEmpty()) {
      String mmxMetaJSON = GsonData.getGson().toJson(mmxMeta);
      Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);
      mmxMetaElement.setText(mmxMetaJSON);
    }

    if (!metadata.isEmpty()) {
      String metaJSON = GsonData.getGson().toJson(metadata);
      Element metaElement = mmxElement.addElement(Constants.MMX_META);
      metaElement.setText(metaJSON);
    }

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);
    if (msgType != null) {
      payloadElement.addAttribute(Constants.MMX_ATTR_MTYPE, msgType);
    }
    if (contentType != null) {
      payloadElement.addAttribute(Constants.MMX_ATTR_CTYPE, contentType);
    }
    DateFormat fmt = Utils.buildISO8601DateFormat();
    String formattedDateTime = fmt.format(new Date(utcTime));
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = EMPTY;
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK,
        buildChunkAttributeValue(text));

    message.setType(Message.Type.chat);
    message.setFrom(buildFromJID());
    message.setTo(buildToJID());
    if (receipt) {
      // add the element for requesting read receipt
      message.addChildElement(Constants.XMPP_REQUEST,
          Constants.XMPP_NS_RECEIPTS);
    }
    // https://magneteng.atlassian.net/browse/MOB-2035
    // add a body
    message.setBody(MMXServerConstants.MESSAGE_BODY_DOT);
    return message;
  }

  private JID buildFromJID() {
    MMXid sender = (MMXid) mmxMeta.getHeader(MmxHeaders.FROM, null);
    if (sender == null)
      return null;
    return new JID(JIDUtil.makeNode(sender.getUserId(), appId), domain, null);
  }
  
  private JID buildToJID() {
    MMXid[] tos = (MMXid[]) mmxMeta.getHeader(MmxHeaders.TO, null);
    if (tos == null || tos.length == 0) {
      return null;
    }
    String userId, devId;
    if (tos.length > 1) {
      userId = Constants.MMX_MULTICAST;
      devId = null;
    } else {
      userId = tos[0].getUserId();
      devId = tos[0].getDeviceId();
      if (userId.indexOf('@') >= 0) {
        userId = JID.escapeNode(userId);
      }
    }
    return new JID(JIDUtil.makeNode(userId, appId), domain, devId);
  }

  /**
   * Build the chunk attribute value
   * 
   * @param content
   * @return
   */
  public static String buildChunkAttributeValue(String content) {
    int byteCount = 0;
    try {
      byteCount = content.getBytes("utf-8").length;
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn("Exception in counting bytes", e);
      throw new IllegalArgumentException(e);
    }
    return 0 + "/" + byteCount + "/" + byteCount;
  }
}
