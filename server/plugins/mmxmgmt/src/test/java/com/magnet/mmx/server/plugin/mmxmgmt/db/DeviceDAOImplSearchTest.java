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

import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.device.DeviceSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import org.junit.ClassRule;
import org.junit.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for the DeviceDAOImpl
 */
public class DeviceDAOImplSearchTest extends BaseDbTest {

  @ClassRule
  public static DataSourceResource dataSourceRule = new DataSourceResource(TestDataSource.APP_DATA_1, TestDataSource.DEVICE_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());


  @Test
  public void testConnection() throws Exception {
    Connection conn = dataSourceRule.getDataSource().getConnection();
    assertNotNull(conn);
  }

  @Test
  public void testSearch1() {
    DeviceDAO dao = new DeviceDAOImpl(connectionProvider);

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
    DeviceDAO dao = new DeviceDAOImpl(connectionProvider);

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
    DeviceDAO dao = new DeviceDAOImpl(connectionProvider);

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
