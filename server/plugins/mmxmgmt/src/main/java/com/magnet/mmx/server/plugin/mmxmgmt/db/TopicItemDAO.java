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
 */
public interface TopicItemDAO {
  public void persist(TopicItemEntity entity);

  public int getCount(String serviceId, String nodeId, String since, String until);

  public List<TopicItemEntity> getItems(String serviceId, String nodeId, int maxItems, String order);

  public List<TopicItemEntity> getItemsSince(String serviceId, String nodeId, int maxItems, String since);

  public List<TopicItemEntity> getItemsUntil(String serviceId, String nodeId, int maxItems, String until);

  public List<TopicItemEntity> getItems(String serviceId, String nodeId, int maxItems,
                                        String since, String until, String order);

  /**
   * This method has major performance issue.
   * @param id
   * @return
   * @deprecated Use {@link #deleteTopicItem(String, String)}
   */
  @Deprecated
  public int deleteTopicItem(String id);

  /**
   * Delete an item from a topic persisted in Pubsub Module.
   * @param nodeId A topic node ID.
   * @param id An item ID.
   * @return
   */
  public int deleteTopicItem(String nodeId, String id);

  /**
   * This method has major performance issue.
   * @param id
   * @return
   * @deprecated Use {@link #findById(String, String)}
   */
  @Deprecated
  public TopicItemEntity findById(String id);

  /**
   * Find an item by its ID from a topic persisted in Pubsub Module.
   * @param nodeId A topic node ID.
   * @param id An item ID.
   * @return
   */
  public TopicItemEntity findById(String nodeId, String id);


}
