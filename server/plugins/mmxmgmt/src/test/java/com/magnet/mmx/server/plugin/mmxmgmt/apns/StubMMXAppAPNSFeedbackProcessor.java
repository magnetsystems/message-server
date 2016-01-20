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

import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;

import java.util.List;

/**
 */
public class StubMMXAppAPNSFeedbackProcessor extends MMXAppAPNSFeedbackProcessor {
  private static final int MAX_POOL_SIZE = 5;
  private final int[] poolCount = { MAX_POOL_SIZE };;
  private final List<String> badTokens;


  public StubMMXAppAPNSFeedbackProcessor(ConnectionProvider provider, String appId,
      boolean productionCert, List<String> badTokens) {
    super(provider, appId, productionCert);
    this.badTokens = badTokens;
  }

  @Override
  protected APNSConnection getAPNSConnection(String appId, boolean production) {
    // Simulate the GenericKeyedPoolObject for blocking wait
    try {
      synchronized(poolCount) {
        if (poolCount[0] > 0) {
          --poolCount[0];
        } else {
          poolCount.wait();
        }
      }
    } catch (InterruptedException e) {
      // It has been waiting for too long, a test driver aborts the wait.
      return null;
    }
    APNSConnection connection = new StubAPNSConnection(appId, production, 10L, badTokens);
    return connection;
  }

  @Override
  protected void returnConnection(APNSConnection connection) {
    // Simulate the GenericKeyedPoolObject.
    synchronized(poolCount) {
      if (poolCount[0] < MAX_POOL_SIZE) {
        ++poolCount[0];
        poolCount.notify();
      }
    }
  }
}
