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

import org.jivesoftware.openfire.XMPPServer;
import org.xmpp.packet.JID;

import com.magnet.mmx.protocol.Constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to parse multi-tenant JID.  A normal full JID has a format of
 * "node@domain/resource", and a multi-tenant JID has a raw format of
 * "userID%appId@domain/resource".  Using the JID escaping, the node consists
 * of "userID%appId" and the userID allows email address format as well.
 */
public class JIDUtil {
  public static final String APP_ID_DELIMITER = "%";
  public static final char RESOURCE_DELIMITER = '/';
  public static final char DOMAIN_DELIMITER = '@';
  private final static String USER_NAME_PATTERN_STRING = Constants.AT_SIGN_DISALLOWED ?
      "^[a-zA-Z0-9_\\.\\-]*$" : "^[a-zA-Z0-9@_\\.\\-]*$";
  private static final Pattern USER_NAME_PATTERN = Pattern.compile(USER_NAME_PATTERN_STRING);

  /**
   * Get the human readable node which consists of unescaped userID and appId.
   * @param jid
   * @return A node of "userID%appId".
   */
  public static String getReadableNode(JID jid) {
    return JID.unescapeNode(jid.getNode());
  }

  /**
   * Get the userId (without appId) from the node part.  The userId from the
   * node part will be returned without unescaping.
   * @param node The node part of the JID.
   * @return The user ID without the appId, or null.
   */
  public static String getUserId(String node) {
    if (node == null) {
      return null;
    }
    int delpos = node.lastIndexOf(APP_ID_DELIMITER);
    if (delpos < 0) {
      return node;
    }
    return node.substring(0, delpos);
  }

  /**
   * Get the escaped user ID from the node part.
   * @param jid
   * @return The user ID without the appId.
   */
  public static String getUserId(JID jid) {
    if (jid == null) {
      return null;
    }
    String node = jid.getNode();
    return getUserId(node);
  }

  /**
   * Get the human readable (unescaped) user ID from the node part.
   * @param jid
   * @return A human readable user ID without the appId.
   */
  public static String getReadableUserId(JID jid) {
    if (jid == null) {
      return null;
    }
    String node = JID.unescapeNode(jid.getNode());
    return getUserId(node);
  }

  /**
   * Get the human readable (unescaped) user ID from the node part.
   * @param jid A string format of a JID.
   * @return A human readable user ID without the appId.
   */
  public static String getReadableUserId(String jid) {
    if (jid == null) {
      return null;
    }
    String node = JID.unescapeNode(jid);
    return getUserId(node);
  }

  /**
   * Get the appId from the node part of the JID.
   * @param jid
   * @return null or the app ID.
   */
  public static String getAppId(JID jid) {
    if (jid == null) {
      return null;
    }
    String node = jid.getNode();
    if (node == null) {
      return null;
    }

    int delpos = node.lastIndexOf(APP_ID_DELIMITER);
    if (delpos < 0) {
      return null;
    }
    return node.substring(delpos+1);
  }

  /**
   * Get the appId from a serialized JID.
   * @param jid
   * @return null or the app ID.
   */
  public static String getAppId(String jid) {
    int index = jid.lastIndexOf(APP_ID_DELIMITER);
    int atIndex = jid.lastIndexOf(DOMAIN_DELIMITER);
    if (index < 0 || atIndex < index) {
      return null;
    }
    return jid.substring(index+1, atIndex);
  }

  /**
   * Get the appId from a serialized JID.
   * @param jid
   * @return null or the app ID.
   */
  public static String getResource(String jid) {
    int index = jid.lastIndexOf(RESOURCE_DELIMITER);
    if (index < 0) {
      return null;
    }
    return jid.substring(index+1);
  }

  /**
   * Make the multi-tenant node ("userID%appID) of the JID.
   * @param userId A user ID.
   * @param appId An app ID.
   * @return The node with userID and appID.
   */
  public static String makeNode(String userId, String appId) {
    return userId + APP_ID_DELIMITER + appId;
  }

  /**
   * Make a bare or full JID from a multi-tenant node.
   * @param userId A user ID.
   * @param appId An app ID.
   * @param resource A resource string for full JID, or null for bare JID.
   * @return A bare or full JID.
   */
  public static JID makeJID(String userId, String appId, String resource) {
    String domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
    return new JID(makeNode(userId, appId), domain, resource, true);
  }

  /**
   * Check if the supplied username has characters that are not allowed.
   * @param username
   * @return true if the username has invalid characters. false otherwise.
   */
  public static boolean checkUsernameForInvalidCharacters(String username) {
    Matcher matcher = USER_NAME_PATTERN.matcher(username);
    return !matcher.matches();
  }
}
