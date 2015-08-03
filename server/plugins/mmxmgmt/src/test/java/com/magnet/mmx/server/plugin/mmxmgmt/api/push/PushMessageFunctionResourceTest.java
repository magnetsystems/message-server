/* Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt.api.push;

import com.magnet.mmx.server.api.v1.DevicesResourceValidationTest;
import com.magnet.mmx.server.plugin.mmxmgmt.api.query.DeviceQuery;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(JMockit.class)
public class PushMessageFunctionResourceTest extends BaseJAXRSTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PushMessageFunctionResourceTest.class);
  private static String baseUri = "http://localhost:8086/mmxmgmt/api/v1/send_push";
  private static BasicDataSource ds;
  private static List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();
  private static String appId = "7wmi73wxin9";
  private static String apiKey = "4111f18a-9fcc-4e84-8cb9-aad6ea7bf024";


  public PushMessageFunctionResourceTest() {
    super(baseUri);
  }

  @BeforeClass
  public static void staticSetup() throws Exception {
    setupDatabase();
  }


  public static void setupDatabase() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    DBTestUtil.setBasicDataSource(ds);
    DBTestUtil.setupMockDBUtil();
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DevicesResourceValidationTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    {
      InputStream xmlInput = DevicesResourceValidationTest.class.getResourceAsStream("/data/device-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }


  @Test
  public void testSendPushMessage() throws Exception {
    WebTarget target = getClient().target(getBaseURI());
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    SendPushMessageRequest request = new SendPushMessageRequest();
    String[] devIds = {"dev1", "dev2"};
    Target dtarget = new Target();
    dtarget.setDeviceIds(Arrays.asList(devIds));
    request.setBody("This is a test");
    request.setTarget(dtarget);
    Response response = invocationBuilder.post(Entity.entity(request, MediaType.APPLICATION_JSON));
    int statusCode = response.getStatus();
    assertEquals("Non matching status code", Response.Status.OK.getStatusCode(), statusCode);
    SendPushMessageResponse pushResponse = response.readEntity(SendPushMessageResponse.class);
    response.close();
    List<Unsent> unsent = pushResponse.getUnsentList();
    Count count = pushResponse.getCount();
    assertEquals("Non matching requested count", devIds.length, count.getRequested());
    assertEquals("Non matching unsent count", devIds.length, count.getUnsent());
    assertEquals("Non matching unsent list", devIds.length, unsent.size());
  }


  @Test
  public void testSendPushMessageUsingTags() throws Exception {
    WebTarget target = getClient().target(getBaseURI());
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    SendPushMessageRequest request = new SendPushMessageRequest();
    String[] tags = {"dev1", "dev2"};
    Target dtarget = new Target();
    DeviceQuery query = new DeviceQuery();
    dtarget.setDeviceQuery(query);
    query.setTags(Arrays.asList(tags));
    request.setBody("This is a test");
    request.setTarget(dtarget);
    Response response = invocationBuilder.post(Entity.entity(request, MediaType.APPLICATION_JSON));
    int statusCode = response.getStatus();
    assertEquals("Non matching status code", Response.Status.OK.getStatusCode(), statusCode);
    SendPushMessageResponse pushResponse = response.readEntity(SendPushMessageResponse.class);
    response.close();
    List<Unsent> unsent = pushResponse.getUnsentList();
    Count count = pushResponse.getCount();
    assertEquals("Non matching requested count", 0, count.getRequested());
    assertEquals("Non matching unsent count", 0, count.getUnsent());
    assertEquals("Non matching unsent list", 0, unsent.size());
  }

}
