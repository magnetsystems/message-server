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

/**
 * DAO interface for topic roles.
 */
public interface TopicRoleDAO {

  /**
   * Get a list of roles for the passed in topic identifier
   * @param serviceId
   * @param nodeId
   * @return
   */
  public List<String> getTopicRoles (String serviceId, String nodeId);

  /**
   * Add the specified list of roles to specified topic identifier.
   * If the topic already has the roles in the passed in list, those are ignored.
   * Entries in roles list can't be empty string. They need to be lowercase.
   * @param serviceId
   * @param nodeId
   * @param roles
   */
  public void addTopicRoles (String serviceId, String nodeId, List<String> roles);


}
