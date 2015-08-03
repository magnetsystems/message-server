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

import java.sql.SQLException;
import java.util.List;

/**
 */
public interface TagDAO {
  /**
   * Persist a tag entity
   * @param tagEntity
   */
  public int persist(TagEntity tagEntity) throws SQLException;

  /**
   * Get all tags;
   *
   * @return
   */
  public List<TagEntity> getAllTags(String appId);

  /**
   * Get tag entities for device
   *
   * @param entity
   * @return
   */
  public List<TagEntity> getTagEntitiesForDevice(DeviceEntity entity);

  /**
   * Get tags for device;
   *
   */

  public List<String> getTagsForDevice(DeviceEntity entity);

  public void deleteTagsForDevice(DeviceEntity entity, List<String> tagnames) throws DbInteractionException;

  public void deleteAllTagsForDevice(DeviceEntity entity) throws DbInteractionException;

  /**
   * Get list of all devices where the devices have all specified tags attached
   *
   * @param tagNames
   * @return
   */
  public List<Integer> getDeviceIdsBooleanAnd(List<String> tagNames, String appId);

  /**
   * Get list of all devices where devices have atleast one of specified tags
   *
   * @param tagNames
   * @return
   */
  public List<Integer> getDeviceIdsBooleanOr(List<String> tagNames, String appId);

  /**
   * create device Tag
   *
   */

  public int  createDeviceTag(String tagname, String appId, String deviceId) throws Exception;

  public int createDeviceTag(String tagname, String appId, int deviceIdFK) throws DbInteractionException;

  /**
   * Create tag for a user
   *
   * @param tagname
   * @param appId
   * @param username username should be in the format {real_username}%{app_id}
   * @return
   * @throws Exception
   */
  public int createUsernameTag(String tagname, String appId, String username) throws Exception;

  /**
   * Returns a list of tags for the username
   *
   * @param appId
   * @param username username should in the format [real_username]%[app_id]
   * @return
   * @throws Exception
   */
  public List<String> getTagsForUsername(String appId, String username) throws Exception;

  public List<TagEntity> getTagEntitiesForUsername(String appId, String username) throws Exception;

  public void deleteTagsForUsername(String appId, String username, List<String> tagnames) throws DbInteractionException;

  public void deleteAllTagsForUsername(String appId, String username) throws DbInteractionException;

  public int createTopicTag(String tagname, String appId, String serviceId, String nodeId) throws DbInteractionException;

  public List<String> getTagsForTopic(String appId, String serviceId, String nodeId) throws Exception;

  public List<TagEntity> getTagEntitiesForTopic(String appId, String serviceId, String nodeId) throws Exception;

  public void deleteTagsForTopic(List<String> tagnames, String appId, String serviceId, String nodeId) throws DbInteractionException;

  public void deleteAllTagsForTopic(String appId, String serviceId, String nodeId);
}
