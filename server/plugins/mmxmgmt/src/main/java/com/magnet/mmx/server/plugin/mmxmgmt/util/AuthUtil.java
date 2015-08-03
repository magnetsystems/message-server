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

import org.jivesoftware.openfire.admin.AdminManager;
import org.jivesoftware.openfire.auth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

/**
 */
public class AuthUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthUtil.class);
  private static final String KEY_AUTHORIZATION = "Authorization";
  private static final String KEY_BASIC = "Basic";

  /**
   * Use the information from the request to see if it has the Basic authorization header. If it does
   * validate it.
   * @param request
   * @return true if the user is authorized and false if the user is not authorized.
   * @throws IOException
   */
  public static boolean isAuthorized(HttpServletRequest request) throws IOException {
    String username = null;
    String password = null;
    boolean authorized = false;
    String authHeader = request.getHeader(KEY_AUTHORIZATION);
    if (authHeader != null) {
      StringTokenizer st = new StringTokenizer(authHeader);
      if (st.hasMoreTokens()) {
        String basic = st.nextToken();
        if (basic.equalsIgnoreCase(KEY_BASIC)) {
          try {
            String credentials = new String(org.apache.commons.codec.binary.Base64.decodeBase64(st.nextToken().getBytes()), MMXServerConstants.UTF8_ENCODING);
            LOGGER.debug("Auth header {} ", authHeader);
            int p = credentials.indexOf(":");
            if (p != -1) {
              username = credentials.substring(0, p).trim();
              password = credentials.substring(p + 1).trim();
            } else {
              LOGGER.warn("Invalid authentication token");
            }
          } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Couldn't retrieve authentication", e);
          }
        }
      }
    } else {
      LOGGER.info("Request is missing the authorization header");
    }
    AuthToken token = null;
    if (username != null && password != null) {
      try {
        token = AuthFactory.authenticate(username, password);
      } catch (ConnectionException e) {
        LOGGER.error("isAuthorized : ", e);
      } catch (InternalUnauthenticatedException e) {
        LOGGER.error("isAuthorized : ", e);
      } catch (UnauthorizedException e) {
        LOGGER.error("isAuthorized : ", e);
      }
    }
    if (token != null) {
      AdminManager manager = AdminManager.getInstance();
      authorized = manager.isUserAdmin(username, false);
      if (!authorized) {
        LOGGER.info("User:{} is not an admin. Not granting access", username);
      }
    }
    return authorized;
  }
}
