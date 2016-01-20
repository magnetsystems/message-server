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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DevicePushTokenInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Per App feedback processor.
 */
public class MMXAppAPNSFeedbackProcessor implements Callable<MMXAppAPNSFeedbackProcessResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAppAPNSFeedbackProcessor.class);
  private final ConnectionProvider provider;
  private final String appId;
  private final boolean productionCert;

  /**
   * Constructor.
   *
   * @param provider
   * @param appId
   * @param productionCert
   */
  public MMXAppAPNSFeedbackProcessor(ConnectionProvider provider, String appId, boolean productionCert) {
    this.provider = provider;
    this.appId = appId;
    this.productionCert = productionCert;
  }

  @Override
  public MMXAppAPNSFeedbackProcessResult call() throws Exception {
    int count = 0;
    APNSConnection connection = null;
    long startTime = System.nanoTime();

    try {
      connection = getAPNSConnection(appId, productionCert);
      if (connection == null) {
        LOGGER.warn("Unable to invalidate tokens for appId:{}; no APNS "+
            "connection available.  Is APNS certification misconfigured?", appId);
      } else {
        List<String> tokens = connection.getInactiveDeviceTokens();
        DevicePushTokenInvalidator invalidator = new DevicePushTokenInvalidator();
        for (String token : tokens) {
          invalidator.invalidateToken(appId, PushType.APNS, token);
          count++;
        }
        LOGGER.info("Invalidated:{} tokens for appId:{}", count, appId);
      }
    } finally {
      // Fix MAX-40 for APNS connection leak.
      if (connection != null) {
        returnConnection(connection);
      }
    }
    long endTime = System.nanoTime();

    MMXAppAPNSFeedbackProcessResult result = new MMXAppAPNSFeedbackProcessResult();
    result.setInvalidatedCount(count);
    result.setAppId(appId);
    result.setProductionApnsCert(productionCert);

    LOGGER.info("Completed processing APNS feedback for appId:{} in {} milliseconds", appId,
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));

    return result;
  }

  protected APNSConnection getAPNSConnection(String appId, boolean production) {
    APNSConnectionPool pool = APNSConnectionPoolImpl.getInstance();
    APNSConnection connection = pool.getConnection(appId, production);
    return connection;
  }

  protected void returnConnection (APNSConnection connection) {
    APNSConnectionPool connectionPool = APNSConnectionPoolImpl.getInstance();
    connectionPool.returnConnection(connection);
  }
}
