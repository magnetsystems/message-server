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
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.DataSourceFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 */
public class TestOpenfireConnectionProvider implements ConnectionProvider {
  private DataSource ds;
  private static boolean isSchemaCreated;

  public TestOpenfireConnectionProvider() throws Exception {
    InputStream inputStream = MMXTopicTagsResourceTest.class.getResourceAsStream("/test.properties");

    Properties testProperties = new Properties();
    testProperties.load(inputStream);

    final String url = testProperties.getProperty("db.url");
    final String user = testProperties.getProperty("db.user");
    final String password = testProperties.getProperty("db.password");
    final String driver = testProperties.getProperty("db.driver");

    if(driver.toLowerCase().contains("hsqldb")) {
      //DatabaseManager dbManager = null;
      System.out.println("--------creating data source by EmbeddedDatabaseBuilder for url " + url);
      EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();

      //builder.addScript("database/openfire_hsqldb.sql");
      if(isSchemaCreated) {
        System.out.println("--------DataSource schema is already created for url " + url);
      } else {
        builder.addScript("database/openfire_hsqldb.sql");
        isSchemaCreated = true;
      }

      ds = builder
              .setType(EmbeddedDatabaseType.HSQL)
                      .setName("openfire_db_test")
//              .setDataSourceFactory(new DataSourceFactory() {
//                @Override
//                public ConnectionProperties getConnectionProperties() {
//                  return new ConnectionProperties() {
//                    @Override
//                    public void setDriverClass(Class<? extends Driver> driverClass) {
//                      //dataSource.setDriverClass(driverClass);
//                    }
//
//                    @Override
//                    public void setUrl(String url) {
//                      //dataSource.setUrl(url);
//                    }
//
//                    @Override
//                    public void setUsername(String username) {
//                      //dataSource.setUsername(username);
//                    }
//
//                    @Override
//                    public void setPassword(String password) {
//                      //dataSource.setPassword(password);
//                    }
//                  };
//                }
//
//                @Override
//                public DataSource getDataSource() {
//                  BasicDataSource basicDataSource = new BasicDataSource();
//                  basicDataSource.setDriverClassName(driver);
//                  basicDataSource.setUsername(user);
//                  basicDataSource.setPassword(password);
//                  basicDataSource.setUrl(url);
//
//                  return basicDataSource;
//                }
//              })
              .build();
    } else {
      System.out.println("--------creating BasicDataSource for url " + url);

      BasicDataSource basicDataSource = new BasicDataSource();
      basicDataSource.setDriverClassName(driver);
      basicDataSource.setUsername(user);
      basicDataSource.setPassword(password);
      basicDataSource.setUrl(url);

      ds = basicDataSource;
    }

    System.out.println("--------created data source for url " + url + " : " + ds);
  }

  public DataSource getDataSource() {
    return ds;
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
