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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AppConfigurationEntityDAOImplTest {
  private static BasicDataSource ds;

  @BeforeClass
  public static void setupDB() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/app-data-1.xml");
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
  public void testUpdateConfiguration() throws Exception {
    String appId = "i0sq7ddvi18";
    String key = "wakeup.mute.period.minutes";
    String value = "25";

    AppConfigurationEntityDAO dao = new AppConfigurationEntityDAOImpl(new BasicDataSourceConnectionProvider(ds));
    dao.updateConfiguration(appId, key, value);

    List<AppConfigurationEntity> list = dao.getConfigurations(appId);
    Map<String, String> configMap = new HashMap<String, String>(list.size());
    for (AppConfigurationEntity config: list) {
      String k = config.getKey();
      String v = config.getValue();
      configMap.put(k, v);
    }
    assertTrue("Key:" + key + " doesn't exist in the map", configMap.containsKey(key));
    assertEquals("Value for key:" + key + " doesn't match expected", value, configMap.get(key));
  }


  @Test
  public void testUpdateConfigurationWithMultipleValues() throws Exception {
    String appId = "7wmi73wxin9";
    AppConfigurationEntityDAO dao = new AppConfigurationEntityDAOImpl(new BasicDataSourceConnectionProvider(ds));
    String[] keys = {"wakeup.mute.period.minutes", "max.message.rate.sec", "max.push.rate.sec"};
    String[] values = {"25", "70", "200"};

    for (int i = 0; i < keys.length; i++) {
      dao.updateConfiguration(appId, keys[i], values[i]);
    }

    List<AppConfigurationEntity> list = dao.getConfigurations(appId);
    Map<String, String> configMap = new HashMap<String, String>(list.size());
    for (AppConfigurationEntity config : list) {
      String k = config.getKey();
      String v = config.getValue();
      configMap.put(k, v);
    }
    int size = list.size();
    assertEquals("Size doesn't match expected size", 3, size);

    for (int i = 0; i < keys.length; i++) {
      assertTrue("Config key:" + keys[i] + " not found.", configMap.containsKey(keys[i]));
      assertEquals("For Config key:" + keys[i] + " value doesn't match", values[i], configMap.get(keys[i]));
    }

    /**
     * Read a single configuration
     */
    String key = keys[1];
    String expectedValue = values[1];
    AppConfigurationEntity entity = dao.getConfiguration(appId, key);
    assertEquals("Non matching value for key:" +key, expectedValue, entity.getValue());

    /**
     * Delete a configuration
     */
    dao.deleteConfiguration(appId, keys[0]);
    List<AppConfigurationEntity> afterDelete = dao.getConfigurations(appId);
    Map<String, String> afterDeleteConfigMap = new HashMap<String, String>(afterDelete.size());
    for (AppConfigurationEntity config : afterDelete) {
      String k = config.getKey();
      String v = config.getValue();
      afterDeleteConfigMap.put(k, v);
    }
    assertFalse("Config key:" + keys[0] + " was found.", afterDeleteConfigMap.containsKey(keys[0]));
  }
}