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

import com.magnet.mmx.protocol.DevReg;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXDeviceStats;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXDeviceCountResult;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 */
public interface DeviceDAO {

  public void persist(DeviceEntity entity);
  /**
   * Add a device to the database
   * @param ownerId bare jabber id of the owner of the device
   * @param appId id of the app that is registering the device
   * @param request object with all the necessary device parameters
   * @return
   * @throws DbInteractionException
   */
  public int addDevice (String ownerId, String appId, DevReg request) throws DbInteractionException;

  /**
   * Get a device using the passed in values
   * @param deviceId
   * @param type
   * @param appId
   * @return
   * @throws DbInteractionException
   */
  public DeviceEntity getDevice(String deviceId, OSType type, String appId) throws DbInteractionException;

  /**
   * Update the device information. Device to be updated is identified by the deviceId,
   * type, and appId value.
   * @param deviceId not null deviceId
   * @param type not null type
   * @param appId appId  which the device is associated with
   * @param update not null request object with updated information  @throws DbInteractionException
   * @param ownerId the updated ownerId
   * @param status  updated device status
   * @return the number of devices updated
   */
  public int updateDevice(String deviceId, OSType type, String appId, DevReg update, String ownerId, DeviceStatus status) throws DbInteractionException;


  public int deactivateDevice(String deviceId) throws DbInteractionException;

  /**
   * Get devices for the specified appkey, userId and device status
   * @param appId
   * @param userId
   * @param status
   * @return List<DeviceEntity> can be empty but not null
   */
  public List<DeviceEntity> getDevices (String appId, String userId, DeviceStatus status);

  /**
   * Get a device using the appId,userId and deviceId
   * @param appId
   * @param userId
   * @param deviceId
   * @param status
   * @return DeviceEntity
   */
  public DeviceEntity getDeviceUsingId(String appId, String userId, String deviceId, DeviceStatus status);

  /**
   * Get a device using appId,deviceId, and status
   * @param appId
   * @param deviceId
   * @param status
   * @return
   */
  public DeviceEntity getDeviceUsingId(String appId, String deviceId, DeviceStatus status);

  public DeviceEntity getDevice(String appId, String deviceId) throws SQLException, DeviceNotFoundException;

  public MMXDeviceCountResult getMMXDeviceCountByAppId(String appId, String ownerJid);

  /**
   * Search devices with the supplied information.
   * @param appId
   * @param searchOption
   * @param valueHolder
   * @param sortOption
   * @param info
   * @return
   */
  public SearchResult<DeviceEntity> searchDevices(String appId, DeviceSearchOption searchOption, ValueHolder valueHolder,
                                                    DeviceSortOption sortOption, PaginationInfo info);


  public Map<String, MMXDeviceStats> getDeviceStats(List<String> appIdList);

  public List<String> getOwnerIdsByDeviceIdAndAppId(String appId, String deviceId);

  public int getActiveDevicesForApp(String appId);

  public List<DeviceEntity> getDevices (String appId, List<String> deviceIds, DeviceStatus status);

  /**
   * Get all devices with a specific tag
   */

  public List<DeviceEntity> getDevicesForTag(String tagName, String appId);

  /**
   * If tagnames = {"tagA", "tagB", "tagC"}
   * return device list is of devices with "tagA" OR "tagB" OR "tagC"
   * @param tagNames
   * @param appId
   * @return
   */

  public List<DeviceEntity> getDevicesForTagORed(List<String> tagNames, String appId);

  public List<DeviceEntity> getDevicesForTagANDed(List<String> tagNames, String appId);

  public List<DeviceEntity> getDevicesByAppId(String appId);


  public List<DeviceEntity> getDevices(QueryBuilderResult query);

  /**
   * Change the push status to invalid for the resource identified by the passed in params.
   * @param appId
   * @param pushType
   * @param token
   */
  public void invalidateToken (String appId, PushType pushType, String token);


  public void updatePushStatus(String deviceId, OSType type, String appId, PushStatus status) throws DbInteractionException;

  /**
   * Search devices by executing the query and the paginate the results using the pagination information.
   * @param query
   * @param pinfo
   * @return
   */
  public SearchResult<DeviceEntity> searchDevices(QueryBuilderResult query, PaginationInfo pinfo);
}
