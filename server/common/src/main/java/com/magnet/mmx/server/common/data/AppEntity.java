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


package com.magnet.mmx.server.common.data;

import com.magnet.mmx.server.common.spi.IEncryptor;
import com.magnet.mmx.server.common.utils.EncryptorFactory;
import org.jivesoftware.openfire.auth.AuthFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 *  Representation of the App records
 */
public class AppEntity {

  public static final String TABLE_MMX_APP = "mmxApp";
  public static final String COL_ID = "id";
  public static final String COL_APP_NAME = "appName";
  public static final String COL_SERVER_USER_ID = "serverUserId";
  public static final String COL_APP_ID = "appId";
  public static final String COL_API_KEY = "apiKey";
  public static final String COL_ENCRYPTED_API_KEY = "encryptedApiKey";
  public static final String COL_GOOGLE_API_KEY = "googleApiKey";
  public static final String COL_GOOGLE_PROJECT_ID = "googleProjectId";
  public static final String COL_APNS_CERT = "apnsCert";
  public static final String COL_APNS_CERT_PLAIN_PASSWORD = "apnsCertPlainPassword";
  public static final String COL_APNS_CERT_ENCRYPTED_PASSWORD = "apnsCertEncryptedPassword";
  public static final String COL_OWNER_ID = "ownerId";
  public static final String COL_GUEST_USER_ID = "guestUserId";
  public static final String COL_CREATION_DATE = "creationDate";
  public static final String COL_MODIFICATION_DATE = "modificationDate";
  public static final String COL_GUEST_SECRET = "guestSecret";
  public static final String COL_OWNER_EMAIL = "ownerEmail";
  public static final String COL_APNS_CERT_PRODUCTION = "apnsCertProduction";
  /*
   * query string to get mmxApp entity
   */
  public static final String APP_QUERY_STRING = "select " +
      COL_ID + "," +
      COL_APP_NAME + "," +
      COL_SERVER_USER_ID + "," +
      COL_APP_ID + "," +
      COL_API_KEY + "," +
      COL_ENCRYPTED_API_KEY + "," +
      COL_GOOGLE_API_KEY + "," +
      COL_GOOGLE_PROJECT_ID + "," +
      COL_APNS_CERT + "," +
      COL_APNS_CERT_PLAIN_PASSWORD + "," +
      COL_APNS_CERT_ENCRYPTED_PASSWORD + "," +
      COL_OWNER_ID + "," +
      COL_GUEST_USER_ID + "," +
      COL_CREATION_DATE + "," +
      COL_MODIFICATION_DATE + "," +
      COL_GUEST_SECRET + "," +
      COL_OWNER_EMAIL + "," +
      COL_APNS_CERT_PRODUCTION +
      " from " + TABLE_MMX_APP;

  private int id;
  private String name;
  private String serverUserId;
  private String appId;
  private String appAPIKey;
  private String googleAPIKey;
  private String googleProjectId;
  private byte[] apnsCert;
  private String apnsCertPassword;
  private boolean apnsCertProduction;

  private String guestUserId;
  private String guestSecret;
  private String ownerId;
  private String ownerEmail;

  private Date creationDate;
  private Date modificationdate;

  public String getGuestSecret() {
    return guestSecret;
  }

  public void setGuestSecret(String guestSecret) {
    this.guestSecret = guestSecret;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAppAPIKey() {
    return appAPIKey;
  }

  public void setAppAPIKey(String appAPIKey) {
    this.appAPIKey = appAPIKey;
  }

  public String getGoogleAPIKey() {
    return googleAPIKey;
  }

  public void setGoogleAPIKey(String googleAPIKey) {
    this.googleAPIKey = googleAPIKey;
  }

  public byte[] getApnsCert() {
    return apnsCert;
  }

  public void setApnsCert(byte[] apnsCert) {
    this.apnsCert = apnsCert;
  }

  public String getGoogleProjectId() {
    return googleProjectId;
  }

  public void setGoogleProjectId(String googleProjectId) {
    this.googleProjectId = googleProjectId;
  }

  public String getApnsCertPassword() {
    return apnsCertPassword;
  }

  public void setApnsCertPassword(String apnsCertPassword) {
    this.apnsCertPassword = apnsCertPassword;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getModificationdate() {
    return modificationdate;
  }

  public void setModificationdate(Date modificationdate) {
    this.modificationdate = modificationdate;
  }

  public String getGuestUserId() {
    return guestUserId;
  }

  public void setGuestUserId(String guestUserId) {
    this.guestUserId = guestUserId;
  }

  public String getServerUserId() {
    return serverUserId;
  }

  public void setServerUserId(String serverUserId) {
    this.serverUserId = serverUserId;
  }

  public String getOwnerId() {
    return ownerId;
  }
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public boolean isApnsCertProduction() {
    return apnsCertProduction;
  }

  public void setApnsCertProduction(boolean apnsProductionCert) {
    this.apnsCertProduction = apnsProductionCert;
  }

  public static class AppEntityBuilder {

    /**
     * Build the AppEntity using the result.
     * @param rs not null result set. We expect all the appentity columns to be in the result set.
     * @return
     */
    public AppEntity build(ResultSet rs) throws SQLException {
      IEncryptor encryptor = null;
      try {
        encryptor = new EncryptorFactory().createInstance();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InstantiationException e) {
        e.printStackTrace();
      }
      int appId = rs.getInt(COL_ID);
      String appName = rs.getString(COL_APP_NAME);
      String serverUserId = rs.getString(COL_SERVER_USER_ID);
      String appKey = rs.getString(COL_APP_ID);
      String apiKey = rs.getString(COL_API_KEY);
      if (apiKey == null) {
        String encryptedApiKey = rs.getString(COL_ENCRYPTED_API_KEY);
        if (encryptor != null) {
          apiKey = encryptor.getDecrypted(encryptedApiKey);
        } else {
          apiKey = AuthFactory.decryptPassword(encryptedApiKey);
        }
      }
      String googleApiKey = rs.getString(COL_GOOGLE_API_KEY);
      String googleProjectId = rs.getString(COL_GOOGLE_PROJECT_ID);
      byte[] apnsCert = rs.getBytes(COL_APNS_CERT);
      String apnsPassword = rs.getString(COL_APNS_CERT_PLAIN_PASSWORD);
      if (apnsPassword == null) {
        String encryptedApnsPassword = rs.getString(COL_APNS_CERT_ENCRYPTED_PASSWORD);
        if (encryptor != null) {
          apnsPassword = encryptor.getDecrypted(encryptedApnsPassword);
        } else {
          apnsPassword = AuthFactory.decryptPassword(encryptedApnsPassword);
        }
      }
      String ownerId = rs.getString(COL_OWNER_ID);
      String guestUserId = rs.getString(COL_GUEST_USER_ID);
      String guestSecret = rs.getString(COL_GUEST_SECRET);
      Date creationDate = rs.getTimestamp(COL_CREATION_DATE);
      Date modificationDate = rs.getTimestamp(COL_MODIFICATION_DATE);
      String ownerEmail = rs.getString(COL_OWNER_EMAIL);
      int environment = rs.getInt(COL_APNS_CERT_PRODUCTION);
      boolean production = false;
      if (!rs.wasNull()) {
        production = (environment == 1);
      }

      AppEntity entity = new AppEntity();
      entity.setId(appId);
      entity.setName(appName);
      entity.setAppAPIKey(apiKey);
      entity.setAppId(appKey);
      entity.setGoogleAPIKey(googleApiKey);
      entity.setGoogleProjectId(googleProjectId);
      entity.setApnsCert(apnsCert);
      entity.setApnsCertPassword(apnsPassword);
      entity.setServerUserId(serverUserId);
      entity.setOwnerId(ownerId);
      entity.setGuestUserId(guestUserId);
      entity.setGuestSecret(guestSecret);
      entity.setCreationDate(creationDate);
      entity.setModificationdate(modificationDate);
      entity.setOwnerEmail(ownerEmail);
      entity.setApnsCertProduction(production);

      return entity;

    }
  }
}
