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

import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.push.ResolutionException;

import java.util.List;

/**
 * Resolves Device Entity targets
 */
public class DeviceTargetResolver implements TargetResolver<DeviceEntity> {

  @Override
  public List<DeviceEntity> resolve(String appId, Target target) throws ResolutionException {
    if (target == null) {
      throw new ResolutionException("no valid targets defined");
    }
    List<String> deviceIds = target.getDeviceIds();
    if (deviceIds != null && !deviceIds.isEmpty()) {
      DeviceDAO dao = getDeviceDAO();
      return dao.getDevices(appId, deviceIds, DeviceStatus.ACTIVE);
    } else if (target.getDeviceQuery() != null) {
      DeviceQueryBuilder builder = new DeviceQueryBuilder();
      QueryBuilderResult query = builder.buildQuery(target.getDeviceQuery(), appId);
      DeviceDAO deviceDAO = getDeviceDAO();
      List<DeviceEntity> entityList = deviceDAO.getDevices(query);
      return entityList;
    } else {
      throw new ResolutionException("no valid targets defined");
    }
  }


  protected DeviceDAO getDeviceDAO() {
    DeviceDAO dao = new DeviceDAOImpl(getConnectionProvider());
    return dao;
  }


  protected ConnectionProvider getConnectionProvider() {
    ConnectionProvider provider = new OpenFireDBConnectionProvider();
    return provider;
  }
}
