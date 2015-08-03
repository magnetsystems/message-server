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

import com.magnet.mmx.server.plugin.mmxmgmt.search.UserEntityPostProcessor;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 */
@XmlRootElement
@JsonPropertyOrder({"username", "name", "email", "creationDate", "modificationDate"})
public class UserEntity {

  private String username;
  private String name;
  private String email;
  private Date creationDate;
  private Date modificationDate;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  public static class UserEntityBuilder {
    public UserEntity build(ResultSet rs) throws SQLException {
      String userId = rs.getString("username");
      String name = rs.getString("name");
      String email = rs.getString("email");
      Date creationDate = new Date(Long.parseLong(rs.getString("creationDate")));
      Date modificationDate = new Date(Long.parseLong(rs.getString("modificationDate")));
      UserEntity entity = new UserEntity();
      entity.setUsername(userId);
      entity.setName(name);
      entity.setEmail(email);
      entity.setCreationDate(creationDate);
      entity.setModificationDate(modificationDate);
      return entity;
    }

    /**
     * Use the specified prefix as the prefix for the column names.
     * @param rs
     * @param prefix
     * @return
     * @throws SQLException
     */
    public UserEntity build(ResultSet rs, String prefix) throws SQLException {
      return build(rs, prefix, false);
    }

    /**
     * Build user entity
     * @param rs -- result set using the supplied prefix
     * @param prefix -- not null prefix.
     * @param removeAppSuffix flag indicating if we should remove the app Suffix from user Id
     * @return
     * @throws SQLException
     */
    public UserEntity build(ResultSet rs, String prefix, boolean removeAppSuffix) throws SQLException {
      String userId = rs.getString(prefix + "username");
      String name = rs.getString(prefix +"name");
      String email = rs.getString(prefix +"email");
      Date creationDate = new Date(Long.parseLong(rs.getString(prefix +"creationDate")));
      Date modificationDate = new Date(Long.parseLong(rs.getString(prefix +"creationDate")));
      UserEntity entity = new UserEntity();
      entity.setUsername(userId);
      entity.setName(name);
      entity.setEmail(email);
      entity.setCreationDate(creationDate);
      entity.setModificationDate(modificationDate);

      if (removeAppSuffix) {
        new UserEntityPostProcessor().postProcess(entity);
      }
      return entity;
    }
  }
}
