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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.WakeupNotifier;

/**
 * Interface for sending push (aka ping) messages
 */
public interface PushSender {

  /**
   * Send a push message
   * @param request request encapsulating information about the recipient and message that needs to be pushed.
   * @return PushResult encapsulating the result of the push.
   */
  public PushResult push(PushRequest request);

  /**
   * Push a payload using the supplied clientToken and notification context.
   *
   * @param appId
   * @param clientToken
   * @param payload
   * @param config
   * @return
   */
  public PushResult push(String appId, String clientToken, MMXPayload payload, WakeupNotifier.NotificationSystemContext config);

}
