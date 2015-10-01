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
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 */
public class DeviceTargetResolverTest {

  @ClassRule
  public static BaseDbTest.DataSourceResource dataSourceRule = new BaseDbTest.DataSourceResource(new Runnable() {
            @Override
            public void run() {
              DBTestUtil.cleanTables(new String[] {"mmxTag"}, connectionProvider);
            }
          },
          new Runnable() {
            @Override
            public void run() {
              DBTestUtil.cleanTables(new String[] {"mmxTag"}, connectionProvider);
            }
          },
          TestDataSource.TAG_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());

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
        return connectionProvider;
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
        return connectionProvider;
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
        return connectionProvider;
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
        return connectionProvider;
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
        return connectionProvider;
      }
    };

    List<DeviceEntity> entityList = resolver.resolve(appId, target);
    assertNotNull(entityList);
    int size = entityList.size();
    assertEquals("Non matching entity list size", 1, size);

  }
}
