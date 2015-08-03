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
package com.magnet.mmx.server.plugin.mmxmgmt.api.tags;

import com.cedarsoftware.util.io.JsonWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.common.utils.DefaultOpenfireEncryptor;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
*/
@RunWith(JMockit.class)
public class MMXDeviceTagsResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXDeviceTagsResourceTest.class);
  private static String baseUri = "http://localhost:8086/mmxmgmt/api/v1/devices";
  private static BasicDataSource ds;
  private static AppEntity appEntity;
  private static List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public MMXDeviceTagsResourceTest() {
    super(baseUri);
  }

  @BeforeClass
  public static void staticSetup() throws Exception {
    setupDatabase();
  }

  public static void setupDatabase() throws Exception {
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

    ds = new BasicDataSource();
    ds.setDriverClassName(driver);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setUrl(url);

    DBTestUtil.setBasicDataSource(ds);
    new MockUp<AppDAOImpl>() {
      @Mock
      protected String getEncrypted(String value) {
        return value;
      }
    };
    DBTestUtil.setupMockDBUtil();
    MMXDeviceTagsResourceTest.appEntity = createRandomApp();
    for(int i=0 ; i < 5; i++) {
      deviceEntityList.add(createRandomDeviceEntity(appEntity, i));
    }
  }

  public static AppEntity createRandomApp() throws Exception  {

      String serverUserId = "serverUser";
      String appName = "devicetagresourcetestapp";
      String appId = RandomStringUtils.randomAlphanumeric(10);
      String apiKey = UUID.randomUUID().toString();
      String googleApiKey = UUID.randomUUID().toString();
      String googleProjectId = RandomStringUtils.randomAlphanumeric(8);
      String apnsPwd = RandomStringUtils.randomAlphanumeric(10);
      String ownerId = RandomStringUtils.randomAlphabetic(10);
      String ownerEmail = RandomStringUtils.randomAlphabetic(4) + "@magnet.com";
      String guestSecret = RandomStringUtils.randomAlphabetic(10);
      boolean apnsProductionEnvironment = false;

      AppEntity appEntity = new AppEntity();
      appEntity.setServerUserId(serverUserId);
      appEntity.setName(appName);
      appEntity.setAppId(appId);
      appEntity.setAppAPIKey(apiKey);
      appEntity.setGoogleAPIKey(googleApiKey);
      appEntity.setGoogleProjectId(googleProjectId);
      appEntity.setApnsCertPassword(apnsPwd);
      appEntity.setOwnerId(ownerId);
      appEntity.setOwnerEmail(ownerEmail);
      appEntity.setGuestSecret(guestSecret);
      appEntity.setApnsCertProduction(apnsProductionEnvironment);
      DBTestUtil.getAppDAO().persist(appEntity);
      return appEntity;
  }
  private static DeviceEntity createRandomDeviceEntity(AppEntity appEntity, int index) {
    DeviceEntity deviceEntity = new DeviceEntity();
    deviceEntity.setAppId(appEntity.getAppId());
    deviceEntity.setName(RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
    deviceEntity.setOsType(OSType.ANDROID);
    deviceEntity.setCreated(new Date());
    deviceEntity.setOwnerId(appEntity.getOwnerId());
    deviceEntity.setStatus(DeviceStatus.ACTIVE);
    deviceEntity.setDeviceId("mmxdevicetagsresourcetestdevice" + "_" + index);
    deviceEntity.setPhoneNumber("+213" + RandomStringUtils.randomNumeric(7));
    deviceEntity.setPhoneNumberRev(StringUtils.reverse(deviceEntity.getPhoneNumber()));
    DBTestUtil.getDeviceDAO().persist(deviceEntity);
    return deviceEntity;
  }
  @Before
  public void setup() {
    setupMocks();
  }

  public void setupMocks() {
    new MockUp<AppDAOImpl>() {
      @Mock
      protected String getEncrypted(String value) {
        return value;
      }
    };
    new MockUp<DefaultOpenfireEncryptor>() {
      @Mock
      public String getDecrypted(String value) {
        return value;
      }

      @Mock
      public String getEncrypted(String value) {
        return value;
      }
    };
  }

  private Response setTags(String deviceId, List<String> tagStrList) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    WebTarget service = getClient().target(getBaseURI() + "/" + deviceId + "/tags");
    TagList tagList = new TagList();
    tagList.setTags(new ArrayList<String>(tagStrList));
    LOGGER.trace("setTags : deviceTags=\n{}", gson.toJson(tagList));
    Invocation.Builder invocationBuilder =
        service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());

    Response response = invocationBuilder.post(Entity.entity(tagList, MediaType.APPLICATION_JSON));
    return response;
  }

  private Response getTagsForDeviceId(String deviceId) {
    WebTarget service = getClient().target(getBaseURI() + "/" + deviceId+"/tags");
    Invocation.Builder invocationBuilder =
        service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    return response;
  }

  private Response deleteTagForDeviceId(String deviceId, String tag) {
    WebTarget service = getClient().target(getBaseURI() + "/" + deviceId+"/tags/" + tag);
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.delete();
    return response;
  }

  private Response deleteAllTagsForDeviceId(String deviceId) {
    WebTarget service = getClient().target(getBaseURI() + "/" + deviceId+"/tags");
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.delete();
    return response;
  }

  @Test
  public void testSetGetTags() throws Exception {

    Response response = setTags(deviceEntityList.get(0).getDeviceId(),new ArrayList<String>(Arrays.asList("tag1", "tag2", "tag3")));

    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    response.close();

    response = setTags(deviceEntityList.get(1).getDeviceId(),new ArrayList<String>(Arrays.asList("tag4", "tag5", "tag6")));
    assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    response.close();

    {
      Response getResp = getTagsForDeviceId(deviceEntityList.get(0).getDeviceId());
      assertEquals(Response.Status.OK.getStatusCode(), getResp.getStatus());
      String jsonString = getResp.readEntity(String.class);
      validateContainsTags(jsonString, deviceEntityList.get(0).getDeviceId(), new ArrayList<String>(Arrays.asList("tag1", "tag2", "tag3")));
      getResp.close();
    }

    //other device now
    {
      Response getResp = getTagsForDeviceId(deviceEntityList.get(1).getDeviceId());
      validateContainsTags(getResp.readEntity(String.class), deviceEntityList.get(1).getDeviceId(), new ArrayList<String>(Arrays.asList("tag4", "tag5", "tag6")));
      getResp.close();
    }

    //Delete a tag
    {
      Response deleteResponse = deleteTagForDeviceId(deviceEntityList.get(0).getDeviceId(), "tag1");
      assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
      deleteResponse.close();
    }

    {
      Response getResp = getTagsForDeviceId(deviceEntityList.get(0).getDeviceId());
      String jsonString = getResp.readEntity(String.class);
      validateNotContainsTags(jsonString, deviceEntityList.get(0).getDeviceId(), new ArrayList<String>(Arrays.asList("tag1")));
      validateContainsTags(jsonString, deviceEntityList.get(0).getDeviceId(), new ArrayList<String>(Arrays.asList("tag2", "tag3")));
      getResp.close();
    }

    {
      Response deleteResponse = deleteTagForDeviceId(deviceEntityList.get(0).getDeviceId(), "tag2");
      assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
      deleteResponse.close();
    }

    {
      Response getResp = getTagsForDeviceId(deviceEntityList.get(0).getDeviceId());
      String jsonString = getResp.readEntity(String.class);
      validateNotContainsTags(jsonString, deviceEntityList.get(0).getDeviceId(), new ArrayList<String>(Arrays.asList("tag1", "tag2")));
      validateContainsTags(jsonString, deviceEntityList.get(0).getDeviceId(), new ArrayList<String>(Arrays.asList("tag3")));
      getResp.close();
    }

    {
      Response deleteResponse = deleteAllTagsForDeviceId(deviceEntityList.get(1).getDeviceId());
      assertEquals(Response.Status.OK.getStatusCode(), deleteResponse.getStatus());
      deleteResponse.close();
    }

    {
      Response getResp = getTagsForDeviceId(deviceEntityList.get(1).getDeviceId());
      String jsonString = getResp.readEntity(String.class);
      validateNotContainsTags(jsonString, deviceEntityList.get(1).getDeviceId(), new ArrayList<String>(Arrays.asList("tag4", "tag5", "tag6")));
      getResp.close();
    }
  }

  @Test
  public void testInvalidDeviceId() {
    List<String> tagList = Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6");
    String deviceId = "SomeinvalidDevIceID";

    //set
    {
      Response setResp = setTags(deviceId,tagList);
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), setResp.getStatus());
      setResp.close();
    }

    //get
    {
      Response getResp = getTagsForDeviceId(deviceId);
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResp.getStatus());
      getResp.close();
    }

    //delete
    {
      Response deleteResp = deleteTagForDeviceId(deviceId, "abracadabra");
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteResp.getStatus());
      deleteResp.close();
    }

    //delete all
    {
      Response deleteResp = deleteAllTagsForDeviceId(deviceId);
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteResp.getStatus());
      deleteResp.close();
    }
  }

  @Test
  public void testInvalidTagChars() {
    List<String> tagList1 = Arrays.asList(RandomStringUtils.randomAlphabetic(1000), "tag2", "tag3", "tag4", "tag5", "tag6");
    List<String> tagList2 = Arrays.asList("tag1", "", "tag3", "tag4", "tag5", "tag6");
    List<String> tagList3 = Arrays.asList("tag1", null, "tag3", "tag4", "tag5", "tag6");

    String deviceId = deviceEntityList.get(0).getDeviceId();

    //set tags
    {
      Response response = setTags(deviceId, tagList1);
      assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
      response.close();
    }

    //set tags
    {
      Response response = setTags(deviceId, tagList2);
      assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
      response.close();
    }

    {
      Response response = setTags(deviceId, tagList3);
      assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
      response.close();
    }
  }

  private void validateContainsTags(String jsonString, String deviceId, List<String> tags) throws Exception {
    LOGGER.trace("testSetGetTags : getResp=\n{}", JsonWriter.formatJson(jsonString));
    DeviceTagInfo deviceTagInfo = gson.fromJson(jsonString, new TypeToken<DeviceTagInfo>() {
    }.getType());
    LOGGER.trace("testSetGetTags : receivedJson=\n{}", deviceTagInfo);
    assertEquals(deviceTagInfo.getDeviceId(), deviceId);
    assertTrue(deviceTagInfo.getTags().containsAll(tags));
  }

  private void validateNotContainsTags(String jsonString, String deviceId, List<String> tags) throws Exception {
    LOGGER.trace("testSetGetTags : getResp=\n{}", JsonWriter.formatJson(jsonString));
    DeviceTagInfo deviceTagInfo = gson.fromJson(jsonString, new TypeToken<DeviceTagInfo>() {
    }.getType());
    LOGGER.trace("testSetGetTags : receivedJson=\n{}", deviceTagInfo);
    assertEquals(deviceTagInfo.getDeviceId(), deviceId);
    assertFalse(deviceTagInfo.getTags().containsAll(tags));
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String statementStr1 = "DELETE FROM mmxApp WHERE appName LIKE '%"+"devicetagresourcetestapp"+"%'";
    final String statementStr2 = "DELETE FROM mmxDevice WHERE deviceId LIKE '%"+"mmxdevicetagsresourcetestdevice"+"%'";
    final String statementStr4 = "DELETE FROM mmxTag WHERE tagname LIKE '%" + "tag" + "%'";

    Connection conn = null;
    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt2 = null;
    PreparedStatement pstmt4 = null;

    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt1 = conn.prepareStatement(statementStr1);
      pstmt2 = conn.prepareStatement(statementStr2);
      pstmt4 = conn.prepareStatement(statementStr4);
      pstmt1.execute();
      pstmt2.execute();
      pstmt4.execute();
    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}", e);
    } finally {
      CloseUtil.close(LOGGER, pstmt1, conn);
      CloseUtil.close(LOGGER, pstmt2);
      CloseUtil.close(LOGGER, pstmt4);
    }
  }
}
