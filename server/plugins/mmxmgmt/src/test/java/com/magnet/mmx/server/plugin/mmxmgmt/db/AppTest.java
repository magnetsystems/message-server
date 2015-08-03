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

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AppIDGenerator;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;
import com.magnet.mmx.util.Base64;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AppTest {

  private static BasicDataSource ds;

  @BeforeClass
  public static void setupDB() throws Exception {
    ds = UnitTestDSProvider.getDataSource();
    //clean any existing records and load some records into the database.
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    {
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream("/data/app-data-1.xml");
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
    // load test encryptor class
    Class.forName(EncryptorForTest.class.getName());
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
  public void sanityTest() {
    AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    AppEntity entity = null;
    String appID = AppIDGenerator.generate();
    String serverUserId = "serveruserid";
    //String guestUserId = "guestuserid";
    String appName = "app1";
    String apiKey = "apikey";
    String googleProjectId = "googleprojectid";
    String gooleApiKey = "apikey";
    byte[] apnsCert = "/config/apnscert".getBytes();
    String apnsPasswd = "apnspwd";
    String ownerId = "magnetdev";
    String ownerEmail = "no-reply@magnet.com";
    String guestSecret = "superSecr8t";

    try {
      entity = dao.createApp(serverUserId, appName,appID,apiKey,
                  gooleApiKey,googleProjectId, apnsPasswd,ownerId, ownerEmail, guestSecret, false );
    } catch (Exception ex) {
      fail("failed to create app " + ex.getMessage());
    }

    try {
      AppEntity entity1 = dao.getAppForAppKey(entity.getAppId());
      int id = entity1.getId();
      assertNotNull(entity1);
      assertEquals(appName, entity1.getName());
      assertEquals(appID, entity1.getAppId());
      assertEquals(serverUserId, entity1.getServerUserId());
      assertEquals(apiKey, entity1.getAppAPIKey());
      assertEquals(googleProjectId, entity1.getGoogleProjectId());
      assertEquals(gooleApiKey, entity1.getGoogleAPIKey());
      assertNull(entity1.getApnsCert());
      assertEquals(apnsPasswd, entity1.getApnsCertPassword());
      assertEquals(ownerId, entity1.getOwnerId());
      assertEquals(guestSecret, entity1.getGuestSecret());
      assertEquals(ownerEmail, entity1.getOwnerEmail());
      assertFalse(entity1.isApnsCertProduction());
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("failed to get app "+ex.getMessage());
    }

    try {
      dao.deleteApp(appID);

    } catch (Exception ex) {
      fail("failed to delete app by id " + ex.getMessage());
    }

    try {
      AppEntity entity1 = dao.getAppForAppKey(entity.getAppId());
      assertTrue("App found with appKey:" + entity.getAppId(), entity1 == null);
    } catch (Exception ex) {
      // exception expected
      fail("failed to retrieve app by id " + ex.getMessage());
    }
  }

  /**
   * Test creating app with name matching an existing app.
   */
  @Test
  public void testDuplicateAppName() {
    AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    String appID = AppIDGenerator.generate();
    String serverUserId = "serveruserid";
    String guestSecret = "guestsecret";
    String appName = "LOGIN3_TEST_APP";
    String apiKey = "apikey";
    String googleProjectId = "googleprojectid";
    String gooleApiKey = "apikey";
    String apnsPasswd = "apnspwd";
    String ownerId = "admin@login3s-macbook-pro.local";
    String ownerEmail = "no-reply@magnet.com";

    boolean gotException = false;
    try {
      dao.createApp(serverUserId, appName,appID,apiKey,
          gooleApiKey,googleProjectId, apnsPasswd,ownerId, ownerEmail, guestSecret, false);
    } catch (AppAlreadyExistsException e) {
      gotException = true;
    } catch (Exception ex) {
      fail("failed to create app " + ex.getMessage());
    }
    assertTrue("Didn't get the correct exception", gotException);
  }

  /**
   * Test fetching app with app name.MMXMessageHandlingRule.java:40
   */
  @Test
  public void testGetAppUsingName() {
    AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));
    AppEntity entity = null;
    String appName = "login3_test_app";
    String ownerId = "admin@login3s-macbook-pro.local";
    try {
      entity = dao.getAppForName(appName, ownerId);
     } catch (Exception ex) {
      fail("failed to get app " + ex.getMessage());
    }
    assertNotNull("Got null app object for app name:" + appName, entity);
  }

  @Test
  public void testUpdateAPNsCertificate() {
    try {
      String appId = "i0sq7ddvi17";
      InputStream certStream = DeviceDAOImplTest.class.getResourceAsStream("/apnscert.p12");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      byte[] buffer = new byte[1024];
      int read = -1;
      while ((read = certStream.read(buffer,0, buffer.length)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      byte[] certificate = outputStream.toByteArray();
      AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));
      dao.updateAPNsCertificate(appId, certificate);

      AppEntity revised = dao.getAppForAppKey(appId);
      byte[] readCert = revised.getApnsCert();

      String readCertBase64 = Base64.encodeBytes(readCert);
      String origCertBase64 = Base64.encodeBytes(certificate);

      assertEquals("Not matching certs", origCertBase64, readCertBase64);


    } catch (Exception e){
      fail ("failed: testUpdateAPNsCertificate" + e.getMessage());
      //e.printStackTrace();
    }
  }


  @Test
  public void testCreateAndUpdateTest() {
    try {
      AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));
      AppEntity entity = null;
      String appID = AppIDGenerator.generate();
      String serverUserId = "supercool";
      String appName = "CreateAndUpdateApp";
      String apiKey = "apikey";
      String googleProjectId = "googleprojectid";
      String gooleApiKey = "apikey";
      String apnsPasswd = "apnspwd";
      String ownerId = "magnetdev";
      String ownerEmail = "no-reply@magnet.com";
      String guestSecret = "superSecr8t";

      try {
        entity = dao.createApp(serverUserId, appName,appID,apiKey,
            gooleApiKey,googleProjectId, apnsPasswd,ownerId, ownerEmail, guestSecret, true );
      } catch (Exception ex) {
        fail("failed to create app " + ex.getMessage());
      }
      assertNotNull("Created app is null", entity);
      // now perform an update
      String updatedName =  "new app name";
      String updateGoogleApiKey = "super-g-key";
      String updatedOwnerEmail = "login3@yahoo.com";
      String updatedSecret = "newsecret";
      dao.updateApp(appID, updatedName, updateGoogleApiKey, null, null, updatedOwnerEmail, updatedSecret, true);

      //re get
      AppEntity revised = dao.getAppForAppKey(appID);
      assertEquals("non matching app name", updatedName, revised.getName());
      assertEquals("non matching google api key", updateGoogleApiKey, revised.getGoogleAPIKey());
      assertEquals("non matching owner email", updatedOwnerEmail, revised.getOwnerEmail());
      assertEquals("non matching apns password", apnsPasswd, revised.getApnsCertPassword());
      assertEquals("non matching guest secret", updatedSecret, revised.getGuestSecret());
      assertEquals("non matching apns production environment", true, revised.isApnsCertProduction());

    } catch (Exception e){
      fail ("failed: testCreateAndUpdateTest" + e.getMessage());
    }
  }


  @Test
  public void testFetchingAppsForOwner() {
    String ownerId = "admin@login3s-macbook-pro.local";
    AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));

    List<AppEntity> list = dao.getAppsForOwner(ownerId);

    assertNotNull("AppList is null", list);
    assertEquals("App list doesn't have expected size", 3, list.size());

  }

  @Ignore
  @Test
  public void testFetchingAllApps() {
    AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));

    List<AppEntity> list = dao.getAllApps();

    assertNotNull("AppList is null", list);
    assertEquals("App list doesn't have expected size", 4, list.size());

  }

  @Test
  public void testUpdateAPNsCertificateAndPassword() {
    try {
      String appId = "i0sq7ddvi17";
      InputStream certStream = DeviceDAOImplTest.class.getResourceAsStream("/apnscert.p12");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      byte[] buffer = new byte[1024];
      int read = -1;
      while ((read = certStream.read(buffer,0, buffer.length)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      byte[] certificate = outputStream.toByteArray();
      AppDAO dao = new NonEncryptingAppDAOImpl(new BasicDataSourceConnectionProvider(ds));
      String password = "testing";
      dao.updateAPNsCertificateAndPassword(appId, certificate, password);

      AppEntity revised = dao.getAppForAppKey(appId);
      byte[] readCert = revised.getApnsCert();

      String readCertBase64 = Base64.encodeBytes(readCert);
      String origCertBase64 = Base64.encodeBytes(certificate);

      assertEquals("Not matching certs", origCertBase64, readCertBase64);
      assertEquals("Not matching passwords", password, revised.getApnsCertPassword());

    } catch (Exception e){
      fail ("failed: testUpdateAPNsCertificate" + e.getMessage());
    }
  }

  /**
   * Stub implementation of AppDAO that doesn't use the AuthFactory stuff.
   */
  public static class NonEncryptingAppDAOImpl extends AppDAOImpl {


    public NonEncryptingAppDAOImpl(ConnectionProvider provider) {
      super(provider);
    }

    @Override
    protected String getEncrypted(String value) {
      return Helper.reverse(value);
    }

    @Override
    protected String getDecrypted(String value) {
      return Helper.reverse(value);
    }
  }

}
