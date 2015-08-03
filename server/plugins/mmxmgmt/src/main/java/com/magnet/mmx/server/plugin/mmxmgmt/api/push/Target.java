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
package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.UserQuery;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class Target {

  private List<String> deviceIds;

  private DeviceQuery deviceQuery;

  private UserQuery userQuery;

  public UserQuery getUserQuery() {
    return userQuery;
  }

  public void setUserQuery(UserQuery userQuery) {
    this.userQuery = userQuery;
  }

  public Target() {
    deviceIds = new ArrayList<String>(10);
  }

  public List<String> getDeviceIds() {
    return deviceIds;
  }

  public void setDeviceIds(List<String> deviceIds) {
    this.deviceIds = deviceIds;
  }

  public DeviceQuery getDeviceQuery() {
    return deviceQuery;
  }

  public void setDeviceQuery(DeviceQuery deviceQuery) {
    this.deviceQuery = deviceQuery;
  }
}
