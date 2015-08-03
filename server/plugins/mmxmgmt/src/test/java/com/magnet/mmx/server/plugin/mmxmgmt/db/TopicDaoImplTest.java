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
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
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
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 */
@RunWith(JMockit.class)
public class TopicDaoImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicDaoImplTest.class);
  private static BasicDataSource ds;
  private static final String NODE_PREFIX = "my_node";
  private static final String APP_ID = "aaaaa";

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
  }

  @Test
  public void createTopics() throws Exception {
    LOGGER.trace("createTopics : test creation");
    for(int i=0; i < 5; i++) {
      TopicEntity entity = new TopicEntity();
      entity.setServiceId("pubsub");
      String name = NODE_PREFIX + i;
      entity.setNodeId("/" + APP_ID + "/*/" + name);
      entity.setParent(APP_ID);
      entity.setName(name);
      entity.setDescription("old description " + i);
      DBTestUtil.getTopicDAO().persist(entity);
    }

    LOGGER.trace("createTopics : test read");
    for(int i=0; i < 5 ; i++) {
      String name = NODE_PREFIX + i;
      TopicEntity entity = DBTestUtil.getTopicDAO().getTopic("pubsub", "/" + APP_ID + "/*/" + name);
      assertNotEquals(entity, null);
      String description = entity.getDescription();
      assertEquals(description, "old description " + i);
      LOGGER.trace("createTopics : read entity = {}", entity);
    }

    LOGGER.trace("createTopics : test update");
    for(int i=0; i < 5; i++) {
      TopicEntity entity = new TopicEntity();
      entity.setServiceId("pubsub");
      String name = NODE_PREFIX + i;
      entity.setNodeId("/" + APP_ID + "/*/" + name);
      entity.setParent(APP_ID);
      entity.setName(name);
      entity.setDescription("new description " + i);
      DBTestUtil.getTopicDAO().persist(entity);
    }

    LOGGER.trace("createTopics : test read after update");
    for(int i=0; i < 5 ; i++) {
      String name = NODE_PREFIX + i;
      TopicEntity entity = DBTestUtil.getTopicDAO().getTopic("pubsub", "/" + APP_ID + "/*/" + name);
      assertNotEquals(entity, null);
      String description = entity.getDescription();
      assertEquals(description, "new description " + i);
      LOGGER.trace("createTopics : read entity = {}", entity);
    }

    LOGGER.trace("createTopics : test delete");
    for(int i=0; i < 5 ; i++) {
      String name = NODE_PREFIX + i;
      DBTestUtil.getTopicDAO().deleteTopic("pubsub", "/" + APP_ID + "/*/" + name);
    }

    LOGGER.trace("createTopics : test read after delete");
    for(int i=0; i < 5 ; i++) {
      String name = NODE_PREFIX + i;
      TopicEntity entity = DBTestUtil.getTopicDAO().getTopic("pubsub", "/" + APP_ID + "/*/" + name);
      assertEquals(entity, null);
      LOGGER.trace("createTopics : read entity = {}", entity);
    }
  }

  @AfterClass
  public static void cleanupDatabase() throws Exception {
    final String statementStr1 = "DELETE FROM ofPubsubNode WHERE nodeID LIKE '%"+NODE_PREFIX+"%'";

    Connection conn = null;
    PreparedStatement pstmt1 = null;

    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt1 = conn.prepareStatement(statementStr1);
      pstmt1.execute();
    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}", e);
    } finally {
      CloseUtil.close(LOGGER, pstmt1, conn);
    }
  }
}
