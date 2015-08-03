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

import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.MMXTopicTagsResourceTest;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jivesoftware.database.ConnectionProvider;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 */
public class TestOpenfireConnectionProvider implements ConnectionProvider {
  private BasicDataSource ds;

  public TestOpenfireConnectionProvider() throws Exception {
    InputStream inputStream = MMXTopicTagsResourceTest.class.getResourceAsStream("/test.properties");

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

  @Override
  public boolean isPooled() {
    return true;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return ds.getConnection();
  }

  @Override
  public void start() {

  }

  @Override
  public void restart() {

  }

  @Override
  public void destroy() {

  }
}
