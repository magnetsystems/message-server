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
package com.magnet.mmx.server.plugin.mmxmgmt.api.topics;

import com.cedarsoftware.util.io.JsonWriter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.common.utils.DefaultOpenfireEncryptor;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPubSubItem;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPubSubPayload;
import com.magnet.mmx.server.plugin.mmxmgmt.message.PubSubItemResult;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jivesoftware.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.junit.AfterClass;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 */

@RunWith(JMockit.class)
public class MMXTopicsItemsResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXTopicsItemsResourceTest.class);
  private static String baseUri = "http://localhost:8086/mmxmgmt/api/v1/topics";

  private static BasicDataSource ds;
  private static String SERVICE_ID = "pubsub";
  private static String USERNAME = "bbbbbbb";
  private static String TOPIC_NAME = "cccccccc";
  private static String JID = "user1%aaaaaaa@localhost/testdevice";
  private static final Date testStartTime = new Date();
  private static final List<TopicItemEntity> topicItemEntityList = new ArrayList<TopicItemEntity>();
  private static AppEntity appEntity;

  public MMXTopicsItemsResourceTest() {
    super(baseUri);
  }


  @BeforeClass
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
    DBTestUtil.setupMockDBUtil();
    appEntity = createRandomApp();
    generatePubsubItems();
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

  public static void generatePubsubItems() {
    for(int i=1; i <= 10; i++) {
      TopicItemEntity entity = new TopicItemEntity();
      entity.setServiceId(SERVICE_ID);
      entity.setNodeId(getNodeId());
      entity.setId(RandomStringUtils.randomAlphanumeric(10));
      entity.setJid(JID);
      Date creationDate = new DateTime(testStartTime).minusDays(i).toDate();
      entity.setPayload(getRandomPublishedItemPayload(creationDate));
      String dateStr = StringUtils.dateToMillis(creationDate);
      LOGGER.trace("generatePubsubItems : persisting with date={}", creationDate);
      entity.setCreationDate(dateStr);
      topicItemEntityList.add(entity);
      DBTestUtil.getTopicItemDAO().persist(entity);
    }
  }
  
  @Test
  public void getPubsubItemsBasic() throws Exception {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    WebTarget service = getClient().target(getBaseURI() + "/" + TOPIC_NAME + "/items");

    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    String jsonString = response.readEntity(String.class);
    LOGGER.trace("getPubsubItemsBasic : jsonString={}", JsonWriter.formatJson(jsonString));
    response.close();

    PubSubItemResult result = gson.fromJson(jsonString, new TypeToken<PubSubItemResult>() {
    }.getType());

    List<MMXPubSubItem> receivedList = result.getItems();


    ArrayList<MMXPubSubItem> originalList = Lists.newArrayList(Helper.getPublishedItems(appEntity.getAppId(), topicItemEntityList));
    sortAscending(originalList);

    for(int i=0; i < receivedList.size(); i++) {
      assertEquals(originalList.get(i), receivedList.get(i));
    }
  }

  @Test
  public void getPubsubItemsBySince() throws Exception {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    int reverseTimeOffset = RandomUtils.nextInt(1,10);
    String dateSince = new DateTime(testStartTime).minusDays(reverseTimeOffset).toString();
    LOGGER.trace("getPubsubItemsBySince : dateSince={}", dateSince);
    WebTarget service = getClient().target(getBaseURI() + "/" + TOPIC_NAME + "/items")
            .queryParam(MMXTopicsItemsResource.SINCE_PARAM, dateSince);

    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    String jsonString = response.readEntity(String.class);
    LOGGER.trace("getPubsubItemsBySince : jsonString={}", JsonWriter.formatJson(jsonString));
    response.close();
    PubSubItemResult result = gson.fromJson(jsonString, new TypeToken<PubSubItemResult>() {
    }.getType());

    List<MMXPubSubItem> receivedList = result.getItems();

    ArrayList<MMXPubSubItem> originalList = Lists.newArrayList(Helper.getPublishedItems(appEntity.getAppId(), topicItemEntityList));
    sortAscending(originalList);
    assertEquals(receivedList.size(), reverseTimeOffset);
    for(int i=0; i < receivedList.size(); i++) {
      assertEquals(originalList.get(i + 10 - reverseTimeOffset), receivedList.get(i));
    }
  }

  @Test
  public void getPubsubItemsByUntil() throws Exception {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    int reverseTimeOffset = RandomUtils.nextInt(1,10);
    WebTarget service = getClient().target(getBaseURI() + "/" + TOPIC_NAME + "/items")
            .queryParam(MMXTopicsItemsResource.UNTIL_PARAM, new DateTime(testStartTime).minusDays(reverseTimeOffset).toString());

    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    String jsonString = response.readEntity(String.class);
    LOGGER.trace("getPubsubItemsByUntil : jsonString={}", JsonWriter.formatJson(jsonString));
    response.close();
    PubSubItemResult result = gson.fromJson(jsonString, new TypeToken<PubSubItemResult>() {
    }.getType());

    List<MMXPubSubItem> receivedList = result.getItems();

    ArrayList<MMXPubSubItem> originalList = Lists.newArrayList(Helper.getPublishedItems(appEntity.getAppId(), topicItemEntityList));
    sortDescending(originalList);
    assertEquals(receivedList.size(), 11 - reverseTimeOffset);
    LOGGER.trace("getPubsubItemsByUntil : reverseTimeOffset={}, size = {}", reverseTimeOffset, receivedList.size());
    LOGGER.trace("getPubsubItemsByUntil : \nreceivedList=\n{}, \noriginalList=\n{}", receivedList, originalList);
    for(int i=0; i < receivedList.size(); i++) {
      assertEquals(originalList.get(i + reverseTimeOffset - 1), receivedList.get(i));
    }
  }

  @Test
  public void getPubsubItemsByUntilAndSinceDescending() throws Exception {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    int start = RandomUtils.nextInt(1, 9);
    int end = RandomUtils.nextInt(start+1, 10);

    WebTarget service = getClient().target(getBaseURI() + "/" + TOPIC_NAME + "/items")
            .queryParam(MMXTopicsItemsResource.SINCE_PARAM, new DateTime(testStartTime).minusDays(10).plusDays(start).toString())
            .queryParam(MMXTopicsItemsResource.UNTIL_PARAM, new DateTime(testStartTime).minusDays(10).plusDays(end).toString())
            .queryParam(MMXTopicsItemsResource.SORT_PARAM, MMXServerConstants.SORT_ORDER_DESCENDING);
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    String jsonString = response.readEntity(String.class);
    LOGGER.trace("getPubsubItemsByUntil : jsonString={}", JsonWriter.formatJson(jsonString));
    response.close();

    PubSubItemResult result = gson.fromJson(jsonString, new TypeToken<PubSubItemResult>() {
    }.getType());

    List<MMXPubSubItem> receivedList = result.getItems();

    ArrayList<MMXPubSubItem> originalList = Lists.newArrayList(Helper.getPublishedItems(appEntity.getAppId(), topicItemEntityList));
    sortDescending(originalList);

    for(int i=0; i < receivedList.size(); i++) {
      assertEquals(originalList.get(i + 9 -end), receivedList.get(i));
    }
  }

  @Test
  public void getPubsubItemsByUntilAndSinceAscending() throws Exception {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    int start = RandomUtils.nextInt(1, 9);
    int end = RandomUtils.nextInt(start+1, 10);

    WebTarget service = getClient().target(getBaseURI() + "/" + TOPIC_NAME + "/items")
            .queryParam(MMXTopicsItemsResource.SINCE_PARAM, new DateTime(testStartTime).minusDays(10).plusDays(start).toString())
            .queryParam(MMXTopicsItemsResource.UNTIL_PARAM, new DateTime(testStartTime).minusDays(10).plusDays(end).toString())
            .queryParam(MMXTopicsItemsResource.SORT_PARAM, MMXServerConstants.SORT_ORDER_ASCENDING);
    Invocation.Builder invocationBuilder =
            service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    Response response = invocationBuilder.get();
    String jsonString = response.readEntity(String.class);
    LOGGER.trace("getPubsubItemsByUntil : jsonString={}", JsonWriter.formatJson(jsonString));
    response.close();
    PubSubItemResult result = gson.fromJson(jsonString, new TypeToken<PubSubItemResult>() {
    }.getType());

    List<MMXPubSubItem> receivedList = result.getItems();
    ArrayList<MMXPubSubItem> originalList = Lists.newArrayList(Helper.getPublishedItems(appEntity.getAppId(), topicItemEntityList));
    sortAscending(originalList);

    for(int i=0; i < receivedList.size(); i++) {
      assertEquals(originalList.get(start + i), receivedList.get(i));
    }
  }

  private static String getNodeId() {
    return "/" + appEntity.getAppId() + "/" + "*" +  "/" + TOPIC_NAME;
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String statementStr1 = "DELETE FROM mmxApp WHERE appName LIKE '%"+"mmxtopicstagsresourcetesttopic"+"%'";
    final String statementStr2 = "DELETE FROM ofPubsubItem where serviceID = ? AND nodeID = ?";
    Connection conn = null;
    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt2 = null;

    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt1 = conn.prepareStatement(statementStr1);
      pstmt1.executeUpdate();
      pstmt2 = conn.prepareStatement(statementStr2);
      pstmt2.setString(1, SERVICE_ID);
      pstmt2.setString(2, getNodeId());
      pstmt2.executeUpdate();

    } catch(SQLException e) {
      LOGGER.error("cleanupDatabase : caught exception cleaning ofPubsubItem");
    } finally {
      CloseUtil.close(LOGGER, pstmt2, conn);
    }
  }

  public static String getRandomPublishedItemPayload(Date creationDate) {
    String base = "<mmx xmlns=\"com.magnet:msg:payload\">" +
                  "<meta>%s</meta>" +
                  "<payload mtype=\"text/plain\" chunk=\"0/7/7\" stamp=\"%s\">%s</payload></mmx>";
    int numKeys = RandomUtils.nextInt(1,10);
    String keySuffix = RandomStringUtils.randomAlphabetic(3);
    Map<String, String> map = new HashMap<String, String>();
    for(int i=0; i < numKeys; i++) {
      map.put("key" + keySuffix+i, RandomStringUtils.randomAlphabetic(10));
    }
    Gson gson = new Gson();
    String mapStr = gson.toJson(map);
    return String.format(base, mapStr,  new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(creationDate), RandomStringUtils.randomAlphabetic(25));
  }

  private void sortAscending(List<MMXPubSubItem> list) {
    Collections.sort(list, new Comparator<MMXPubSubItem>(){
      public int compare(MMXPubSubItem o1, MMXPubSubItem o2){
        MMXPubSubPayload payload1 = o1.getPayload();
        MMXPubSubPayload payload2 = o2.getPayload();
        DateTime d1 = new DateTime(payload1.getCreationDate());
        DateTime d2 = new DateTime(payload2.getCreationDate());
        return DateTimeComparator.getInstance().compare(d1, d2);
      }
    });
  }

  private void sortDescending(List<MMXPubSubItem> list) {
    Collections.sort(list, new Comparator<MMXPubSubItem>(){
      public int compare(MMXPubSubItem o1, MMXPubSubItem o2){
        MMXPubSubPayload payload1 = o1.getPayload();
        MMXPubSubPayload payload2 = o2.getPayload();
        DateTime d1 = new DateTime(payload1.getCreationDate());
        DateTime d2 = new DateTime(payload2.getCreationDate());
        return DateTimeComparator.getInstance().compare(d2, d1);
      }
    });
  }
}
