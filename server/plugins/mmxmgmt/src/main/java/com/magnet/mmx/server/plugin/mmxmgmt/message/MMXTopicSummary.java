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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"topicName", "publishedItemCount", "lastPublishedTime"})
public class MMXTopicSummary {
  private String userId;
  private String topicName;
  private int publishedItemCount;
  private String lastPublishedTime;

  public MMXTopicSummary(String userId, String topicName, int publishedItemCount, Date lastPublishedItem) {
    this.userId = userId;
    this.topicName = topicName;
    this.publishedItemCount = publishedItemCount;
    DateTime d = new DateTime(lastPublishedItem, DateTimeZone.UTC);
    this.lastPublishedTime = d.toString();
  }
  
  public MMXTopicSummary(String topicName, int publishedItemCount, Date lastPublishedItem) {
    this.topicName = topicName;
    this.publishedItemCount = publishedItemCount;
    DateTime d = new DateTime(lastPublishedItem, DateTimeZone.UTC);
    this.lastPublishedTime = d.toString();
  }

  public String getUserId() {
    return userId;
  }
  
  public void setUserId(String userId) {
    this.userId = userId;
  }
  
  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public int getPublishedItemCount() {
    return publishedItemCount;
  }

  public void setPublishedItemCount(int publishedItemCount) {
    this.publishedItemCount = publishedItemCount;
  }

  public String getLastPublishedTime() {
    return lastPublishedTime;
  }

  public void setLastPublishedTime(String lastPublishedTime) {
    this.lastPublishedTime = lastPublishedTime;
  }

  @Override
  public String toString() {
    return "MMXTopicSummary{" +
            "userId='" + userId + '\'' +
            ", topicName='" + topicName + '\'' +
            ", publishedItemCount=" + publishedItemCount +
            ", lastPublishedTime=" + lastPublishedTime +
            '}';
  }
}
