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

import com.magnet.mmx.protocol.TopicAction.PublisherType;

/**
 * Request object describing a topic create request.
 */
public class TopicCreateInfo {
  private int maxItems = -1;
  private String topicName;
  private String description;
  private boolean personalTopic;
  private boolean subscriptionEnabled = true;
  private boolean subscribeOnCreate;
  private PublisherType publishPermission;

  public TopicCreateInfo() {
  }

  public int getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(int maxItems) {
    this.maxItems = maxItems;
  }

  public boolean isPersonalTopic() {
    return personalTopic;
  }
  
  public void setPersonalTopic(boolean personalTopic) {
    this.personalTopic = personalTopic;
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
  
  public boolean isSubscribeOnCreate() {
    return subscribeOnCreate;
  }
  
  public void setSubscribeOnCreate(boolean subscribeOnCreate) {
    this.subscribeOnCreate = subscribeOnCreate;
  }

  public PublisherType getPublishPermission() {
    return publishPermission;
  }

  public void setPublishPermission(PublisherType permission) {
    this.publishPermission = permission;
  }
  
  @Override
  public String toString() {
    return "TopicInfo{" +
            "maxItems=" + maxItems +
            ", topicName='" + topicName + '\'' +
            ", description='" + description + '\'' +
            ", personalTopic='" + personalTopic + '\'' +
            ", subscriptionEnabled=" + subscriptionEnabled +
            ", subscribeOnCreate=" + subscribeOnCreate +
            ", publishPermission=" + publishPermission +
            '}';
  }
}
