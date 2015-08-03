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
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConfig;
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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
//TODO: Fix the failing tests.
public class MessageDAOImplSearchTest {
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
    DatabaseConfig dbConfig = con.getConfig();
    dbConfig.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MessageDAOImplTest.CustomDataTypeFactory());
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/message-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/wakeup-queue-1.xml");
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
  public void testSearch1() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.TARGET_DEVICE_ID;

    int chunk = 5;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, "device2", MessageSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset));
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 7;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  /**
   * No search. just get the complete list
   */
  @Test
  public void testSearch2() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.TARGET_DEVICE_ID;

    int chunk = 8;
    int offset = 2;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, null, (ValueHolder) null, MessageSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset), true);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 10;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch3ByState() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.TARGET_DEVICE_ID;

    int chunk = 8;
    int offset = 2;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, MessageSearchOption.MESSAGE_STATE, MessageEntity.MessageState.DELIVERY_ATTEMPTED.name(), MessageSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset));
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 7;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch3ByDateSent() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.DATE_SENT;

    long start = 1411664400L; //2014-09-25 10:00:00 PST
    long end = 1411869600L; //2014-09-27 19:00:00 PST

    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(Long.toString(start));
    valueHolder.setValue2(Long.toString(end));

    int chunk = 8;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder, MessageSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset), true);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 4;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
    int[] expectedIds = {5,6,7,8};
    Set<Integer> resultIds = new HashSet<Integer>(expectedTotal);
    for (MessageEntity entity : results.getResults()) {
      int id = entity.getId();
      Integer wrapped = Integer.valueOf(id);
      if (resultIds.contains(wrapped)) {
        fail("Got duplicated ids which is not acceptable");
      }
      resultIds.add(wrapped);
    }
    //assert that the set has all the expected ids
    for (int expected : expectedIds) {
      assertTrue("Did n't get message with id:" + expected, resultIds.contains(expected) );
    }

  }

  @Test
  public void testSearch3ByDateAck() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.DATE_ACKNOWLEDGED;

    long start = 1411664400L; //2014-09-25 10:00:00 PST

    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(Long.toString(start));
    valueHolder.setValue2(null);

    int chunk = 8;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder, MessageSortOption.defaultSortOption(), PaginationInfo.build(chunk, offset), true);
    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 1;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
    int[] expectedIds = {8};
    Set<Integer> resultIds = new HashSet<Integer>(expectedTotal);
    for (MessageEntity entity : results.getResults()) {
      int id = entity.getId();
      Integer wrapped = Integer.valueOf(id);
      if (resultIds.contains(wrapped)) {
        fail("Got duplicated ids which is not acceptable");
      }
      resultIds.add(wrapped);
    }
    //assert that the set has all the expected ids
    for (int expected : expectedIds) {
      assertTrue("Did n't get message with id:" + expected, resultIds.contains(expected) );
    }

  }

  @Test
   public void testSearch5BySingleMessageId() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.MESSAGE_ID;
    String messageIdList = "13966ac5baa289a8";
    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(messageIdList);

    int chunk = 3;
    int offset = 2;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build(MessageSearchOption.MESSAGE_STATE.name(), SortOrder.ASCENDING.name()).get(0), PaginationInfo.build(chunk, offset), true);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 1;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch4ByMessageIdList() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = MessageSearchOption.MESSAGE_ID;
    String messageIdList = "13966ac5baa289a8,1396563a44077710,1396563a44077711";
    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(messageIdList);

    int chunk = 3;
    int offset = 2;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build(MessageSearchOption.MESSAGE_STATE.name(), SortOrder.ASCENDING.name()).get(0), PaginationInfo.build(chunk, offset), true);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 3;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }


  @Test
  public void testSearch4SortByTargetDeviceId() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = null;
    String deviceIdValue = "device2";
    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(deviceIdValue);

    int chunk = 10;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build("targetdevid", SortOrder.DESCENDING.name()).get(0), PaginationInfo.build(chunk, offset), true);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 10;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch4SortByState() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = null;
    String deviceIdValue = "device2";
    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(deviceIdValue);

    int chunk = 10;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build("state", SortOrder.DESCENDING.name()).get(0), PaginationInfo.build(chunk, offset), true);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 10;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testSearch4SortByMessageId() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "PrivateApp1";
    MessageSearchOption searchOption = null;
    String deviceIdValue = "device2";
    ValueHolder valueHolder = new ValueHolder();
    valueHolder.setValue1(deviceIdValue);

    int chunk = 10;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build("messageid", SortOrder.DESCENDING.name()).get(0), PaginationInfo.build(chunk, offset), true);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 10;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testFilteringOutReceiptMessages() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i26u1lmv7uc";
    MessageSearchOption searchOption = null;
    ValueHolder valueHolder = new ValueHolder();

    int chunk = 10;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build("messageid", SortOrder.DESCENDING.name()).get(0), PaginationInfo.build(chunk, offset), true);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 4;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

  @Test
  public void testIncludingReceiptMessages() {
    MessageDAO messageDAO = new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));

    String appId = "i26u1lmv7uc";
    MessageSearchOption searchOption = null;
    ValueHolder valueHolder = new ValueHolder();

    int chunk = 10;
    int offset = 0;

    SearchResult<MessageEntity> results = messageDAO.searchMessages(appId, searchOption, valueHolder,
        MessageSortOption.build("messageid", SortOrder.DESCENDING.name()).get(0), PaginationInfo.build(chunk, offset),
        false);

    assertNotNull(results);

    int total = results.getTotal();
    int expectedTotal = 6;
    assertEquals("Non matching chunk size", chunk, results.getSize().intValue());
    assertEquals("Non matching total size", expectedTotal, total);

    //check that we only go the expected number in the result list
    assertTrue("Unexpected list size", results.getResults().size() <= chunk);
  }

}
