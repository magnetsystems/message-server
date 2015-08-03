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

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MMXInAppMessageStats;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.MessageSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DAO for message records
 */
public interface MessageDAO {

  /**
   * Persist a message entity
   * @param entity
   * @throws DbInteractionException
   */
  public void persist(MessageEntity entity) throws DbInteractionException;

  /**
   * Get a list of messages with provided state value
   *
   * @param state
   * @return
   */
  public List<MessageEntity> getMessages(MessageEntity.MessageState state) throws DbInteractionException;

  /**
   * Update the message identified by the message and deviceId and change its state to the new state
   * @param messageId
   * @param deviceId
   * @param state
   * @throws DbInteractionException
   */
  public void updateMessageState(String messageId, String deviceId, MessageEntity.MessageState state) throws DbInteractionException;

  /**
   * Purge messages that have been delivered.
   *
   * @param start
   * @param end
   * @return the count of messages delivered.
   * @throws DbInteractionException
   */
  public int purgeDeliveredMessages(Date start, Date end) throws DbInteractionException;

  /**
   * Get a list of messages for retry processing.
   * @param timeSinceLastWakeup - number of seconds that must was elapsed since the last wakeup and the message
   *                            state being in WAKEUP_SENT
   * @param utcTime current time in number of "seconds" since epoch
   * @param maxRetryCount maximum number of retries.
   * @return
   * @throws DbInteractionException
   */
  public List<MessageEntity> getMessagesForRetryProcessing(int timeSinceLastWakeup, long utcTime, int maxRetryCount) throws DbInteractionException;

  /**
   * Mark all the messages identified by the messageId as RECEIVED.
   * @param messageId
   * @param receivedByDeviceId
   */
  public void messageReceived(String messageId, String receivedByDeviceId);

  /**
   * Mark a message identified the passed in parameters as DELIVERED.
   * @param appId
   * @param deviceId
   * @param messageId
   * @return int number of message updated.
   */
  public int messageDelivered(String appId, String deviceId, String messageId);

  /**
   * Search messages for a specific appId using the search option, searchValue.
   * Order the results using the sortOption. Paginate results using supplied pagination
   * info.
   * @param appId
   * @param searchOption
   * @param searchValue
   * @param sortOption
   * @param info
   * @return
   */
  public SearchResult<MessageEntity> searchMessages(String appId, MessageSearchOption searchOption, String searchValue,
                                                    MessageSortOption sortOption, PaginationInfo info);
  /**
   * Search messages for a specific appId using the search option and search value holder.
   * Order the results using the sortOption. Paginate results using supplied pagination
   * info.
   * @param appId
   * @param searchOption
   * @param valueHolder
   * @param sortOption
   * @param info
   * @param filterReceiptMessages - set to true if you want to filter out receipt messages.
   * @return
   */
  public SearchResult<MessageEntity> searchMessages(String appId, MessageSearchOption searchOption, ValueHolder valueHolder,
                                                    MessageSortOption sortOption, PaginationInfo info, boolean filterReceiptMessages);


  public Map<String, MMXInAppMessageStats> getMessageStats(List<String> appIdList);

  /**
   * Mark message as timedout if they are in WAKEUP_SENT state and have been queued for more
   * than timeout minutes.
   * @param utcTime current time in number of "seconds" since epoch
   * @param timeoutMinutes time out interval in minutes for example 180 minutes
   * @return
   */
  public int messageTimeout(long utcTime, int timeoutMinutes);

  /**
   * Update the message identified by the message and deviceId to set its state to WAKEUP_SENT.
   * @param messageId
   * @param deviceId
   * @throws DbInteractionException
   */
  public void wakeupSent(String messageId, String deviceId) throws DbInteractionException;

  /**
   * Get a message entity using the messageId and deviceId
   * @param messageId
   * @param deviceId
   * @return
   * @throws DbInteractionException
   */
  public MessageEntity get(String messageId, String deviceId) throws DbInteractionException;

  /**
   * Get a list of message for the supplied appId and messageId
   * @param appId
   * @param messageId
   * @return
   */
  public List<MessageEntity> getMessages(String appId, String messageId);


  /**
   * Update messages in WAKEUP_SENT or WAKEUP_REQUIRED state to PENDING using the supplied
   * information. This is used during device token invalidation.
   * @param appId
   * @param type
   * @param token
   * @return
   */
  public int changeStateToPending(String appId, PushType type, String token);

  /**
   * Change message in WAKEUP_SENT or WAKEUP_REQUIRED state to PENDING using the supplied
   * information. This is for handling app api key problem.
   * @param appId
   * @param messageId
   * @param deviceId
   * @return count of messages that were updated.
   */
  public int changeStateToPending(String appId, String messageId, String deviceId);
}
