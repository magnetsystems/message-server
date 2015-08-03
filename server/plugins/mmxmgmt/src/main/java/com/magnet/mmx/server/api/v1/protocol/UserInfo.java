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
package com.magnet.mmx.server.api.v1.protocol;

import com.magnet.mmx.protocol.Constants;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UserEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.util.Helper;

import java.util.Date;

/**
 */
public class UserInfo {
  private String username;
  private String name;
  private String email;
  private Date creationDate;
  private Date modificationDate;

  public UserInfo() {
  }

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

  public static UserInfo fromUserEntity(UserEntity entity) {
    UserInfo info = new UserInfo();
    info.setUsername(Helper.removeSuffix(entity.getUsername(), Character.toString(Constants.APP_ID_DELIMITER)));
    info.setEmail(entity.getEmail());
    info.setCreationDate(entity.getCreationDate());
    info.setModificationDate(entity.getModificationDate());
    info.setName(entity.getName());
    return info;
  }
}
