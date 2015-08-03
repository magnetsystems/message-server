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

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 */
public class UnitTestDSProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnitTestDSProvider.class);

  private static DSConfig dsConfig = null;

  static {
    try {
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
      dsConfig = new DSConfig();
      dsConfig.setHost(host);
      dsConfig.setPort(port);
      dsConfig.setUser(user);
      dsConfig.setPassword(password);
      dsConfig.setDriver(driver);
      dsConfig.setSchema(schema);
      dsConfig.setUrl(url);


    } catch (IOException e) {
      LOGGER.warn("Initialization problems. Unit test will fail", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Get unit test datasource
   * @return
   */
  public static BasicDataSource getDataSource() {
    BasicDataSource ds = new BasicDataSource();
    ds.setDriverClassName(dsConfig.getDriver());
    ds.setUsername(dsConfig.getUser());
    ds.setPassword(dsConfig.getPassword());
    ds.setUrl(dsConfig.getUrl());
    return ds;
  }

  /**
   * For Datasoure configuration
   */
  private static class DSConfig {
    String host;
    String port;
    String user;
    String password;
    String schema;
    String driver;

    public String getDriver() {
      return driver;
    }

    public void setDriver(String driver) {
      this.driver = driver;
    }

    String url;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getPort() {
      return port;
    }

    public void setPort(String port) {
      this.port = port;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getSchema() {
      return schema;
    }

    public void setSchema(String schema) {
      this.schema = schema;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }
}
