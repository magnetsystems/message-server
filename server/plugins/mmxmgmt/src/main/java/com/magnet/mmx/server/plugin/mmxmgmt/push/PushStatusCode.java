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
public enum PushStatusCode {
  SUCCESSFUL(200, "Successful"),
  BAD_APP_ID(400, "Bad app id"),
  BAD_DEVICE_ID(400, "Bad decvice id"),
  BAD_COMMAND_VALUE (400, "Invalid command value"),
  INVALID_APP_ID (403, "Invalid app id"),
  INVALID_DEVICE_ID (403, "Invalid deviceId"),
  INVALID_USER_DEVICE_PAIR(403, "Invalid user-device pair"),
  INVALID_PUSH_SERVICE_KEY(403, "Push service key not found for specified app"),
  INVALID_TOKEN_FOR_DEVICE (403, "Device not push capable"),
  ERROR_SENDING_PUSH(405, "Push service error"),
  EXCEEDED_PUSH_MESSAGE_RATE(403, "Push message rate exceeded")
  ;
  private int code;
  private String message;

  PushStatusCode(int c, String m) {
    code = c;
    message = m;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
