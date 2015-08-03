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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplSearchTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
/**
 */
public class AppEntityDBLoadingEntityCacheTest {
  private static BasicDataSource ds;
  private static AppEntityDBLoadingEntityCache appCache;
  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

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

    AppEntityDBLoadingEntityCache.AppEntityDBLoader loader = new AppEntityDBLoadingEntityCache.AppEntityDBLoader(new BasicDataSourceConnectionProvider(ds));
    appCache = new AppEntityDBLoadingEntityCache(10, loader);

  }


  @Test
  public void testValidGet() throws Exception {
    String appId = "i0sq7ddvi18";
    AppEntity appEntity = appCache.get(appId);
    assertNotNull("App Entity is null" , appEntity);
    String readAppId = appEntity.getAppId();
    assertEquals("non matching appid", appId, readAppId);
  }

  @Test
  public void testGetAppIdNotExisting() throws Exception {
    String appId = "foobar";
    AppEntity appEntity = appCache.get(appId);
    assertNull("App Entity is null", appEntity);
  }
}
