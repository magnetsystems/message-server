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

package com.magnet.mmx.server.plugin.mmxmgmt.handler;

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.APNS;
import com.magnet.mmx.protocol.AppCreate;
import com.magnet.mmx.protocol.AppRead;
import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.protocol.GCM;
import com.magnet.mmx.protocol.MMXStatus;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppAlreadyExistsException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDoesntExistException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppManagementException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DbInteractionException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.event.MMXMaxAppLimitReachedEvent;
import com.magnet.mmx.server.plugin.mmxmgmt.monitoring.MaxAppLimitExceededException;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertEventsManager;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AlertsUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AppIDGenerator;
import com.magnet.mmx.util.AppHelper;
import com.magnet.mmx.util.Utils;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletResponse;

public class MMXAppManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAppManager.class) ;

  private XMPPServer server = XMPPServer.getInstance();

  private static MMXAppManager instance = null;

  protected MMXAppManager() {
  }

  public static MMXAppManager getInstance() {
    if(instance == null) {
      instance = new MMXAppManager();
    }
    return instance;
  }

  public AppRead.Response getApp(String appKey) throws AppDoesntExistException {
    AppDAO dao = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity entity  = dao.getAppForAppKey(appKey);

    if (entity == null) {
      throw new AppDoesntExistException("App with specified id not found");
    }
    AppRead.Response response = new AppRead.Response();
    response.setOwnerId(entity.getOwnerId());
    response.setApiKey(entity.getAppAPIKey());
    response.setAppId(entity.getAppId());
    response.setAppName(entity.getName());
    response.setGuestUserSecret(entity.getGuestSecret());
    response.setCreationDate(Utils.buildISO8601DateFormat().format(entity.getCreationDate()));
    APNS apns = new APNS(null, entity.getApnsCertPassword());
    GCM gcm = new GCM(entity.getGoogleProjectId(), entity.getGoogleAPIKey());
    response.setGcm(gcm);
    response.setApns(apns);

    return response;
  }

  public AppCreate.Response createApp(String appName, String serverUserId,
                                      String serverUserKey, String guestSecret,
                                      String googleApiKey, String googleProjectId,
                                      String apnsPwd, String ownerID, String ownerEmail, boolean productionApnsCert)
                            throws MaxAppLimitExceededException, AppAlreadyExistsException, AppManagementException {
    if(AlertsUtil.maxAppsLimitReached(ownerID)) {
      LOGGER.trace("createApp : reached app limit for ownerId={}", ownerID);
      int limit = AlertsUtil.getMaxAppLimit();
      AlertEventsManager.post(new MMXMaxAppLimitReachedEvent(limit, ownerID));
      throw new MaxAppLimitExceededException("max app limit reached");
    }
    if (!AppHelper.validateAppName(appName)) {
      throw new IllegalArgumentException("invalid app name");
    }
    if(!Strings.isNullOrEmpty(ownerEmail)) {
      try {
        InternetAddress address = new InternetAddress(ownerEmail);
        address.validate();
      } catch (AddressException e) {
        throw new AppManagementException("invalid owner email address");
      }
    }
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    String appId = AppIDGenerator.generate();
    String apiKey = AppHelper.generateApiKey();
    // Generate a secret key for the guest password, or a common key between
    // client app and app server.
    if (guestSecret == null || guestSecret.isEmpty()) {
      guestSecret = AppHelper.generateRandomKey();
    }
    // Add all these entries in our database.
    // TODO: if server user ID is null, don't create the app server user account.
    serverUserId = (serverUserId == null) ? 
        AppHelper.generateRandomPositiveKey() : serverUserId;
    String serverUser = AppHelper.generateUser(null, serverUserId, appId);
    if (serverUserKey == null) {
      serverUserKey = AppHelper.generateRandomKey();  // generate a random one
    }
    User suser = null;
    AppEntity app = null;
    try {
      if (serverUser != null) {
        suser = createUser(serverUser, serverUserKey,
          userNameToDisplayName(appName + " ", serverUser, null));
      }

      app = appDAO.createApp(((suser == null) ? null : suser.getUsername()),
          appName, appId,
          apiKey, googleApiKey, googleProjectId, apnsPwd,
          ownerID, ownerEmail, guestSecret, productionApnsCert);
    } catch (UserAlreadyExistsException e) {
      throw new AppManagementException(e);
    } catch (AppAlreadyExistsException e) {
      // Remove user that we already created above and return an error.
      if (suser != null) {
        deleteUser(suser);
      }
      throw e;
    } catch (DbInteractionException e) {
      if (suser != null) {
        deleteUser(suser);
      }
      throw e;
    }
    // create the app collection topic node
    MMXTopicManager topicManager = MMXTopicManager.getInstance();
    topicManager.createCollectionNode(suser.getUsername(), appId, null);
    AppCreate.Response response = new AppCreate.Response();
    response.setApiKey(apiKey);
    response.setAppId(appId);
    response.setCreationDate(app.getCreationDate());
    response.setGuestSecret(guestSecret);
    return response;
  }



  public MMXStatus updateApp(String appId, String appName, String googleApiKey, String googleProjectId, String apnsCertInBase64,
                        String apnsPwd) throws AppDoesntExistException {

    if (appName != null && !AppHelper.validateAppName(appName)) {
      throw new IllegalArgumentException("invalid app name");
    }
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    appDAO.updateApp(appId, appName, googleApiKey, googleProjectId, apnsPwd, null, null, false);

    MMXStatus result = new MMXStatus();
    result.setCode(HttpServletResponse.SC_OK);
    result.setMessage("Successfully updated app " + appId);

    return result;
  }

  public void deleteApp(String appId) throws AppDoesntExistException, UserNotFoundException {
    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    String userName = appEntity.getServerUserId();
    if (userName != null) {
      User user = server.getUserManager().getUser(userName);
      if (user != null) {
        // delete the OS topic and its children.
        MMXTopicManager topicManager =  MMXTopicManager.getInstance();
        // delete the app server user.
        server.getUserManager().deleteUser(user);
      }
    }
    // now delete the bootstrap client user
    String clientBootstrapUserId = appEntity.getGuestUserId();
    if (clientBootstrapUserId != null) {
      User user = server.getUserManager().getUser(clientBootstrapUserId);
      if (user != null) {
        server.getUserManager().deleteUser(user);
      }
    }

    //TODO cleanup pending messages, devices, message status
    // delete all the users for this appId

    appDAO.deleteApp(appId);
  }

  private User createUser(String userName, String password, String displayName)
                            throws UserAlreadyExistsException {
    if (userName.length() >= 64) {
      throw new IllegalArgumentException("user name too long");
    }

    boolean usePlainPassword = JiveGlobals.getBooleanProperty("user.usePlainPassword");
    if (usePlainPassword && password.length() >= 32) {
      throw new IllegalArgumentException("password too long");
    }

    XMPPServer server = XMPPServer.getInstance();
    User user = server.getUserManager().createUser(userName, password,
        displayName, null);
    return user;
  }

  private void deleteUser(User serverUser) {
    XMPPServer server = XMPPServer.getInstance();
    server.getUserManager().deleteUser(serverUser);
  }
  
  // Keep letters or digits before % or @.  First letter will be capitalized.
  private String userNameToDisplayName(String prefix, String userName, String suffix) {
    int len;
    StringBuilder sb = new StringBuilder(userName.length());
    int delpos = userName.lastIndexOf(Constants.APP_ID_DELIMITER);
    if (delpos <= 0) {
      len = userName.length();
    } else {
      len = delpos;
    }
    boolean is1stLetter = true;
    for (int i = 0; i < len; i++) {
      char c = userName.charAt(i);
      if (c == '@')
        break;
      if (Character.isLetterOrDigit(c)) {
        c = is1stLetter ? Character.toUpperCase(c) : Character.toLowerCase(c);
        sb.append(c);
        is1stLetter = false;
      } else {
        if (!is1stLetter)
          sb.append(' ');
        is1stLetter = true;
      }
    }
    // Remove trailing space
    if (sb.charAt(sb.length()-1) == ' ') {
      sb.setLength(sb.length()-1);
    }
    
    int maxPreLen = 0, preLen = 0, maxSufLen = 0, sufLen = 0;
    if (prefix != null)
      maxPreLen = preLen = prefix.length();
    if (suffix != null)
      maxSufLen = sufLen = suffix.length();
    // DB schema limits the display name to be 100 characters.
    if ((preLen + sufLen + sb.length()) > 100) {
      len = (100 - sb.length()) / 2;
      maxPreLen = Math.min(len, preLen);
      maxSufLen = Math.min((len + len - maxPreLen), sufLen);
    }
    if ((maxPreLen + maxSufLen) == 0)
      return sb.toString();
    StringBuilder buf = new StringBuilder(maxPreLen + sb.length() + maxSufLen);
    if (prefix != null) {
      buf.append(maxPreLen != preLen ? prefix.substring(0, maxPreLen) : prefix);
    }
    buf.append(sb);
    if (suffix != null) {
      buf.append(maxSufLen != sufLen ? suffix.substring(0, maxSufLen) : suffix);
    }
    return buf.toString();
  }

}
