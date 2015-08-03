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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 */
public class TopicEntity {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicEntity.class);
  
  private static final String CL_SERVICE_IDS = "serviceID";
  private static final String CL_SERVICE_ID = "serviceID";
  private static final String CL_NODE_ID = "nodeID";
  private static final String CL_LEAF = "leaf";
  private static final String CL_CREATION_DATE = "creationDate";
  private static final String CL_MODIFICATION_DATE = "modificationDate";
  private static final String CL_PARENT = "parent";
  private static final String CL_DELIVER_PAYLOADS = "deliverPayloads";
  private static final String CL_MAX_PAYLOAD_SIZE = "maxPayloadSize";
  private static final String CL_PERSIST_ITEMS = "persistItems";
  private static final String CL_MAX_ITEMS = "maxItems";
  private static final String CL_NOTIFY_CHANGES = "notifyConfigChanges";
  private static final String CL_NOTIFY_DELETE = "notifyDelete";
  private static final String CL_NOTIFY_RETRACT = "notifyRetract";
  private static final String CL_PRESENCE_BASED = "presenceBased";
  private static final String CL_SEND_ITEM_SUBSSCRIBE = "sendItemSubscribe";
  private static final String CL_PUBLISHER_MODEL = "publisherModel";
  private static final String CL_SUBSCRIPTION_ENABLED = "subscriptionEnabled";
  private static final String CL_CONFIG_SUBSCRIPTION = "configSubscription";
  private static final String CL_ACCESS_MODEL = "accessModel";
  private static final String CL_PAYLOAD_TYPE = "payloadType";
  private static final String CL_BODY_XSLT = "bodyXSLT";
  private static final String CL_DATAFORM_XSLT = "dataformXSLT";
  private static final String CL_CREATOR = "creator";
  private static final String CL_DESCRIPTION = "description";
  private static final String CL_LANGUAGE = "language";
  private static final String CL_NAME = "name";
  private static final String CL_REPLYPOLICY= "replyPolicy";
  private static final String CL_ASSOCIATION_POLICY = "associationPolicy";
  private static final String CL_MAX_LEAF_NODES = "maxLeafNodes";

  String serviceId;
  String nodeId;
  boolean leaf;
  Date creationDate = new Date();
  Date modificationDate = new Date();
  String parent;
  boolean deliverPayloads = true;
  int maxPayloadSize = 2097152;
  boolean persistItems = true;
  int maxItems = 50000;
  boolean notifyConfigChanges = true;
  boolean notifyDelete = true;
  boolean notifyRetract = false;
  boolean presenceBased = false;
  boolean sendItemSubscribe = true;
  String publisherModel = "open";
  boolean subscriptionEnabled = true;
  boolean configSubscription = false;
  String  accessModel = "open";
  String payloadType = "";
  String bodyXSLT = "";
  String dataformXSLT = "";
  String creator ="";
  String description ="";
  String language = "English";
  String name;
  String replyPolicy = "owner";
  String associationPolicy = null;
  int maxLeafNodes = -1;

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public boolean isLeaf() {
    return leaf;
  }

  public void setLeaf(boolean leaf) {
    this.leaf = leaf;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public boolean isDeliverPayloads() {
    return deliverPayloads;
  }

  public void setDeliverPayloads(boolean deliverPayloads) {
    this.deliverPayloads = deliverPayloads;
  }

  public int getMaxPayloadSize() {
    return maxPayloadSize;
  }

  public void setMaxPayloadSize(int maxPayloadSize) {
    this.maxPayloadSize = maxPayloadSize;
  }

  public boolean isPersistItems() {
    return persistItems;
  }

  public void setPersistItems(boolean persistItems) {
    this.persistItems = persistItems;
  }

  public int getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(int maxItems) {
    this.maxItems = maxItems;
  }

  public boolean isNotifyConfigChanges() {
    return notifyConfigChanges;
  }

  public void setNotifyConfigChanges(boolean notifyConfigChanges) {
    this.notifyConfigChanges = notifyConfigChanges;
  }

  public boolean isNotifyDelete() {
    return notifyDelete;
  }

  public void setNotifyDelete(boolean notifyDelete) {
    this.notifyDelete = notifyDelete;
  }

  public boolean isNotifyRetract() {
    return notifyRetract;
  }

  public void setNotifyRetract(boolean notifyRetract) {
    this.notifyRetract = notifyRetract;
  }

  public boolean isPresenceBased() {
    return presenceBased;
  }

  public void setPresenceBased(boolean presenceBased) {
    this.presenceBased = presenceBased;
  }

  public boolean isSendItemSubscribe() {
    return sendItemSubscribe;
  }

  public void setSendItemSubscribe(boolean sendItemSubscribe) {
    this.sendItemSubscribe = sendItemSubscribe;
  }

  public String getPublisherModel() {
    return publisherModel;
  }

  public void setPublisherModel(String publisherModel) {
    this.publisherModel = publisherModel;
  }

  public boolean isSubscriptionEnabled() {
    return subscriptionEnabled;
  }

  public void setSubscriptionEnabled(boolean subscriptionEnabled) {
    this.subscriptionEnabled = subscriptionEnabled;
  }

  public boolean isConfigSubscription() {
    return configSubscription;
  }

  public void setConfigSubscription(boolean configSubscription) {
    this.configSubscription = configSubscription;
  }

  public String getAccessModel() {
    return accessModel;
  }

  public void setAccessModel(String accessModel) {
    this.accessModel = accessModel;
  }

  public String getPayloadType() {
    return payloadType;
  }

  public void setPayloadType(String payloadType) {
    this.payloadType = payloadType;
  }

  public String getBodyXSLT() {
    return bodyXSLT;
  }

  public void setBodyXSLT(String bodyXSLT) {
    this.bodyXSLT = bodyXSLT;
  }

  public String getDataformXSLT() {
    return dataformXSLT;
  }

  public void setDataformXSLT(String dataformXSLT) {
    this.dataformXSLT = dataformXSLT;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getReplyPolicy() {
    return replyPolicy;
  }

  public void setReplyPolicy(String replyPolicy) {
    this.replyPolicy = replyPolicy;
  }

  public String getAssociationPolicy() {
    return associationPolicy;
  }

  public void setAssociationPolicy(String associationPolicy) {
    this.associationPolicy = associationPolicy;
  }

  public int getMaxLeafNodes() {
    return maxLeafNodes;
  }

  public void setMaxLeafNodes(int maxLeafNodes) {
    this.maxLeafNodes = maxLeafNodes;
  }

  public static class TopicEntityBuilder {
    public TopicEntity build(ResultSet rs) {
      TopicEntity topicEntity = new TopicEntity();

      try {
        topicEntity.setServiceId(rs.getString(CL_SERVICE_ID));
        topicEntity.setNodeId(rs.getString(CL_NODE_ID));
        topicEntity.setLeaf(rs.getBoolean(CL_LEAF));

        String creationDate = rs.getString(CL_CREATION_DATE);
        if(!Strings.isNullOrEmpty(creationDate)) {
          topicEntity.setCreationDate(new Date(Long.parseLong(creationDate)));
        }
        String modificationDate = rs.getString(CL_MODIFICATION_DATE);
        if(!Strings.isNullOrEmpty(modificationDate)) {
          topicEntity.setCreationDate(new Date(Long.parseLong(creationDate)));
        }

        topicEntity.setParent(rs.getString(CL_PARENT));
        topicEntity.setDeliverPayloads(rs.getBoolean(CL_DELIVER_PAYLOADS));
        topicEntity.setMaxPayloadSize(rs.getInt(CL_MAX_PAYLOAD_SIZE));
        topicEntity.setPersistItems(rs.getBoolean(CL_PERSIST_ITEMS));
        topicEntity.setMaxItems(rs.getInt(CL_MAX_ITEMS));
        topicEntity.setNotifyConfigChanges(rs.getBoolean(CL_NOTIFY_CHANGES));
        topicEntity.setNotifyDelete(rs.getBoolean(CL_NOTIFY_DELETE));
        topicEntity.setNotifyRetract(rs.getBoolean(CL_NOTIFY_RETRACT));
        topicEntity.setPresenceBased(rs.getBoolean(CL_PRESENCE_BASED));
        topicEntity.setSendItemSubscribe(rs.getBoolean(CL_SEND_ITEM_SUBSSCRIBE));
        topicEntity.setPublisherModel(rs.getString(CL_PUBLISHER_MODEL));
        topicEntity.setSubscriptionEnabled(rs.getBoolean(CL_SUBSCRIPTION_ENABLED));
        topicEntity.setConfigSubscription(rs.getBoolean(CL_CONFIG_SUBSCRIPTION));
        topicEntity.setAccessModel(rs.getString(CL_ACCESS_MODEL));
        topicEntity.setPayloadType(rs.getString(CL_PAYLOAD_TYPE));
        topicEntity.setBodyXSLT(rs.getString(CL_BODY_XSLT));
        topicEntity.setDataformXSLT(rs.getString(CL_DATAFORM_XSLT));
        topicEntity.setCreator(rs.getString(CL_CREATOR));
        topicEntity.setDescription(rs.getString(CL_DESCRIPTION));
        topicEntity.setLanguage(rs.getString(CL_LANGUAGE));
        topicEntity.setName(rs.getString(CL_NAME));
        topicEntity.setReplyPolicy(rs.getString(CL_REPLYPOLICY));
        topicEntity.setAssociationPolicy(rs.getString(CL_ASSOCIATION_POLICY));
        topicEntity.setMaxLeafNodes(rs.getInt(CL_MAX_LEAF_NODES));
      } catch (SQLException e) {
        LOGGER.error("build : error contructing topic entity", topicEntity);
      }
      return topicEntity;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TopicEntity)) return false;

    TopicEntity entity = (TopicEntity) o;

    if (leaf != entity.leaf) return false;
    if (!nodeId.equals(entity.nodeId)) return false;
    if (!serviceId.equals(entity.serviceId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = serviceId.hashCode();
    result = 31 * result + nodeId.hashCode();
    result = 31 * result + (leaf ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TopicEntity{" +
            "serviceId='" + serviceId + '\'' +
            ", nodeId='" + nodeId + '\'' +
            ", leaf=" + leaf +
            ", creationDate=" + creationDate +
            ", modificationDate=" + modificationDate +
            ", parent='" + parent + '\'' +
            ", deliverPayloads=" + deliverPayloads +
            ", maxPayloadSize=" + maxPayloadSize +
            ", persistItems=" + persistItems +
            ", maxItems=" + maxItems +
            ", notifyConfigChanges=" + notifyConfigChanges +
            ", notifyDelete=" + notifyDelete +
            ", notifyRetract=" + notifyRetract +
            ", presenceBased=" + presenceBased +
            ", sendItemSubscribe=" + sendItemSubscribe +
            ", publisherModel='" + publisherModel + '\'' +
            ", subscriptionEnabled=" + subscriptionEnabled +
            ", configSubscription=" + configSubscription +
            ", accessModel='" + accessModel + '\'' +
            ", payloadType='" + payloadType + '\'' +
            ", bodyXSLT='" + bodyXSLT + '\'' +
            ", dataformXSLT='" + dataformXSLT + '\'' +
            ", creator='" + creator + '\'' +
            ", description='" + description + '\'' +
            ", language='" + language + '\'' +
            ", name='" + name + '\'' +
            ", replyPolicy='" + replyPolicy + '\'' +
            ", associationPolicy='" + associationPolicy + '\'' +
            ", maxLeafNodes=" + maxLeafNodes +
            '}';
  }
}
