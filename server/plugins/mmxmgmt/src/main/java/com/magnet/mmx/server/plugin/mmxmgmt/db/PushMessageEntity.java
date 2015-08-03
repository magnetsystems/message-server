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
public class PushMessageEntity {

  private String messageId;
  private String deviceId;
  private String appId;
  private Date dateSent;
  private Date dateAcknowledged;
  private Long dateSentUTC;
  private Long dateAcknowledgedUTC;
  private PushMessageType type;
  private PushMessageState state;

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public Date getDateSent() {
    return dateSent;
  }


  public Date getDateAcknowledged() {
    return dateAcknowledged;
  }

  public PushMessageType getType() {
    return type;
  }

  public void setType(PushMessageType type) {
    this.type = type;
  }

  public PushMessageState getState() {
    return state;
  }

  public void setState(PushMessageState state) {
    this.state = state;
  }

  public Long getDateSentUTC() {
    return dateSentUTC;
  }

  public void setDateSentUTC(Long dateSentUTC) {
    this.dateSentUTC = dateSentUTC;
    if (this.dateSentUTC != null) {
      this.dateSent = new Date(dateSentUTC.longValue()*1000L);
    }
  }

  public Long getDateAcknowledgedUTC() {
    return dateAcknowledgedUTC;
  }

  public void setDateAcknowledgedUTC(Long dateAcknowledgedUTC) {
    this.dateAcknowledgedUTC = dateAcknowledgedUTC;
    if (this.dateAcknowledgedUTC != null) {
      dateAcknowledged = new Date(dateAcknowledgedUTC.longValue()*1000L);
    }
  }

  /**
   * Enum for push messahe type.
   */
  public static enum PushMessageType {
    /**
     * Initiated from console
     */
    @Deprecated
    CONSOLEPING,
    /**
     * One way ping. Initiation using IQ, Acknowledgment is using call back url
     */
    IQ_PING,
    /**
     * Initiation and acknowledgement is using IQ.
     */
    IQ_PINGPONG,
    /**
     * Initiation and acknowledgement is using IQ
     */
    IQ_PONG,

    /**
     * Initiation using IQ
     */
    IQ_PUSH,
    /**
     * Push message initiated using REST API
     */
    API_PUSH,
    /**
     * Ping message initiated using REST API
     */
    API_PING,
    /**
     * Ping message initiated using IQ
     */
    PROTOCOL_PING,
    /**
     * Push message initiated using IQ
     */
    PROTOCOL_PUSH
  }

  public static enum PushMessageState {
    /**
     * Message has been acknowledged
     */
    ACKNOWLEDGED,
    /**
     * Message has been pushed
     */
    PUSHED,
    ;
  }

  @Override
  public String toString() {
    return "PushMessageEntity{" +
            "messageId='" + messageId + '\'' +
            ", deviceId='" + deviceId + '\'' +
            ", appId='" + appId + '\'' +
            ", dateSent=" + dateSent +
            ", dateAcknowledged=" + dateAcknowledged +
            ", type=" + type +
            ", state=" + state +
            '}';
  }

  public static class PushMessageEntityBuilder {

    /**
     * Build the PushMessageEntity using the resultset.
     *
     * @param rs not null result set. We expect all the PushMessageEntity columns
     *           to be in the result set.
     * @return
     */
    public PushMessageEntity build(ResultSet rs) throws SQLException {
      String deviceId = rs.getString("deviceId");
      String messageId = rs.getString("messageId");
      String appId = rs.getString("appId");
      PushMessageState state = PushMessageState.valueOf(rs.getString("state"));
      PushMessageType type = PushMessageType.valueOf(rs.getString("type"));

      Long dateSentTS = null;
      {
        long temp = rs.getLong("dateSentUTC");
        if (!rs.wasNull()) {
          dateSentTS = Long.valueOf(temp);
        }
      }
      Long dateAckTS = null;
      {
        long temp = rs.getLong("dateAcknowledgedUTC");
        if (!rs.wasNull()) {
          dateAckTS = Long.valueOf(temp);
        }
      }

      PushMessageEntity entity = new PushMessageEntity();
      entity.setDeviceId(deviceId);
      entity.setMessageId(messageId);
      entity.setState(state);
      entity.setAppId(appId);
      entity.setType(type);
      entity.setDateSentUTC(dateSentTS);
      entity.setDateAcknowledgedUTC(dateAckTS);
      return entity;
    }
  }
}
