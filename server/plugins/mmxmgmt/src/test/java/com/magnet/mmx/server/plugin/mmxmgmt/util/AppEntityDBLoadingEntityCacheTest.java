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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 */
public class AppEntityDBLoadingEntityCacheTest {

  private static AppEntityDBLoadingEntityCache appCache;

  @ClassRule
  public static BaseDbTest.DataSourceResource dataSourceRule = new BaseDbTest.DataSourceResource(TestDataSource.APP_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());


  @BeforeClass
  public static void setup() throws Exception {
    AppEntityDBLoadingEntityCache.AppEntityDBLoader loader = new AppEntityDBLoadingEntityCache.AppEntityDBLoader(connectionProvider);
    appCache = new AppEntityDBLoadingEntityCache(10, loader);
  }


  @Test
  public void testValidGet() throws Exception {
    String appId = "i0sq7ddvi18";
    AppEntity appEntity = appCache.get(appId);
    assertNotNull("App Entity is null" , appEntity);
    String readAppId = appEntity.getAppId();
    assertEquals("non matching appid", appId, readAppId);
  }

  @Test
  public void testGetAppIdNotExisting() throws Exception {
    String appId = "foobar";
    AppEntity appEntity = appCache.get(appId);
    assertNull("App Entity is null", appEntity);
  }
}
