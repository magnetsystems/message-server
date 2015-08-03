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
public class MMXHttpRateExceededEvent extends MMXAppEvent {
  private int rate;

  public MMXHttpRateExceededEvent(String appId, int rate) {
    super(appId);
    this.rate = rate;
  }

  public int getRate() {
    return rate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MMXHttpRateExceededEvent)) return false;
    if (!super.equals(o)) return false;

    MMXHttpRateExceededEvent that = (MMXHttpRateExceededEvent) o;

    if (rate != that.rate) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + rate;
    return result;
  }

  @Override
  public String toString() {
    return "MMXHttpRateExceededEvent{" +
            "rate=" + rate +
            "} " + super.toString();
  }
}
