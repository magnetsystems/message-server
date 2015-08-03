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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 */
public class AlertsUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertsUtil.class);

  public static boolean maxAppsLimitReached(String ownerId) {
    List<String> appids = DBUtil.getAppDAO().getAllAppIds(ownerId);
    int maxAppPerCluster = getMaxAppLimit();

    if(maxAppPerCluster > 0 && appids != null && appids.size() >= maxAppPerCluster) {
      LOGGER.trace("maxAppsLimitReached : {}", maxAppPerCluster);
      return true;
    }
    return false;
  }

  public static boolean maxDevicesPerAppLimitReached(String appId, String ownerJid) {
    MMXDeviceCountResult result = DBUtil.getDeviceDAO().getMMXDeviceCountByAppId(appId, ownerJid);
    int devicePerAppLimit = getMaxDevicePerAppLimit();

    LOGGER.trace("maxDevicesPerAppLimitReached : result = {}, devicePerAppLimit = {}", result, devicePerAppLimit);

    if(devicePerAppLimit > 0 && result.getDevicesPerApp() >= devicePerAppLimit) {
      LOGGER.trace("maxDevicesPerAppLimitReached : {}", devicePerAppLimit);
      return true;
    }
    return false;
  }

  public static int getMaxXmppRate() {
    return get(MMXConfigKeys.MAX_XMPP_RATE, MMXServerConstants.DEFAULT_MAX_XMPP_RATE);
  }

  public static int getMaxHttpRate() {
    return get(MMXConfigKeys.MAX_HTTP_RATE, MMXServerConstants.DEFAULT_MAX_HTTP_RATE);
  }

  public static int getMaxAppLimit() {
    return get(MMXConfigKeys.MAX_APP_PER_OWNER, MMXServerConstants.DEFAULT_MAX_APP_PER_OWNER);
  }

  public static int getMaxDevicePerAppLimit() {
    return get(MMXConfigKeys.MAX_DEVICES_PER_APP, MMXServerConstants.DEFAULT_MAX_DEVICES_PER_APP);
  }

  private static int get(String key, int defaultValue) {
    return MMXConfiguration.getConfiguration().getInt(key, defaultValue);
  }

}
