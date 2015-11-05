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
public class ChannelInfo {
  private int maxItems = -1;
  private String publisherType;
  private String channelName;
  private String description;
  private boolean subscriptionEnabled = true;


  public ChannelInfo() {
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

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
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
    if (!(o instanceof ChannelInfo)) return false;

    ChannelInfo channelInfo = (ChannelInfo) o;

    if (maxItems != channelInfo.maxItems) return false;
    if (subscriptionEnabled != channelInfo.subscriptionEnabled) return false;
    if (description != null ? !description.equals(channelInfo.description) : channelInfo.description != null) return false;
    if (publisherType != null ? !publisherType.equals(channelInfo.publisherType) : channelInfo.publisherType != null)
      return false;
    if (channelName != null ? !channelName.equals(channelInfo.channelName) : channelInfo.channelName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = maxItems;
    result = 31 * result + (publisherType != null ? publisherType.hashCode() : 0);
    result = 31 * result + (channelName != null ? channelName.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (subscriptionEnabled ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ChannelInfo{" +
            "maxItems=" + maxItems +
            ", publisherType='" + publisherType + '\'' +
            ", channelName='" + channelName + '\'' +
            ", description='" + description + '\'' +
            ", subscriptionEnabled=" + subscriptionEnabled +
            '}';
  }
}
