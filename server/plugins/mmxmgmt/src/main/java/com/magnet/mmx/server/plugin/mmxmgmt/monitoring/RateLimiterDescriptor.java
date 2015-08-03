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

package com.magnet.mmx.server.plugin.mmxmgmt.monitoring;

public class RateLimiterDescriptor {
  String type;
  String appId;
  long permitsPerSecond = -1;

  public RateLimiterDescriptor(String type, String appId, long permitsPerSecond) {
    this.type = type;
    this.appId = appId;
    this.permitsPerSecond = permitsPerSecond;
  }

  public String getType() {
    return type;
  }

  public long getPermitsPerSecond() {
    return permitsPerSecond;
  }

  public String getAppId() {
    return appId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RateLimiterDescriptor)) return false;

    RateLimiterDescriptor that = (RateLimiterDescriptor) o;

    if (permitsPerSecond != that.permitsPerSecond) return false;
    if (appId != null ? !appId.equals(that.appId) : that.appId != null) return false;
    if (!type.equals(that.type)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + (appId != null ? appId.hashCode() : 0);
    result = 31 * result + (int) (permitsPerSecond ^ (permitsPerSecond >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "RateLimiterDescriptor{" +
            "type='" + type + '\'' +
            ", appId='" + appId + '\'' +
            ", permitsPerSecond=" + permitsPerSecond +
            '}';
  }
}
