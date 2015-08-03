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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Composite entity composed of a device and a user.
 */
public class EndPointEntity {
  private DeviceEntity device;
  private UserEntity userEntity;

  public DeviceEntity getDevice() {
    return device;
  }

  public void setDevice(DeviceEntity device) {
    this.device = device;
  }

  public UserEntity getUserEntity() {
    return userEntity;
  }

  public void setUserEntity(UserEntity userEntity) {
    this.userEntity = userEntity;
  }

  public static class EndPointEntityBuilder {
    public EndPointEntity build(ResultSet rs, String userPrefix, String endpointPrefix) throws SQLException {
      EndPointEntity entity = new EndPointEntity();
      UserEntity user = new UserEntity.UserEntityBuilder().build(rs, userPrefix, true);
      DeviceEntity device = new DeviceEntity.DeviceEntityBuilder().buildLimited(rs, endpointPrefix);
      entity.setDevice(device);
      entity.setUserEntity(user);
      return entity;
    }
  }
}
