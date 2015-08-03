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

/**
 */

public enum NotificationResult {
  /**
   * The underlying notification sender has successfully delivered the
   * notification.
   */
  DELIVERED(),

  /**
   * The underlying notification sender plans to deliver the message, and does
   * not need reminding to deliver.
   */
  DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER(),

  /**
   * The underlying notification sender plans to deliver the message, but
   * needs reminding to deliver on periodic intervals.
   */
  DELIVERY_IN_PROGRESS_REMIND_AGAIN(),

  /**
   * The underlying notification sender failed to deliver the message, and all
   * re-attempts should be aborted.
   */
  DELIVERY_FAILED_PERMANENT(),

  /**
   * The underlying notification sender failed to deliver the message because of invalid token,
   * and all re-attempts should be aborted.
   */
  DELIVERY_FAILED_INVALID_TOKEN(),

  /**
   * Can't find available underlying notification sender for the device type and route hint,
   * and all re-attempts should be aborted.
   */
  DELIVERY_FAILED_NO_SENDER(),

  /**
   * Requested payload is too big.
   */
  DELIVERY_FAILED_MESSAGE_TOO_BIG(),

  /**
   * INVALID API KEY
   */
  DELIVERY_FAILED_INVALID_API_KEY();


//  NotificationResult() {
//    this.closing = closing;
//  }
//
//  public boolean isClosingResult() {
//    return closing;
//  }
}

