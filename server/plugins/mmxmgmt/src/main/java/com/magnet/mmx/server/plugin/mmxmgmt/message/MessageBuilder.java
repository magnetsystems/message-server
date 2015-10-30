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
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.Utils;

/**
 * Builder that builds the Message object
 */
public class MessageBuilder {
  private static Logger LOGGER = LoggerFactory.getLogger(MessageBuilder.class);

  private AppEntity appEntity;
  private DeviceEntity deviceEntity;
  private long utcTime;
  private String domain;
  private MessageIdGenerator idGenerator;
  private String senderId;
  private String userId;
  private String replyTo;
  private Map<String, String> metadata;
  private boolean receipt;
  private static final String EMPTY = "";

  public MessageBuilder setAppEntity(AppEntity appEntity) {
    this.appEntity = appEntity;
    return this;
  }

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

  public MessageBuilder setIdGenerator(MessageIdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  public MessageBuilder setSenderId(String senderId) {
    this.senderId = senderId;
    return this;
  }

  public MessageBuilder setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public MessageBuilder setReplyTo(String replyTo) {
    this.replyTo = replyTo;
    return this;
  }

  public MessageBuilder setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  public MessageBuilder setReceipt(boolean receipt) {
    this.receipt = receipt;
    return this;
  }

  public Message build() {
    Message message = new Message();
    String id = idGenerator.generate(userId, appEntity.getAppId(),
        deviceEntity != null ? deviceEntity.getDeviceId() : null);
    message.setID(id);
    Element mmxElement = message.addChildElement(Constants.MMX,
        Constants.MMX_NS_MSG_PAYLOAD);

    // construct the <mmxmeta> stanza with To, From, mmxdistributed(?)
    MmxHeaders mmxMeta = new MmxHeaders();
    if (senderId != null) {
      mmxMeta.put(MmxHeaders.FROM, new MMXid(senderId, null));
    }
    if (userId != null) {
      mmxMeta.put(MmxHeaders.TO, new MMXid[] {new MMXid(userId, null)});
    }
    // mmxMeta.put("mmxdistributed", Boolean.TRUE);
    String mmxMetaJSON = GsonData.getGson().toJson(mmxMeta);
    Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);
    mmxMetaElement.setText(mmxMetaJSON);

    if (replyTo != null) {
      if (metadata == null) {
        metadata = new HashMap<String, String>();
      }
      metadata.put(MMXServerConstants.REPLY_TO, formatReplyTo(replyTo));
    }

    if (metadata == null) {
      metadata = new HashMap<String, String>();
    }

    Map<String, String> meta = metadata;
    String metaJSON = GsonData.getGson().toJson(meta);
    Element metaElement = mmxElement.addElement(Constants.MMX_META);
    metaElement.setText(metaJSON);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);

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

  private JID buildToJID() {
    String appId = appEntity.getAppId();
    // Fix for MOB-839
    if (userId.indexOf('@') >= 0) {
      userId = JID.escapeNode(userId);
    }
    JID toJID = new JID(userId + JIDUtil.APP_ID_DELIMITER + appId, domain,
        deviceEntity != null ? deviceEntity.getDeviceId() : null);
    return toJID;
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

  /**
   * Build from JID using the application's server user. This is for:
   * https://magneteng.atlassian.net/browse/MOB-772. If the app doesn't have a
   * server user we fall back to console user.
   * 
   * @return from JID.
   */
  private JID buildFromJID() {
    String serverUser = appEntity.getServerUserId();
    if (serverUser == null) {
      LOGGER.warn("Server user for app with id:" + appEntity.getAppId()
          + " is null.");
      serverUser = MMXConfiguration.getConfiguration().getString(
          MMXConfigKeys.CONSOLE_MESSAGE_SEND_USER, "console");
    }
    // note: serverUser is stored with the appId suffix included and in escaped
    // format
    JID toJID = new JID(serverUser, domain, null);
    return toJID;
  }

  private String formatReplyTo(String replyTo) {
    String extractedAppId = JIDUtil.getAppId(replyTo);
    if (extractedAppId == null) {
      StringBuilder builder = new StringBuilder();
      // Fix for MOB-839.
      builder.append((replyTo.indexOf('@') >= 0) ? JID.escapeNode(replyTo)
          : replyTo);
      builder.append(JIDUtil.APP_ID_DELIMITER);
      builder.append(appEntity.getAppId());
      builder.append("@");
      builder.append(domain);
      return builder.toString();
    } else {
      if (!extractedAppId.equalsIgnoreCase(appEntity.getAppId())) {
        throw new IllegalArgumentException("Bad reply-to address specified");
      }
      return replyTo;
    }
  }

}
