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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplSearchTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class DeviceHolderTest {
  private static BasicDataSource ds;
  @Before
  public void setUp() throws Exception {
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
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/device-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  @Test
  public void testHasDevices() {
    String [] deviceIds = {"7215B73D-5325-49E1-806A-2E4A5B3F7020", "398668AF-2395-4B64-B300-60F0ABC7459F", "12345678987654321"};

    DeviceDAO deviceDAO = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
    String appId = "AAABSNIBKOstQST7";

    List<DeviceEntity> deviceEntityList = deviceDAO.getDevices(appId, Arrays.asList(deviceIds), DeviceStatus.ACTIVE);
    DeviceHolder holder = DeviceHolder.build(deviceEntityList);
    assertTrue("Expected to have IOS devices", holder.hasDevices(PushType.APNS));
    assertTrue("Expected to have GCM devices", holder.hasDevices(PushType.GCM));
  }


  @Test
  public void testGetDevicesofType() {
    String [] deviceIds = {"7215B73D-5325-49E1-806A-2E4A5B3F7020", "398668AF-2395-4B64-B300-60F0ABC7459F", "12345678987654321"};

    DeviceDAO deviceDAO = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
    String appId = "AAABSNIBKOstQST7";

    List<DeviceEntity> deviceEntityList = deviceDAO.getDevices(appId, Arrays.asList(deviceIds), DeviceStatus.ACTIVE);
    DeviceHolder holder = DeviceHolder.build(deviceEntityList);

    List<DeviceEntity> iosList = holder.getDevices(PushType.APNS);
    assertNotNull("ios list is null", iosList);
    assertTrue("ios list is empty", !iosList.isEmpty());

    List<DeviceEntity> androidList = holder.getDevices(PushType.GCM);
    assertNotNull("android list is null", androidList);
    assertTrue("android list is empty", !androidList.isEmpty());
  }
}
