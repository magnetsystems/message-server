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

  @Test
  public void test1Call() throws Exception {
    String appId = "f2oi5ejp8di";
    String[] badTokens = {"59AC2DAE4EEC46C8AD8DB01C41D0F89519800B459CEC75CF89C225E3661EC075"};

    StubMMXAppAPNSFeedbackProcessor processor = new StubMMXAppAPNSFeedbackProcessor(new BasicDataSourceConnectionProvider(ds), appId, true, Arrays.asList(badTokens));

    MMXAppAPNSFeedbackProcessResult result = processor.call();
    assertNotNull("Expecting a not null result", result);
    int invalid = badTokens.length;
    assertEquals("Non matching invalidated count", invalid, result.getInvalidatedCount());
  }

}
