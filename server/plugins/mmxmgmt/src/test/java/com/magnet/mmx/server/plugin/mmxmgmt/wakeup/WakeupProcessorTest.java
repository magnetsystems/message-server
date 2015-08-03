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
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntityDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntityDAOImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class WakeupProcessorTest {

  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    InputStream inputStream = DeviceDAOImplTest.class.getResourceAsStream("/test.properties");

    Properties testProperties = new Properties();
    testProperties.load(inputStream);

    String host = testProperties.getProperty("db.host");
    String port = testProperties.getProperty("db.port");
    String user = testProperties.getProperty("db.user");
    String password = testProperties.getProperty("db.password");
    String driver = testProperties.getProperty("db.driver");
    String schema = testProperties.getProperty("db.schema");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + schema;

    ds = new BasicDataSource();
    ds.setDriverClassName(driver);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setUrl(url);
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/wakeup-queue-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/message-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  @AfterClass
  public static void teardown() {
    try {
      ds.close();
    } catch (SQLException e) {
    }
  }

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

    private WakeupEntityDAO dao = new WakeupEntityDAOImpl(new BasicDataSourceConnectionProvider(ds));
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
      MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
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
