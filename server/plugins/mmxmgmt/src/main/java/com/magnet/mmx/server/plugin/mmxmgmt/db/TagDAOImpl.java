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
import com.magnet.mmx.util.TagUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 */

public class TagDAOImpl implements TagDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(TagDAOImpl.class);

  private ConnectionProvider provider;



  public TagDAOImpl(ConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public int persist(TagEntity tagEntity) {
    validateEntity(tagEntity);
    final String statementStr = "INSERT INTO mmxTag " + getColumnNames(tagEntity)  + " VALUES " + getValues(tagEntity);
    Connection conn = null;
    ResultSet rs = null;
    PreparedStatement pstmt = null;
    int id = -1;
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      LOGGER.trace("persist : executing query={}", pstmt);
      id = pstmt.executeUpdate();
    } catch (Exception e) {
      LOGGER.error("persist : caught exception tagEntity={}", tagEntity, e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return id;
  }

  private String getColumnNames(TagEntity tagEntity) {
    List<String> columnNames = new ArrayList<String>();
    columnNames.add(TagEntity.CL_TAGNAME);
    columnNames.add(TagEntity.CL_APPID);
    if(tagEntity.getMmxDeviceIdFK() >=0 || !Strings.isNullOrEmpty(tagEntity.getDeviceId())) {
       columnNames.add(TagEntity.CL_DEV_ID);
    }
    if(!Strings.isNullOrEmpty(tagEntity.getUsername())) {
      columnNames.add(TagEntity.CL_USER_NAME);
    }
    return SqlUtil.getCommaSepBraces(columnNames);
  }

  private String getValues(TagEntity tagEntity) {
    List<String> values = new ArrayList<String>();
    values.add(getSingleQuoted(tagEntity.getTagname()));
    values.add(getSingleQuoted(tagEntity.getAppId()));

    if(!Strings.isNullOrEmpty(tagEntity.getDeviceId()))
      values.add(constructNestedSelectSQL(tagEntity.getDeviceId(), tagEntity.getAppId()));
    else if (tagEntity.getMmxDeviceIdFK() > 0 )
      values.add(Integer.toString(tagEntity.getMmxDeviceIdFK()));

    if(!Strings.isNullOrEmpty(tagEntity.getUsername()))
      values.add(getSingleQuoted(tagEntity.getDeviceId()));

    return SqlUtil.getCommaSepBraces(values);
  }

  private void validateEntity(TagEntity entity) throws IllegalArgumentException {
    if(Strings.isNullOrEmpty(entity.getTagname()))
      throw new IllegalArgumentException("Invalid tagname");
    if(Strings.isNullOrEmpty(entity.getAppId()))
     throw new IllegalArgumentException("Invalid appId");
    if( (Strings.isNullOrEmpty(entity.getDeviceId()) && entity.getMmxDeviceIdFK() <= 0) && Strings.isNullOrEmpty(entity.getUsername()))
      throw new IllegalArgumentException("Atleast one resource id should be set : deviceId or username");
  }

  private String getSingleQuoted(String s) {
    return "'" + s + "'";
  }

  @Override
  public List<TagEntity> getAllTags(String appId) {
    final String statementStr = "SELECT * from mmxTag where appId = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TagEntity> tagEntityList = new ArrayList<TagEntity>();

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      LOGGER.trace("getAllTags : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        TagEntity e = new TagEntity.TagEntityBuilder().build(rs);
        tagEntityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getAllTags : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return tagEntityList;
  }

  @Override
  public List<TagEntity> getTagEntitiesForDevice(DeviceEntity entity) {
    final String statementStr = "SELECT * from mmxTag where appId = ? and deviceId = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TagEntity> entityList = new ArrayList<TagEntity>();

    try {
      conn = provider.getConnection();

      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, entity.getAppId());
      pstmt.setInt(2, entity.getId());
      rs =pstmt.executeQuery();
      LOGGER.trace("getTagEntitiesForDevice : executing statement={}", pstmt);
      while(rs.next()) {
        TagEntity e = new TagEntity.TagEntityBuilder().build(rs);
        entityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getTagEntitiesForDevice : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return entityList;
  }

  @Override
  public List<String> getTagsForDevice(DeviceEntity entity) {
    final String statementStr = "SELECT tagname from mmxTag where appId = ? and deviceId = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<String> tags = new ArrayList<String>();

    try {
      conn = provider.getConnection();

      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, entity.getAppId());
      pstmt.setInt(2, entity.getId());
      rs =pstmt.executeQuery();
      LOGGER.trace("getTagEntitiesForDevice : executing statement={}", pstmt);
      while(rs.next()) {
        tags.add(rs.getString("tagname"));
      }
    } catch (SQLException e) {
      LOGGER.error("getTagEntitiesForDevice : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return tags;
  }

  @Override
  public List<Integer> getDeviceIdsBooleanAnd(List<String> tagNames, String appId) {
    return null;
  }

  @Override
  public List<Integer> getDeviceIdsBooleanOr(List<String> tagNames, String appId) {
    return null;
  }

  @Override
  public int createUsernameTag(String tagname, String appId, String username) throws Exception {
    TagUtil.validateTag(tagname);

    final String statementStr = "INSERT INTO mmxTag (tagname, appId, creationDate, username) VALUES (?,?,?,?)";
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, tagname);
      pstmt.setString(2, appId);
      pstmt.setString(3, new Timestamp(System.currentTimeMillis()).toString());
      pstmt.setString(4, username);
      LOGGER.trace("createUsernameTag : executing query : {}", pstmt);
      return pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("createUsernameTag : {}");
      throw e;
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public List<String> getTagsForUsername(String appId, String username) throws Exception {
    final String statementStr = "SELECT tagname from mmxTag where appId = ? and username = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<String> tags = new ArrayList<String>();

    try {
      conn = provider.getConnection();

      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, username);
      rs = pstmt.executeQuery();
      LOGGER.trace("getTagEntitiesForDevice : executing statement={}", pstmt);
      while(rs.next()) {
        tags.add(rs.getString("tagname"));
      }
    } catch (SQLException e) {
      LOGGER.error("getTagEntitiesForDevice : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return tags;
  }

  @Override
  public List<TagEntity> getTagEntitiesForUsername(String appId, String username) throws Exception {
    final String statementStr = "SELECT * from mmxTag where appId = ? and username = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TagEntity> entityList = new ArrayList<TagEntity>();

    try {
      conn = provider.getConnection();

      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, username);
      rs =pstmt.executeQuery();
      LOGGER.trace("getTagEntitiesForUsername : executing statement={}", pstmt);
      while(rs.next()) {
        TagEntity e = new TagEntity.TagEntityBuilder().build(rs);
        entityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getTagEntitiesForUsername : {}");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
    return entityList;
  }

  @Override
  public void deleteTagsForUsername(String appId, String username, List<String> tagnames) throws DbInteractionException {
    final String unboundStatementStr = "DELETE FROM mmxTag WHERE appId = ? AND username = ?"
     + (tagnames == null ? "" : " AND tagname IN " + SqlUtil.getCommaSepBracesSingleQuoted(tagnames));
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, username);
      LOGGER.trace("deleteTagsForUsername : executing query : {}", pstmt);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("deleteTagsForUsername : caught exception deleting tags for username={}", username, e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public void deleteAllTagsForUsername(String appId, String username) {
    deleteTagsForUsername(appId, username, null);
  }

  @Override
  public int createDeviceTag(String tagname, String appId, String deviceId) throws Exception {
    TagUtil.validateTag(tagname);
    
    final String statementStr = "INSERT INTO mmxTag (tagname, appId, creationDate, deviceId) VALUES (?,?,?,(select id from mmxDevice where deviceId=? AND appId=?))";

    Connection conn = null;
    PreparedStatement pstmt = null;
    
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, tagname);
      pstmt.setString(2, appId);
      pstmt.setString(3, new Timestamp(System.currentTimeMillis()).toString());
      pstmt.setString(4, deviceId);
      pstmt.setString(5, appId);
      LOGGER.trace("createDeviceTag : executing query : {}", pstmt);
      return pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("createDeviceTag : {}");
      throw e;
    } finally {
       CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public int createDeviceTag(String tagname, String appId, int deviceIdFK) throws DbInteractionException {
    final String statementStr = "INSERT INTO mmxTag (tagname, appId, creationDate, deviceId) VALUES (?,?,?,?)";
    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, tagname);
      pstmt.setString(2, appId);
      pstmt.setString(3, new Timestamp(System.currentTimeMillis()).toString());
      pstmt.setInt(4, deviceIdFK);
      LOGGER.trace("createDeviceTag : executing query : {}", pstmt);
      return pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("createDeviceTag : caught exception creating device tag={}", tagname, e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public void deleteTagsForDevice(DeviceEntity entity, List<String> tagnames) throws DbInteractionException {
    final String unboundStatementStr = "DELETE FROM mmxTag WHERE appId = ? AND deviceId = ? " +
            (tagnames == null ? "" : " AND tagname IN " + SqlUtil.getCommaSepBracesSingleQuoted(tagnames));
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, entity.getAppId());
      pstmt.setInt(2, entity.getId());
      LOGGER.trace("deleteTagsForDevice : executing query : {}", pstmt);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("deleteTagsForDevice : caught exception deleting tags for device={}", entity.getDeviceId(), e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public void deleteAllTagsForDevice(DeviceEntity entity) throws DbInteractionException {
    deleteTagsForDevice(entity, null);
  }

  @Override
  public int createTopicTag(String tagname, String appId, String serviceId, String nodeId) throws DbInteractionException {
    TagUtil.validateTag(tagname);
    
    final String statementStr = "INSERT INTO mmxTag (tagname, appId, creationDate, serviceID, nodeID) VALUES (?,?,?,?,?)";
    LOGGER.trace("createTopicTag : tagname={}, appId={}, serviceId={}, nodeId={}", new Object[]{tagname, appId, serviceId, nodeId});

    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, tagname);
      pstmt.setString(2, appId);
      pstmt.setString(3, new Timestamp(System.currentTimeMillis()).toString());
      pstmt.setString(4, serviceId);
      pstmt.setString(5, nodeId);
      LOGGER.trace("createTopicTag : executing query : {}", pstmt);
      return pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("createTopicTag : caught exception creating topic tag {}", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public List<String> getTagsForTopic(String appId, String serviceId, String nodeId) throws Exception {
    final String statementStr = "SELECT tagname from mmxTag where appId = ? and nodeID = ? and serviceID = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<String> tags = new ArrayList<String>();

    try {
      conn = provider.getConnection();

      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, nodeId);
      pstmt.setString(3, serviceId);
      rs = pstmt.executeQuery();
      LOGGER.trace("getTagsForTopic : executing statement={}", pstmt);
      while(rs.next()) {
        tags.add(rs.getString("tagname"));
      }
    } catch (SQLException e) {
      LOGGER.error("getTagsForTopic : {}");
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }
    return tags;
  }

  @Override
  public List<TagEntity> getTagEntitiesForTopic(String appId, String serviceId, String nodeId) throws SQLException {
    final String statementStr = "SELECT * from mmxTag where appId = ? and nodeId = ? and serviceId = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<TagEntity> entityList = new ArrayList<TagEntity>();
    LOGGER.trace("getTagEntitiesForTopic : appId={},nodeId={},serviceId={}", new Object[]{appId, nodeId, serviceId});
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, nodeId);
      pstmt.setString(3, serviceId);
      rs =pstmt.executeQuery();
      LOGGER.trace("getTagEntitiesForTopic : executing statement={}", pstmt);
      while(rs.next()) {
        TagEntity e = new TagEntity.TagEntityBuilder().build(rs);
        entityList.add(e);
      }
    } catch (SQLException e) {
      LOGGER.error("getTagEntitiesForTopic : {}", e);
      throw e;
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, conn);
    }
    return entityList;
  }

  @Override
  public void deleteTagsForTopic(List<String> tagnames, String appId, String serviceId, String nodeId) throws DbInteractionException {
    final String statementStr = "DELETE FROM mmxTag WHERE appId = ? AND serviceId = ? AND nodeId = ?"
            + (tagnames == null ? "" : " AND tagname IN " + SqlUtil.getCommaSepBracesSingleQuoted(tagnames));
    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
      conn = provider.getConnection();
      LOGGER.trace("deleteTagsForTopic : statementStr={}", statementStr);
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, appId);
      pstmt.setString(2, serviceId);
      pstmt.setString(3, nodeId);
      pstmt.executeUpdate();
      LOGGER.trace("deleteTagsForTopic : executing statement={}", pstmt);
    } catch (SQLException e) {
      LOGGER.error("deleteTagsForTopic : caught exception for nodeId={}", nodeId, e);
      throw new DbInteractionException(e);
    } finally {
      LOGGER.trace("deleteTagsForTopic : closing connection");
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @Override
  public void deleteAllTagsForTopic(String appId, String serviceId, String nodeId) {
    deleteTagsForTopic(null, appId, serviceId, nodeId);
  }

  private String constructNestedSelectSQL(String deviceId, String appId) {
    final String statementString = " (SELECT id FROM mmxDevice where deviceId='%s' AND appId='%s') ";
    return String.format(statementString, deviceId, appId);
  }
}
