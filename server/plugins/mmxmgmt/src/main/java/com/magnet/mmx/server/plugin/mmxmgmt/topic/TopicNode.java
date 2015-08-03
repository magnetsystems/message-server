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
package com.magnet.mmx.server.plugin.mmxmgmt.topic;

import com.magnet.mmx.protocol.MMXTopicId;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.ConfigureForm;
import com.magnet.mmx.util.GsonData;
import com.magnet.mmx.util.TopicHelper;

import org.jivesoftware.openfire.pubsub.LeafNode;
import org.jivesoftware.openfire.pubsub.Node;

import java.util.List;

/**
 */
public class TopicNode {
  String userId;
  String topicName;
  boolean collection;
  Integer subscriptionCount;
  String description;
  String creationDate;
  String modificationDate;
  boolean persistent;
  Integer maxItems;
  Integer maxPayloadSize;
  String publisherType;
  List<String> tags;
  boolean subscriptionEnabled;

  public String getUserId() {
    return userId;
  }
  
  public void setUserId(String userId) {
    this.userId = userId;
  }
  
  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isCollection() {
    return collection;
  }

  public void setCollection(boolean collection) {
    this.collection = collection;
  }


  public Integer getSubscriptionCount() {
    return subscriptionCount;
  }

  public void setSubscriptionCount(Integer subscriptionCount) {
    this.subscriptionCount = subscriptionCount;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String creationDate) {
    this.creationDate = creationDate;
  }

  public String getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(String modificationDate) {
    this.modificationDate = modificationDate;
  }

  public boolean isPersistent() {
    return persistent;
  }

  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  public Integer getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(Integer maxItems) {
    this.maxItems = maxItems;
  }

  public Integer getMaxPayloadSize() {
    return maxPayloadSize;
  }

  public void setMaxPayloadSize(Integer maxPayloadSize) {
    this.maxPayloadSize = maxPayloadSize;
  }

  public String getPublisherType() {
    return publisherType;
  }

  public void setPublisherType(String publisherType) {
    this.publisherType = publisherType;
  }

  public void setSubscriptionEnabled(boolean subscriptionEnabled) {
    this.subscriptionEnabled = subscriptionEnabled;
  }

  public boolean isSubscriptionEnabled() {
    return subscriptionEnabled;
  }

  public String toJSON() {
    return GsonData.getGson().toJson(this);
  }

  public static TopicNode build(String appId, Node node) {
    TopicNode tn = new TopicNode();
    tn.setDescription(node.getDescription());
    tn.setCollection(node.isCollectionNode());
    MMXTopicId tid = TopicHelper.parseNode(node.getNodeID());
    tn.setUserId(tid.getUserId());
    tn.setTopicName(tid.getName());
    tn.setSubscriptionCount(node.getAllSubscriptions().size());
    tn.setCollection(node.isCollectionNode());
    tn.setSubscriptionEnabled(node.isSubscriptionEnabled());
    if (!node.isCollectionNode()) {
      LeafNode leafNode = (LeafNode) node;
      tn.setMaxItems(leafNode.getMaxPublishedItems());
          tn.setMaxPayloadSize(leafNode.getMaxPayloadSize());
          tn.setPersistent(leafNode.isPersistPublishedItems());
          tn.setPublisherType(ConfigureForm.convert(leafNode.getPublisherModel()).name());
    } else {
      tn.setMaxItems(0);
      tn.setMaxPayloadSize(0);
      tn.setPersistent(false);
      tn.setPublisherType(null);
    }
    return tn;
  }

}
