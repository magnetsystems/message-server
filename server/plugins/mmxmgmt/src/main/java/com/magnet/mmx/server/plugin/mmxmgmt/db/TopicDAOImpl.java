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
import com.magnet.mmx.server.plugin.mmxmgmt.util.SqlUtil;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class TopicDAOImpl implements TopicDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicDAOImpl.class);

  private ConnectionProvider provider;

  public TopicDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void persist(TopicEntity entity) throws Exception {
    final String statementStr = "INSERT INTO ofPubsubNode " +
    "(serviceID, nodeID, leaf, creationDate, modificationDate, parent, deliverPayloads, maxPayloadSize, " +
            "persistItems, maxItems, notifyConfigChanges, notifyDelete, notifyRetract, " +
            "presenceBased, sendItemSubscribe, publisherModel, subscriptionEnabled, " +
            "configSubscription, accessModel, payloadType, bodyXSLT, dataformXSLT, creator, description, language, name, " +
            "replyPolicy, associationPolicy, maxLeafNodes )" + SqlUtil.getValueListUnboundedStr(29)  + " " +
            " ON DUPLICATE KEY UPDATE leaf=?,creationDate=?, modificationDate=?, parent=?, deliverPayloads=?, maxPayloadSize=?," +
            "persistItems=?, maxItems=?, notifyConfigChanges=?, notifyDelete=?, notifyRetract=?,"  +
            "presenceBased=?, sendItemSubscribe=?, publisherModel=?, subscriptionEnabled=?," +
            "configSubscription=?, accessModel=?, payloadType=?, bodyXSLT=?, dataformXSLT=?, creator=?, description=?, language=?, name=?," +
            "replyPolicy=?, associationPolicy=?, maxLeafNodes=?";

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr, PreparedStatement.RETURN_GENERATED_KEYS);
      pstmt.setString(1, entity.getServiceId());
      pstmt.setString(2, entity.getNodeId());
      pstmt.setBoolean(3, entity.isLeaf());
      pstmt.setString(4, StringUtils.dateToMillis(entity.getCreationDate()));
      pstmt.setString(5, StringUtils.dateToMillis(entity.getModificationDate()));
      pstmt.setString(6, entity.getParent());
      pstmt.setBoolean(7, entity.isDeliverPayloads());
      pstmt.setInt(8, entity.getMaxPayloadSize());
      pstmt.setBoolean(9, entity.isPersistItems());
      pstmt.setInt(10, entity.getMaxItems());
      pstmt.setBoolean(11, entity.isNotifyConfigChanges());
      pstmt.setBoolean(12, entity.isNotifyDelete());
      pstmt.setBoolean(13, entity.isNotifyRetract());
      pstmt.setBoolean(14, entity.isPresenceBased());
      pstmt.setBoolean(15, entity.isSendItemSubscribe());
      pstmt.setString(16, entity.getPublisherModel());
      pstmt.setBoolean(17, entity.isSubscriptionEnabled());
      pstmt.setBoolean(18, entity.isConfigSubscription());
      pstmt.setString(19, entity.getAccessModel());
      pstmt.setString(20, entity.getPayloadType());
      pstmt.setString(21, entity.getBodyXSLT());
      pstmt.setString(22, entity.getDataformXSLT());
      pstmt.setString(23, entity.getCreator());
      pstmt.setString(24, entity.getDescription());
      pstmt.setString(25, entity.getLanguage());
      pstmt.setString(26, entity.getName());
      pstmt.setString(27, entity.getReplyPolicy());
      pstmt.setString(28, entity.getAssociationPolicy());
      pstmt.setInt(29, entity.getMaxItems());
      pstmt.setBoolean(30, entity.isLeaf());
      pstmt.setString(31, StringUtils.dateToMillis(entity.getCreationDate()));
      pstmt.setString(32, StringUtils.dateToMillis(entity.getModificationDate()));
      pstmt.setString(33, entity.getParent());
      pstmt.setBoolean(34, entity.isDeliverPayloads());
      pstmt.setInt(35, entity.getMaxPayloadSize());
      pstmt.setBoolean(36, entity.isPersistItems());
      pstmt.setInt(37, entity.getMaxItems());
      pstmt.setBoolean(38, entity.isNotifyConfigChanges());
      pstmt.setBoolean(39, entity.isNotifyDelete());
      pstmt.setBoolean(40, entity.isNotifyRetract());
      pstmt.setBoolean(41, entity.isPresenceBased());
      pstmt.setBoolean(42, entity.isSendItemSubscribe());
      pstmt.setString(43, entity.getPublisherModel());
      pstmt.setBoolean(44, entity.isSubscriptionEnabled());
      pstmt.setBoolean(45, entity.isConfigSubscription());
      pstmt.setString(46, entity.getAccessModel());
      pstmt.setString(47, entity.getPayloadType());
      pstmt.setString(48, entity.getBodyXSLT());
      pstmt.setString(49, entity.getDataformXSLT());
      pstmt.setString(50, entity.getCreator());
      pstmt.setString(51, entity.getDescription());
      pstmt.setString(52, entity.getLanguage());
      pstmt.setString(53, entity.getName());
      pstmt.setString(54, entity.getReplyPolicy());
      pstmt.setString(55, entity.getAssociationPolicy());
      pstmt.setInt(56, entity.getMaxItems());


      LOGGER.trace("persist : statement={}", pstmt);
      pstmt.executeUpdate();

    } catch (SQLException e) {
      LOGGER.error("persist : caught exception persisting topic = {}", entity, e);
      throw new DbInteractionException("Unable to persist topic");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public void deleteTopic(String serviceId, String nodeId) throws Exception {
    final String statementStr = "DELETE FROM ofPubsubNode where serviceID = ? AND nodeID = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    LOGGER.trace("deleteTopicTags : serviceId={}, nodeId={}", serviceId, nodeId);

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("deleteTopicTags : serviceId={}, nodeId={}", serviceId, nodeId);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public TopicEntity getTopic(String serviceId, String nodeId) throws DbInteractionException {
    final String statementStr = "SELECT * FROM ofPubsubNode where serviceID = ? AND nodeID = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    TopicEntity entity = null;
    LOGGER.trace("getTopicName : serviceId={}, nodeId={}", serviceId, nodeId);

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      rs = pstmt.executeQuery();

      if(rs.next()) {
        TopicEntity.TopicEntityBuilder builder = new TopicEntity.TopicEntityBuilder();
        entity = builder.build(rs);
        return entity;
      }
    } catch (SQLException e) {
      LOGGER.error("getTopicName : {}");
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }

    return entity;
  }

  @Override
  public List<TopicEntity> getTopicsForTag(String tagname, String appId) {
    final String statementStr = "select distinct ofPubsubNode.* from mmxTag inner join ofPubsubNode on ofPubsubNode.nodeID = mmxTag.nodeID AND ofPubsubNode.serviceID = mmxTag.serviceID where mmxTag.tagname=? and mmxTag.appId=?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TopicEntity> topicEntityList = new ArrayList<TopicEntity>();
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, tagname);
      pstmt.setString(2, appId);
      rs = pstmt.executeQuery();

      while(rs.next()) {
        TopicEntity e = new TopicEntity.TopicEntityBuilder().build(rs);
        topicEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getTopicsForTag : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return topicEntityList;
  }

  @Override
  public List<TopicEntity> getTopicsForTagOR(List<String> tagNameList, String appId) {
    List<TopicEntity> topicEntityList = new ArrayList<TopicEntity>();

    if(Strings.isNullOrEmpty(appId)) {
      LOGGER.error("getTopicsForTagOR : appIs is null or empty");
    }

    if(tagNameList == null || tagNameList.size() == 0) {
      LOGGER.error("getTopicsForTagOR : tagNames list is empty");
    }
    final String statementStr = "SELECT DISTINCT ofPubsubNode.* FROM mmxTag " +
                                "INNER JOIN ofPubsubNode ON " +
                                "mmxTag.nodeID = ofPubsubNode.nodeID AND " +
                                "mmxTag.serviceID = ofPubsubNode.serviceID " +
                                "WHERE mmxTag.tagname " +
                                "IN ( " + SqlUtil.getQs(tagNameList.size()) + " ) AND mmxTag.appId= ?";

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      int index = 1;
      for(String tagName : tagNameList) {
        pstmt.setString(index++, tagName);
      }
      pstmt.setString(index, appId);
      LOGGER.trace("getTopicsForTagOR : executing : {}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TopicEntity e = new TopicEntity.TopicEntityBuilder().build(rs);
        topicEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getTopicsForTagOR : caught exception tagNameList={}, appId={}", tagNameList, appId, e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return topicEntityList;
  }

  @Override
  public List<TopicEntity> getTopicsForTagAND(List<String> tagnames, String appId) {
    List<TopicEntity> topicEntityList = new ArrayList<TopicEntity>();
    final String statementStr = "SELECT DISTINCT * FROM ( SELECT DISTINCT tempresult.serviceID AS serviceIDs, tempresult.nodeID AS nodeIDs FROM  mmxTag AS tempresult " + getInnerJoinStatements(tagnames, appId) +
            " ) AS result1 INNER JOIN ( SELECT * FROM ofPubsubNode) as result2 ON  result1.serviceIDs = result2.serviceID AND result1.nodeIDs = result2.nodeID";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      LOGGER.trace("getTopicsForTagAND : executing query={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TopicEntity e = new TopicEntity.TopicEntityBuilder().build(rs);
        topicEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getTopicsForTagAND : caught exception tagNames={}, appId={}", tagnames, appId, e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return topicEntityList;
  }

  private String getInnerJoinStatements(List<String >tagNames, String appId) {
    String innerJoinTemplate = " INNER JOIN (SELECT DISTINCT nodeID, serviceID FROM mmxTag WHERE tagname='%s' and appId='%s') as temp%d USING (serviceID, nodeID) " ;
    StringBuffer sb = new StringBuffer(30*tagNames.size());
    int size = tagNames.size();
    for(int i=0; i < size; i++) {
      sb.append(String.format(innerJoinTemplate, tagNames.get(i), appId, i) + " ");
    }
    return sb.toString();
  }
}
