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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 */
public class PushAccountEntity {

  private int id;
  private String userId;
  private int appId;
  private String type;
  private String deviceToken;
  private String deviceId;
  private Date createdOn;
  private String modelInfo;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getAppId() {
    return appId;
  }

  public void setAppId(int appId) {
    this.appId = appId;
  }

  public String getDeviceToken() {
    return deviceToken;
  }

  public void setDeviceToken(String deviceToken) {
    this.deviceToken = deviceToken;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }

  public String getModelInfo() {
    return modelInfo;
  }

  public void setModelInfo(String modelInfo) {
    this.modelInfo = modelInfo;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public static class PushAccountEntityBuilder {

    /**
     * Build the AppEntity using the result.
     *
     * @param rs not null result set. We expect all the appentity columns to be in the result set.
     * @return
     */
    public PushAccountEntity build(ResultSet rs) throws SQLException {
      int id = rs.getInt("id");
      int appId = rs.getInt("app_id");
      String JID = rs.getString("JID");
      String type = rs.getString("type");
      String token = rs.getString("device_token");
      Date created = rs.getDate("date_created");
      String deviceId = rs.getString("device_id");
      String modelInfo = rs.getString("model_info");

      PushAccountEntity entity = new PushAccountEntity();
      entity.setId(id);
      entity.setAppId(appId);
      entity.setUserId(JID);
      entity.setDeviceToken(token);
      entity.setCreatedOn(created);
      entity.setDeviceId(deviceId);
      entity.setModelInfo(modelInfo);
      entity.setType(type);
      return entity;
    }
  }
}
