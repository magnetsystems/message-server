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

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.BaseDbTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.utils.TestDataSource;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;

/**
 */
public class DevicePushTokenInvalidatorTest extends BaseDbTest {

  @ClassRule
  public static DataSourceResource dataSourceRule = new DataSourceResource(TestDataSource.APP_DATA_1, TestDataSource.DEVICE_DATA_1, TestDataSource.MESSAGE_DATA_1, TestDataSource.WAKEUP_QUEUE_DATA_1);

  private static ConnectionProvider connectionProvider = new BasicDataSourceConnectionProvider(dataSourceRule.getDataSource());


  @Test
  public void testInvalidateToken() throws Exception {
    String appId = "i26u1lmv7uc";
    String token = "bogustoken";
    DevicePushTokenInvalidator invalidator = new StubDevicePushTokenInvalidator(dataSourceRule.getDataSource());
    invalidator.invalidateToken(appId, PushType.APNS, token);
    //WakeupEntityDAO

  }


  private static class StubDevicePushTokenInvalidator extends DevicePushTokenInvalidator {
    private javax.sql.DataSource ds;

    private StubDevicePushTokenInvalidator(DataSource ds) {
      this.ds = ds;
    }

    @Override
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }
  }
}
