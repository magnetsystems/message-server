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
package com.magnet.mmx.server.api.v1.protocol;

/**
 */
public class TopicInfo {
  private int maxItems = -1;
  private String publisherType;
  private String topicName;
  private String description;
  private boolean subscriptionEnabled = true;


  public TopicInfo() {
  }

  public int getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(int maxItems) {
    this.maxItems = maxItems;
  }

  public String getPublisherType() {
    return publisherType;
  }

  public void setPublisherType(String publisherType) {
    this.publisherType = publisherType;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isSubscriptionEnabled() {
    return subscriptionEnabled;
  }

  public void setSubscriptionEnabled(boolean subscriptionEnabled) {
    this.subscriptionEnabled = subscriptionEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TopicInfo)) return false;

    TopicInfo topicInfo = (TopicInfo) o;

    if (maxItems != topicInfo.maxItems) return false;
    if (subscriptionEnabled != topicInfo.subscriptionEnabled) return false;
    if (description != null ? !description.equals(topicInfo.description) : topicInfo.description != null) return false;
    if (publisherType != null ? !publisherType.equals(topicInfo.publisherType) : topicInfo.publisherType != null)
      return false;
    if (topicName != null ? !topicName.equals(topicInfo.topicName) : topicInfo.topicName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = maxItems;
    result = 31 * result + (publisherType != null ? publisherType.hashCode() : 0);
    result = 31 * result + (topicName != null ? topicName.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (subscriptionEnabled ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TopicInfo{" +
            "maxItems=" + maxItems +
            ", publisherType='" + publisherType + '\'' +
            ", topicName='" + topicName + '\'' +
            ", description='" + description + '\'' +
            ", subscriptionEnabled=" + subscriptionEnabled +
            '}';
  }
}
