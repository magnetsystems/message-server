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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

/**
 */
public class WakeupProcessorTest {

  @ClassRule
  public static BaseDbTest.DataSourceResource dataSourceRule = new BaseDbTest.DataSourceResource(TestDataSource.MESSAGE_DATA_1, TestDataSource.WAKEUP_QUEUE_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());

  @Test
  public void testWakeupProcess() {
    WakeupProcessor processor = new StubWakeupProcessor(new ReentrantLock());
    processor.run();
    WakeupNotifier notifier = processor.getGCMWakeupNotifier();

    int callCount =  ((StubWakeupNotifier) notifier).callCount;

    assertEquals("Non matching call count", 4, callCount);

    WakeupEntityDAO dao = processor.getWakeupEntityDAO();

    List<WakeupEntity> toProcess = dao.poll(10);

    assertTrue("We still have items to process which is not expected", toProcess.isEmpty());

  }


  public static class StubWakeupProcessor extends  WakeupProcessor {

    private WakeupEntityDAO dao = new WakeupEntityDAOImpl(connectionProvider);
    private WakeupNotifier notifier = new StubWakeupNotifier();
    private WakeupNotifier apnsNotifier = new StubAPNSWakeupNotifier();

    public StubWakeupProcessor(Lock lock) {
      super(lock);
    }

    @Override
    protected WakeupEntityDAO getWakeupEntityDAO() {
      return dao;
    }

    protected MessageDAO getMessageDAO() {
      MessageDAO messageDAO = new MessageDAOImpl(connectionProvider);
      return messageDAO;
    }

    @Override
    protected WakeupNotifier getGCMWakeupNotifier() {
      return notifier;
    }

    @Override
    protected WakeupNotifier getAPNSWakeupNotifier() {
      return apnsNotifier;
    }

    @Override
    protected AppEntity getAppEntity(String appId) {
      AppEntity appEntity = new AppEntity();
      appEntity.setAppId(appId);
      return appEntity;
    }
  }

  public static class StubWakeupNotifier implements WakeupNotifier {
    private int callCount = 0;
    @Override
    public List<NotificationResult> sendNotification(List<String> deviceTokens, String payload, NotificationSystemContext context) {
      String senderIdentifier = null;
      if (context instanceof GCMWakeupNotifierImpl.GCMNotificationSystemContext) {
        senderIdentifier = ((GCMWakeupNotifierImpl.GCMNotificationSystemContext) context).getSenderIdentifier();
      }

      assertTrue("device tokens is empty", !deviceTokens.isEmpty());
      assertTrue("payload is not null", (payload != null && !payload.isEmpty()));
      assertNotNull("senderIdentifier is null", senderIdentifier);

      List<NotificationResult> rv = Collections.singletonList(NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER);
      callCount ++;
      return rv;
    }
  }

  public static class StubAPNSWakeupNotifier implements WakeupNotifier {
    private int callCount = 0;
    @Override
    public List<NotificationResult> sendNotification(List<String> deviceTokens, String payload, NotificationSystemContext context) {
      String senderIdentifier = null;
      boolean correctContext = context instanceof APNSWakeupNotifierImpl.APNSNotificationSystemContext;

      assertTrue("device tokens is empty", !deviceTokens.isEmpty());
      assertTrue("payload is not null", (payload != null && !payload.isEmpty()));
      assertTrue("context is not APNS", correctContext);

      List<NotificationResult> rv = Collections.singletonList(NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER);
      callCount ++;
      return rv;
    }
  }

}
