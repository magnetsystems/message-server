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
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.util.TimeUtil;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.Date;

/**
 */
public class TopicMessageBuilder {
  private TopicPostMessageRequest request;
  private AppEntity appEntity;
  private long utcTime;
  private String domain;
  private MessageIdGenerator idGenerator;
  private String topicId;
  private String appId;

  private static final String PUBSUB_NS = "http://jabber.org/protocol/pubsub";
  private static final String PUBSUB_NAME = "pubsub";
  private static final String PUBLISH_NAME = "publish";
  private static final String ATTRIBUTE_NODE = "node";
  private static final String ITEM_NAME = "item";


  public TopicMessageBuilder setRequest(TopicPostMessageRequest request) {
    this.request = request;
    return this;
  }

  public TopicMessageBuilder setAppEntity(AppEntity appEntity) {
    this.appEntity = appEntity;
    return this;
  }

  public TopicMessageBuilder setUtcTime(long utcTime) {
    this.utcTime = utcTime;
    return this;
  }

  public TopicMessageBuilder setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public TopicMessageBuilder setIdGenerator(MessageIdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  public String getTopicId() {
    return topicId;
  }

  public TopicMessageBuilder setTopicId(String topicId) {
    this.topicId = topicId;
    return this;
  }

  public String getAppId() {
    return appId;
  }

  public TopicMessageBuilder setAppId(String appId) {
    this.appId = appId;
    return this;
  }

  /**
   * <iq>
   * <pubsub>
   * <publish>
   * <item>
   * <mmx>
   * <p/>
   * </mmx>
   * </item>
   * </publish>
   * </pubsub>
   * </iq>
   *
   * @return
   */

  public IQ build() {

    if (topicId == null) {
      throw new IllegalArgumentException("topicId needs to be specified");
    }

    IQ message = new IQ();

    String id = idGenerator.generateTopicMessageId(appId, topicId);
    String toAddress = "pubsub." + domain;
    JID from = buildFromJID(appEntity, domain);

    message.setType(IQ.Type.set);
    message.setID(id);
    message.setTo(toAddress);
    message.setFrom(from);

    Element pubsubElement = message.setChildElement(PUBSUB_NAME, PUBSUB_NS);
    Element publishElement = pubsubElement.addElement(PUBLISH_NAME);
    publishElement.addAttribute(ATTRIBUTE_NODE, topicId);
    Element itemElement = publishElement.addElement(ITEM_NAME);
    //we need to have an id attribute for the item element
    itemElement.addAttribute(Constants.XMPP_ATTR_ID, idGenerator.generateItemIdentifier(topicId));
    Element mmxElement = itemElement.addElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);
    payloadElement.addAttribute(Constants.MMX_ATTR_CTYPE, request.getContentType());
    payloadElement.addAttribute(Constants.MMX_ATTR_MTYPE, request.getMessageType());

    String formattedDateTime = TimeUtil.toString(new Date(utcTime));
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = request.getContent();
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));

    return message;
  }


  public static JID buildFromJID(AppEntity appEntity, String domain) {
    String serverUser = appEntity.getServerUserId();
    String appId = appEntity.getAppId();
    JID toJID = new JID(JIDUtil.makeNode(serverUser, appId), domain, null);
    return toJID;
  }
}
