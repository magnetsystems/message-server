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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 */
public class CloseUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloseUtil.class);

  private CloseUtil() {}


  public static void close(ResultSet rs) {
    close(LOGGER, rs);
  }

  /**
   * Close the resultset. If an exception is encountered it is logged but not thrown
   * @param logger logger that should be used for logging the exception if any
   * @param rs result set that needs to closed
   */
  public static void close(Logger logger, ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        logger.warn("Exception in closing resultset", e);
      }
    }
  }

  public static void close(PreparedStatement pstmt) {
    close(LOGGER, pstmt);
  }

  /**
   * Close prepared statement
   * @param logger
   * @param pstmt
   */
  public static void close(Logger logger, PreparedStatement pstmt) {
    if (pstmt != null) {
      try {
        pstmt.close();
      } catch (SQLException e) {
        logger.warn("Exception in closing prepared statement", e);
      }
    }
  }

  /**
   * Close connection
   * @param logger
   * @param conn
   */
  public static void close (Logger logger, Connection conn) {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        logger.warn("Exception in closing connection", e);
      }
    }
  }

  public static void close (Logger logger, ResultSet rs, PreparedStatement pstmt, Connection conn) {
    close(logger, rs);
    close(logger, pstmt);
    close(logger, conn);
  }

  public static void close (Logger logger, PreparedStatement pstmt, Connection conn) {
    close(logger, pstmt);
    close(logger, conn);
  }
}




