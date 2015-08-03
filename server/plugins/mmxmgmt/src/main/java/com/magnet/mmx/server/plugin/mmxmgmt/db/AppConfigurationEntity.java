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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class that represents a record in the mmxAppConfiguration
 */
public class AppConfigurationEntity {
  private int id;
  private String appId;
  private String key;
  private String value;


  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AppConfigurationEntity{");
    sb.append("appId='").append(appId).append('\'');
    sb.append(", id=").append(id);
    sb.append(", key='").append(key).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public static AppConfigurationEntity build(ResultSet rs) throws SQLException {
    AppConfigurationEntity config = new AppConfigurationEntity();
    int id = rs.getInt("id");
    String appId = rs.getString("appId");
    String key = rs.getString("configKey");
    String value = rs.getString("configValue");

    config.setId(id);
    config.setAppId(appId);
    config.setKey(key);
    config.setValue(value);

    return config;
  }
}
