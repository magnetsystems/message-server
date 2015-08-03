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

import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DateRange;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.PushMessageQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGenerator;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGeneratorImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 */
public class PushMessageDAOImplTest {
  private static BasicDataSource ds;

  @BeforeClass
  public static void setUp() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/pushmessage-data-1.xml");
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
  public void testAdd() throws Exception {
    PushMessageEntity message = new PushMessageEntity();
    String appId = "i0sq7ddvi17";
    String deviceId = "8933536df7038e1b7";
    message.setAppId(appId);
    message.setDeviceId(deviceId);
    PushIdGenerator generator = new PushIdGeneratorImpl();
    String messageId = generator.generateId(appId, deviceId);

    message.setMessageId(messageId);
    message.setState(PushMessageEntity.PushMessageState.PUSHED);
    message.setType(PushMessageEntity.PushMessageType.CONSOLEPING);

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    try {
      dao.add(message);
    } catch (DbInteractionException e) {
      fail ("Adding push message failed");
    }
  }

  @Test
  public void testGetUsingId() throws Exception {
    String messageId = "4427076bbd5d71f9d902f49991009b02";

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    try {
      PushMessageEntity entity = dao.getPushMessage(messageId);
      assertNotNull("Push Message entity is null", entity);
      String expectedDeviceId = "8933536df7038e1b7";
      assertEquals("Non matching deviceId", expectedDeviceId, entity.getDeviceId());
    } catch (DbInteractionException e) {
      fail ("testGetUsingId failed");
    }
  }

  @Test
  public void testAcknowledgePushMessage() throws Exception {
    String messageId = "4427076bbd5d71f9d902f49991009b02";

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    try {
      int count = dao.acknowledgePushMessage(messageId, new Date());
      PushMessageEntity entity = dao.getPushMessage(messageId);
      assertNotNull("Push Message entity is null", entity);
      String expectedDeviceId = "8933536df7038e1b7";
      assertEquals("Non matching deviceId", expectedDeviceId, entity.getDeviceId());
      PushMessageEntity.PushMessageState state = entity.getState();
      assertEquals("Non matching state", PushMessageEntity.PushMessageState.ACKNOWLEDGED, state);
      assertEquals("Non matching update count", 1, count);
    } catch (DbInteractionException e) {
      fail ("testAcknowledgePushMessage failed");
    }
  }


  @Test
  public void testGetUsingAppIdAndDeviceId() throws Exception {
    String appId = "i0sq7ddvi17";
    String deviceId = "8933536df7038e1b7";
    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    try {
      List<PushMessageEntity> entityList = dao.getPushMessages(appId, deviceId);
      assertNotNull("Push Message entity is null", entityList);
      String expectedDeviceId = "8933536df7038e1b7";
      assertEquals("Non matching deviceId", expectedDeviceId, entityList.get(0).getDeviceId());
    } catch (DbInteractionException e) {
      fail ("testGetUsingId failed");
    }
  }


  @Test
  public void testSearchDevicesUsingQueryBuilder() throws Exception {
    String appId = "i26u1lmv7uc";
    String deviceId = "8D2F9E5595E9989FEF3D1D3A5BA0FBE0BB318ED0";

    PushMessageQuery pushMessageQuery = new PushMessageQuery();
    pushMessageQuery.setDeviceIds(Arrays.asList(deviceId));

    SortInfo sortInfo = SortInfo.build("dateSent", "descending");
    PaginationInfo paginationInfo = PaginationInfo.build(6, 0);
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();

    QueryBuilderResult qResult = builder.buildPaginationQueryWithOrder(pushMessageQuery, appId, paginationInfo, sortInfo);

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    SearchResult<PushMessageEntity> results = dao.getPushMessagesWithPagination(qResult, paginationInfo);
    assertNotNull(results);

    int size = results.getSize();
    int total = results.getTotal();
    int expectedSize = 6;
    int expectedTotal = 10;
    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);
  }


  @Test
  public void testSearchPushMessagesUsingTimeRange_MOB_1953() throws Exception {
    String appId = "i26u1lmv7uc";
    PushMessageQuery pushMessageQuery = new PushMessageQuery();
    DateRange sentRange = new DateRange();
    sentRange.setStart(1423641600);
    sentRange.setEnd(1423728000);
    pushMessageQuery.setDateSent(sentRange);

    SortInfo sortInfo = SortInfo.build("dateSent", "descending");
    int expectedSize = 10;
    PaginationInfo paginationInfo = PaginationInfo.build(expectedSize, 0);
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();

    QueryBuilderResult qResult = builder.buildPaginationQueryWithOrder(pushMessageQuery, appId, paginationInfo, sortInfo);

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    SearchResult<PushMessageEntity> results = dao.getPushMessagesWithPagination(qResult, paginationInfo);
    assertNotNull(results);

    int size = results.getSize();
    int total = results.getTotal();
    int expectedTotal = 5;
    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);
  }

  @Test
  public void testSearchPushMessagesUsingStartTime_MOB_1953() throws Exception {
    String appId = "i26u1lmv7uc";
    PushMessageQuery pushMessageQuery = new PushMessageQuery();
    DateRange sentRange = new DateRange();
    sentRange.setStart(1423641600);
    sentRange.setEnd(null);
    pushMessageQuery.setDateSent(sentRange);

    SortInfo sortInfo = SortInfo.build("dateSent", "descending");
    int expectedSize = 10;
    PaginationInfo paginationInfo = PaginationInfo.build(expectedSize, 0);
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();

    QueryBuilderResult qResult = builder.buildPaginationQueryWithOrder(pushMessageQuery, appId, paginationInfo, sortInfo);

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    SearchResult<PushMessageEntity> results = dao.getPushMessagesWithPagination(qResult, paginationInfo);
    assertNotNull(results);

    int size = results.getSize();
    int total = results.getTotal();
    int expectedTotal = 15;
    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);
  }

  @Test
  public void testSearchPushMessagesUsingEndTime_MOB_1953() throws Exception {
    String appId = "i26u1lmv7uc";
    PushMessageQuery pushMessageQuery = new PushMessageQuery();
    DateRange sentRange = new DateRange();
    sentRange.setStart(null);
    sentRange.setEnd(1423728000);
    pushMessageQuery.setDateSent(sentRange);

    SortInfo sortInfo = SortInfo.build("dateSent", "descending");
    int expectedSize = 10;
    PaginationInfo paginationInfo = PaginationInfo.build(expectedSize, 0);
    PushMessageQueryBuilder builder = new PushMessageQueryBuilder();

    QueryBuilderResult qResult = builder.buildPaginationQueryWithOrder(pushMessageQuery, appId, paginationInfo, sortInfo);

    PushMessageDAO dao = new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
    SearchResult<PushMessageEntity> results = dao.getPushMessagesWithPagination(qResult, paginationInfo);
    assertNotNull(results);

    int size = results.getSize();
    int total = results.getTotal();
    int expectedTotal = 38;
    assertEquals("Non matching size", expectedSize, size);
    assertEquals("Non matching total", expectedTotal, total);
  }
}
