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

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents a record in the Wakeup table which is a queueing table.
 */
public class WakeupEntity {
  private int id; //auto generated
  private String deviceId;
  private String token;
  private String appId;
  private PushType type;
  private String senderIdentifier;
  private String payload;
  private String messageId;
  private Long dateCreated;
  private Long dateSent;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public PushType getType() {
    return type;
  }

  public void setType(PushType type) {
    this.type = type;
  }

  public String getSenderIdentifier() {
    return senderIdentifier;
  }

  public void setSenderIdentifier(String senderIdentifier) {
    this.senderIdentifier = senderIdentifier;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public Long getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Long dateCreated) {
    this.dateCreated = dateCreated;
  }

  public Long getDateSent() {
    return dateSent;
  }

  public void setDateSent(Long dateSent) {
    this.dateSent = dateSent;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("WakeupEntity{");
    sb.append("id=").append(id);
    sb.append(", deviceId='").append(deviceId).append('\'');
    sb.append(", token='").append(token).append('\'');
    sb.append(", type=").append(type);
    sb.append(", senderIdentifier='").append(senderIdentifier).append('\'');
    sb.append(", payload='").append(payload).append('\'');
    sb.append(", messageId='").append(messageId).append('\'');
    sb.append(", dateCreated=").append(dateCreated);
    sb.append(", dateSent=").append(dateSent);
    sb.append('}');
    return sb.toString();
  }

  public static class WakeupEntityBuilder {

    /**
     * Build the WakeupEntity using the result.
     *
     * @param rs not null result set. We expect all the Wakeup entity columns to be in the result set.
     * @return
     */
    public WakeupEntity build(ResultSet rs) throws SQLException {
      int id = rs.getInt("id");
      String deviceId = rs.getString("deviceId");
      String messageId = rs.getString("messageId");
      String clientToken = rs.getString("clientToken");
      PushType pushType = Helper.enumeratePushType(rs.getString("tokenType"));
      String payload = rs.getString("payload");
      Long dateCreated = null;
      {
        long temp = rs.getLong("dateCreatedUTC");
        if (!rs.wasNull()) {
          dateCreated = Long.valueOf(temp);
        }
      }
      String senderIdentifier = rs.getString("googleApiKey");
      String appId = rs.getString("appId");
      WakeupEntity entity = new WakeupEntity();
      entity.setId(id);
      entity.setDeviceId(deviceId);
      entity.setMessageId(messageId);
      entity.setPayload(payload);
      entity.setSenderIdentifier(senderIdentifier);
      entity.setType(pushType);
      entity.setDateCreated(dateCreated);
      entity.setToken(clientToken);
      entity.setAppId(appId);

      return entity;
    }
  }
}
