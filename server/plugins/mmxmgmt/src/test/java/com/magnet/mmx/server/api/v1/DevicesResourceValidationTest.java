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
package com.magnet.mmx.server.api.v1;

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
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

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test to test the validation in the DevicesResource for the search devices API.
 */
@RunWith(JMockit.class)
public class DevicesResourceValidationTest extends BaseJAXRSTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevicesResourceValidationTest.class);
  private static String baseUri = "http://localhost:8086/mmxmgmt/api/v1/devices";
  private static BasicDataSource ds;
  private static List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();
  private static String appId = "7wmi73wxin9";
  private static String apiKey = "4111f18a-9fcc-4e84-8cb9-aad6ea7bf024";


  public DevicesResourceValidationTest() {
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
  public void testInvalidSortByValue() {
    WebTarget target = getClient().target(getBaseURI())
        .queryParam(MMXServerConstants.SORT_BY_PARAM, "badvalue")
        .queryParam("os_type", OSType.ANDROID.name());
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    ErrorResponse errorCode = response.readEntity(ErrorResponse.class);
    int statusCode = response.getStatus();
    response.close();
    assertEquals("Non matching status code", Response.Status.BAD_REQUEST.getStatusCode(), statusCode);
    assertNotNull("Error message is null", errorCode.getMessage());
  }

  @Test
  public void testInvalidSortOrderValue() {
    WebTarget target = getClient().target(getBaseURI())
        .queryParam(MMXServerConstants.SORT_ORDER_PARAM, "ascd")
        .queryParam("os_type", OSType.ANDROID.name());
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    ErrorResponse errorCode = response.readEntity(ErrorResponse.class);
    int statusCode = response.getStatus();
    response.close();
    assertEquals("Non matching status code", Response.Status.BAD_REQUEST.getStatusCode(), statusCode);
    assertNotNull("Error message is null", errorCode.getMessage());
    assertEquals("No matching code value", ErrorCode.INVALID_SORT_ORDER_VALUE.getCode(), errorCode.getCode());
  }

  @Test
  public void testInvalidDeviceStatusValue() {
    WebTarget target = getClient().target(getBaseURI())
        .queryParam(DevicesResource.DEVICE_STATUS_PARAM_KEY, "ascd")
        .queryParam("os_type", OSType.ANDROID.name());
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    ErrorResponse errorCode = response.readEntity(ErrorResponse.class);
    int statusCode = response.getStatus();
    response.close();
    assertEquals("Non matching status code", Response.Status.BAD_REQUEST.getStatusCode(), statusCode);
    assertNotNull("Error message is null", errorCode.getMessage());
    assertEquals("No matching code value", ErrorCode.INVALID_DEVICE_STATUS_VALUE.getCode(), errorCode.getCode());
  }

  @Test
  public void testInvalidDateRangeSinceValue() {
    String badDate = "20150401";
    WebTarget target = getClient().target(getBaseURI())
        .queryParam(DevicesResource.REGISTERED_SINCE_PARAM_KEY, badDate);
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    ErrorResponse errorCode = response.readEntity(ErrorResponse.class);
    int statusCode = response.getStatus();
    response.close();
    assertEquals("Non matching status code", Response.Status.BAD_REQUEST.getStatusCode(), statusCode);
    assertNotNull("Error message is null", errorCode.getMessage());
    assertEquals("No matching code value", ErrorCode.INVALID_DEVICE_REGISTERED_SINCE_VALUE.getCode(), errorCode.getCode());
  }

  @Test
  public void testInvalidDateRangeUntilValue() {
    String badDate = "20150401";
    WebTarget target = getClient().target(getBaseURI())
        .queryParam(DevicesResource.REGISTERED_UNTIL_PARAM_KEY, badDate);
    Invocation.Builder invocationBuilder =
        target.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appId);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, apiKey);

    Response response = invocationBuilder.get();
    ErrorResponse errorCode = response.readEntity(ErrorResponse.class);
    int statusCode = response.getStatus();
    response.close();
    assertEquals("Non matching status code", Response.Status.BAD_REQUEST.getStatusCode(), statusCode);
    assertNotNull("Error message is null", errorCode.getMessage());
    assertEquals("No matching code value", ErrorCode.INVALID_DEVICE_REGISTERED_UNTIL_VALUE.getCode(), errorCode.getCode());
  }

}
