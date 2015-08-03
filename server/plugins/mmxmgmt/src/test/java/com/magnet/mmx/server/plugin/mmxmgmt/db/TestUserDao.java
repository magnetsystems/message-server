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
public class TestUserDao {
  private ConnectionProvider provider;
  private static final Logger LOGGER = LoggerFactory.getLogger(TestUserDao.class);

  public TestUserDao(ConnectionProvider provider) {
    this.provider = provider;
  }

  public void persist(UserEntity entity) throws Exception {
    final String statementStr = "INSERT IGNORE into ofUser (username, plainPassword, encryptedPassword, name, email, creationDate, modificationDate) VALUES (?, ?, ?, ?, ?, ?, ?)";

    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
      conn = provider.getConnection();
      pstmt = conn.prepareStatement(statementStr);
      pstmt.setString(1, entity.getUsername());
      pstmt.setString(2, entity.getPlainPassword());
      pstmt.setString(3, entity.getEncryptedPassword());
      pstmt.setString(4, entity.getName());
      pstmt.setString(5, entity.getEmail());
      pstmt.setString(6, entity.getCreationDate());
      pstmt.setString(7, entity.getModificationDate());
      pstmt.executeUpdate();
    } catch (SQLException e) {
      LOGGER.error("persist : {}");
    } finally {
       CloseUtil.close(LOGGER, pstmt, conn);
    }
  }

  public static class UserEntity {
    String username;
    String usernameNoAppId;
    String plainPassword;
    String encryptedPassword;
    String name;
    String email;
    String creationDate;
    String modificationDate;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPlainPassword() {
      return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
      this.plainPassword = plainPassword;
    }

    public String getEncryptedPassword() {
      return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
      this.encryptedPassword = encryptedPassword;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getCreationDate() {
      return creationDate;
    }

    public void setCreationDate(String creationDate) {
      this.creationDate = creationDate;
    }

    public String getModificationDate() {
      return modificationDate;
    }

    public void setModificationDate(String modificationDate) {
      this.modificationDate = modificationDate;
    }

    public String getUsernameNoAppId() {
      return usernameNoAppId;
    }

    public void setUsernameNoAppId(String usernameNoAppId) {
      this.usernameNoAppId = usernameNoAppId;
    }
  }
}
