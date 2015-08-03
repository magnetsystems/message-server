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

package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for the DeviceDAOImpl
 */
public class DeviceDAOImplSearchTest {

  private static BasicDataSource ds;

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
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/device-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  @AfterClass
  public static void teardown() {
    try {
      ds.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testConnection() throws Exception {
    Connection conn = ds.getConnection();
    assertNotNull(conn);
  }

  @Test
  public void testSearch1() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "AAABSNIBKOstQST7";
    DeviceSearchOption searchOption = DeviceSearchOption.USERNAME;

    ValueHolder holder = new ValueHolder();
    holder.setValue1("magnet.way");

    int chunk = 5;
    int offset = 0;

    SearchResult<DeviceEntity> results = dao.searchDevices(appId, searchOption, holder, DeviceSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset));
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal =5;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);

    DeviceEntity entity = results.getResults().get(0);

    assertNull("Reverse phone number is not null", entity.getPhoneNumberRev());
  }

  @Test
  public void testSearch2() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "AAABSNIBKOstQST7";
    DeviceSearchOption searchOption = DeviceSearchOption.USERNAME;

    ValueHolder holder = new ValueHolder();
    holder.setValue1("login3");

    int chunk = 5;
    int offset = 0;

    SearchResult<DeviceEntity> results = dao.searchDevices(appId, searchOption, holder, DeviceSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset));
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 5;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);

    DeviceEntity entity = results.getResults().get(0);

    assertNull("Reverse phone number is not null", entity.getPhoneNumberRev());
  }


  @Test
  public void testListDevicesUsingDeviceIdList() {
    DeviceDAO dao = new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "AAABSNIBKOstQST7";
    List<String> deviceIds = new ArrayList<String>(10);
    deviceIds.add("12345678987654321");
    deviceIds.add("12345678987654322");
    deviceIds.add("00001LINUX");

    List<DeviceEntity> results = dao.getDevices(appId, deviceIds, DeviceStatus.ACTIVE);
    assertNotNull(results);

    //check that we only got the expected number in the result list
    assertTrue("Unexpected list size", results.size() == deviceIds.size());

  }
}
