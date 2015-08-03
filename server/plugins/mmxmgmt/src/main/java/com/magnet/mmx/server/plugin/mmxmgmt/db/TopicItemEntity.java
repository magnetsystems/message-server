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

/**
 */
public class TopicItemEntity {
  private static final String CL_SERVICE_ID = "serviceID";
  private static final String CL_NODE_ID = "nodeID";
  private static final String CL_ID = "id";
  private static final String CL_JID = "jid";
  private static final String CL_CREATION_DATE = "creationDate";
  private static final String CL_PAYLOAD = "payload";

  private String serviceId;
  private String nodeId;
  String id;
  String jid;
  String creationDate;
  String payload;

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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getJid() {
    return jid;
  }

  public void setJid(String jid) {
    this.jid = jid;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String creationDate) {
    this.creationDate = creationDate;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public static class TopicItemEntityBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicItemEntityBuilder.class);
    public TopicItemEntity build(ResultSet rs) {
      TopicItemEntity topicItemEntity = new TopicItemEntity();
      try {
        topicItemEntity.setServiceId(rs.getString(CL_SERVICE_ID));
        topicItemEntity.setNodeId(rs.getString(CL_NODE_ID));
        topicItemEntity.setCreationDate(rs.getString(CL_CREATION_DATE));
        topicItemEntity.setId(rs.getString(CL_ID));
        topicItemEntity.setJid(rs.getString(CL_JID));
        topicItemEntity.setPayload(rs.getString(CL_PAYLOAD));
      } catch (Exception e) {
        LOGGER.error("build : Error building topic entity", e);
      }
      return topicItemEntity;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TopicItemEntity)) return false;

    TopicItemEntity that = (TopicItemEntity) o;

    if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) return false;
    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (jid != null ? !jid.equals(that.jid) : that.jid != null) return false;
    if (!nodeId.equals(that.nodeId)) return false;
    if (payload != null ? !payload.equals(that.payload) : that.payload != null) return false;
    if (!serviceId.equals(that.serviceId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = serviceId.hashCode();
    result = 31 * result + nodeId.hashCode();
    result = 31 * result + (id != null ? id.hashCode() : 0);
    result = 31 * result + (jid != null ? jid.hashCode() : 0);
    result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
    result = 31 * result + (payload != null ? payload.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TopicItemEntity{" +
            "serviceId='" + serviceId + '\'' +
            ", nodeId='" + nodeId + '\'' +
            ", id='" + id + '\'' +
            ", jid='" + jid + '\'' +
            ", creationDate='" + creationDate + '\'' +
            ", payload='" + payload + '\'' +
            '}';
  }
}
