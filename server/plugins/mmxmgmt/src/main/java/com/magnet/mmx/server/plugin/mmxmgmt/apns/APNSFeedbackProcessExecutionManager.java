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

import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXExecutors;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.SchedulerThreadFactory;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * APNSFeedbackProcessExecutionManager - This has the scheduler that runs the Feedback process
 * at specified frequency.
 */
public class APNSFeedbackProcessExecutionManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(APNSFeedbackProcessExecutionManager.class);
  private static final String POOL_NAME = "APNSFeedbackProcessExecutionManager";
  private static final String APNS_FEEDBACK_TASK_KEY = "apnsFeedbackTaskKey";
  private Lock lock;

  private final int threadPoolSize = 1;
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(threadPoolSize, new SchedulerThreadFactory(POOL_NAME));

  public void start(int initialDelayMinutes, int intervalMinutes) {
    try {
      LOGGER.trace("start : creating lock");
      lock = CacheFactory.getLock(APNS_FEEDBACK_TASK_KEY);
    } catch (Exception e) {
      LOGGER.error("start : caught exception getting lock instance");
    }
    LOGGER.info("scheduling APNSFeedbackProcessExecutionManager. initialDelay(min):{} interval(min):{}",
        initialDelayMinutes, intervalMinutes);
    scheduler.scheduleAtFixedRate(new APNSFeedbackProcessor(lock), initialDelayMinutes, intervalMinutes, TimeUnit.MINUTES);
  }

  public void stop() {
    LOGGER.info("stopping APNSFeedbackProcessExecutionManager");
    scheduler.shutdown();
    /**
     * Also shutdown the thread pool used for per app feedback processing.
     */
    ExecutorService service = MMXExecutors.getOrCreate(APNSFeedbackProcessor.THREAD_POOL_NAME,
        APNSFeedbackProcessor.THREAD_POOL_SIZE);
    service.shutdown();
    MMXExecutors.remove(APNSFeedbackProcessor.THREAD_POOL_NAME);
    releaseLock();
  }

  private void releaseLock() {
    if(lock != null) {
      try {
        lock.unlock();
      } catch (Exception e) {
        LOGGER.error("releaseLock : Ignorable - caught exception releasing clustered lock", e);
      }
    }
  }
}
