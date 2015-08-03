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

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.util.ServerNotInitializedException;
import com.magnet.mmx.server.plugin.mmxmgmt.util.UserManagerService;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.util.Map;

/**
 * Implementation of MMXUserManager using Openfire  UserManager.
 */
public class OFMMXUserManagerImpl implements MMXUserManager {
  private Logger LOGGER = LoggerFactory.getLogger(OFMMXUserManagerImpl.class.getName());

  private UserManager userManager;

  public OFMMXUserManagerImpl() {
    this.userManager = XMPPServer.getInstance().getUserManager();
  }

  @Override
  public boolean isUserIdTaken(String userId) {
    boolean rv = false;
    try {
      userId = JID.escapeNode(userId);
      User user = userManager.getUser(userId);
      if (user != null) {
        rv = true;
      }
    } catch (UserNotFoundException e) {
    } catch (Throwable e) {
      LOGGER.error("Exception in isUserIdToken", e);
    }
    return rv;
  }

  @Override
  public User createUser(String userId, String password, String name, String email, Map<String, String> metadata) {
    User newUser = null;
    try {
      userId = JID.escapeNode(userId);
      newUser = userManager.createUser(userId, password, name, email);
    } catch (UserAlreadyExistsException e) {
      return null;
    } catch (Throwable e) {
      LOGGER.error("Exception in createUser", e);
      return null;
    }

    if (metadata != null && !metadata.isEmpty()) {
      Map<String, String> userProps = newUser.getProperties();

      for (String key : metadata.keySet()) {
        String value = metadata.get(key);
        userProps.put(key, value);
      }
    }
    return newUser;
  }

  @Override
  public void deleteUser(String userId) {
    try {
      userId = JID.escapeNode(userId);
      User user = userManager.getUser(userId);
      userManager.deleteUser(user);
      UserManagerService.terminateSessions(user.getUsername());
    } catch (UserNotFoundException e) {
      LOGGER.warn("Exception in deleteUser", e);
    } catch (ServerNotInitializedException e) {
      LOGGER.warn("Exception in deleteUser", e);
    } catch (Throwable e) {
      LOGGER.error("Exception in deleteUser", e);
    }
  }

  @Override
  public void markRemoveCurrentGuestUser(String userId) {
    try {
      userId = JID.escapeNode(userId);
      User user = userManager.getUser(userId);
      Map<String, String> props = user.getProperties();
      if (props.containsKey(Constants.MMX_PROP_NAME_USER_GUEST_MODE)) {
        if (true == Boolean.parseBoolean(props.get(Constants.MMX_PROP_NAME_USER_GUEST_MODE))) {
          props.put(Constants.MMX_PROP_NAME_USER_GUEST_MODE, Constants.MMX_PROP_VALUE_USER_GUEST_REMOVE);
        }
      }
    } catch (UserNotFoundException e) {
      //catch and ignore
    } catch (Throwable e) {
      LOGGER.error("Exception in markRemoveCurrentGuestUser", e);
    }

  }
  
  /**
   * Reset the user's password after verifying with question/password.
   * @param userId The user name (with %appId)
   * @param question A question being asked.
   * @param answer A lower case answer.
   * @return The email that the temporary password is sent to.
   */
//  @Override
//  public String resetPassword(String userId, String question, String answer, 
//                               String text) {
//    try {
//      User user = userManager.getUser(userId);
//      Map<String, String> props = user.getProperties();
//      String hashedAnswer = props.get(question);
//      if (hashedAnswer != null && answer != null && 
//          hashedAnswer.equals(CryptoUtil.generateMd5(answer.toLowerCase()))) {
//        String email = user.getEmail();
//        EmailService emailSvc = EmailService.getInstance();
//        if (email != null && !email.isEmpty() && emailSvc != null) {
//          String tmpPwd = AppHelper.generateRandomKey();
//          HashMap<String, String> map = new HashMap<String, String>();
//          map.put("name", user.getName());
//          map.put("password", tmpPwd);
//          map.put("userid", userId);
//          if (text == null) {
//            text = "${name},\n\n"+
//                   "The temporary password for your app: ${password}\n"+
//                   "Please change it after you log in.";
//          }
//          emailSvc.sendMessage(user.getName(), email, "MMX Admin", "no-reply@magnet.com",
//                "Your request to reset password", Utils.eval(text, map), null);
//          user.setPassword(tmpPwd);
//          return email;
//        }
//      }
//      return null;
//    } catch (Throwable e) {
//      return null;
//    }
//  }
}
