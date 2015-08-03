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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXClusterableTask;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * APNS Feedback processor queries APNS to retrieve information about failed push notifications.
 * PushStatus for Devices for which APNS reports a failure is changed to INVALID.
 */
public class APNSFeedbackProcessor extends MMXClusterableTask implements Runnable {
  static final String THREAD_POOL_NAME = "APNSFeedbackProcessor";
  static final int THREAD_POOL_SIZE = 5;
  private Logger LOGGER = LoggerFactory.getLogger(APNSFeedbackProcessor.class);

  public APNSFeedbackProcessor(Lock lock) {
    super(lock);
  }

  @Override
  public void run() {
    if(!canExecute()) {
      LOGGER.trace("APNSFeedbackProcessor.run() : Unable to acquire clustered lock, not running");
      return;
    }

    LOGGER.debug("APNSFeedbackProcessor.run() : Successfully acquired APNSFeedbackProcessor lock");

    ExecutorService executorService = MMXExecutors.getOrCreate(THREAD_POOL_NAME, THREAD_POOL_SIZE);
    long startTime =  System.nanoTime();
    AppDAO appDAO = new AppDAOImpl(getConnectionProvider());
    List<AppEntity> apps = appDAO.getAllApps();

    for (AppEntity app : apps) {
      if (app.getApnsCert() != null) {
        MMXAppAPNSFeedbackProcessor processor = new MMXAppAPNSFeedbackProcessor(getConnectionProvider(), app.getAppId(), app.isApnsCertProduction());
        executorService.submit(processor);
      }
    }
    long endTime = System.nanoTime();
    LOGGER.info("Completed run execution in {} milliseconds",
        TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS));
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

}
