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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushResult;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Target;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Unsent;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.BasicDataSourceConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplSearchTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceTargetResolver;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.push.DeviceHolder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushAPNSPayloadBuilder;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class APNSPushMessageSenderTest {
  private static BasicDataSource ds;

  private static String BAD_TOKEN_DEVICE_ID = "9D49D5B1-8694-48A3-9D00-EC00ACE25794";
  private static String VALID_IOS_DEVICE_ID = "7215B73D-5325-49E1-806A-2E4A5B3F7020";
  private static String PUSH_INVALID_DEVICE_ID = "7215B73D-5325-49E1-806A-2E4A5B3F8000";

  @BeforeClass
  public static void setup() throws Exception {
    ds = UnitTestDSProvider.getDataSource();

    DBTestUtil.cleanTables(new String[] {"mmxTag"}, new BasicDataSourceConnectionProvider(ds));
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
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    APNSPushMessageSender sender = new StubAPNSPushMessageSender(appDAO.getAppForAppKey(appId));

    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    builder.setTitle("Unit test");
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
  public void testSendPushMissingCertificate() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {"7215B73D-5325-49E1-806A-2E4A5B3F7020", BAD_TOKEN_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    APNSPushMessageSender sender = new StubAPNSPushMessageSender(appDAO.getAppForAppKey(appId), null);

    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    builder.setTitle("Unit test");
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
  public void testSendPushValidAndInvalid() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {VALID_IOS_DEVICE_ID, BAD_TOKEN_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    APNSConnection connection = new CountingAPNSConnection(appId, false);
    APNSPushMessageSender sender = new StubAPNSPushMessageSender(appDAO.getAppForAppKey(appId), connection);

    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    builder.setTitle("Unit test");
    PushResult result = sender.sendPush(iosDevices, builder);
    assertNotNull("Expected not null result", result);
    int request = result.getCount().getRequested();
    int sent = result.getCount().getSent();
    int unsent = result.getCount().getUnsent();

    assertEquals("Non matching requested count", deviceIds.length, request);
    assertEquals("Non matching sent count", 1, sent);
    assertEquals("Non matching unsent count", 1, unsent);
  }

  @Test
  public void testSendPushToDeviceWithInvalidStatus() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {PUSH_INVALID_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    APNSConnection connection = new CountingAPNSConnection(appId, false);
    APNSPushMessageSender sender = new StubAPNSPushMessageSender(appDAO.getAppForAppKey(appId), connection);

    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    builder.setTitle("Unit test");
    PushResult result = sender.sendPush(iosDevices, builder);
    assertNotNull("Expected not null result", result);
    int request = result.getCount().getRequested();
    int sent = result.getCount().getSent();
    int unsent = result.getCount().getUnsent();

    assertEquals("Non matching requested count", deviceIds.length, request);
    assertEquals("Non matching sent count", 0, sent);
    assertEquals("Non matching unsent count", 1, unsent);

    Unsent notsent = result.getUnsentList().get(0);
    assertEquals("Non matching unsent message", ErrorMessages.ERROR_UNDELIVERABLE_TOKEN, notsent.getMessage());
  }

  @Test
  public void testSendPushValidDeviceWithBigPayload() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {VALID_IOS_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    APNSConnection connection = new CountingAPNSConnection(appId, false);
    APNSPushMessageSender sender = new StubAPNSPushMessageSender(appDAO.getAppForAppKey(appId), connection);

    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    StringBuilder sbuilder = new StringBuilder();
    int size = 3000;
    for (int i=0; i< size; i++) {
      sbuilder.append("H");
    }
    builder.setTitle(sbuilder.toString());
    PushResult result = sender.sendPush(iosDevices, builder);
    assertNotNull("Expected not null result", result);
    int request = result.getCount().getRequested();
    int sent = result.getCount().getSent();
    int unsent = result.getCount().getUnsent();

    assertEquals("Non matching requested count", deviceIds.length, request);
    assertEquals("Non matching sent count", 0, sent);
    assertEquals("Non matching unsent count", 1, unsent);

    Unsent problem = result.getUnsentList().get(0);
    int code = problem.getCode();
    assertEquals("Non matching error code", ErrorCode.APNS_SIZE_EXCEEDED.getCode(), code);
  }

  /**
   * Case where APNSConnection throws a NetworkIOException
   * @throws Exception
   */
  @Test
  public void testSendPushWithAPNSConnectionThrowingConnectionException() throws Exception {
    String appId  = "AAABSNIBKOstQST7";
    String[] deviceIds = {VALID_IOS_DEVICE_ID};
    DeviceTargetResolver resolver = new StubTargetResolver();
    Target target = new Target();
    target.setDeviceIds(Arrays.asList(deviceIds));
    DeviceHolder holder = DeviceHolder.build(resolver.resolve(appId, target));
    List<DeviceEntity> iosDevices = holder.getDevices(PushType.APNS);
    AppDAO appDAO = new AppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    APNSConnection connection = new ExceptionThrowingAPNSConnection(appId, false);
    APNSPushMessageSender sender = new StubAPNSPushMessageSender(appDAO.getAppForAppKey(appId), connection);

    MMXPushAPNSPayloadBuilder builder = new MMXPushAPNSPayloadBuilder();
    StringBuilder sbuilder = new StringBuilder();
    int size = 10;
    for (int i=0; i< size; i++) {
      sbuilder.append("H");
    }
    builder.setTitle(sbuilder.toString());
    PushResult result = sender.sendPush(iosDevices, builder);
    assertNotNull("Expected not null result", result);
    int request = result.getCount().getRequested();
    int sent = result.getCount().getSent();
    int unsent = result.getCount().getUnsent();

    assertEquals("Non matching requested count", deviceIds.length, request);
    assertEquals("Non matching sent count", 0, sent);
    assertEquals("Non matching unsent count", 1, unsent);

    Unsent problem = result.getUnsentList().get(0);
    int code = problem.getCode();
    assertEquals("Non matching error code", ErrorCode.APNS_CONNECTION_EXCEPTION.getCode(), code);
  }


  static class StubTargetResolver extends DeviceTargetResolver {
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }
  }

  static class StubAPNSPushMessageSender extends APNSPushMessageSender {
    private APNSConnection connection = null;

    public StubAPNSPushMessageSender(AppEntity appEntity) {
      this(appEntity, new CountingAPNSConnection(appEntity.getAppId(), appEntity.isApnsCertProduction()));
    }

    public StubAPNSPushMessageSender(AppEntity appEntity, APNSConnection connection) {
      super(appEntity);
      this.connection = connection;
    }

    @Override
    protected APNSConnection getConnection(String appId, boolean production) {
      return this.connection;
    }

    @Override
    protected void returnConnection(APNSConnection connection) {
      //do nothing
    }

    @Override
    protected ConnectionProvider getConnectionProvider() {
      return new BasicDataSourceConnectionProvider(ds);
    }
  }

  private static class CountingAPNSConnection implements APNSConnection {
    private String appId;
    private boolean prod;
    private int count;

    private CountingAPNSConnection(String appId, boolean prod) {
      this.appId = appId;
      this.prod = prod;
    }

    @Override
    public void send(String deviceToken, String payload) {
      send(deviceToken, payload, null);
    }

    @Override
    public void send(String deviceToken, String payload, Integer ttl) {
      count++;
    }

    @Override
    public String getAppId() {
      return appId;
    }

    @Override
    public boolean isApnsProductionCert() {
      return prod;
    }

    @Override
    public List<String> getInactiveDeviceTokens() {
      return Collections.emptyList();
    }
  }

  private static class ExceptionThrowingAPNSConnection extends CountingAPNSConnection {
    private ExceptionThrowingAPNSConnection(String appId, boolean prod) {
      super(appId, prod);
    }

    @Override
    public void send(String deviceToken, String payload, Integer ttl) {
      throw new APNSConnectionException("java.net.SocketException: Connection closed by remote host");
    }
  }

}
