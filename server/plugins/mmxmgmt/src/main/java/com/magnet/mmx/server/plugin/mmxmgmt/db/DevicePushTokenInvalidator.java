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


import com.magnet.mmx.protocol.PushType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Device Token invalidator that takes care of invalidating a device token.
 * When a device token is invalidated following things are done
 * 1. push status for device is set to INVALID in mmxDevice table
 * 2. Any queued wakeup request for that device are deleted.
 * 3. State of messages associated with those queued requests is changed from WAKEUP_REQUIRED to PENDING
 */
public class DevicePushTokenInvalidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevicePushTokenInvalidator.class);


  /**
   * Invalidate token
   * @param appId
   * @param pushType
   * @param token
   */
  public void invalidateToken (String appId, PushType pushType, String token) {
    LOGGER.info("invalidateToken: Invalidating token for appId:{} Push Type:{} Token:{}", appId, pushType.name(), token);
    long startTime = System.nanoTime();

    DeviceDAO deviceDAO = getDeviceDAO();
    deviceDAO.invalidateToken(appId, pushType, token);

    MessageDAO messageDAO = getMessageDAO();
    int messageCount = messageDAO.changeStateToPending(appId, pushType, token);

    WakeupEntityDAO wakeupEntityDAO = getWakeupEntityDAO();
    int wkCount = wakeupEntityDAO.remove(appId, pushType, token);
    LOGGER.info("Counts.. message:{} queued wakeup:{}", messageCount, wkCount);
    long endTime = System.nanoTime();
    LOGGER.info("invalidated token for appId:{} Push Type:{} Token:{} in {} milliseconds", appId, pushType.name(), token,
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  protected DeviceDAO getDeviceDAO() {
    return new DeviceDAOImpl(getConnectionProvider());
  }

  protected MessageDAO getMessageDAO() {
    return new MessageDAOImpl(getConnectionProvider());
  }

  protected WakeupEntityDAO getWakeupEntityDAO() {
    return new WakeupEntityDAOImpl(getConnectionProvider());
  }

}
