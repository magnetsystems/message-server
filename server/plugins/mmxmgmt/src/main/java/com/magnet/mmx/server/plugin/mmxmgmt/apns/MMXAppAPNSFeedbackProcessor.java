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
  private ConnectionProvider provider;
  private String appId;
  private boolean productionCert;

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
    long startTime = System.nanoTime();
    APNSConnection connection = getAPNSConnection(appId, productionCert);

    List<String> tokens = connection.getInactiveDeviceTokens();
    DeviceDAO deviceDAO = new DeviceDAOImpl(provider);
    int count = 0;
    DevicePushTokenInvalidator invalidator = new DevicePushTokenInvalidator();
    for (String token : tokens) {
      invalidator.invalidateToken(appId, PushType.APNS, token);
      count++;
    }
    LOGGER.info("Invalidated:{} tokens for appId:{}", count, appId);

    MMXAppAPNSFeedbackProcessResult result = new MMXAppAPNSFeedbackProcessResult();
    result.setInvalidatedCount(count);
    result.setAppId(appId);
    result.setProductionApnsCert(productionCert);
    long endTime = System.nanoTime();

    LOGGER.info("Completed processing APNS feedback for appId:{} in {} milliseconds", appId,
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));

    return result;
  }

  protected APNSConnection getAPNSConnection(String appId, boolean production) {
    APNSConnectionPool pool = APNSConnectionPoolImpl.getInstance();
    APNSConnection connection = pool.getConnection(appId, production);
    return connection;
  }


}
