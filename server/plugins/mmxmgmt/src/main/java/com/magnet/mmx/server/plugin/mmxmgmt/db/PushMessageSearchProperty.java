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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

/**
 */
public enum PushMessageSearchProperty {

  /**
   * deviceId
   */
  DEVICEID,
  /**
   * When was it sent
   */
  SENT,
  /**
   * When was it acknowledged
   */
  ACK,
  /**
   * What state it is in
   */
  STATE,
  /**
   * Type of push message
   */
  TYPE,

  ;

  /**
   * Get a PushMessageSearch Property for the specified key. The key is compared
   * case insensitively.
   * @param key
   * @return PushMessageSearchProperty for the supplied key. null if not found.
   */
  public static PushMessageSearchProperty find(String key) {
    if (key == null) {
      return null;
    }
    PushMessageSearchProperty rv = null;
    PushMessageSearchProperty[] values = PushMessageSearchProperty.values();
    int size = values.length;
    for (int i = 0; i < size && rv == null; i++) {
      PushMessageSearchProperty prop = values[i];
      if (prop.name().equalsIgnoreCase(key)) {
        rv = prop;
      }
    }
    return rv;
  }

}
