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
package com.magnet.mmx.server.common.utils;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.common.data.AppEntity.AppEntityBuilder;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public final class MmxAppUtil {
  private static final Logger Log = LoggerFactory.getLogger(MmxAppUtil.class) ;

  private static final String QUERY_APP = AppEntity.APP_QUERY_STRING + " WHERE appId = ?";
  private static final String DOMAIN_DELIMITER = "@";
  private static final String APPID_DELIMITER = "%";


  public static String extractUserName(String username) throws UnauthorizedException {
    String result = username.trim().toLowerCase();
    if (username.contains(DOMAIN_DELIMITER)) {
      // Check that the specified domain matches the server's domain
      int index = username.lastIndexOf(DOMAIN_DELIMITER);
      String domain = username.substring(index + 1);
      result = username.substring(0, index);
    }
    return result;
  }
  // Extract and check app id.
  public static String checkAppId(String username) throws UnauthorizedException {
    if (username.contains(APPID_DELIMITER)) {
      // Check that the specified domain matches the server's domain
      int index = username.indexOf(APPID_DELIMITER);
      String appId = username.substring(index + 1);
      Log.info("app id is "+appId);

      AppEntity appEntity = appFound(appId);
      if (appEntity != null) {
        username = username.substring(0, index);
      } else {
        // Unknown app key. Return authentication failed.
        throw new UnauthorizedException("Fail to authenticate the connection for user " + username  + ". Cannot find app with app id "+appId);
      }
    }
    return username;
  }

  static AppEntity appFound(String appId) {
    Log.info("MmxAuthProvider: check if app exists using appId "+appId);
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    ResultSet resultSet = null;
    boolean ret = true;
    AppEntity resultApp = null;
    try {
      con = DbConnectionManager.getConnection();
      pstmt = con.prepareStatement(QUERY_APP);
      pstmt.setString(1, appId);

      resultSet = pstmt.executeQuery();
      if (resultSet.first()) {
        resultApp = new AppEntityBuilder().build(resultSet);
      }

      resultSet.close();
      pstmt.close();
    }
    catch (SQLException e) {
      Log.error(e.getMessage(), e);
    }
    finally {
      DbConnectionManager.closeConnection(rs, pstmt, con);
    }
    return resultApp;

  }

}
