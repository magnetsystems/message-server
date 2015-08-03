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
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.CloseUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TopicEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.TopicHelper;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.junit.*;
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
 *
 */
public class MMXTopicTagsResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXTopicTagsResourceTest.class);
  private static String baseUri = "http://localhost:8086/mmxmgmt/api/v1/topics";
  private static BasicDataSource ds;
  private static AppEntity appEntity;
  private static List<TopicEntity> appTopicEntityList = new ArrayList<TopicEntity>();
  private static List<TopicEntity> personalTopicEntityList = new ArrayList<TopicEntity>();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public MMXTopicTagsResourceTest() {
    super(baseUri);
  }

  @BeforeClass
  public static void setupDatabase() throws Exception {
    java.util.logging.Logger.getLogger("com.google.inject").setLevel(java.util.logging.Level.SEVERE);
    org.apache.log4j.Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.DEBUG);
    org.apache.log4j.Logger.getLogger("org.jboss.resteasy").setLevel(Level.OFF);
    InputStream inputStream = MMXTopicTagsResourceTest.class.getResourceAsStream("/test.properties");

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
    appEntity = createRandomApp();
    for(int i=0 ; i < 5; i++) {
      appTopicEntityList.add(createRandomTopicEntity(appEntity, null, i));
    }
    for(int i=0; i < 5; i++) {
      personalTopicEntityList.add(createRandomTopicEntity(appEntity, "user" + i, i));
    }
    LOGGER.warn("Finished setupDatabase");
  }

  public static TopicEntity createRandomTopicEntity(AppEntity appEntity, String username, int index) throws Exception {
    TopicEntity entity = new TopicEntity();
    entity.setServiceId("pubsub");
    String name = "mmxtopicstagsresourcetesttopic" + index;
    entity.setNodeId("/" + appEntity.getAppId() + (username == null ? "/*/" : "/"+username+"/") + name);
    entity.setParent(appEntity.getAppId());
    entity.setName(name);
    entity.setDescription("old description " + index);
    DBTestUtil.getTopicDAO().persist(entity);
    return entity;
  }

  public static AppEntity createRandomApp() throws Exception  {
    String serverUserId = "serverUser";
    String appName = "topictagresourcetestapp";
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
  private Response setTags(String topicId, List<String> tags) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    WebTarget service = getClient().target(getBaseURI() + "/"+topicId+"/tags");
    TagList tagList = new TagList();
    tagList.setTags(tags);
    LOGGER.trace("setTags : topicTags=\n{}", gson.toJson(tagList));
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());

    Response response = invocationBuilder.post(Entity.entity(tagList, MediaType.APPLICATION_JSON));
    return response;
  }

  private Response getTags(String topicId) {
    LOGGER.trace("getTags : topicId={}", topicId);
    WebTarget service = getClient().target(getBaseURI() + "/" + topicId + "/tags");

    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    return response;
  }

  private Response deleteTag(String topicId, String tag) {
    LOGGER.trace("deleteTag : topicId={}, tag={}", topicId, tag);
    WebTarget service = getClient().target(getBaseURI() + "/" + topicId+ "/tags"+"/" + tag);
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.delete();
    return response;
  }

  private Response deleteAllTags(String topicId) {
    LOGGER.trace("deleteAllTags : topicId={}", topicId);
    WebTarget service = getClient().target(getBaseURI() + "/" + topicId+ "/tags");
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.delete();
    return response;
  }

  @Test
  public void testSetGetDeleteTags() throws Exception {
    List<String> tagList = Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6");
    String topicId = TopicHelper.parseNode(appTopicEntityList.get(0).getNodeId()).getName();

    //set tags
    {
      Response response = setTags(topicId, tagList);
      assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
      response.close();
    }

    //get tags
    {
      Response response = getTags(topicId);
      assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
      String jsonString = response.readEntity(String.class);
      validateContainsTags(jsonString, topicId, Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6"));
      response.close();
    }

    // delete a tag
    {
      Response response = deleteTag(topicId, "tag3");
      assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
      response.close();
    }

    {
      Response response = getTags(topicId);
      String jsonString = response.readEntity(String.class);
      validateContainsTags(jsonString, topicId, Arrays.asList("tag1", "tag2", "tag4", "tag5", "tag6"));
      validateNotContainsTags(jsonString, topicId, Arrays.asList("tag3"));
      response.close();
    }

    //delete all tags
    {
      Response response = deleteAllTags(topicId);
      assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
      response.close();
    }

    {
      Response response = getTags(topicId);
      String jsonString = response.readEntity(String.class);
      validateNotContainsTags(jsonString, topicId, Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6"));
      response.close();
    }
  }

  @Test
  public void testInvalidTopicTag() {
    List<String> tagList = Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6");
    String topicId = "SomeinvalidTOpic";

    //set tags
    {
      Response response = setTags(topicId, tagList);
      assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
      response.close();
    }

    //get tags
    {
      Response response = getTags(topicId);
      assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
      response.close();
    }

    //delete a tag
    {
      Response response = deleteTag(topicId, "tag3");
      assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
      response.close();
    }

    //delete all tags
    {
      Response response = deleteAllTags(topicId);
      assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
      response.close();
    }

  }

  @Test
  public void testInvalidTagChars() {
    List<String> tagList1 = Arrays.asList(RandomStringUtils.randomAlphabetic(1000), "tag2", "tag3", "tag4", "tag5", "tag6");
    List<String> tagList2 = Arrays.asList("tag1", "", "tag3", "tag4", "tag5", "tag6");
    List<String> tagList3 = Arrays.asList("tag1", null, "tag3", "tag4", "tag5", "tag6");
    String topicId = TopicHelper.parseNode(appTopicEntityList.get(0).getNodeId()).getName();

    //set tags
    {
      Response response = setTags(topicId, tagList1);
      assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
      response.close();
    }

    //set tags
    {
      Response response = setTags(topicId, tagList2);
      assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
      response.close();
    }

    //set tags
    {
      Response response = setTags(topicId, tagList3);
      assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
      response.close();
    }

  }

  private void validateContainsTags(String jsonString, String topicId, List<String> tags) throws Exception {
    LOGGER.trace("testSetGetTags : getResp=\n{}", JsonWriter.formatJson(jsonString));
    TopicTagInfo topicTagInfo = gson.fromJson(jsonString, new TypeToken<TopicTagInfo>() {
    }.getType());
    LOGGER.trace("testSetGetTags : receivedJson=\n{}", topicTagInfo);
    assertEquals(topicTagInfo.getTopicName(), topicId);
    assertTrue(topicTagInfo.getTags().containsAll(tags));
  }

  private void validateNotContainsTags(String jsonString, String topicId, List<String> tags) throws Exception {
    LOGGER.trace("testSetGetTags : getResp=\n{}", JsonWriter.formatJson(jsonString));
    TopicTagInfo topicTagInfo = gson.fromJson(jsonString, new TypeToken<TopicTagInfo>() {
    }.getType());
    LOGGER.trace("testSetGetTags : receivedJson=\n{}", topicTagInfo);
    assertEquals(topicTagInfo.getTopicName(), topicId);
    assertFalse(topicTagInfo.getTags().containsAll(tags));
  }
  private Set getSetFromTagInfoList(List<TopicTagInfo> topicTagInfos) {
    Set<String> topicIds = new HashSet<String>();
    for(TopicTagInfo tagInfo : topicTagInfos) {
      topicIds.add(tagInfo.getTopicName());
    }
    return topicIds;
  }

  private Set getSetFromEntityList(List<TopicEntity> topicEntities) {
    Set<String> topicIds = new HashSet<String>();
    for(TopicEntity topicEntity : topicEntities) {
      topicIds.add(topicEntity.getNodeId());
    }
    return topicIds;
  }

  @After
  public void cleanupTags() {
    final String statementStr = "DELETE FROM mmxTag WHERE tagname LIKE '%" + "tag" + "%'";
    Connection conn = null;
    PreparedStatement pstmt = null;
    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt = conn.prepareStatement(statementStr);

      pstmt.execute();

    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}", e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String statementStr1 = "DELETE FROM mmxApp WHERE appName LIKE '%"+"mmxtopicstagsresourcetesttopic"+"%'";
    final String statementStr2 = "DELETE FROM ofPubsubNode WHERE nodeID LIKE '%"+"topictagresourcetestapp"+"%'";
    final String statementStr3 = "DELETE FROM mmxTag WHERE tagname LIKE '%" + "tag" + "%'";

    Connection conn = null;
    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt2 = null;
    PreparedStatement pstmt3 = null;

    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt1 = conn.prepareStatement(statementStr1);
      pstmt2 = conn.prepareStatement(statementStr2);
      pstmt3 = conn.prepareStatement(statementStr3);
      pstmt1.execute();
      pstmt2.execute();
      pstmt3.execute();
    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}", e);
    } finally {
      CloseUtil.close(LOGGER, pstmt1, conn);
      CloseUtil.close(LOGGER, pstmt2);
      CloseUtil.close(LOGGER, pstmt3);
    }
  }

}
