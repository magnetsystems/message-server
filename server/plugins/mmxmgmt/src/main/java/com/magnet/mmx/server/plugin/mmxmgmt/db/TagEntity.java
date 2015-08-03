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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 */
public class TagEntity {
  private static final Logger LOGGER = LoggerFactory.getLogger(TagEntity.class);

  public static final String TBL_NAME = "mmxTag";
  public static final String CL_ID = "id";
  public static final String CL_APPID = "appId";
  public static final String CL_CREATION_DATE = "creationDate";
  public static final String CL_TAGNAME = "tagname";
  public static final String CL_DEV_ID = "deviceId";
  public static final String CL_USER_NAME = "username";
  public static final String CL_NODE_ID = "nodeID";
  public static final String CL_SERVICE_ID = "serviceID";

  private int id;
  private String tagname;
  private String appId;
  private Date creationDate;
  private int mmxDeviceIdFK =-1;
  private String deviceId=null;
  private String username;
  private String nodeID;
  private String serviceID;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTagname() {
    return tagname;
  }

  public void setTagname(String tagname) {
    this.tagname = tagname;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public int getMmxDeviceIdFK() {
    return mmxDeviceIdFK;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setMmxDeviceIdFK(int mmxDeviceIdFK) {
    this.mmxDeviceIdFK = mmxDeviceIdFK;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getNodeID() {
    return nodeID;
  }

  public void setNodeID(String nodeID) {
    this.nodeID = nodeID;
  }

  public String getServiceID() {
    return serviceID;
  }

  public void setServiceID(String serviceID) {
    this.serviceID = serviceID;
  }

  public boolean isDeviceTag() {
    return deviceId != null;
  }

  public boolean isUserTag() {
    return username != null;
  }

  public boolean isTopicTag() {
    return serviceID != null && nodeID != null;
  }

  public static class TagEntityBuilder {
    public TagEntity build(ResultSet rs) throws SQLException {
      int localId;
      Date localCreationDate;
      String localTagname;
      String localAppId;
      int localDeviceId;
      String localUsername;
      String localNodeID;
      String localServiceID;

      try {
        localId = rs.getInt(CL_ID);
        localTagname = rs.getString(CL_TAGNAME);
        localCreationDate = rs.getTimestamp(CL_CREATION_DATE);
        localAppId = rs.getString(CL_APPID);
        localDeviceId = rs.getInt(CL_DEV_ID);
        localUsername = rs.getString(CL_USER_NAME);
        localNodeID = rs.getString(CL_NODE_ID);
        localServiceID = rs.getString(CL_SERVICE_ID);
      } catch (SQLException e) {
        LOGGER.error("build : Error building ", e);
        throw e;
      }

      TagEntity tagEntity = new TagEntity();

      tagEntity.setId(localId);
      tagEntity.setTagname(localTagname);
      tagEntity.setCreationDate(localCreationDate);
      tagEntity.setAppId(localAppId);
      tagEntity.setMmxDeviceIdFK(localDeviceId);
      tagEntity.setUsername(localUsername);
      tagEntity.setNodeID(localNodeID);
      tagEntity.setServiceID(localServiceID);
      return tagEntity;
    }
  }

  @Override
  public String toString() {
    return "TagEntity{" +
            "username='" + username + '\'' +
            ", deviceId='" + deviceId + '\'' +
            ", mmxDeviceIdFK=" + mmxDeviceIdFK +
            ", creationDate=" + creationDate +
            ", appId='" + appId + '\'' +
            ", tagname='" + tagname + '\'' +
            ", id=" + id +
            '}';
  }
}
