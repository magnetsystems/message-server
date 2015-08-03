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

import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.jivesoftware.util.StringUtils;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(JMockit.class)
public class TopicItemDAOImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicItemDAOImplTest.class);
  private static BasicDataSource ds;
  private static String SERVICE_ID = "pubsub" + RandomStringUtils.randomAlphabetic(5);
  private static String NODE_ID = "/aaaaaaa/bbbbbbb/cccccccc" + RandomStringUtils.randomAlphabetic(5);
  private static String JID = "user1%aaaaaaa@localhost/testdevice";
  private static final Date testStartTime = new Date();

  @BeforeClass
  public static void setupDatabase() throws Exception {
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

    DBTestUtil.setBasicDataSource(ds);
    generatePubsubItems();
  }

  public static void generatePubsubItems() {
    for(int i=1; i <= 10; i++) {
      TopicItemEntity entity = new TopicItemEntity();
      entity.setServiceId(SERVICE_ID);
      entity.setNodeId(NODE_ID);
      entity.setId(RandomStringUtils.randomAlphanumeric(10));
      entity.setJid(JID);
      entity.setPayload(String.format("%03d", 11 - i));
      DateTime dt = new DateTime(testStartTime);
      dt = dt.minusDays(i);
      Date d = dt.toDate();
      String dateStr = StringUtils.dateToMillis(d);
      entity.setCreationDate(dateStr);
      DBTestUtil.getTopicItemDAO().persist(entity);
    }
  }

  @Test
  public void testUntil() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    DateTime referenceDate = new DateTime(testStartTime).minusDays(11);
    for(int i=1 ; i < 11; i++ ) {
      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsUntil(SERVICE_ID, NODE_ID, 10,
              StringUtils.dateToMillis(referenceDate.plusDays(i).toDate()));
      assertEquals(i, topicItemList.size());
      for(TopicItemEntity entity : topicItemList) {
        LOGGER.trace("testUntil : item={}", entity.getPayload());
      }

    }
  }

  @Test
  public void testUntilWithMaxItems() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    DateTime referenceDate = new DateTime(testStartTime).minusDays(11);
    for(int i=1 ; i <= 10; i++ ) {
      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsUntil(SERVICE_ID, NODE_ID, 5,
              StringUtils.dateToMillis(referenceDate.plusDays(i).toDate()));
      assertTrue(topicItemList.size() <= 5);
    }
  }

  @Test
  public void testUntilWithMaxItemsCheckOrder() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    DateTime referenceDate = new DateTime(testStartTime).minusDays(6);

    List<TopicItemEntity> topicItemList = topicItemDAO.getItemsUntil(SERVICE_ID, NODE_ID, 5,
            StringUtils.dateToMillis(referenceDate.toDate()));

    assertEquals(topicItemList.size(), 5);

    for(int i=0; i < 5; i++) {
      TopicItemEntity entity = topicItemList.get(i);
      assertEquals(entity.getPayload(), String.format("%03d", 5-i));
    }
  }

  @Test
  public void testSince() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    DateTime referenceDate = new DateTime(testStartTime).minusDays(11);
    for(int i=1 ; i < 11; i++ ) {
      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsSince(SERVICE_ID, NODE_ID, 10,
              StringUtils.dateToMillis(referenceDate.plusDays(i).toDate()));
      assertEquals(11-i, topicItemList.size());
    }
  }

  @Test
  public void testSinceWithMaxItems() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    DateTime referenceDate = new DateTime(testStartTime).minusDays(11);
    for(int i=1 ; i <= 10; i++ ) {
      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsSince(SERVICE_ID, NODE_ID, 5,
              StringUtils.dateToMillis(referenceDate.plusDays(i).toDate()));
      assertTrue(topicItemList.size() <= 5);
    }
  }

  @Test
  public void testSinceWithMaxItemsCheckOrder() {
    {
      TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
      DateTime referenceDate = new DateTime(testStartTime).minusDays(11);

      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsSince(SERVICE_ID, NODE_ID, 5,
              StringUtils.dateToMillis(referenceDate.toDate()));

      assertEquals(topicItemList.size(), 5);

      for (int i = 0; i < 5; i++) {
        TopicItemEntity entity = topicItemList.get(i);
        assertEquals(entity.getPayload(), String.format("%03d", i + 1));
      }
    }

    {
      TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
      DateTime referenceDate = new DateTime(testStartTime).minusDays(5);

      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsSince(SERVICE_ID, NODE_ID, 5,
            StringUtils.dateToMillis(referenceDate.toDate()));

      assertEquals(topicItemList.size(), 5);

      for(int i=0; i < 5; i++) {
        TopicItemEntity entity = topicItemList.get(i);
        assertEquals(entity.getPayload(), String.format("%03d", 6+i));
      }
    }
  }

  @Test
  public void testUntilAndSinceDescending() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();

    {
      DateTime sinceDate = new DateTime(testStartTime).minusDays(11);
      DateTime untilDate = new DateTime(testStartTime);

      for (int i = 0; i < 5; i++) {
        sinceDate = sinceDate.plusDays(1);
        untilDate = untilDate.minusDays(1);
        List<TopicItemEntity> topicItemList = topicItemDAO.getItems(SERVICE_ID, NODE_ID, 10,
                StringUtils.dateToMillis(sinceDate.toDate()), StringUtils.dateToMillis(untilDate.toDate()), MMXServerConstants.SORT_ORDER_DESCENDING);
        for (int j = 0; j < topicItemList.size(); j++) {
          TopicItemEntity entity = topicItemList.get(j);
          assertEquals(entity.getPayload(), String.format("%03d", 10 - (i + j)));
        }
      }
    }
  }

  @Test
  public void testUntilAndSinceDescendingWithLimit() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();

    {
      DateTime sinceDate = new DateTime(testStartTime).minusDays(11);
      DateTime untilDate = new DateTime(testStartTime);

      for (int i = 0; i < 5; i++) {
        sinceDate = sinceDate.plusDays(1);
        untilDate = untilDate.minusDays(1);
        List<TopicItemEntity> topicItemList = topicItemDAO.getItems(SERVICE_ID, NODE_ID, 5,
                StringUtils.dateToMillis(sinceDate.toDate()), StringUtils.dateToMillis(untilDate.toDate()), MMXServerConstants.SORT_ORDER_DESCENDING);
        assertTrue(topicItemList.size() <= 5);
        for (int j = 0; j < topicItemList.size(); j++) {
          TopicItemEntity entity = topicItemList.get(j);
          assertEquals(entity.getPayload(), String.format("%03d", 10 - (i + j)));
        }
      }
    }
  }

  @Test
  public void testUntilAndSinceAscending() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    {
      DateTime sinceDate = new DateTime(testStartTime).minusDays(11);
      DateTime untilDate = new DateTime(testStartTime);

      for (int i = 0; i < 5; i++) {
        sinceDate = sinceDate.plusDays(1);
        untilDate = untilDate.minusDays(1);
        List<TopicItemEntity> topicItemList = topicItemDAO.getItems(SERVICE_ID, NODE_ID, 10,
                StringUtils.dateToMillis(sinceDate.toDate()), StringUtils.dateToMillis(untilDate.toDate()), MMXServerConstants.SORT_ORDER_ASCENDING);
        for (int j = 0; j < topicItemList.size(); j++) {
          TopicItemEntity entity = topicItemList.get(j);
          assertEquals(entity.getPayload(), String.format("%03d", i + j + 1));
        }
      }
    }
  }

  @Test
  public void testUntilAndSinceAscendingWithLimit() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();
    {
      DateTime sinceDate = new DateTime(testStartTime).minusDays(11);
      DateTime untilDate = new DateTime(testStartTime);

      for (int i = 0; i < 5; i++) {
        sinceDate = sinceDate.plusDays(1);
        untilDate = untilDate.minusDays(1);
        List<TopicItemEntity> topicItemList = topicItemDAO.getItems(SERVICE_ID, NODE_ID, 5,
                StringUtils.dateToMillis(sinceDate.toDate()), StringUtils.dateToMillis(untilDate.toDate()), MMXServerConstants.SORT_ORDER_ASCENDING);
        assertTrue(topicItemList.size() <= 5);
        for (int j = 0; j < topicItemList.size(); j++) {
          TopicItemEntity entity = topicItemList.get(j);
          assertEquals(entity.getPayload(), String.format("%03d", i + j + 1));
        }
      }
    }
  }


  @Test
  public void test() {
    TopicItemDAO topicItemDAO = DBTestUtil.getTopicItemDAO();

    //Test until with maxItems and order
    {
      DateTime referenceDate = new DateTime(testStartTime).minusDays(11);
      LOGGER.trace("test : reference Date = {}", referenceDate.toDate());
      for(int i=0 ; i < 10; i++ ) {
        List<TopicItemEntity> topicItemList = topicItemDAO.getItemsUntil(SERVICE_ID, NODE_ID, 5,
                StringUtils.dateToMillis(referenceDate.plusDays(i).toDate()));
        assertTrue(topicItemList.size() <= 5);
      }
    }

    {
      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsUntil(SERVICE_ID, NODE_ID, 10,
              StringUtils.dateToMillis(new DateTime(testStartTime).minusDays(15).toDate()));
      LOGGER.trace("test : topicItemList={}", topicItemList.size());
      assertEquals(topicItemList.size(), 0);
    }

    {
      List<TopicItemEntity> topicItemList = topicItemDAO.getItemsSince(SERVICE_ID, NODE_ID, 10,
              StringUtils.dateToMillis(new DateTime(testStartTime).toDate()));

    }
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String unboundStr = "DELETE FROM ofPubsubItem where serviceID = ? AND nodeID = ?";
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt = conn.prepareStatement(unboundStr);
      pstmt.setString(1, SERVICE_ID);
      pstmt.setString(2, NODE_ID);
      pstmt.executeUpdate();
    } catch(SQLException e) {
      LOGGER.error("cleanupDatabase : caught exception cleaning ofPubsubItem");
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }
}
