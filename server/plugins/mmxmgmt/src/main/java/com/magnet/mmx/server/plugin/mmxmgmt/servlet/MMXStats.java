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

import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class MMXStats {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXStats.class);
  List<String> appIdList;
  List<MMXInAppMessageStats> appMessagePerAppStats;
  List<MMXDeviceStats> deviceAppStats;
  List<MMXPushMessageStats> pushMessageStats;

  public MMXStats(List<String> appIdList) {
    this.appIdList = appIdList;
  }

  public MMXAppStats[] getAppStats() {
    List<MMXAppStats> mmxAppStats = new ArrayList<MMXAppStats>();

    Map<String, MMXInAppMessageStats> inAppMessageStats = getMessageDao().getMessageStats(appIdList);
    Map<String, MMXPushMessageStats> pushMessageStats = getPushMessageDao().getPushMessageStats(appIdList);
    LOGGER.trace("getAppStats : getting device stats");
    Map<String, MMXDeviceStats> deviceStats = getDeviceDao().getDeviceStats(appIdList);

    for(String appId : appIdList) {
      MMXAppStats appStats = new MMXAppStats(appId, inAppMessageStats.get(appId),
                                              pushMessageStats.get(appId), deviceStats.get(appId));
      mmxAppStats.add(appStats);
    }

    MMXAppStats[] mmxAppStatsArr = new MMXAppStats[mmxAppStats.size()];
    mmxAppStats.toArray(mmxAppStatsArr);
    return mmxAppStatsArr;
  }

  public MMXAppStats getFirst() {
    List<MMXAppStats> mmxAppStats = new ArrayList<MMXAppStats>();

    Map<String, MMXInAppMessageStats> inAppMessageStats = getMessageDao().getMessageStats(appIdList);
    Map<String, MMXPushMessageStats> pushMessageStats = getPushMessageDao().getPushMessageStats(appIdList);
    Map<String, MMXDeviceStats> deviceStats = getDeviceDao().getDeviceStats(appIdList);

    for(String appId : appIdList) {
      MMXAppStats appStats = new MMXAppStats(appId, inAppMessageStats.get(appId),
              pushMessageStats.get(appId), deviceStats.get(appId));
      mmxAppStats.add(appStats);
    }

    MMXAppStats[] mmxAppStatsArr = new MMXAppStats[mmxAppStats.size()];
    mmxAppStats.toArray(mmxAppStatsArr);
    return mmxAppStats.get(0);
  }

  private MessageDAO getMessageDao() {
    return new MessageDAOImpl(new OpenFireDBConnectionProvider());
  }

  private PushMessageDAO getPushMessageDao() {
    return new PushMessageDAOImpl(new OpenFireDBConnectionProvider());
  }

  private DeviceDAO getDeviceDao() {
    return new DeviceDAOImpl(new OpenFireDBConnectionProvider());
  }

}
