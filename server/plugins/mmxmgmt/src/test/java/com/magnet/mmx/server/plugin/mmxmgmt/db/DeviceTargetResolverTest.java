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

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.Operator;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
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
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 */
public class DeviceTargetResolverTest {

  private static BasicDataSource ds;

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    DBTestUtil.cleanTables(new String[] {"mmxTag"}, new BasicDataSourceConnectionProvider(ds));
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/tag-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  @AfterClass
  public static void teardown() {
    try {
      //delete stuff from tag so that other test that recreate device data don't run into issues
      DBTestUtil.cleanTables(new String[]{"mmxTag"}, new BasicDataSourceConnectionProvider(ds));
      ds.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testResolveUsingOsType() throws Exception {
    String appId = "azi6dw1weq";
    Target target = new Target();
    DeviceQuery query = new DeviceQuery();
    query.setOsType(OSType.ANDROID);
    target.setDeviceQuery(query);
    DeviceTargetResolver resolver = new DeviceTargetResolver() {
      @Override
      protected ConnectionProvider getConnectionProvider() {
        return new BasicDataSourceConnectionProvider(ds);
      }
    };

    List<DeviceEntity> entityList = resolver.resolve(appId, target);
    assertNotNull(entityList);
    int size = entityList.size();
    assertEquals("Non matching entity list size", 2, size);
  }

  @Test
  public void testResolveUsingTags() throws Exception {
    String appId = "azi6dw1weq";
    Target target = new Target();
    DeviceQuery query = new DeviceQuery();
    query.setTags(new ArrayList<String>());
    query.getTags().add("secure");
    target.setDeviceQuery(query);
    DeviceTargetResolver resolver = new DeviceTargetResolver() {
      @Override
      protected ConnectionProvider getConnectionProvider() {
        return new BasicDataSourceConnectionProvider(ds);
      }
    };

    List<DeviceEntity> entityList = resolver.resolve(appId, target);
    assertNotNull(entityList);
    int size = entityList.size();
    assertEquals("Non matching entity list size", 2, size);

  }

  @Test
  public void testResolveUsingMultipleTags() throws Exception {
    String appId = "azi6dw1weq";
    Target target = new Target();
    DeviceQuery query = new DeviceQuery();
    query.setTags(new ArrayList<String>());
    query.getTags().add("secure");
    query.getTags().add("office");
    target.setDeviceQuery(query);
    DeviceTargetResolver resolver = new DeviceTargetResolver() {
      @Override
      protected ConnectionProvider getConnectionProvider() {
        return new BasicDataSourceConnectionProvider(ds);
      }
    };
    List<DeviceEntity> entityList = resolver.resolve(appId, target);
    assertNotNull(entityList);
    int size = entityList.size();
    assertEquals("Non matching entity list size", 3, size);
  }

  @Test
  public void testResolveUsingTagsAndOSType() throws Exception {
    String appId = "azi6dw1weq";
    Target target = new Target();
    DeviceQuery query = new DeviceQuery();
    query.setTags(new ArrayList<String>());
    query.getTags().add("secure");
    query.setOsType(OSType.ANDROID);
    target.setDeviceQuery(query);
    DeviceTargetResolver resolver = new DeviceTargetResolver() {
      @Override
      protected ConnectionProvider getConnectionProvider() {
        return new BasicDataSourceConnectionProvider(ds);
      }
    };

    List<DeviceEntity> entityList = resolver.resolve(appId, target);
    assertNotNull(entityList);
    int size = entityList.size();
    assertEquals("Non matching entity list size", 3, size);
  }

  @Test
  public void testResolveUsingTagsAndOSTypeUsingAND() throws Exception {
    String appId = "azi6dw1weq";
    Target target = new Target();
    DeviceQuery query = new DeviceQuery();
    query.setTags(new ArrayList<String>());
    query.getTags().add("secure");
    query.setOsType(OSType.ANDROID);
    query.setOperator(Operator.AND);
    target.setDeviceQuery(query);
    DeviceTargetResolver resolver = new DeviceTargetResolver() {
      @Override
      protected ConnectionProvider getConnectionProvider() {
        return new BasicDataSourceConnectionProvider(ds);
      }
    };

    List<DeviceEntity> entityList = resolver.resolve(appId, target);
    assertNotNull(entityList);
    int size = entityList.size();
    assertEquals("Non matching entity list size", 1, size);

  }
}
