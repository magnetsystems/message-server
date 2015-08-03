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
package com.magnet.mmx.server.plugin.mmxmgmt.api.user;

import com.cedarsoftware.util.io.JsonWriter;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.common.utils.DefaultOpenfireEncryptor;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.CloseUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.TestUserDao;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
*/
@RunWith(JMockit.class)
public class MMXUsersResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXUsersResourceTest.class);
  private static final String baseUri = "http://localhost:8086/mmxmgmt/api/v1/users";

  private static BasicDataSource ds;
  private static AppEntity appEntity;
  private static List<TestUserDao.UserEntity> userEntityList = new ArrayList<TestUserDao.UserEntity>();

  public MMXUsersResourceTest() {
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
    MMXUsersResourceTest.appEntity = createRandomApp();
    for(int i=0; i < 10; i++) {
      userEntityList.add(createRandomUser(appEntity, i));
    }
  }

  private static TestUserDao.UserEntity createRandomUser(AppEntity appEntity, int index) throws Exception {
    TestUserDao.UserEntity ue = new TestUserDao.UserEntity();
    ue.setEmail(RandomStringUtils.randomAlphabetic(5) + "@" + RandomStringUtils.randomAlphabetic(8) + ".com");
    ue.setName(RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
    ue.setPlainPassword(RandomStringUtils.randomAlphabetic(10));
    ue.setEncryptedPassword(RandomStringUtils.randomAlphabetic(80));
    ue.setCreationDate(org.jivesoftware.util.StringUtils.dateToMillis(new Date()));
    ue.setModificationDate(org.jivesoftware.util.StringUtils.dateToMillis(new Date()));
    ue.setUsername("MMXUsersResourceTestUser" + "_" + index + "%" + appEntity.getAppId());
    ue.setUsernameNoAppId("MMXUsersResourceTestUser" + "_" + index);
    DBTestUtil.getTestUserDao().persist(ue);
    return ue;
  }

  public static AppEntity createRandomApp() throws Exception  {

    String serverUserId = "serverUser";
    String appName = "usersresourcetestapp";
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
  public void testSearch10() throws Exception {
    WebTarget service = getClient().target(getBaseURI());
    Invocation.Builder invocationBuilder =
        service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());

    Response response = invocationBuilder.get();

    String body = response.readEntity(String.class);
    LOGGER.trace("testSearch : body:{}", JsonWriter.formatJson(body));
  }

  @Test
  public void testSearchByName() throws Exception {
    String name = userEntityList.get(0).getName();
    WebTarget service = getClient().target(getBaseURI()).queryParam(MMXUsersResource.NAME_PARAM, name);
    LOGGER.trace("testSearch1 : querying by name={}", name);

    Invocation.Builder invocationBuilder =
        service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());

    Response response = invocationBuilder.get();
    String body = response.readEntity(String.class);
    LOGGER.trace("testSearch : body:{}", JsonWriter.formatJson(body));
  }

  @Test
  public void testSearchByEmail() throws Exception {
    String email = userEntityList.get(0).getEmail();
    WebTarget service = getClient().target(getBaseURI()).queryParam(MMXUsersResource.EMAIL_PARAM, email);

    LOGGER.trace("testSearchByEmail : querying by email={}", email);

    Invocation.Builder invocationBuilder =
        service.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());

    Response response = invocationBuilder.get();
    String body = response.readEntity(String.class);

    LOGGER.trace("testSearchByEmail : {}", JsonWriter.formatJson(body));
  }

  @AfterClass
  public static void cleanupDatabase() {
    final String statementStr1 = "DELETE FROM mmxApp WHERE appName LIKE '%"+"usersresourcetestapp"+"%'";
    final String statementStr3 = "DELETE FROM ofUser WHERE username LIKE '%" + "MMXUsersResourceTestUser"+"%'";
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
