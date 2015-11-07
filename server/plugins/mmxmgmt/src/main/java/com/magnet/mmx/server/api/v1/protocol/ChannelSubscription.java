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
public class ChannelSubscription {
  private String userId;            // a user ID for user topicName, or null for global topicName
  private String channelName;             // the topicName name
  private String subscriptionId;    // the subscription ID
  private String deviceId;          // device identifier associated with this subscription.

  public String getUserId() {
    return userId;
  }

  public String getChannelName() {
    return channelName;
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
  public static ChannelSubscription build (NodeSubscription nodeSubscription) {
    JID subscriberJID = nodeSubscription.getJID();
    String subscriberJIDNode = subscriberJID.getNode();
    String userId = JIDUtil.getUserId(subscriberJIDNode);
    String resource = subscriberJID.getResource();
    AppTopic topic = TopicHelper.parseTopic(nodeSubscription.getNode().getNodeID());
    String topicName = topic.getName();
    ChannelSubscription subscription = new ChannelSubscription();
    subscription.subscriptionId = nodeSubscription.getID();
    subscription.channelName = topicName;
    subscription.userId = userId;
    subscription.deviceId = resource;
    return subscription;
  }
}
