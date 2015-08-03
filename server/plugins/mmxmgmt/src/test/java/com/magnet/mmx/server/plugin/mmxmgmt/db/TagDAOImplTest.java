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

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */

@RunWith(JMockit.class)
public class TagDAOImplTest {
  private final static int NUM_APPS = 2;
  private final static int NUM_DEVICES_PER_APP = 60;
  private final static int COMMON_DEVICES = 30;
  private final static int NUM_USERS_PER_APP = 10;
  private final static int NUM_TOPICS = 10;
  private final static String TOPICS_PREFIX = "tagtesttopic";
  private final static String APP_NAME_PREFIX = "tagapp";
  private final static String DEVICE_ID_PREFIX = "tagdevice";
  private final static String USERS_PREFIX = "taguser";
  private final static String TAG_PREFIX = "testTag";

  private static List<AppEntity> appEntityList = new ArrayList<AppEntity>();
  private static List<DeviceEntity> deviceEntityList = new ArrayList<DeviceEntity>();
  private static List<TestUserDao.UserEntity> userList = new ArrayList<TestUserDao.UserEntity>();
  private static List<TopicEntity> topicList = new ArrayList<TopicEntity>();


  private static final Logger LOGGER = LoggerFactory.getLogger(TagDAOImplTest.class);

  private final String innerJointTemplate = " INNER JOIN (SELECT DISTINCT deviceId FROM mmxTag WHERE tagname='%s' and appId='%s') as temp%d USING (deviceId) " ;

  private static BasicDataSource ds;


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

    setupMocks();
    generateAppData();
    generateDeviceData();
    generateUserData();
    generateTopicData();
    tagDevices();
  }

  private static void tagDevicesWithTagEntity() throws Exception {
    for(int i = 0; i < deviceEntityList.size(); i++) {
      DeviceEntity entity = deviceEntityList.get(i);
      if ((i + 1) % 1 == 0) {
        DBTestUtil.getTagDAO().persist(getTagEntityFromDeviceEntity(entity, 1));
      }
      if ((i + 1) % 2 == 0) {
        DBTestUtil.getTagDAO().persist(getTagEntityFromDeviceEntity(entity, 2));
      }
      if ((i + 1) % 3 == 0) {
        DBTestUtil.getTagDAO().persist(getTagEntityFromDeviceEntity(entity, 3));
      }
      if ((i + 1) % 4 == 0) {
        DBTestUtil.getTagDAO().persist(getTagEntityFromDeviceEntity(entity, 4));
      }
      if((i + 1) % 5 == 0) {
        DBTestUtil.getTagDAO().persist(getTagEntityFromDeviceEntity(entity, 5));
      }
      LOGGER.trace("tagDevicesWithTagEntity : i={}", i);
    }
  }

  private static void tagDevices() throws Exception {
    for(int i = 0; i < deviceEntityList.size(); i++) {
      DeviceEntity entity = deviceEntityList.get(i);
      if ((i + 1) % 1 == 0) {
        DBTestUtil.getTagDAO().createDeviceTag(TAG_PREFIX + "_1", entity.getAppId(), entity.getDeviceId());
      }
      if ((i + 1) % 2 == 0) {
        DBTestUtil.getTagDAO().createDeviceTag(TAG_PREFIX + "_2", entity.getAppId(), entity.getDeviceId());
      }
      if ((i + 1) % 3 == 0) {
        DBTestUtil.getTagDAO().createDeviceTag(TAG_PREFIX + "_3", entity.getAppId(), entity.getDeviceId());
      }
      if ((i + 1) % 4 == 0) {
        DBTestUtil.getTagDAO().createDeviceTag(TAG_PREFIX + "_4", entity.getAppId(), entity.getDeviceId());
      }
      if((i + 1) % 5 == 0) {
        DBTestUtil.getTagDAO().createDeviceTag(TAG_PREFIX + "_5", entity.getAppId(), entity.getDeviceId());
      }
      LOGGER.trace("tagDevicesWithTagEntity : i={}", i);
    }
  }

  private static TagEntity getTagEntityFromDeviceEntity(DeviceEntity entity, int index) {
    TagEntity tagEntity = new TagEntity();
    tagEntity.setAppId(entity.getAppId());
    tagEntity.setTagname(TAG_PREFIX + "_" + index);
    tagEntity.setDeviceId(entity.getDeviceId());
    return tagEntity;
  }
  public static void setupMocks() {
    new MockUp<AppDAOImpl>() {
      @Mock
      protected String getEncrypted(String value) {
        return value;
      }
    };
  }

  private static void generateAppData() throws Exception {
    for(int i=0; i < NUM_APPS; i++) {
      appEntityList.add(createRandomApp(i));
    }
  }

  private static void generateDeviceData() throws Exception {
    for(int i=0; i < NUM_APPS; i++) {
      for(int j=0; j < NUM_DEVICES_PER_APP; j++) {
        String suffix = ( j < COMMON_DEVICES) ? new Integer(j).toString() : new Integer(j).toString() + "_" + appEntityList.get(i).getName();
        createRandomDeviceEntity(appEntityList.get(i), suffix);
      }
    }
  }

  private static void generateUserData() throws Exception {
    for (int i=0; i < NUM_APPS; i++) {
      for(int j=0; j < NUM_USERS_PER_APP; j++) {
        TestUserDao.UserEntity ue = new TestUserDao.UserEntity();
        ue.setEmail(RandomStringUtils.randomAlphabetic(5) + "@" + RandomStringUtils.randomAlphabetic(8) + ".com");
        ue.setName(RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
        ue.setPlainPassword(RandomStringUtils.randomAlphabetic(10));
        ue.setEncryptedPassword(RandomStringUtils.randomAlphabetic(80));
        ue.setCreationDate(org.jivesoftware.util.StringUtils.dateToMillis(new Date()));
        ue.setModificationDate(org.jivesoftware.util.StringUtils.dateToMillis(new Date()));
        ue.setUsername(USERS_PREFIX + "_" + j + "%" + appEntityList.get(0).getAppId());
        userList.add(ue);
        DBTestUtil.getTestUserDao().persist(ue);
      }
    }
  }

  private static void generateTopicData() throws Exception {
    AppEntity appEntity = appEntityList.get(0);
    String appId = appEntity.getAppId();
    for(int i=0; i < NUM_TOPICS; i++) {
      TopicEntity entity = new TopicEntity();
      entity.setServiceId("pubsub");
      String name =  TOPICS_PREFIX + i;
      entity.setNodeId("/" + appId + "/*/" + name);
      entity.setParent(appId);
      entity.setName(name);
      entity.setDescription("this is a test topic" + i);
      topicList.add(entity);
      DBTestUtil.getTopicDAO().persist(entity);
    }
  }
  private static DeviceEntity createRandomDeviceEntity(AppEntity appEntity, String index) {
    DeviceEntity deviceEntity = new DeviceEntity();
    deviceEntity.setAppId(appEntity.getAppId());
    deviceEntity.setName(RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
    deviceEntity.setOsType(OSType.ANDROID);
    deviceEntity.setCreated(new Date());
    deviceEntity.setOwnerId(appEntity.getOwnerId());
    deviceEntity.setStatus(DeviceStatus.ACTIVE);
    deviceEntity.setDeviceId(DEVICE_ID_PREFIX + "_" + index);
    deviceEntity.setPhoneNumber("+213" + RandomStringUtils.randomNumeric(7));
    deviceEntity.setPhoneNumberRev(StringUtils.reverse(deviceEntity.getPhoneNumber()));
    DBTestUtil.getDeviceDAO().persist(deviceEntity);
    deviceEntityList.add(deviceEntity);
    return deviceEntity;
  }

  private static AppEntity createRandomApp(int index) throws Exception {
    String serverUserId = "serverUser";
    String appName = APP_NAME_PREFIX + index;
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

  @Test
  public void testDeviceTagIQGetQuery() {
    List<String> deviceIdList = new ArrayList<String>();
    for(int i=0; i < COMMON_DEVICES; i++) {
      deviceIdList.add(DEVICE_ID_PREFIX + "_" + i);
    }

    List<DeviceEntity> entityList = DBTestUtil.getDeviceDAO().getDevices(appEntityList.get(0).getAppId(), deviceIdList, DeviceStatus.ACTIVE);

    for(int i=0; i < COMMON_DEVICES; i++) {
      List<String> tags = DBTestUtil.getTagDAO().getTagsForDevice(entityList.get(i));
      LOGGER.trace("testDeviceTagIQGetQuery : tags={}", tags);
      if((i+1) % 1 == 0) {
        assert(tags.contains(TAG_PREFIX + "_" + 1));
      }
      if((i+1) % 2 == 0) {
        assert(tags.contains(TAG_PREFIX + "_" + 2));
      }
      if((i+1) % 3 == 0) {
        assert(tags.contains(TAG_PREFIX + "_" + 3));
      }
      if((i+1) % 4 == 0) {
        assert(tags.contains(TAG_PREFIX + "_" + 4));
      }
      if((i+1) % 5 == 0) {
        assert(tags.contains(TAG_PREFIX + "_" + 5));
      }
    }
  }

  @Test
  public void testDeviceTagIQSetQuery() throws Exception {
    final String tag = TAG_PREFIX + "_omnitag";

    List<String> deviceIdList = new ArrayList<String>();
    for(int i=0; i < COMMON_DEVICES; i++) {
      deviceIdList.add(DEVICE_ID_PREFIX + "_" + i);
    }

    List<DeviceEntity> entityList = DBTestUtil.getDeviceDAO().getDevices(appEntityList.get(0).getAppId(), deviceIdList, DeviceStatus.ACTIVE);
    for(int i=0 ; i< entityList.size(); i++) {
      DBTestUtil.getTagDAO().createDeviceTag(tag, entityList.get(i).getAppId(), entityList.get(i).getId());
    }

    for(int i=0; i < entityList.size(); i++) {
      List<String> tags = DBTestUtil.getTagDAO().getTagsForDevice(entityList.get(0));
      assert(tags.contains(TAG_PREFIX + "_omnitag"));
    }
  }

  @Test
  public void testUserTagIQSetQuery() throws Exception {
    final String tag = TAG_PREFIX + "_omnitag";
    LOGGER.trace("testUserTagIQSetQuery : tag prefix : {}", tag);
    List<String> usernameList = new ArrayList<String>();
    for(int i=0; i < NUM_APPS; i++) {
      for (int j=0; j < NUM_USERS_PER_APP; j++) {
        for(int k=0; k < 5; k++)
          DBTestUtil.getTagDAO().createUsernameTag(tag + "_" + k, appEntityList.get(i).getAppId(),
                                                   userList.get(i * NUM_USERS_PER_APP + j).getUsername());
      }
    }

    for(int i=0; i < NUM_APPS; i++) {
      for (int j=0; j < NUM_USERS_PER_APP; j++) {
        List<TagEntity> tagEntities = DBTestUtil.getTagDAO().
                                      getTagEntitiesForUsername(appEntityList.get(i).getAppId(),
                                              userList.get(i * NUM_USERS_PER_APP + j).getUsername());

        List<String> tags = new ArrayList<String>();

        for(TagEntity e : tagEntities) {
          tags.add(e.getTagname());
        }

        for(int k=0; k <5; k++ ) {
          assert(tags.contains(tag + "_" + k));
        }
      }
    }
  }

  @Test
  public void testTopicTagSetGet() throws Exception {
    final String tag = TAG_PREFIX + "_omnitag";
    LOGGER.trace("testTopicTagSet : tag prefix : {}", tag);

    String appId = appEntityList.get(0).getAppId();
    TagDAO tagDao = DBTestUtil.getTagDAO();
    for(int i=0; i < NUM_TOPICS; i++) {
      tagDao.createTopicTag(tag, appId, topicList.get(i).getServiceId(), topicList.get(i).getNodeId());
      tagDao.createTopicTag(TAG_PREFIX + "_uniquetag_" + i, appId, topicList.get(i).getServiceId(), topicList.get(i).getNodeId());
    }

    for(int i=0; i < topicList.size(); i++) {
      List<String> tagList = tagDao.getTagsForTopic(appId, topicList.get(i).getServiceId(), topicList.get(i).getNodeId());
      assertEquals(tagList.size(), 2);
      assertTrue(tagList.contains(tag));
      assertTrue(tagList.contains(TAG_PREFIX + "_uniquetag_" + i));
    }

    TopicDAO topicDao = DBTestUtil.getTopicDAO();
    {
      List<TopicEntity> topicEntityList = topicDao.getTopicsForTag(TAG_PREFIX + "_omnitag", appId);
      assertEquals(topicEntityList.size(), NUM_TOPICS);
      for (int i = 0; i < NUM_TOPICS; i++) {
        TopicEntity topicEntity = topicEntityList.get(i);
        assertTrue(topicList.contains(topicEntity));
      }
    }

    for(int i=0 ; i < NUM_TOPICS; i++) {
      List<TopicEntity> topicEntityList = topicDao.getTopicsForTag(TAG_PREFIX + "_uniquetag_" + i, appId);
      assertEquals(topicEntityList.size(), 1);
      TopicEntity topicEntity = topicEntityList.get(0);
      assertTrue(topicList.contains(topicEntity));
    }

    for(int i=0 ; i < NUM_TOPICS; i++) {
      List<String> tags = Arrays.asList(TAG_PREFIX + "_omnitag", TAG_PREFIX + "_uniquetag_" + i);
      List<TopicEntity> topicEntityList = topicDao.getTopicsForTagAND(tags, appId);
      assertEquals(topicEntityList.size(), 1);
      TopicEntity topicEntity2 = topicEntityList.get(0);
      assertTrue(topicList.contains(topicEntity2));
    }

    for(int i=0 ; i < NUM_TOPICS; i++) {
      List<String> tags = Arrays.asList(TAG_PREFIX + "_omnitag", TAG_PREFIX + "_uniquetag_" + i);
      List<TopicEntity> topicEntityList = topicDao.getTopicsForTagOR(tags, appId);
      assertEquals(topicEntityList.size(), NUM_TOPICS);
      for(int j=0; j < topicEntityList.size(); j++) {
        TopicEntity topicEntity = topicEntityList.get(j);
        assertTrue(topicList.contains(topicEntity));
      }
    }
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String statementStr1 = "DELETE FROM mmxApp WHERE appName LIKE '%"+APP_NAME_PREFIX+"%'";
    final String statementStr2 = "DELETE FROM mmxDevice WHERE deviceId LIKE '%"+DEVICE_ID_PREFIX+"%'";
    final String statementStr3 = "DELETE FROM ofUser WHERE username LIKE '%" + USERS_PREFIX+"%'";
    final String statementStr4 = "DELETE FROM mmxTag WHERE tagname LIKE '%" + TAG_PREFIX + "%'";
    final String statementStr5 = "DELETE FROM ofPubsubNode WHERE serviceID = 'pubsub'";

    Connection conn = null;
    PreparedStatement pstmt1 = null;
    PreparedStatement pstmt2 = null;
    PreparedStatement pstmt3 = null;
    PreparedStatement pstmt4 = null;
    PreparedStatement pstmt5 = null;

    try {
      conn = UnitTestDSProvider.getDataSource().getConnection();
      pstmt1 = conn.prepareStatement(statementStr1);
      pstmt2 = conn.prepareStatement(statementStr2);
      pstmt3 = conn.prepareStatement(statementStr3);
      pstmt4 = conn.prepareStatement(statementStr4);
      pstmt5 = conn.prepareStatement(statementStr5);

      pstmt1.execute();
      pstmt2.execute();
      pstmt3.execute();
      pstmt4.execute();
      pstmt5.execute();

    } catch (SQLException e) {
      LOGGER.error("cleanupDatabase : {}", e);
    } finally {
       CloseUtil.close(LOGGER, pstmt1, conn);
       CloseUtil.close(LOGGER, pstmt2);
       CloseUtil.close(LOGGER, pstmt3);
       CloseUtil.close(LOGGER, pstmt4);
       CloseUtil.close(LOGGER, pstmt5);
    }
  }
}
