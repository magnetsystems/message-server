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

/**
 */
public final class PushConstants {

  private PushConstants() {};

  public static final String KEY_PUSH_MESSAGE_ID = "pushmessageid";
  public static final String PUSH_MESSAGE_TYPE = "ping";
  public static final String ERROR_INVALID_APPID = "Supplied application id is invalid.";
  public static final String ERROR_INVALID_DEVID = "Supplied device id is invalid.";
  public static final String ERROR_INVALID_PUSH_REQUEST = "Supplied push request is invalid.";
  public static final String ERROR_INVALID_PUSH_DEVICE = "Sending push message is not supported for requested device";

  public static final String PUSH_STATUS_OK = "OK";
  public static final String PUSH_STATUS_ERROR = "ERROR";

  public static final String EQUAL = Character.toString('=');
  public static final String QUESTION = Character.toString('?');


}
