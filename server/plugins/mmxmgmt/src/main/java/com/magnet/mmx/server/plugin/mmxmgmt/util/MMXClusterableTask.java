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

import org.jivesoftware.openfire.cluster.ClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;

/**
 */
public abstract class MMXClusterableTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXClusterableTask.class);
  private Lock lock;

  public MMXClusterableTask(Lock lock) {
    this.lock = lock;
  }

  protected boolean canExecute() {
    if(ClusterManager.isClusteringEnabled()) {
      LOGGER.debug("canExecute : clustering is enabled");
      try {
        if(lock != null) {
          boolean result = lock.tryLock();
          LOGGER.trace("canExecute : tryLockResult={}", result);
          return result;
        } else {
          LOGGER.debug("canExecute : lock has not been set, cannot execute task");
          return false;
        }
      } catch (Exception e) {
        LOGGER.error("canExecute : error trying to acquire distributed lock", e);
        return false;
      }
    } else {
      LOGGER.debug("canExecute : clustering is disabled return true");
      return true;
    }
  }
}
