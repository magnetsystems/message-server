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

import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXPushMessageStats;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DAO for the push message records
 */
public interface PushMessageDAO {

  /**
   * Add a new push message record
   * @param entity
   */
  public void add (PushMessageEntity entity);

  /**
   * Get a list of push message entities for a given appId and deviceId
   * @param appId
   * @param deviceId
   * @return
   */
  public List<PushMessageEntity> getPushMessages(String appId, String deviceId);

  /**
   * Get a PushMessageEntity object for the supplied messageId
   * @param messageId
   * @return
   */
  public PushMessageEntity getPushMessage(String messageId);


  /**
   * Acknowledge push message
   * @param messageId
   * @return the count of messages updated
   */
  public int acknowledgePushMessage(String messageId, Date dateAcknowledged);

  public Map<String, MMXPushMessageStats> getPushMessageStats(List<String> appIdList);

  /**
   * Search for Push Messages
   * @param query
   * @param info
   * @return
   */
  public SearchResult<PushMessageEntity> getPushMessagesWithPagination(QueryBuilderResult query, PaginationInfo info);


}
