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
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.WebConstants;
import com.magnet.mmx.util.Base64;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the AppDAO.
 */
public class AppDAOImpl implements AppDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppDAOImpl.class);

  private static final String INSERT_APP = "INSERT INTO mmxApp (serverUserId, appName, apiKey, encryptedApiKey, " +
      "googleApiKey, googleProjectId, apnsCert, apnsCertPlainPassword, apnsCertEncryptedPassword, " +
      "creationDate, modificationDate, ownerId, appId, guestUserId, guestSecret, ownerEmail, apnsCertProduction) " +
      "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?,?)";

  private static final String QUERY_APP_USING_APPID  = com.magnet.mmx.server.common.data.AppEntity.APP_QUERY_STRING +
      " WHERE appId = ?";

  private static final String QUERY_APP_USING_APIKEY  = com.magnet.mmx.server.common.data.AppEntity.APP_QUERY_STRING +
      " WHERE apiKey = ?";

  private static final String QUERY_APP_USING_APPNAME = com.magnet.mmx.server.common.data.AppEntity.APP_QUERY_STRING +
      " WHERE UPPER(appName) = ? AND ownerId = ?";

  private static final String DELETE_APP_BY_ID = "DELETE FROM mmxApp WHERE appId = ?";

  private static final String GET_ALL_APP_IDS = "SELECT appId FROM mmxApp";

  private static final String GET_ALL_APP_IDS_FOR_OWNER = "SELECT appId from mmxApp where ownerId = ?";

  private static final String UPDATE_APNS_CERTIFICATE_FOR_APP = "UPDATE mmxApp SET apnsCert = ? WHERE appId = ?";

  private static final String UPDATE_APNS_CERTIFICATE_AND_PASSWORD_FOR_APP = "UPDATE mmxApp SET apnsCert = ?,apnsCertEncryptedPassword=? WHERE appId = ?";

  private static final String CLEAR_APNS_CERTIFICATE_FOR_APP = "UPDATE mmxApp SET apnsCert = NULL WHERE appId = ?";

  private static final String CLEAR_APNS_CERTIFICATE_AND_CERTIFICATE_FOR_APP = "UPDATE mmxApp SET apnsCert = NULL,apnsCertEncryptedPassword = NULL, apnsCertPlainPassword = NULL WHERE appId = ?";

  private static final String QUERY_MY_APPS = AppEntity.APP_QUERY_STRING + " WHERE ownerId = ?";

  private ConnectionProvider connectionProvider;

  public AppDAOImpl(ConnectionProvider provider) {
    this.connectionProvider = provider;
  }


  @Override
  public AppEntity getAppForAppKey(String appId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    AppEntity rv = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(QUERY_APP_USING_APPID);
      pstmt.setString(1, appId);

      rs = pstmt.executeQuery();
      if (rs.first()) {
        rv = new AppEntity.AppEntityBuilder().build(rs);
      } else {
        return null;
      }
      rs.close();
      pstmt.close();
    } catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return rv;
  }

  //TODO: Refactor to commonize
  @Override
  public AppEntity getAppUsingAPIKey(String apiKey) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    AppEntity rv = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(QUERY_APP_USING_APIKEY);
      pstmt.setString(1, apiKey);

      rs = pstmt.executeQuery();
      if (rs.first()) {
        rv = new AppEntity.AppEntityBuilder().build(rs);
      } else {
        return null;
      }
      rs.close();
      pstmt.close();
    } catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return rv;
  }

  public void persist(AppEntity a) throws AppAlreadyExistsException {
     createApp(a.getServerUserId(), a.getName(), a.getAppId(), a.getAppAPIKey(), a.getGoogleAPIKey(), a.getGoogleProjectId(), a.getApnsCertPassword(), a.getOwnerId(), a.getOwnerEmail(), a.getGuestSecret(), a.isApnsCertProduction());
  }

  public AppEntity createApp(String serverUserID, String appName,
                             String appId, String apiKey, String googleApiKey,
                             String googleProjectId,
                             String apnsPwd, String ownerId, String ownerEmail, String guestSecret, boolean apnsProductionEnvironment)
                                 throws AppAlreadyExistsException, DbInteractionException {
    /**
     * check if an app with supplied name already exists. if it does
     * then throw AppAlreadyExistsException
     */
    AppEntity other = null;
    other = getAppForName(appName, ownerId);
    if (other != null) {
      LOGGER.warn("App with name [{}] exists", appName);
      throw new AppAlreadyExistsException("Application with name:" + appName + " exists.");
    }

    String encryptedApiKey = getEncrypted(apiKey);
    String apnsEncryptedPwd = getEncrypted(apnsPwd);

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    AppEntity entity = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(INSERT_APP, PreparedStatement.RETURN_GENERATED_KEYS);
      pstmt.setString(1, serverUserID);
      pstmt.setString(2, appName);
      pstmt.setString(3, apiKey);
      pstmt.setString(4, encryptedApiKey);
      pstmt.setString(5, googleApiKey);
      pstmt.setString(6, googleProjectId);
      //set the cert bytes to null
      pstmt.setNull(7, Types.VARBINARY);
      //Note: we no longer save the apns cert password in plain text.
      pstmt.setNull(8, Types.VARCHAR);
      pstmt.setString(9, apnsEncryptedPwd);
      Timestamp now = new Timestamp(new java.util.Date().getTime());
      pstmt.setTimestamp(10, now);
      pstmt.setTimestamp(11, now);
      pstmt.setString(12, ownerId);
      pstmt.setString(13, appId);
      //guest user id should be null
      pstmt.setNull(14, Types.VARCHAR);
      pstmt.setString(15, guestSecret);
      if(ownerEmail != null) {
        pstmt.setString(16, ownerEmail);
      } else {
        pstmt.setNull(16, Types.VARCHAR);
      }
      if (apnsProductionEnvironment) {
        pstmt.setInt(17, 1);
      } else {
        pstmt.setInt(17, 0);
      }
      pstmt.executeUpdate();

      rs = pstmt.getGeneratedKeys();
      Integer id = null;
      if (rs.next()) {
        id = Integer.valueOf(rs.getInt(1));
      }
      rs.close();

      entity = new AppEntity();
      entity.setId(id);
      entity.setName(appName);
      entity.setAppAPIKey(apiKey);
      entity.setAppId(appId);
      entity.setGoogleAPIKey(googleApiKey);
      entity.setGoogleProjectId(googleProjectId);
      entity.setApnsCert(null);
      entity.setApnsCertPassword(apnsPwd);
      entity.setServerUserId(serverUserID);
      entity.setCreationDate(now);
      entity.setModificationdate(now);
      entity.setGuestUserId(null);
      entity.setGuestSecret(guestSecret);
    }
    catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    }
    finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return entity;
  }

  public void deleteApp(String id) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(DELETE_APP_BY_ID);
      pstmt.setString(1, id);
      pstmt.executeUpdate();
      pstmt.close();
    } catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
  }

  /**
   * Encrypt a string value using the openfire AuthFactory
   * @param value
   * @return
   */
  protected String getEncrypted(String value) {
    String encrypted = null;
    try {
      encrypted = AuthFactory.encryptPassword(value);
      // Set password to null so that it's inserted that way.
    } catch (UnsupportedOperationException uoe) {
      // Encrypting the apiKey may have failed. Therefore,

    }
    return encrypted;
  }

  /**
   * Decrypt a string using openfire AuthFactory
   * @param value
   * @return
   */
  protected String getDecrypted(String value) {
    String decrypted = null;
    try {
      decrypted = AuthFactory.decryptPassword(value);
    } catch (UnsupportedOperationException uoe) {
    }
    return decrypted;
  }

  @Override
  public List<String> getAllAppIds(String ownerId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<String> list = new ArrayList<String>();
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(GET_ALL_APP_IDS_FOR_OWNER);
      pstmt.setString(1, ownerId);
      rs = pstmt.executeQuery();
      while(rs.next()) {
        LOGGER.trace("getAllAppIds : appId = {}, ownerId = {}", rs.getString(1));
        list.add(rs.getString(1));
      }
    } catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return list;
  }

  @Override
  public AppEntity getAppForName(String appName, String ownerId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    AppEntity rv = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(QUERY_APP_USING_APPNAME);
      pstmt.setString(1, appName.toUpperCase());
      pstmt.setString(2, ownerId);
      rs = pstmt.executeQuery();
      if (rs.first()) {
        rv = new AppEntity.AppEntityBuilder().build(rs);
      }
      rs.close();
      pstmt.close();
    } catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return rv;
  }

  @Override
  public String getOwnerEmailForApp(String appId) {
    final String queryString = "select ownerEmail from mmxApp where appid = ?";
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String ownerEmail = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(queryString);
      pstmt.setString(1, appId);

      rs = pstmt.executeQuery();
      ownerEmail = rs.getString(1);
      rs.close();
      pstmt.close();
    } catch (SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new DbInteractionException(sqle);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return ownerEmail;
  }

  @Override
  public void updateAPNsCertificate(String appId, byte[] certificate) {
    Connection con = null;
    PreparedStatement pstmt = null;

    if (certificate == null || certificate.length > WebConstants.APNS_CERT_MAX_SIZE) {
      throw new IllegalArgumentException("invalid certificate value");
    }
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(UPDATE_APNS_CERTIFICATE_FOR_APP);
      pstmt.setBytes(1, certificate);
      pstmt.setString(2, appId);
      LOGGER.trace("updateAPNsCertificate :appId={}, certificate={} ", new Object[]{appId, Base64.encodeBytes(certificate)});
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in updating APNs certificate", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public void updateAPNsCertificateAndPassword(String appId, byte[] certificate, String password) {
    Connection con = null;
    PreparedStatement pstmt = null;

    if (certificate == null || certificate.length > WebConstants.APNS_CERT_MAX_SIZE) {
      throw new IllegalArgumentException("invalid certificate value");
    }
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(UPDATE_APNS_CERTIFICATE_AND_PASSWORD_FOR_APP);
      pstmt.setBytes(1, certificate);
      String apnsEncryptedPwd = getEncrypted(password);
      pstmt.setString(2, apnsEncryptedPwd);
      pstmt.setString(3, appId);

      LOGGER.trace("updateAPNsCertificateAndPassword :appId={}, certificate={} ", new Object[]{appId, Base64.encodeBytes(certificate)});
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in updating APNs certificate", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public void clearAPNsCertificate(String appId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(CLEAR_APNS_CERTIFICATE_FOR_APP);
      pstmt.setString(1, appId);
      LOGGER.trace("clearAPNsCertificate :appId={}, statement={}", new Object[]{appId,  pstmt});
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in clearAPNsCertificate", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }

  @Override
  public void clearAPNsCertificateAndPassword(String appId) {
    Connection con = null;
    PreparedStatement pstmt = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(CLEAR_APNS_CERTIFICATE_AND_CERTIFICATE_FOR_APP);
      pstmt.setString(1, appId);
      LOGGER.trace("clearAPNsCertificate :appId={}, statement={}", new Object[]{appId,  pstmt});
      int count = pstmt.executeUpdate();
      pstmt.close();
      con.close();
    } catch (SQLException e) {
      LOGGER.warn("SQL Exception in clearAPNsCertificateAndPassword", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, pstmt, con);
    }
  }


  @Override
  public List<AppEntity> getAppsForOwner(String owner) {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<AppEntity> list = new ArrayList<AppEntity>();
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(QUERY_MY_APPS);
      pstmt.setString(1, owner);

      rs = pstmt.executeQuery();
      while (rs.next()) {
        AppEntity entity = new AppEntity.AppEntityBuilder().build(rs);
        list.add(entity);
      }
    } catch (SQLException e) {
      LOGGER.error("SQL Exception in getAppsForOwner", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return list;
  }

  @Override
  public List<AppEntity> getAllApps() {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    List<AppEntity> list = new ArrayList<AppEntity>();
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(AppEntity.APP_QUERY_STRING);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        AppEntity entity = new AppEntity.AppEntityBuilder().build(rs);
        list.add(entity);
      }
    } catch (SQLException e) {
      LOGGER.error("SQL Exception in getAppsForOwner", e);
      throw new DbInteractionException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
    return list;
  }

  @Override
  public void updateApp(String appId, String appName, String googleApiKey, String googleProjectId, String apnsCertPwd, String ownerEmail, String guestSecret, boolean productionApnsCert) throws
      AppDoesntExistException {
    StringBuilder sb = new StringBuilder();
    sb.append("UPDATE mmxApp SET ");
    int count = 0;
    if (appName != null) {
      sb.append("appName = ?");
      count++;
    }
    if (googleApiKey != null) {
      if (count > 0) {
        sb.append(", ");
      }
      sb.append("googleApiKey = ?");
      count++;
    }
    if (googleProjectId != null) {
      if (count > 0) {
        sb.append(", ");
      }
      sb.append("googleProjectId = ?");
      count++;
    }
    if (apnsCertPwd != null) {
      if (count > 0) {
        sb.append(", ");
      }
      sb.append("apnsCertEncryptedPassword = ?");
      count = count + 1;
    }
    if (ownerEmail != null) {
      if (count > 0) {
        sb.append(", ");
      }
      sb.append("ownerEmail = ?");
      count++;
    }

    if (guestSecret != null) {
      if (count > 0) {
        sb.append(", ");
      }
      sb.append("guestSecret = ?");
      count++;
    }

    if (count > 0) {
      sb.append(", ");
    }
    sb.append("apnsCertProduction = ?");
    count++;

    if (count > 0) {
      sb.append(", ");
    }
    if (count > 0) {
      sb.append("modificationDate = ? WHERE appId = ?");
      count = count + 2;
    }

    if (count == 0) {
      //nothing to do here.
      return;
    }

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      con = connectionProvider.getConnection();
      pstmt = con.prepareStatement(sb.toString());
      int ind = 1;
      if (appName != null) {
        pstmt.setString(ind++, appName);
      }
      if (googleApiKey != null) {
        pstmt.setString(ind++, googleApiKey);
      }
      if (googleProjectId != null) {
        pstmt.setString(ind++, googleProjectId);
      }
      if (apnsCertPwd != null) {
        if (apnsCertPwd.isEmpty()) {
          pstmt.setNull(ind++, Types.VARCHAR);
        } else {
          pstmt.setString(ind++, AuthFactory.encryptPassword(apnsCertPwd));
        }
      }
      if (ownerEmail != null) {
        pstmt.setString(ind++, ownerEmail);
      }
      if (guestSecret != null) {
        pstmt.setString(ind++, guestSecret);
      }
      if (productionApnsCert) {
        pstmt.setInt(ind++, 1);
      } else {
        pstmt.setInt(ind++, 0);
      }
      pstmt.setTimestamp(ind++, new Timestamp(new java.util.Date().getTime()));
      pstmt.setString(ind++, appId);
      int updatedCount = pstmt.executeUpdate();
      pstmt.close();

      if (updatedCount != 1) {
        throw new AppDoesntExistException("No app found with appId = " + appId);
      }
    } catch (SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new AppDoesntExistException(e);
    } finally {
      CloseUtil.close(LOGGER, rs, pstmt, con);
    }
  }
}
