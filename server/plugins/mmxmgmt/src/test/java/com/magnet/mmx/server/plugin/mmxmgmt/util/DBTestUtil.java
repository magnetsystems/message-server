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

import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.util.OFPropertyDAO;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 */

public class DBTestUtil {
  private static BasicDataSource ds;

  public static void setBasicDataSource(BasicDataSource ds) {
    DBTestUtil.ds = ds;
  }

  public static void setDataSourceFromPropertyFile() throws Exception {
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
  }

  public static DeviceDAO getDeviceDAO() {
    return new DeviceDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static AppDAO getAppDAO() {
    return new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static MessageDAO getMessageDAO() {
    return new MessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static PushMessageDAO getPushMessageDAO() {
    return new PushMessageDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static WakeupEntityDAO getWakeupEntityDAO() {
    return new WakeupEntityDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static TagDAO getTagDAO() {
    return new TagDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static TopicDAO getTopicDAO() { return new TopicDAOImpl(new BasicDataSourceConnectionProvider(ds));}

  public static TopicItemDAO getTopicItemDAO() { return new TopicItemDAOImpl(new BasicDataSourceConnectionProvider(ds));}
  /**
   * Delete all records from the tables specified in tables array. The deletes are executed in
   * the same order as the entries in the tables array.
   *
   * WARNING: This DELETES data and is not undoable. You need to ensure that you are connected to
   * a test database and not a production database.
   *
   * @param tables
   * @param ds
   */
  public static void cleanTables (String[] tables, BasicDataSourceConnectionProvider ds) {
    String template = "DELETE FROM %s";
    try {
      Connection connection = ds.getConnection();
      for (String table : tables) {
        String statement = String.format(template, table);
        PreparedStatement preparedStatement = connection.prepareStatement(statement);
        preparedStatement.execute();
      }
    } catch (SQLException e) {

    }
  }

  public static TestUserDao getTestUserDao() {
    return new TestUserDao(new BasicDataSourceConnectionProvider(ds));
  }

  public static UserDAO getUserDao() {
    return new UserDAOImpl(new BasicDataSourceConnectionProvider(ds));
  }

  public static OFPropertyDAO getOfPropDao() {
    return new OFPropertyDAO(new BasicDataSourceConnectionProvider(ds));
  }

  public static void setupMockDBUtil() {
    new MockUp<DBUtil>() {
      @Mock
      public DeviceDAO getDeviceDAO() {
        return DBTestUtil.getDeviceDAO();
      }

      @Mock
      public AppDAO getAppDAO() {
        return DBTestUtil.getAppDAO();
      }

      @Mock
      public MessageDAO getMessageDAO() {
        return DBTestUtil.getMessageDAO();
      }

      @Mock
      public PushMessageDAO getPushMessageDAO() {
        return DBTestUtil.getPushMessageDAO();
      }

      @Mock
      public WakeupEntityDAO getWakeupEntityDAO() {
        return DBTestUtil.getWakeupEntityDAO();
      }

      @Mock
      public TagDAO getTagDAO() {
        return DBTestUtil.getTagDAO();
      }

      @Mock
      public UserDAO getUserDAO() {
        return DBTestUtil.getUserDao();
      }

      @Mock
      public TopicDAO getTopicDAO() { return DBTestUtil.getTopicDAO(); }

      @Mock
      public TopicItemDAO getTopicItemDAO() { return DBTestUtil.getTopicItemDAO(); }
    };
  }
}
