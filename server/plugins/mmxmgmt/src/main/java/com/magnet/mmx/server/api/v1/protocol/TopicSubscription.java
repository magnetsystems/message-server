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

import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.util.AppTopic;
import com.magnet.mmx.util.TopicHelper;
import org.jivesoftware.openfire.pubsub.NodeSubscription;
import org.xmpp.packet.JID;

/**
 */
public class TopicSubscription {
  private String username;            // a user ID for user topicName, or null for global topicName
  private String topicName;             // the topicName name
  private String subscriptionId;    // the subscription ID
  private String deviceId;          // device identifier associated with this subscription.

  public String getUsername() {
    return username;
  }

  public String getTopicName() {
    return topicName;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  /**
   * Build a topicName subscription object using the node subscription information.
   * @param nodeSubscription
   * @return
   */
  public static TopicSubscription build (NodeSubscription nodeSubscription) {
    JID subscriberJID = nodeSubscription.getJID();
    String subscriberJIDNode = subscriberJID.getNode();
    String username = JIDUtil.getUserId(subscriberJIDNode);
    String resource = subscriberJID.getResource();
    AppTopic topic = TopicHelper.parseTopic(nodeSubscription.getNode().getNodeID());
    String topicName = topic.getName();
    TopicSubscription subscription = new TopicSubscription();
    subscription.subscriptionId = nodeSubscription.getID();
    subscription.topicName = topicName;
    subscription.username = username;
    subscription.deviceId = resource;
    return subscription;
  }
}
