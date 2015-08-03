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
package com.magnet.mmx.server.plugin.mmxmgmt.api;

/**
 * Enum for the error codes.
 */
public enum ErrorCode {
  SEND_MESSAGE_ISE (10),
  AUTH_BAD_APP_ID (11),
  AUTH_APPID_APIKEY_MISMATCH(12),
  AUTH_MISSING (13),
  SEND_PUSH_MESSAGE_ISE (14),
  DEVICE_MISSING_TOKEN (15),
  APNS_SEND_FAILURE (16),
  APNS_INVALID_CERTIFICATE (17),
  GCM_INVALID_GOOGLE_API_KEY(18),
  GCM_INVALID_TOKEN (19),
  GCM_SEND_FAILURE (20),
  APNS_SIZE_EXCEEDED(21),
  GCM_SIZE_EXCEEDED(22),
  SEND_PING_MESSAGE_ISE (23),
  SEND_PING_MESSAGE_BAD_REQUEST(24),
  SEND_PUSH_MESSAGE_BAD_REQUEST(25),
  APNS_CONNECTION_EXCEPTION(26),
  SEND_MESSAGE_NO_TARGET(27),
  GET_MESSAGE_BY_ID_ISE(28),
  SEARCH_TOPIC_BAD_APP_ID(29),
  SEARCH_TOPIC_ISE(30),
  ILLEGAL_ARGUMENT(31),
  APP_OWNER_ID_MISSING (32),
  AUTH_APPID_OWNERID_MISMATCH(33),
  APNS_INVALID_TOKEN (34),
  INVALID_DEVICE_ID(35),
  ISE_DEVICES_GET_BY_ID(36),
  ISE_DEVICES_SEARCH(37),
  INVALID_DEVICE_SEARCH_CRITERIA(38),
  INVALID_SORT_BY_VALUE(39),
  INVALID_SORT_ORDER_VALUE(40),
  INVALID_DEVICE_STATUS_VALUE(41),
  INVALID_DEVICE_REGISTERED_SINCE_VALUE(42),
  INVALID_DEVICE_REGISTERED_UNTIL_VALUE(43),
  LIST_SUBSCRIPTION_ISE(44),
  POST_TOPIC_MESSAGE_ISE(45),
  TOPIC_SUMMARY_INVALID_TOPIC_NAME(46),
  UNKNOWN_ERROR(47),
  INVALID_USER_NAME(48),
  INVALID_USER_PASSWORD(49),
  SEND_MESSAGE_USERNAME_LIST_TOO_BIG(50),
  APNS_PASSWORD_MISSING(51),
  TOPIC_PUBLISH_FORBIDDEN (52),
  TOPIC_ITEMS_BY_ID(53),
  RATE_LIMIT_EXCEEDED((54))
  ;

  private int code;

  private ErrorCode (int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }

}
