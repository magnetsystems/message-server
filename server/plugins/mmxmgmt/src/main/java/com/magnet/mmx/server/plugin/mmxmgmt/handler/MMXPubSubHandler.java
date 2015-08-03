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

import java.util.List;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.protocol.TopicInfo;
import com.magnet.mmx.protocol.SendLastPublishedItems;
import com.magnet.mmx.protocol.TagSearch;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXTopicManager.StatusCode;
import com.magnet.mmx.server.plugin.mmxmgmt.util.IQUtils;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.util.GsonData;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.jivesoftware.openfire.pubsub.PublishedItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

/**
 * Handler for pubsub send_last_published_item.  Current implementation requires
 * the client to keep track of last delivery time.
 * <p>
 * The correct implementation should have a column "lastDelivery" (datetime) in
 * ofPubsubSubscription table. If "lastDelivery" is less than the "creationDate"
 * of a published item, it will be sent.  It will be updated when calling
 * {@link #sendLastPublishedItem(PublishedItem, NodeSubscription, JID)}.
 */
public class MMXPubSubHandler extends IQHandler {
  private static Logger Log = LoggerFactory.getLogger(MMXPubSubHandler.class);

  /**
   * Constructor that takes the module name
   * @param moduleName
   */
  public MMXPubSubHandler(String moduleName) {
    super(moduleName);
  }

  @Override
  public IQ handleIQ(IQ iq) throws UnauthorizedException {
    JID from = iq.getFrom();
    String appId = JIDUtil.getAppId(from);
    Element element = iq.getChildElement();
    String payload = element.getText();
    String commandId = element.attributeValue(Constants.MMX_ATTR_COMMAND);

    if (commandId == null || commandId.isEmpty() || commandId.trim().isEmpty()) {
      return IQUtils.createErrorIQ(iq,
          StatusCode.INVALID_COMMAND.getMessage()+commandId,
          StatusCode.INVALID_COMMAND.getCode());
    }

    try {
      MMXStatus status;
      Constants.PubSubCommand command = Constants.PubSubCommand.valueOf(commandId);
      if (Log.isDebugEnabled()) {
        Log.debug("Processing command:" + command.toString());
      }
      MMXTopicManager topicMgr = MMXTopicManager.getInstance();
      switch (command) {
      case getlatest:
        status = topicMgr.processSendLastPublishedItems(from, appId,
            SendLastPublishedItems.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case listtopics:
        TopicAction.ListResponse lstresp = topicMgr.listTopics(from, appId,
            TopicAction.ListRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(lstresp));
      case createtopic:
        status = topicMgr.createTopic(from, appId, 
            TopicAction.CreateRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case deletetopic:
        status = topicMgr.deleteTopic(from, appId,
            TopicAction.DeleteRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case getTopic:
        TopicInfo info = topicMgr.getTopic(from, appId,
            MMXTopicId.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(info));
//      case publish:
//        TopicOps.PublishResponse pubresp = publishToTopic(from, appId,
//            TopicOps.PublishRequest.fromJson(payload)));
//        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(pubresp));
      case retract:
        status = topicMgr.retractFromTopic(from, appId, 
            TopicAction.RetractRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case retractall:
        status = topicMgr.retractAllFromTopic(from, appId, 
            TopicAction.RetractAllRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case subscribe:
        TopicAction.SubscribeResponse resp = topicMgr.subscribeTopic(from, appId,
            TopicAction.SubscribeRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(resp));
      case unsubscribe:
        status = topicMgr.unsubscribeTopic(from, appId,
            TopicAction.UnsubscribeRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case unsubscribeForDev:
        status = topicMgr.unsubscribeForDev(from, appId, 
            TopicAction.UnsubscribeForDevRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case getSummary:
        TopicAction.SummaryResponse sumresp = topicMgr.getSummary(from, appId,
            TopicAction.SummaryRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(sumresp));
      case getTags:
        TopicAction.TopicTags tagsresp = topicMgr.getTags(from, appId,
            MMXTopicId.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(tagsresp));
      case setTags:
        status = topicMgr.setTags(from, appId,
            TopicAction.TopicTags.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case addTags:
        status = topicMgr.addTags(from, appId,
            TopicAction.TopicTags.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case removeTags:
        status = topicMgr.removeTags(from, appId,
            TopicAction.TopicTags.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(status));
      case queryTopic:
        TopicAction.TopicQueryResponse qryresp = topicMgr.queryTopic(from, 
            appId, TopicAction.TopicQueryRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(qryresp));
      case searchTopic:
        TopicAction.TopicQueryResponse srchresp = topicMgr.searchTopic(from, 
            appId, TopicAction.TopicSearchRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(srchresp));
      case fetch:
        TopicAction.FetchResponse fetchresp = topicMgr.fetchItems(from, appId, 
            TopicAction.FetchRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, fetchresp.toJson());
      case searchByTags:
        List<TopicInfo> topiclist = topicMgr.searchByTags(from, 
            appId, TagSearch.fromJson(payload));
        return IQUtils.createResultIQ(iq, GsonData.getGson().toJson(topiclist));
      case getItems:
        TopicAction.FetchResponse getresp = topicMgr.getItems(from, appId, 
            TopicAction.ItemsByIdsRequest.fromJson(payload));
        return IQUtils.createResultIQ(iq, getresp.toJson());
      }
    } catch (IllegalArgumentException e) {
      Log.info("Invalid pubsub command string:" + commandId, e);
      return IQUtils.createErrorIQ(iq, e.getMessage(), StatusCode.BAD_REQUEST.getCode());
    } catch (MMXException e) {
      return IQUtils.createErrorIQ(iq, e.getMessage(), e.getCode());
    }
    return IQUtils.createErrorIQ(iq,
        StatusCode.INVALID_COMMAND.getMessage() + commandId,
        StatusCode.INVALID_COMMAND.getCode());
  }

  @Override
  public IQHandlerInfo getInfo() {
    return new IQHandlerInfo(Constants.MMX, Constants.MMX_NS_PUBSUB);
  }
}
