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
 */
public final class ErrorMessages {

  private ErrorMessages() {};

  public static final String ERROR_INVALID_DEVICE_TOKEN = "Device token is either missing or invalid";
  public static final String ERROR_INVALID_APNS_CERT = "APNS connection couldn't be established with supplied certificate";
  public static final String ERROR_INVALID_GOOGLE_API_KEY = "Google API Key is invalid (empty or null)";
  public static final String ERROR_INVALID_GCM_TOKEN = "GCM Token is invalid";
  public static final String ERROR_UNKNOWN_APNS_SEND_ISSUE = "APNS Message send failure";
  public static final String ERROR_UNKNOWN_GCM_SEND_ISSUE = "GCM Message send failure";
  public static final String ERROR_APNS_PAYLOAD_SIZE = "Payload exceeds the maximum size allowed";
  public static final String ERROR_APNS_CONNECTION_PROBLEM = "Unable to establish connection. Check your application certificate.";
  public static final String ERROR_GCM_PAYLOAD_SIZE = "Payload exceeds the maximum size allowed";
  public static final String ERROR_SEND_PING_INVALID_TARGET = "Request doesn't define valid targets";
  public static final String ERROR_SEND_PUSH_INVALID_TARGET = "Request doesn't define valid targets";
  public static final String ERROR_SEND_MESSAGE_INVALID_USER_ID_DEVICE_ID = "Request doesn't contain a valid " +
      "user name/deviceId/target information";
  public static final String ERROR_UNDELIVERABLE_TOKEN = "Push service has reported token as being undeliverable";
  public static final String ERROR_INVALID_DEVICE_ID = "Supplied deviceId is invalid or empty";
  public static final String ERROR_DEVICE_NOT_FOUND = "Device with supplied id not found";
  public static final String ERROR_INVALID_DEVICE_SEARCH_CRITERIA = "Request doesn't contain valid device search criteria";
  public static final String ERROR_INVALID_SORT_BY_VALUE = "Supplied sort_by value:%s is invalid";
  public static final String ERROR_INVALID_SORT_ORDER_VALUE = "Supplied sort_order value:%s is invalid";
  public static final String ERROR_INVALID_DEVICE_STATUS_VALUE = "Supplied status value:%s is invalid";
  public static final String ERROR_INVALID_REGISTERED_SINCE_VALUE = "Supplied registered since value:%s is invalid. It should be in ISO 8601 format";
  public static final String ERROR_INVALID_REGISTERED_UNTIL_VALUE = "Supplied registered until value:%s is invalid. It should be in ISO 8601 format";
  public static final String ERROR_INVALID_USERNAME_VALUE = "Supplied username is empty";
  public static final String ERROR_USERNAME_INVALID_LENGTH= "Username must be %d to %d characters long";
  public static final String ERROR_INVALID_PASSWORD_VALUE = "Supplied user password is empty";
  public static final String ERROR_PASSWORD_INVALID_LENGTH= "User password must not exceed %d characters long";
  public static final String ERROR_TOPIC_NOT_FOUND = "Topic with name:%s not found";
  public static final String ERROR_USERNAME_NOT_FOUND = "Username not found";
  public static final String ERROR_USERNAME_LIST_TOO_LONG = "Supplied username list exceeds the maximum allowed. At the most %d names are permitted";
  public static final String ERROR_APNS_CERT_PASSWORD_MISSING = "APNS certificate password is empty or null. APNS certificate password is required";
  public static final String ERROR_TOPIC_PUBLISHING_NOT_ALLOWED = "Topic with name:%s doesn't allow publishing using API";
  public static final String ERROR_TOPIC_INVALID_CONTENT = "Topic message content can't be empty";
  public static final String ERROR_USERNAME_INVALID_CHARACTERS = "Invalid character specified in username";
  public static final String ERROR_USERNAME_EXISTS = "User with username:%s already exists";
  public static final String ERROR_CONFIG_LIST_NULL_OR_EMPTY = "Configuration list in the request is null or empty";
  public static final String ERROR_CONFIG_BAD_KEY = "Configuration contains an invalid key";
  public static final String ERROR_CONFIG_BAD_VALUE = "Configuration contains an invalid value";
  public static final String ERROR_ITEM_ID_LIST_INVALID = "Supplied topic item id list is invalid";
}
