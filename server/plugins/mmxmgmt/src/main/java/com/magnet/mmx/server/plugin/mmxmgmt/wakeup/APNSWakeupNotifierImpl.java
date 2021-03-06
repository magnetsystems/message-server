/*   Copyright (c) 2015-2016 Magnet Systems, Inc.
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
package com.magnet.mmx.server.plugin.mmxmgmt.wakeup;

import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnection;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnectionException;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnectionPool;
import com.magnet.mmx.server.plugin.mmxmgmt.apns.APNSConnectionPoolImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class APNSWakeupNotifierImpl implements WakeupNotifier {
  private static Logger LOGGER = LoggerFactory.getLogger(APNSWakeupNotifierImpl.class);

  @Override
  public List<NotificationResult> sendNotification(List<String> deviceTokens, String payload, NotificationSystemContext context) {

    if (context == null || !(context instanceof APNSNotificationSystemContext)) {
      throw new IllegalArgumentException("Context has to be instance of APNSNotificationSystemContext");
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("JSON Payload for the APNS wakeup notification:{}" , payload);
    }
    APNSNotificationSystemContext apnsContext = (APNSNotificationSystemContext) context;
    boolean apnsCertProduction = apnsContext.isApnsCertProduction();
    String appId = apnsContext.getAppId();
    APNSConnectionPool connectionPool = APNSConnectionPoolImpl.getInstance();
    APNSConnection connection = null;
    List<NotificationResult> results = new ArrayList<NotificationResult>(deviceTokens.size());
    try {
      connection = connectionPool.getConnection(appId, apnsCertProduction);
      if (connection == null) {
        LOGGER.warn("No APNS connection available; maybe the certification is misconfigured.");
        results.add(NotificationResult.DELIVERY_FAILED_PERMANENT);
      } else {
        for (String token : deviceTokens) {
          try {
            connection.send(token, payload);
            results.add(NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER);
          } catch (APNSConnectionException e) {
            LOGGER.warn("Exception in sending APNS wakeup notification", e);
            results.add(NotificationResult.DELIVERY_FAILED_PERMANENT);
          } catch (Throwable e) {
            LOGGER.error("Unexpected exception in sending APNS wakeup notification", e);
            results.add(NotificationResult.DELIVERY_FAILED_PERMANENT);
          }
        }
      }
    } finally {
      if (connection != null) {
        //return the connection back to pool
        connectionPool.returnConnection(connection);
      }
    }
    return results;
  }

  public static class APNSNotificationSystemContext implements NotificationSystemContext {
    private final String appId;
    private final boolean apnsCertProduction;

    /**
     *
     * @param appId
     * @param apnsCertProduction -- production apns cert?
     */
    public APNSNotificationSystemContext(String appId, boolean apnsCertProduction) {
      this.appId = appId;
      this.apnsCertProduction = apnsCertProduction;
    }

    public String getAppId() {
      return appId;
    }

    public boolean isApnsCertProduction() {
      return apnsCertProduction;
    }
  }
}
