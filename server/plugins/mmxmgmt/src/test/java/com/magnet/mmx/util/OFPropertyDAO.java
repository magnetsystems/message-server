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
package com.magnet.mmx.util;

import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 */
public class OFPropertyDAO {
  private static final Logger LOGGER = LoggerFactory.getLogger(OFPropertyDAO.class);

  private ConnectionProvider provider;

  public OFPropertyDAO(ConnectionProvider provider) {
    this.provider = provider;
  }

  public void setValue(String propertyName, String propertyValue) {
    LOGGER.trace("setValue : setting value property={}, value={}", propertyName, propertyValue);
    final String unboundStatementStr = "INSERT IGNORE INTO ofProperty (name, propValue) VALUES (?, ?)";
    PreparedStatement pstmt;
    Connection conn;
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, propertyName);
      pstmt.setString(2, propertyValue);
      LOGGER.trace("setValue : executing statement={}", pstmt);
      pstmt.executeUpdate();
    } catch (Exception e) {
      LOGGER.error("setValue : propertyName={}, propertyValue={}", new Object[]{propertyName, propertyValue, e});
    }
  }

  public String getValue(String propertyName) {
    final String unboundStatementStr = "SELECT propValue FROM ofProperty WHERE propertyName = ?";
    PreparedStatement pstmt;
    Connection conn;
    ResultSet rs;
    String value = null;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, propertyName);
      LOGGER.trace("getValue : executing statement={}", pstmt);
      rs = pstmt.executeQuery();
      if(rs.next()) {
        value = rs.getString(1);
      }
    } catch (Exception e) {
      LOGGER.error("getValue : propertyName={}", propertyName);
    }
    LOGGER.trace("getValue : propertyName={}, read value={}", propertyName, value);
    return value;
  }

  public void deleteProperty(String propertyName) {
    final String unboundStatementStr = "DELETE FROM ofProperty WHERE name = ?";
    PreparedStatement pstmt;
    Connection conn;

    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(unboundStatementStr);
      pstmt.setString(1, propertyName);
      LOGGER.trace("deleteProperty : executing statement={}", pstmt);
      pstmt.executeUpdate();
    } catch (Exception e) {
      LOGGER.error("deleteProperty : propertyName={}", propertyName, e);
    }
  }
}
