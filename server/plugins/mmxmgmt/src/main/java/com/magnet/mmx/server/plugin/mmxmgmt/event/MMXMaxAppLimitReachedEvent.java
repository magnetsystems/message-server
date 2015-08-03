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
public class MMXMaxAppLimitReachedEvent extends MMXEvent {
  int limit;
  String ownerId;

  public MMXMaxAppLimitReachedEvent(int limit, String ownerId) {
    this.limit = limit;
    this.ownerId = ownerId;
  }

  public int getLimit() {
    return limit;
  }

  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXMaxAppLimitReachedEvent)) return false;
    if (!super.equals(o)) return false;

    MMXMaxAppLimitReachedEvent that = (MMXMaxAppLimitReachedEvent) o;

    if (limit != that.limit) return false;
    if (!ownerId.equals(that.ownerId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + limit;
    result = 31 * result + ownerId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "MMXMaxAppLimitReachedEvent{" +
            "limit=" + limit +
            ", ownerId='" + ownerId + '\'' +
            "} " + super.toString();
  }
}
