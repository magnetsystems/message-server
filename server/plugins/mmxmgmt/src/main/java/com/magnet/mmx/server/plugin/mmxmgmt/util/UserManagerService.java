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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.google.common.base.Strings;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.admin.AdminManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.StreamError;

/**
 */
public class UserManagerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserManagerService.class);
  private static XMPPServer xmppServer = XMPPServer.getInstance();

  private static UserManager getUserManager() throws ServerNotInitializedException {
   UserManager userManager = null;
   if(xmppServer != null) {
     userManager = xmppServer.getUserManager();
     if(userManager == null) {
       LOGGER.trace("getUserManager : Unable to get UserManager instance");
       throw new ServerNotInitializedException();
     }
   } else {
     LOGGER.trace("getUserManager : Unable to get XMPPServer instance");
     throw new ServerNotInitializedException();
   }
   return userManager;
  }

  private static SessionManager getSessionManager() throws ServerNotInitializedException {
    SessionManager sessionManager = null;
    if (xmppServer != null) {
      sessionManager = xmppServer.getSessionManager();
    }
    if (sessionManager == null) {
      LOGGER.warn("getSessionManager : Unable to get session manager instance");
      throw new ServerNotInitializedException();
    }
    return sessionManager;
  }

  public static void createUser(String appId, MMXUserInfo userCreationInfo) throws UserAlreadyExistsException, ServerNotInitializedException {
    LOGGER.trace("createUser : appId={}, username={}, password={}, name={}, email={}");
    if(Strings.isNullOrEmpty(userCreationInfo.getUsername()))
      throw new IllegalArgumentException("Illegal username");
    if(Strings.isNullOrEmpty(userCreationInfo.getPassword()))
      throw new IllegalArgumentException("Illegal password");
    User newUser = getUserManager().createUser(userCreationInfo.getMMXUsername(appId), userCreationInfo.getPassword(),
            userCreationInfo.getName(), userCreationInfo.getEmail());
    if(userCreationInfo.getIsAdmin() != null && userCreationInfo.getIsAdmin()) {
      AdminManager adminManager = AdminManager.getInstance();
      if(adminManager == null) {
        throw new ServerNotInitializedException();
      }
      adminManager.addAdminAccount(newUser.getUsername());
    }
  }

  public static void deleteUser(MMXUserInfo userInfo) throws UserNotFoundException, ServerNotInitializedException{
        deleteUser(userInfo.getAppId(), userInfo);
  }

  public static void deleteUser(String appId, MMXUserInfo userInfo) throws UserNotFoundException, ServerNotInitializedException{
    User user = getUserManager().getUser(userInfo.getMMXUsername(appId));
    getUserManager().deleteUser(user);
    try {
      AdminManager adminManager = AdminManager.getInstance();
      if(adminManager != null) {
        adminManager.removeAdminAccount(user.getUsername());
      }
    } catch (Exception e) {
      LOGGER.trace("deleteUser : exception Caught while removing admin account, ignoring exception user={}", user.getUsername());
    }
    /**
     * Need to terminate any open user sessions.
     */
    // Close the user's connection
    terminateSessions(user.getUsername());
  }

  /**
   * Terminate open session for the user with specified user name. Username should be complete ie it should
   * include the % appid.
   * @param username
   */
  public static void terminateSessions(String username) throws ServerNotInitializedException {
    final StreamError error = new StreamError(StreamError.Condition.not_authorized);
    for (ClientSession session : getSessionManager().getSessions(username) ) {
      LOGGER.info("Terminating session for user with name:{}", username);
      session.deliverRawText(error.toXML());
      session.close();
    }
  }


  public static boolean updateUser(String appId, MMXUserInfo userCreationInfo) throws ServerNotInitializedException, UserNotFoundException {
    boolean created = false;
    try {
      User user = getUserManager().getUser(userCreationInfo.getMMXUsername(appId));
      String password = userCreationInfo.getPassword();
      String name = userCreationInfo.getName();
      String email = userCreationInfo.getEmail();
      Boolean isAdmin = userCreationInfo.getIsAdmin();
      if (password != null) user.setPassword(password);
      if (name != null) user.setName(name);
      if (email != null) user.setEmail(email);
      if(isAdmin != null) {
        AdminManager adminManager = AdminManager.getInstance();
        if (isAdmin == true) {
          if(adminManager == null)
            throw new ServerNotInitializedException();
          adminManager.addAdminAccount(user.getUsername());
        } else if (isAdmin == false) {
          adminManager.removeAdminAccount(user.getUsername());
        }
      }
    } catch (UserNotFoundException e) {
      LOGGER.trace("updateUser : user does not exist, creating a user userCreationInfo={}", userCreationInfo);
      try {
        createUser(appId, userCreationInfo);
        created = true;
      } catch (UserAlreadyExistsException e1) {
        LOGGER.error("updateUser : user did not exist but creation failed  userCreationInfo={}", userCreationInfo);
        throw e;
      }
    }
    return created;
  }

  public static MMXUserInfo getUser(String appId, String username) throws UserNotFoundException{

    String mmxUsername = Helper.getMMXUsername(username, appId);
    MMXUserInfo userInfo = new MMXUserInfo();

    try {
      User user = getUserManager().getUser(mmxUsername);
      userInfo.setUsername(username);
      //userInfo.setAppId(appId);
      userInfo.setEmail(user.getEmail());
      userInfo.setIsAdmin(AdminManager.getInstance().isUserAdmin(username, true));
      userInfo.setName(user.getName());
      return userInfo;
    } catch (Exception e) {
      throw new UserNotFoundException(username + " not found", e);
    }
  }
}
