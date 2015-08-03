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
 * Represent a reliable message
 */
public class MessageEntity {

  /**
   * Enum for encapsulating the states for the message.  The priority is used
   * for the state aggregation when a recipient has multiple devices.  Currently
   * each state has a unique priority.  In the future, if multiple states have
   * same priority, the aggregation logic should take device priority and the 
   * latest modified time into consideration.
   */
  public enum MessageState {
    /**
     * Every message starts in this state
     */
    PENDING(0),
    /**
     * Recipient is offline and hence we need to send a wake-up notification
     */
    WAKEUP_REQUIRED(1),
    /**
     * Message wake up has been timed out
     */
    WAKEUP_TIMEDOUT(2),
    /**
     * We are waiting for recipient to wake up
     */
    WAKEUP_SENT(3),
    /**
     * Recipient is online and hence we transitioned to this
     * state
     */
    DELIVERY_ATTEMPTED(4),
    /**
     * XMPP packet has been delivered to the endpoint
     */
    DELIVERED(5),
    /**
     * Message has been processed by the endpoint
     */
    RECEIVED(6);

    private int mPriority;
 
    private MessageState(int priority) {
      mPriority = priority;
    }

    public int getPriority() {
      return mPriority;
    }
  }

  public enum MessageType {
    /**
     * Message that represents a delivery receipt from the receiver to the sender..
     */
    RECEIPT,
    /**
     * regular mmx message
     */
    REGULAR
  }

  private int id;
  private String messageId;
  private Long queuedAtUTC;
  private Long deliveryAckAtUTC;
  private String from;
  private String to;
  private MessageState state;
  private String appId;
  private String deviceId;
  private MessageType type;
  private String sourceMessageId;
  private Date queuedAt;
  private Date deliveryAckAt;

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public Long getQueuedAtUTC() {
    return queuedAtUTC;
  }

  public void setQueuedAtUTC(Long queuedAtUTC) {
    this.queuedAtUTC = queuedAtUTC;
    if (queuedAtUTC != null) {
      queuedAt = new Date(queuedAtUTC.longValue()*1000L);
    }
  }

  public Long getDeliveryAckAtUTC() {
    return deliveryAckAtUTC;
  }

  public void setDeliveryAckAtUTC(Long deliveryAckAtUTC) {
    this.deliveryAckAtUTC = deliveryAckAtUTC;
    if (deliveryAckAtUTC != null) {
      deliveryAckAt = new Date(deliveryAckAtUTC.longValue()*1000L);
    }
  }


  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public MessageState getState() {
    return state;
  }

  public void setState(MessageState state) {
    this.state = state;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public String getSourceMessageId() {
    return sourceMessageId;
  }

  public void setSourceMessageId(String sourceMessageId) {
    this.sourceMessageId = sourceMessageId;
  }

  public Date getDeliveryAckAt() {
    return deliveryAckAt;
  }

  public Date getQueuedAt() {
    return queuedAt;
  }

  public static class MessageEntityBuilder {

    /**
     * Build the MessageEntity using the result.
     *
     * @param rs not null result set. We expect all the message entity columns to be in the result set.
     * @return
     */
    public MessageEntity build(ResultSet rs) throws SQLException {
      int id = rs.getInt("id");
      String deviceId = rs.getString("deviceId");
      String messageId = rs.getString("messageId");
      String fromJID = rs.getString("fromJID");
      String toJID = rs.getString("toJID");
      MessageEntity.MessageState state = MessageEntity.MessageState.valueOf(rs.getString("state"));
      Long dateQueuedUTC = null;
      {
        long temp = rs.getLong("dateQueuedUTC");
        if (!rs.wasNull()) {
          dateQueuedUTC = Long.valueOf(temp);
        }
      }
      Long dateAcked = null;
      {
        long temp = rs.getLong("dateAcknowledgedUTC");
        if (!rs.wasNull()) {
          dateAcked = Long.valueOf(temp);
        }
      }
      String appKey = rs.getString("appId");
      String sourceMessageId = rs.getString("sourceMessageId");
      MessageType type = null;
      String rawType = rs.getString("messageType");
      if (rawType != null) {
        type = MessageType.valueOf(rawType);
      }
      if (type == null) {
        type = MessageType.RECEIPT;
      }

      MessageEntity entity = new MessageEntity();
      entity.setId(id);
      entity.setDeviceId(deviceId);
      entity.setMessageId(messageId);
      entity.setFrom(fromJID);
      entity.setTo(toJID);
      entity.setState(state);
      entity.setQueuedAtUTC(dateQueuedUTC);
      entity.setDeliveryAckAtUTC(dateAcked);
      entity.setAppId(appKey);
      entity.setType(type);
      entity.setSourceMessageId(sourceMessageId);

      return entity;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MessageEntity{");
    sb.append("id=").append(id);
    sb.append(", messageId='").append(messageId).append('\'');
    sb.append(", queuedAtUTC=").append(queuedAtUTC);
    sb.append(", deliveryAckAtUTC=").append(deliveryAckAtUTC);
    sb.append(", from='").append(from).append('\'');
    sb.append(", to='").append(to).append('\'');
    sb.append(", state=").append(state);
    sb.append(", appId='").append(appId).append('\'');
    sb.append(", deviceId='").append(deviceId).append('\'');
    sb.append(", type=").append(type);
    sb.append(", sourceMessageId='").append(sourceMessageId).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
