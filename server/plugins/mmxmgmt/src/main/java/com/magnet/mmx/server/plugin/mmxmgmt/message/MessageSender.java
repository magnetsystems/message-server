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

import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;

/**
 * Interface for sending XMPP messages
 */
public interface MessageSender {

  /**
   * Send a message using the information in the request object.
   *
   * @param appId
   * @param request
   * @return
   */
  public SendMessageResult send(String appId, com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageRequest request);

  /**
   * Post a message to a topic.
   * @param topicName name of the topic
   * @param appId id of the app
   * @param request request describing the message that should be posted.
   * @return
   */
  public TopicPostResult postMessage(String topicName, String appId, TopicPostMessageRequest request);

}
