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
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.common.utils.DefaultOpenfireEncryptor;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
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
public class MMXUserTagsResourceTest extends BaseJAXRSTest {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXUserTagsResourceTest.class);
  private static String baseUri = "http://localhost:8086/mmxmgmt/api/v1/users";

  private static BasicDataSource ds;
  private static AppEntity appEntity;
  private static List<TestUserDao.UserEntity> userEntityList = new ArrayList<TestUserDao.UserEntity>();

  public MMXUserTagsResourceTest() {
    super(baseUri);
  }

  @BeforeClass
  public static void staticSetup() throws Exception {
    setupDatabase();
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
    MMXUserTagsResourceTest.appEntity = createRandomApp();
    userEntityList.add(createRandomUser(appEntity, 1));
    userEntityList.add(createRandomUser(appEntity, 2));
  }

  public static AppEntity createRandomApp() throws Exception  {

    String serverUserId = "serverUser";
    String appName = "usertagresourcetestapp";
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

  private static TestUserDao.UserEntity createRandomUser(AppEntity appEntity, int index) throws Exception {
    TestUserDao.UserEntity ue = new TestUserDao.UserEntity();
    ue.setEmail(RandomStringUtils.randomAlphabetic(5) + "@" + RandomStringUtils.randomAlphabetic(8) + ".com");
    ue.setName(RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
    ue.setPlainPassword(RandomStringUtils.randomAlphabetic(10));
    ue.setEncryptedPassword(RandomStringUtils.randomAlphabetic(80));
    ue.setCreationDate(org.jivesoftware.util.StringUtils.dateToMillis(new Date()));
    ue.setModificationDate(org.jivesoftware.util.StringUtils.dateToMillis(new Date()));
    ue.setUsername("MMXUserTagsResourceTestUser" + "_" + index + "%" + appEntity.getAppId());
    ue.setUsernameNoAppId("MMXUserTagsResourceTestUser" + "_" + index);
    DBTestUtil.getTestUserDao().persist(ue);
    return ue;
  }

  private Response setTags(String username, List<String> tags) {
    WebTarget setService = getClient().target(getBaseURI() + "/" + username + "/tags");
    TagList tagList = new TagList();
    tagList.setTags(tags);

    Invocation.Builder invocationBuilder = setService.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());

    Response response = invocationBuilder.post(Entity.entity(tagList, MediaType.APPLICATION_JSON));
    return response;
  }

  private Response getTags(String username) {
    WebTarget getService = getClient().target(getBaseURI() + "/" + username + "/tags");
    Invocation.Builder invocationBuilder = getService.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    return response;
  }

  private Response deleteTag(String username, String tag) {
    WebTarget getService = getClient().target(getBaseURI() + "/" + username + "/tags/" + tag);
    Invocation.Builder invocationBuilder = getService.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.delete();
    return response;
  }

  private Response deleteAllTags(String username) {
    WebTarget getService = getClient().target(getBaseURI() + "/" + username + "/tags");
    Invocation.Builder invocationBuilder = getService.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.delete();
    return response;
  }

  @Test
  public void testSetGetDelTags() throws Exception {
    String username = Helper.removeSuffix(userEntityList.get(0).getUsername(), "%");
    List<String> tagList1 = Arrays.asList("tag1", "tag2", "tag3");
    List<String> tagList2 = Arrays.asList("tag4", "tag5", "tag6");

    //set tags 1
    {
      Response setResp = setTags(username, tagList1);
      assertEquals(Response.Status.CREATED.getStatusCode(), setResp.getStatus());
      setResp.close();
    }

    //set tags2
    {
      Response setResp = setTags(username, tagList2);
      assertEquals(Response.Status.CREATED.getStatusCode(), setResp.getStatus());
      setResp.close();
    }

    //get the tags
    {
      Response getResp = getTags(username);
      assertEquals(Response.Status.OK.getStatusCode(), getResp.getStatus());
      String jsonString = getResp.readEntity(String.class);
      ArrayList<String> newList = new ArrayList<String>();
      newList.addAll(tagList1);
      newList.addAll(tagList2);
      validateContainsTags(jsonString, username, tagList1);
    }

    /**
     * delete one tag
     */
    {
      Response delResp = deleteTag(username, "tag1");
      assertEquals(delResp.getStatus(), Response.Status.OK.getStatusCode());
      delResp.close();
    }

    {
      Response getResp = getTags(username);
      assertEquals(Response.Status.OK.getStatusCode(), getResp.getStatus());
      String jsonString = getResp.readEntity(String.class);
      ArrayList<String> newList = new ArrayList<String>();
      newList.addAll(tagList1);
      newList.addAll(tagList2);
      newList.remove("tag1");
      validateContainsTags(jsonString, username, newList);
      validateNotContainsTags(jsonString, username, Arrays.asList("tag1"));
      getResp.close();
    }

    /**
     * delete all tags
     */

    {
      Response delResp = deleteAllTags(username);
      assertEquals(delResp.getStatus(), Response.Status.OK.getStatusCode());
      delResp.close();
    }

    //get the tags and check
    {
      Response getResp = getTags(username);
      assertEquals(Response.Status.OK.getStatusCode(), getResp.getStatus());
      String jsonString = getResp.readEntity(String.class);
      ArrayList<String> newList = new ArrayList<String>();
      newList.addAll(tagList1);
      newList.addAll(tagList2);
      validateNotContainsTags(jsonString, username, newList);
      getResp.close();
    }
  }

  @Test
  public void testInvalidUsername() {
    List<String> tagList = Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6");
    String username = "SomeinvalidUsername";

    //set
    {
      Response setResp = setTags(username,tagList);
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), setResp.getStatus());
      setResp.close();
    }

    //get
    {
      Response getResp = getTags(username);
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResp.getStatus());
      getResp.close();
    }

    //delete
    {
      Response deleteResp = deleteTag(username, "abracadabra");
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteResp.getStatus());
      deleteResp.close();
    }

    //delete all
    {
      Response deleteResp = deleteAllTags(username);
      assertEquals(Response.Status.NOT_FOUND.getStatusCode(), deleteResp.getStatus());
      deleteResp.close();
    }
  }

  private void validateContainsTags(String jsonString, String username, List<String> tags) throws Exception {
    LOGGER.trace("testSetGetTags : getResp=\n{}", JsonWriter.formatJson(jsonString));
    UserTagInfo userTagInfo = gson.fromJson(jsonString, new TypeToken<UserTagInfo>() {
    }.getType());
    LOGGER.trace("testSetGetTags : receivedJson=\n{}", userTagInfo);
    assertEquals(userTagInfo.getUsername(), username);
    assertTrue(userTagInfo.getTags().containsAll(tags));
  }

  private void validateNotContainsTags(String jsonString, String username, List<String> tags) throws Exception {
    LOGGER.trace("testSetGetTags : getResp=\n{}", JsonWriter.formatJson(jsonString));
    UserTagInfo userTagInfo = gson.fromJson(jsonString, new TypeToken<UserTagInfo>() {
    }.getType());
    LOGGER.trace("testSetGetTags : receivedJson=\n{}", userTagInfo);
    assertEquals(userTagInfo.getUsername(), username);
    assertFalse(userTagInfo.getTags().containsAll(tags));
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String statementStr1 = "DELETE FROM mmxApp WHERE appName LIKE '%"+"usertagresourcetestapp"+"%'";
    final String statementStr3 = "DELETE FROM ofUser WHERE username LIKE '%" + "MMXUserTagsResourceTestUser"+"%'";
    final String statementStr4 = "DELETE FROM mmxTag WHERE tagname LIKE '%" + "tag" + "%'";

    Connection conn = null;
    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt3 = null;
    PreparedStatement pstmt4 = null;

    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt1 = conn.prepareStatement(statementStr1);
      pstmt3 = conn.prepareStatement(statementStr3);
      pstmt4 = conn.prepareStatement(statementStr4);
      pstmt1.execute();
      pstmt3.execute();
      pstmt4.execute();
    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}", e);
    } finally {
      CloseUtil.close(LOGGER, pstmt1, conn);
      CloseUtil.close(LOGGER, pstmt3);
      CloseUtil.close(LOGGER, pstmt4);
    }
  }
}
