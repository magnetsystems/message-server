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
package com.magnet.mmx.server.plugin.mmxmgmt.gcm;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushResult;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplSearchTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceTargetResolver;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.push.DeviceHolder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushGCMPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushHeader;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class GCMPushMessageSenderTest {

  private static BasicDataSource ds;

  private static String BAD_TOKEN_DEVICE_ID = "12345678987654300";
  private static String VALID_IOS_DEVICE_ID = "7215B73D-5325-49E1-806A-2E4A5B3F7020";

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DeviceDAOImplSearchTest.class.getResourceAsStream("/data/device-data-1.xml");
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
  public void testSendPushBadDeviceToken() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {BAD_TOKEN_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.GCM);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    GCMPushMessageSender sender = new GCMPushMessageSender(appDAO.getAppForAppKey(appId));

    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder();
    builder.setTitle("Unit test");
    builder.setType(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH));
    PushResult result = sender.sendPush(iosDevices, builder);
    assertNotNull("Expected not null result", result);
    int request = result.getCount().getRequested();
    int sent = result.getCount().getSent();
    int unsent = result.getCount().getUnsent();

    assertEquals("Non matching requested count", deviceIds.length, request);
    assertEquals("Non matching sent count", 0, sent);
    assertEquals("Non matching unsent count", deviceIds.length, unsent);
  }

  @Test
  public void testSendPushBadGoogleAPIKey() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {BAD_TOKEN_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.GCM);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    appEntity.setGoogleAPIKey(null);
    GCMPushMessageSender sender = new GCMPushMessageSender(appDAO.getAppForAppKey(appId));

    MMXPushGCMPayloadBuilder builder = new MMXPushGCMPayloadBuilder();
    builder.setTitle("Unit test");
    builder.setType(new MMXPushHeader(Constants.MMX, Constants.MMX_ACTION_CODE_PUSH));
    PushResult result = sender.sendPush(iosDevices, builder);
    assertNotNull("Expected not null result", result);
    int request = result.getCount().getRequested();
    int sent = result.getCount().getSent();
    int unsent = result.getCount().getUnsent();

    assertEquals("Non matching requested count", deviceIds.length, request);
    assertEquals("Non matching sent count", 0, sent);
    assertEquals("Non matching unsent count", deviceIds.length, unsent);
  }

  static class StubTargetResolver extends DeviceTargetResolver {
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }
  }

  static class StubAPNSPushMessageSender extends GCMPushMessageSender {

    StubAPNSPushMessageSender(AppEntity appEntity) {
      super(appEntity);
    }

    @Override
    protected Sender getSender(String googleApiKey) {
      return new CountingGCMSender(googleApiKey);
    }
  }


  static class CountingGCMSender extends Sender {

    private int count;

    CountingGCMSender(String key) {
      super(key);
    }

    @Override
    public MulticastResult send(Message message, List<String> regIds, int retries) throws IOException {
      count++;
      //can't build a MulticastResult.
      //MulticastResult result =  new MulticastResult();
      return null;
    }
  }
}
