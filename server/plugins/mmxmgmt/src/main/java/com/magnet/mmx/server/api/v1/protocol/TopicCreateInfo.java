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
 * Request object describing a topic create request.
 */
public class TopicCreateInfo {
  private int maxItems = -1;
  private String topicName;
  private String description;
  private boolean subscriptionEnabled = true;


  public TopicCreateInfo() {
  }

  public int getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(int maxItems) {
    this.maxItems = maxItems;
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
  public String toString() {
    return "TopicInfo{" +
            "maxItems=" + maxItems +
            ", topicName='" + topicName + '\'' +
            ", description='" + description + '\'' +
            ", subscriptionEnabled=" + subscriptionEnabled +
            '}';
  }
}
