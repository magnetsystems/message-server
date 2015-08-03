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

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub factory
 */
class StubAPNSConnectionKeyedPooledObjectFactory extends
    BaseKeyedPooledObjectFactory<APNSConnectionPoolImpl.APNSConnectionKey, APNSConnection> {
  private static Logger LOGGER = LoggerFactory.getLogger(StubAPNSConnectionKeyedPooledObjectFactory.class);

  private ConcurrentHashMap<APNSConnectionPoolImpl.APNSConnectionKey, Integer> counterMap = new ConcurrentHashMap<APNSConnectionPoolImpl.APNSConnectionKey, Integer>(5);
  private String appWithBadCert = null;

  StubAPNSConnectionKeyedPooledObjectFactory(String appWithBadCert) {
    this.appWithBadCert = appWithBadCert;
  }

  StubAPNSConnectionKeyedPooledObjectFactory() {
    this (null);
  }

  @Override
  public APNSConnection create(APNSConnectionPoolImpl.APNSConnectionKey key) throws Exception {
    if (appWithBadCert !=null && key.getAppId().equals(appWithBadCert)) {
      throw new IllegalArgumentException("Can't create connection for:" + key.getAppId());
    }
    APNSConnection connection = new StubAPNSConnection(key.getAppId(), key.isProduction(), 100L);
    if (counterMap.putIfAbsent(key, Integer.valueOf(1)) != null) {
      synchronized(counterMap) {
        //get the old value
        Integer prev = counterMap.get(key);
        counterMap.put(key, Integer.valueOf(prev.intValue()+1));
      }
    }
    LOGGER.trace("Created object with code:{} and key:{} ", connection.hashCode(), key);
    return connection;
  }

  @Override
  public void destroyObject(APNSConnectionPoolImpl.APNSConnectionKey key, PooledObject<APNSConnection> connection) throws
      Exception {
    long activeFor = connection.getActiveTimeMillis();
    long idleFor = connection.getIdleTimeMillis();
    int hashCode = connection.getObject().hashCode();
    super.destroyObject(key, connection);
    LOGGER.info(String.format("Destroyed APNS Connection for key:%s, active:%d idle:%d hashcode:%d", key, activeFor/1000L, idleFor/1000L, hashCode));
  }

  @Override
  public PooledObject<APNSConnection> wrap(APNSConnection value) {
    return new DefaultPooledObject<APNSConnection>(value);
  }

  public int getCreatedCount(APNSConnectionPoolImpl.APNSConnectionKey key) {
    if (counterMap.containsKey(key)) {
      return counterMap.get(key);
    } else {
      return 0;
    }
  }

  public void clearCounter() {
    counterMap.clear();
  }

}
