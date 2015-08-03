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
package com.magnet.mmx.server.plugin.mmxmgmt.servlet;

/**
 */
public class MMXDeviceStats {
  private String appId;
  private int numDevices=0;
  private int totalActive=0;
  private int totalInActive=0;

  public MMXDeviceStats(String appId) {
    this.appId = appId;
  }

  public int getNumDevices() {
    return numDevices;
  }

  public void setNumDevices(int numDevices) {
    this.numDevices = numDevices;
  }

  public int getTotalActive() {
    return totalActive;
  }

  public void setTotalActive(int totalActive) {
    this.totalActive = totalActive;
  }

  public int getTotalInActive() {
    return totalInActive;
  }

  public void setTotalInActive(int totalInActive) {
    this.totalInActive = totalInActive;
  }

  public void incrementActive() {
    totalActive++;
  }

  public void incrementInactive() {
    totalInActive++;
  }

  public void incrementNumDevices() {
    numDevices++;
  }

  @Override
  public String toString() {
    return "MMXDeviceStats{" +
            "appId='" + appId + '\'' +
            ", numDevices=" + numDevices +
            ", totalActive=" + totalActive +
            ", totalInActive=" + totalInActive +
            '}';
  }
}
