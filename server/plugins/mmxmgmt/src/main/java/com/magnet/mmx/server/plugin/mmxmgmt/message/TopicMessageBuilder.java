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

import java.util.Date;
import java.util.Map;

import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.bot.MMXMetaBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TimeUtil;

/**
 */
public class TopicMessageBuilder {
  private TopicPostMessageRequest request;
  private String pubUserId;
  private long utcTime;
  private String domain;
  private String itemId;
  private String topicId;
  private String appId;

  static final String PUBSUB_NS = "http://jabber.org/protocol/pubsub";
  static final String PUBSUB_NAME = "pubsub";
  static final String PUBLISH_NAME = "publish";
  static final String ATTRIBUTE_NODE = "node";
  static final String ITEM_NAME = "item";

  public TopicMessageBuilder setRequest(TopicPostMessageRequest request) {
    this.request = request;
    return this;
  }

  public TopicMessageBuilder setPubUserId(String pubUserId) {
    this.pubUserId = pubUserId;
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

  public TopicMessageBuilder setItemId(String itemId) {
    this.itemId = itemId;
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

    // This is the IQ id.
    String id = itemId+'-'+Long.toHexString(utcTime);
    String toAddress = "pubsub." + domain;
    JID from = new JID(JIDUtil.makeNode(pubUserId, appId), domain, null);

    message.setType(IQ.Type.set);
    message.setID(id);
    message.setTo(toAddress);
    message.setFrom(from);

    Element pubsubElement = message.setChildElement(PUBSUB_NAME, PUBSUB_NS);
    Element publishElement = pubsubElement.addElement(PUBLISH_NAME);
    publishElement.addAttribute(ATTRIBUTE_NODE, topicId);
    Element itemElement = publishElement.addElement(ITEM_NAME);
    //we need to have an id attribute for the item element
    itemElement.addAttribute(Constants.XMPP_ATTR_ID, itemId);
    Element mmxElement = itemElement.addElement(Constants.MMX, Constants.MMX_NS_MSG_PAYLOAD);

    Map<String, String> meta = request.getContent();
    String metaJSON = GsonData.getGson().toJson(meta);
    Element metaElement = mmxElement.addElement(Constants.MMX_META);
    metaElement.setText(metaJSON);

    Element mmxMetaElement = mmxElement.addElement(Constants.MMX_MMXMETA);
    String mmxMetaJSON = MMXMetaBuilder.buildFrom(JIDUtil.getUserId(pubUserId), null);
    mmxMetaElement.setText(mmxMetaJSON);

    Element payloadElement = mmxElement.addElement(Constants.MMX_PAYLOAD);
    payloadElement.addAttribute(Constants.MMX_ATTR_CTYPE, request.getContentType());
    payloadElement.addAttribute(Constants.MMX_ATTR_MTYPE, request.getMessageType());

    String formattedDateTime = TimeUtil.toString(new Date(utcTime));
    payloadElement.addAttribute(Constants.MMX_ATTR_STAMP, formattedDateTime);
    String text = ""; //empty
    payloadElement.setText(text);
    payloadElement.addAttribute(Constants.MMX_ATTR_CHUNK, MessageBuilder.buildChunkAttributeValue(text));

    return message;
  }
}
