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

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MMXPushHeader {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXPushHeader.class);
  String namespace;
  String actionCode;
  String type = "";

  public MMXPushHeader(String namespace, String actionCode, String type) {
    LOGGER.trace("MMXPushHeader : namespace={}, actionCode={}, type={}", new Object[]{namespace, actionCode, type});
    if(Strings.isNullOrEmpty(namespace) ||
            Strings.isNullOrEmpty(actionCode) ||
            Strings.isNullOrEmpty(type)) {
      throw new IllegalArgumentException("Arguments cannot be null or empty");
    }
    this.namespace = namespace;
    this.actionCode = actionCode;
    this.type = type;
  }

  public MMXPushHeader(String namespace, String actionCode) {
    if(Strings.isNullOrEmpty(namespace) ||
            Strings.isNullOrEmpty(actionCode)) {
      throw new IllegalArgumentException("Arguments cannot be null or empty");
    }
    this.namespace = namespace;
    this.actionCode = actionCode;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getActionCode() {
    return actionCode;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return this.toString(true);
  }

  /**
   * String representation of the header
   * @param includeDelimiter flag to indicate if the suffix delimiter should be included or not.
   * @return
   */
  public String toString(boolean includeDelimiter) {
    StringBuffer buf = new StringBuffer();
    buf.append(namespace + ":" + actionCode);
    if(type != null)
      buf.append(":" + type);
    if (includeDelimiter) {
      buf.append("\r\n");
    }
    return buf.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXPushHeader)) return false;

    MMXPushHeader that = (MMXPushHeader) o;

    if (!actionCode.equals(that.actionCode)) return false;
    if (!namespace.equals(that.namespace)) return false;
    if (!type.equals(that.type)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = namespace.hashCode();
    result = 31 * result + actionCode.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}
