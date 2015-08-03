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

import java.util.List;

/**
 */
public interface WakeupEntityDAO {

  /**
   * Add a wake up entity to the queue.
   * @param entity
   */
  public void offer (WakeupEntity entity);

  /**
   * Get up to maxCount entries from the Wakeup table that have n't be processed (dateSent is not set).
   * @param maxCount
   * @return
   * TODO: Check
   */
  public List<WakeupEntity> poll (int maxCount);

  /**
   * Mark the given wakeup entity as processed. Updating it rather that deleting it
   * helps with the table indexes.
   * @param entity
   */
  public void complete (WakeupEntity entity);


  /**
   * Delete all queued unprocessed wakeup records using the supplied information. This
   * API is for deleting unprocessed wakeup records when a device token is invalidated.
   * @param appId
   * @param type
   * @param token
   */
  public int remove(String appId, PushType type, String token);

  /**
   * Delete the wakeup entity record corresponding to the supplied id.
   * @param wakeupEntityId
   */
  public void remove(int wakeupEntityId);


  /**
   * Get a list of WakeupEntity records that have been queued during the mute period.
   * @param appId appId
   * @param deviceEntity device entity
   * @param mutePeriod in minutes
   * @return List of WakeupEntity records. None if records don't exist.
   */
  public List<WakeupEntity> retrieveOpenOrSentWakeup(String appId, DeviceEntity deviceEntity, int mutePeriod);


}
