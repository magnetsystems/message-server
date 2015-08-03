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
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class APNSConnectionPoolImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(APNSConnectionPoolImplTest.class);

  private static String[] appIds = {"app1", "app2", "app3"};
  private static String appWithBadCert = "badappId";
  private static StubAPNSConnectionKeyedPooledObjectFactory objectFactory;
  private static int MAX_OBJECTS_PER_KEY = 3;

  @BeforeClass
  public static void setUp() throws Exception {
    LOGGER.info("Setting up the pool");
    GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
    config.setMaxTotalPerKey(MAX_OBJECTS_PER_KEY);
    config.setMaxIdlePerKey(3);
    config.setMinEvictableIdleTimeMillis(10*60*1000L);
    config.setTimeBetweenEvictionRunsMillis(1000L);
    objectFactory = new StubAPNSConnectionKeyedPooledObjectFactory(appWithBadCert);
    APNSConnectionPoolImpl.initialize(objectFactory, config);

  }

  @AfterClass
  public static void tearDown() throws Exception {
    APNSConnectionPoolImpl.teardown();
  }

  @Test
  public void testGetAndReturn() {
    APNSConnectionPoolImpl pool = APNSConnectionPoolImpl.getInstance();

    APNSConnection connection = pool.getConnection(appIds[0], true);
    String deviceToken = "d1";
    String payload = "payload";
    try {
      connection.send(deviceToken, payload);
    } catch (Exception e) {
      LOGGER.info("bad things happened", e);
    } finally {
      pool.returnConnection(connection);
      connection = null;
    }
    /*
     * request the connections for the same appId
     */
    for (int i = 0; i < 5; i++) {
      APNSConnection other = pool.getConnection(appIds[i % appIds.length], i % 2 == 0);
      assertTrue("Didn't get a connection", other != null);
      pool.returnConnection(other);
    }
  }

  @Test
  public void testPoolWithMultipleThreads() {
    String testAppId = "multithreadTestApp";
    /**
     * create 5 threads each sending to 100 devices
     */
    int deviceCount = 100;
    int threadCount = 5;
    Map<String, String> payload = new LinkedHashMap<String, String>(100);
    for (int i = 0; i < deviceCount; i++) {
      payload.put("device:" + (i + 1), "JSON Payload{}:" + (i + 1));
    }
    objectFactory.clearCounter();
    APNSConnectionPoolImpl pool = APNSConnectionPoolImpl.getInstance();
    //initialize the pool with connections for all apps

//    for (int i = 0; i < appIds.length; i++) {
//      APNSConnection conn = pool.getConnection(appIds[i], true);
//      pool.returnConnection(conn);
//    }

    CountDownLatch countDownLatch = new CountDownLatch(threadCount);
    Executor executor = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
      private AtomicInteger counter = new AtomicInteger(1);

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("TestThread:" + counter.getAndIncrement());
        return t;
      }
    });
    for (int i = 0; i < threadCount; i++) {
      executor.execute(new SimpleAPNSSenderThread(testAppId, true, payload, countDownLatch));
    }
    //wait for the threads to finish
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    int count = objectFactory.getCreatedCount(new APNSConnectionPoolImpl.APNSConnectionKey(testAppId, true));
    assertEquals("Object got created too many times", MAX_OBJECTS_PER_KEY, count);
  }

  @Test
  public void testGetForAppWithBadCert() {
    APNSConnectionPoolImpl pool = APNSConnectionPoolImpl.getInstance();
    APNSConnection connection = pool.getConnection(appWithBadCert, true);
    assertTrue("Connection is not null", connection == null);
  }

  @Test
  public void testWithBadCertificate() {
    AppEntity entity = new AppEntity();
    entity.setAppId("app1");
    entity.setApnsCert("login3".getBytes());
    entity.setApnsCertPassword("r");
    entity.setApnsCertProduction(false);

    BadCertificateAPNSConnectionKeyedPooledObjectFactory factory = new BadCertificateAPNSConnectionKeyedPooledObjectFactory(null, entity);
    boolean gotException = true;
    try {
      APNSConnection connection = factory.create(new APNSConnectionPoolImpl.APNSConnectionKey(entity.getAppId(), entity.isApnsCertProduction()));
    } catch (APNSConnectionException e) {
      gotException = true;
    } catch (Exception e) {
      e.printStackTrace();
      fail("testWithBadCertificate");
    }
    assertTrue("Didn't get the necessary exception", gotException);
  }

  public static class SimpleAPNSSenderThread implements Runnable {
    private String appId;
    private boolean production;
    private Map<String, String> payLoadMap;
    private CountDownLatch latch;

    public SimpleAPNSSenderThread(String appId, boolean production, Map<String, String> payLoadMap, CountDownLatch latch) {
      this.appId = appId;
      this.production = production;
      this.payLoadMap = payLoadMap;
      this.latch = latch;
    }

    @Override
    public void run() {
      APNSConnectionPoolImpl pool = APNSConnectionPoolImpl.getInstance();
      APNSConnection connection = null;
      try {
        long start = System.nanoTime();
        connection = pool.getConnection(appId, production);
        long end = System.nanoTime();
        LOGGER.debug("Retrieved a connection from the pool in:" + (end - start) / 1000000l + " ms. with code:" + connection.hashCode());

        for (String key : payLoadMap.keySet()) {
          connection.send(key, payLoadMap.get(key));
        }
      } catch (Exception e) {
        LOGGER.info("Exception in sending the payload", e);
      } finally {
        pool.returnConnection(connection);
      }
      //completed sending the message.
      latch.countDown();
    }
  }

  /**
   * Factory impl for testing bad certificate case
   */
  static class BadCertificateAPNSConnectionKeyedPooledObjectFactory
      extends APNSConnectionPoolImpl.APNSConnectionKeyedPooledObjectFactory {

    private AppEntity appEntity;

    BadCertificateAPNSConnectionKeyedPooledObjectFactory(ConnectionProvider provider, AppEntity appEntity) {
      super(provider);
      this.appEntity = appEntity;
    }

    BadCertificateAPNSConnectionKeyedPooledObjectFactory(ConnectionProvider provider) {
      super(provider);
    }

    @Override
    AppEntity getAppEntity(String appId) {
      return appEntity;
    }
  }
}
