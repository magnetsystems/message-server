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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Simple thread factory that add the supplied poolname to each thread created
 * by the factory.
 */
public class SchedulerThreadFactory implements ThreadFactory {
  private String poolName;

  public SchedulerThreadFactory(String poolName) {
    this.poolName = poolName;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = Executors.defaultThreadFactory().newThread(r);
    String name = thread.getName();
    thread.setName(poolName + "-" + name);
    return thread;
  }
}
