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
package com.magnet.mmx.server.plugin.mmxmgmt.event;

/**
 */
public class MMXMaxDevicesPerAppLimitReachedEvent extends MMXAppEvent {
  int limit;

  public MMXMaxDevicesPerAppLimitReachedEvent(String appId, int limit) {
    super(appId);
    this.limit = limit;
  }

  public int getLimit() {
    return limit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXMaxDevicesPerAppLimitReachedEvent)) return false;
    if (!super.equals(o)) return false;

    MMXMaxDevicesPerAppLimitReachedEvent that = (MMXMaxDevicesPerAppLimitReachedEvent) o;

    if (limit != that.limit) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + limit;
    return result;
  }

  @Override
  public String toString() {
    return "MMXMaxAppLimitReachedEvent{" +
            "limit=" + limit +
            "} " + super.toString();
  }
}
