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

import java.util.List;

public interface AppConfigurationEntityDAO {

  /**
   * Get app configuration for a single key.
   * @param appId
   * @param key
   * @return AppConfigurationEntity for the specified key. False other wise.
   */
  public AppConfigurationEntity getConfiguration(String appId, String key);
  /**
   * Get a list of configurations for an appId
   * @param appId
   * @return
   */
  public List<AppConfigurationEntity> getConfigurations(String appId);

  /**
   * Either insert or update a configuration.
   * @param appId
   * @param key
   * @param value
   */
  public void updateConfiguration(String appId, String key, String value);

  /**
   * Delete configuration
   * @param appId
   * @param key
   */
  public void deleteConfiguration(String appId, String key);

}
