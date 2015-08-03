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
package com.magnet.mmx.server.plugin.mmxmgmt.wakeup;

import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 */
public class RetryCheckExecutionManagerImpl implements RetryCheckExecutionManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(RetryCheckExecutionManagerImpl.class);
  private static final String POOL_NAME ="RetryCheck";
  private static final String RETRY_CHECK_LOCK_KEY = "retryCheckLockKey";
  private Lock lock;

  private final int threadPoolSize = 1;
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(threadPoolSize, new SchedulerThreadFactory(POOL_NAME));

  @Override
  public void startRetryCheck(int initialDelay, int interval) {
    try {
      LOGGER.trace("startRetryCheck : creating lock");
      lock = CacheFactory.getLock(RETRY_CHECK_LOCK_KEY);
    } catch (Exception e) {
      LOGGER.error("startRetryCheck : caught exception getting lock instance");
    }
    LOGGER.info("scheduling retry check processor");
    scheduler.scheduleAtFixedRate(new RetryProcessor(lock), initialDelay, interval, TimeUnit.SECONDS);
  }

  @Override
  public void stopRetryCheck() {
    LOGGER.info("stopping retry processor");
    scheduler.shutdown();
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
