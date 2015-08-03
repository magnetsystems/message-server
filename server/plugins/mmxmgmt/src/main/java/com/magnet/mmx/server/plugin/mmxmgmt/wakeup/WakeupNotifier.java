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
package com.magnet.mmx.server.plugin.mmxmgmt.wakeup;

import java.util.List;

/**
 */
public interface WakeupNotifier {

  /**
   * API for sending a notification to one or more devices.
   * @param deviceTokens non null list of device tokens to which we want to send a notification to. This is used for
   *                     identifying the target devices
   * @param payload message that needs to be included in the notification
   * @param context context to provide the details required for each notification system.
   * @return List<NotificationResult> a list representing the notification result.
   */
  public List<NotificationResult> sendNotification(List<String> deviceTokens, String payload, NotificationSystemContext context);

  /**
   * Marker interface for defining the details required by each notification system.
   */
  public interface NotificationSystemContext {}


}
