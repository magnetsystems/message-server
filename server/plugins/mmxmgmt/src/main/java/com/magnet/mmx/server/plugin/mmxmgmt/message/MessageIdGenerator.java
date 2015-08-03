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

/**
 * Interface for generating identifiers.
 */
public interface MessageIdGenerator {

  /**
   * Generate an id using clientId, appId, deviceId and the current timestamp.
   * @param clientId clientId can't be null
   * @param appId appId can't be null
   * @param deviceId can be null
   * @return
   */
  public String generate(String clientId, String appId, String deviceId);

  /**
   * Generate an identifier for a message to be published to a topic.
   * @param appId -- app identifier can't be null
   * @param topicId -- topic identifier can't be null
   * @return
   */
  public String generateTopicMessageId(String appId, String topicId);

  /**
   * Generate an item identifier for the message to be posted to
   * a topic.
   * @param topicId
   * @return
   */
  public String generateItemIdentifier(String topicId);
}
