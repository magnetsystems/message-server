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
package com.magnet.mmx.server.plugin.mmxmgmt.topic;

import org.jivesoftware.openfire.pubsub.NodeSubscription;

/**
 * Class the encapsulates subscription info.
 */
public class SubscriptionInfo {

  private String userId;
  private String topicId;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTopicId() {
    return topicId;
  }

  public void setTopicId(String topicId) {
    this.topicId = topicId;
  }


  public static SubscriptionInfo build(NodeSubscription nodeSubscription) {
    SubscriptionInfo info = new SubscriptionInfo();
    info.setUserId(nodeSubscription.getJID().toFullJID());
    info.setTopicId(nodeSubscription.getNode().getNodeID());
    return info;
  }
}
