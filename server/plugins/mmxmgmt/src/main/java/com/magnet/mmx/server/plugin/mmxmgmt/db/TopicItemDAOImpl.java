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

import com.magnet.mmx.server.plugin.mmxmgmt.util.SqlUtil;
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

public class TopicItemDAOImpl implements TopicItemDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicItemDAOImpl.class);
  private ConnectionProvider provider;

  public TopicItemDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public void persist(TopicItemEntity entity) {
    final String unboundStatementStr = "INSERT INTO ofPubsubItem (serviceID, nodeID, id, jid, creationDate, payload) " +
                                        SqlUtil.getValueListUnboundedStr(6);
    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, entity.getServiceId());
      pstmt.setString(2, entity.getNodeId());
      pstmt.setString(3, entity.getId());
      pstmt.setString(4, entity.getJid());
      pstmt.setString(5, entity.getCreationDate());
      pstmt.setString(6, entity.getPayload());
      LOGGER.trace("persist : executing pstmt={}", pstmt);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("persist : caught exception entity={}", entity, e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public int getCount(String serviceId, String nodeId) {
    final String unboundStatementStr = "SELECT COUNT(*) FROM ofPubsubItem WHERE serviceID=? AND nodeID=?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      LOGGER.trace("getCount : executing pstmt={}", pstmt);
      rs = pstmt.executeQuery();
      if(rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      LOGGER.error("getCount : caught exception serviceId={}, nodeId={}", serviceId, nodeId);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }

    return 0;
  }

  @Override
  public List<TopicItemEntity> getItems(String serviceId, String nodeId, int maxItems, String order) {
    final String unboundStatementStr = "SELECT * FROM ofPubsubItem WHERE serviceID = ? AND nodeID = ? ORDER BY creationDate " + order + " LIMIT " + maxItems;
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TopicItemEntity> topicItemEntityList = new ArrayList<TopicItemEntity>();

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      LOGGER.trace("getItems : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TopicItemEntity e = new TopicItemEntity.TopicItemEntityBuilder().build(rs);
        topicItemEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getItems : caught exception serviceId={}, nodeId={}, maxItems={}",
              new Object[]{serviceId, nodeId, maxItems});
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }
    return topicItemEntityList;
  }

  @Override
  public List<TopicItemEntity> getItemsSince(String serviceId, String nodeId, int maxItems, String since) {
    final String unboundStatementStr = "SELECT * FROM ofPubsubItem WHERE serviceID = ? AND nodeID = ? " +
            "AND creationDate >= ? ORDER BY creationDate ASC LIMIT " + Integer.toString(maxItems);
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TopicItemEntity> topicItemEntityList = new ArrayList<TopicItemEntity>();

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      pstmt.setString(3, since);
      LOGGER.trace("getItems : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TopicItemEntity e = new TopicItemEntity.TopicItemEntityBuilder().build(rs);
        topicItemEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getItems : caught exception serviceId={}, nodeId={}, maxItems={}, until={}",
              new Object[]{serviceId, nodeId, maxItems, since});
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }
    return topicItemEntityList;
  }

  @Override
  public List<TopicItemEntity> getItemsUntil(String serviceId, String nodeId, int maxItems, String until) {
    final String unboundStatementStr = "SELECT * FROM ofPubsubItem WHERE serviceID = ? AND nodeID = ? " +
            "AND creationDate <= ? ORDER BY creationDate DESC LIMIT " + Integer.toString(maxItems);
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TopicItemEntity> topicItemEntityList = new ArrayList<TopicItemEntity>();

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      pstmt.setString(3, until);
      LOGGER.trace("getItems : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TopicItemEntity e = new TopicItemEntity.TopicItemEntityBuilder().build(rs);
        topicItemEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getItems : caught exception serviceId={}, nodeId={}, maxItems={}, until={}",
              new Object[]{serviceId, nodeId, maxItems, until});
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }
    return topicItemEntityList;
  }

  @Override
  public List<TopicItemEntity> getItems(String serviceId, String nodeId, int maxItems, String since, String until, String order) {
    final String unboundStatementStr = "SELECT * FROM ofPubsubItem WHERE serviceID = ? AND nodeID = ? " +
             "AND creationDate BETWEEN ? AND ? ORDER BY creationDate " + order + " LIMIT " + Integer.toString(maxItems);

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TopicItemEntity> topicItemEntityList = new ArrayList<TopicItemEntity>();

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, serviceId);
      pstmt.setString(2, nodeId);
      pstmt.setString(3, since);
      pstmt.setString(4, until);
      LOGGER.trace("getItems : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TopicItemEntity e = new TopicItemEntity.TopicItemEntityBuilder().build(rs);
        topicItemEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getItems : caught exception serviceId={}, nodeId={}, maxItems={}, since={}, until={}, order={}",
                    new Object[]{serviceId, nodeId, maxItems, since, until, order});
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }
    return topicItemEntityList;
  }
}
