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

import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplSearchTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class MMXAppAPNSFeedbackProcessorTest {

  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    DBTestUtil.cleanTables(new String[]{"mmxTag"}, new BasicDataSourceConnectionProvider(ds));
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/device-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  // This alarm thread will run a task after delaying for <code>delay</code>ms.
  // A runnable task is written to interrupt a blocking call (e.g. wait()) after
  // waiting for <code>delay</code>ms.  The caller should abort this alarm if
  // the blocking call is completed.
  private static class Alarm extends Thread {
    private static int sId;
    private boolean mAborted;
    private boolean mExecuted;
    private final long mDelay;
    private final Runnable mTask;

    public Alarm(long delay, Runnable task) {
      super("Alarm-"+(++sId));
      mTask = task;
      mDelay = delay;
    }

    public boolean executed() {
      return mExecuted;
    }

    @Override
    public void run() {
      synchronized(mTask) {
        try {
          System.out.println("Thread "+this.getName()+" waits for "+mDelay+"ms");
          mTask.wait(mDelay);
        } catch (InterruptedException e) {
          System.out.println("Thread "+this.getName()+" is interrupted");
        }
      }
      if (mAborted) {
        System.out.println("Thread "+this.getName()+" is aborted");
        return;
      }
      try {
        mExecuted = true;
        mTask.run();
        System.out.println("Thread "+this.getName()+" executed a task");
      } catch (Throwable e) {
        // The task failed
      }
    }

    public void abort() {
      synchronized(mTask) {
        mAborted = true;
        mTask.notify();
      }
    }
  }

  @Test
  public void test0ReturnConnection() throws Exception {
    String appId = "f2oi5ejp8di";
    String[] badTokens = {};

    StubMMXAppAPNSFeedbackProcessor processor = new StubMMXAppAPNSFeedbackProcessor(
        new BasicDataSourceConnectionProvider(ds), appId, true, Arrays.asList(badTokens));
    APNSConnection connection = processor.getAPNSConnection(appId, true);
    assertNotNull(connection);
    try {
      processor.returnConnection(connection);
    } catch (Throwable e) {
      e.printStackTrace();
      fail("Return connection caught "+e.getClass().getName()+": "+e);
    }
  }

  @Test
  public void test1ConnectionLeakViaCall() throws Exception {
    String appId = "f2oi5ejp8di";
    String[] badTokens = {};

    StubMMXAppAPNSFeedbackProcessor processor = new StubMMXAppAPNSFeedbackProcessor(
        new BasicDataSourceConnectionProvider(ds), appId, true, Arrays.asList(badTokens));

    for (int i = 0; i < 10; i++) {
      System.out.println("Iteration #"+i);
      Alarm alarm = new Alarm(2000, new Runnable() {
        private final Thread mThread = Thread.currentThread();
        @Override
        public void run() {
          // The getConnection() in processor.call() took too long; interrupt the wait.
          mThread.interrupt();
        }
      });
      alarm.start();
      // The call should take about 10ms.  if it does not return in two seconds,
      // issue a failure.
      MMXAppAPNSFeedbackProcessResult result = processor.call();
      if (alarm.executed()) {
        fail("MMXAppAPNSFeedbackProcessor was blocked at iteration "+i);
      } else {
        alarm.abort();
      }
      alarm.join();
    }
  }

  @Test
  public void test2Call() throws Exception {
    String appId = "f2oi5ejp8di";
    String[] badTokens = {"59AC2DAE4EEC46C8AD8DB01C41D0F89519800B459CEC75CF89C225E3661EC075"};

    StubMMXAppAPNSFeedbackProcessor processor = new StubMMXAppAPNSFeedbackProcessor(
        new BasicDataSourceConnectionProvider(ds), appId, true, Arrays.asList(badTokens));

    MMXAppAPNSFeedbackProcessResult result = processor.call();
    assertNotNull("Expecting a not null result", result);
    int invalid = badTokens.length;
    assertEquals("Non matching invalidated count", invalid, result.getInvalidatedCount());
  }

}
