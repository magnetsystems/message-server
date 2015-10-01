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
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import mockit.integration.junit4.JMockit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 */
@RunWith(JMockit.class)
public class TopicDaoImplTest extends BaseDbTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicDaoImplTest.class);
  private static DataSource ds;
  private static final String NODE_PREFIX = "my_node";
  private static final String APP_ID = "aaaaa";

  @BeforeClass
  public static void setupDatabase() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    DBTestUtil.setDataSource(ds);
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
      LOGGER.trace("----------creating Topic : " + entity.getNodeId());
      try {
        DBTestUtil.getTopicDAO().persist(entity);
      } catch (Exception e) {

      }
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
      //entity.setDescription("new description " + i);
      try {
        DBTestUtil.getTopicDAO().persist(entity);
      } catch (Exception e) {

      }
    }

    LOGGER.trace("createTopics : test read after update");
    for(int i=0; i < 5 ; i++) {
      String name = NODE_PREFIX + i;
      TopicEntity entity = DBTestUtil.getTopicDAO().getTopic("pubsub", "/" + APP_ID + "/*/" + name);
      assertNotEquals(entity, null);
      String description = entity.getDescription();
      //assertEquals(description, "new description " + i);
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

    closeDataSource(ds);
  }
}
