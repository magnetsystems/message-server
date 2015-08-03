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
package com.magnet.mmx.server.plugin.mmxmgmt.api;

import com.google.gson.Gson;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXVersion;
import com.magnet.mmx.server.plugin.mmxmgmt.api.tags.MMXTopicTagsResourceTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.BaseJAXRSTest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.DBTestUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
*/
public class MMXVersionResourceTest extends BaseJAXRSTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(MMXVersionResourceTest.class);
  private static final String baseUri = "http://localhost:8086/mmxmgmt/api/v1/mmx/version";
  private static BasicDataSource ds;
  private static AppEntity appEntity;
  public MMXVersionResourceTest() {
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
    LOGGER.warn("Finished setupDatabase");
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

  @Test
  public void getVersion() {
    WebTarget getService = getClient().target(getBaseURI());
    Invocation.Builder invocationBuilder =
            getService.request(MediaType.APPLICATION_JSON);
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_APP_ID, appEntity.getAppId());
    invocationBuilder.header(MMXServerConstants.HTTP_HEADER_REST_API_KEY, appEntity.getAppAPIKey());
    String jsonString = invocationBuilder.get(String.class);
    Gson gson = new Gson();
    MMXVersionResource.Version version = gson.fromJson(jsonString,  MMXVersionResource.Version.class);
    assertEquals(MMXVersion.getVersion(), version.getVersion());
    LOGGER.trace("getVersion : response={}", jsonString);

  }
}
