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
package com.magnet.mmx.server.plugin.mmxmgmt.wakeup;

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushAPNSPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushGCMPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXClusterableTask;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfigKeys;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Processor that checks for messages that we need to retry sending wakeup notifications
 * for.
 * TODO: This is no longer being used. Will be removed soon.
 */
public class RetryProcessor extends MMXClusterableTask implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessor.class.getName());
  // number of seconds between retries = 15 minutes
  private static final int DEFAULT_RETRY_INTERVAL_MINUTES = 15;
  private static final int MIN_RETRY_INTERVAL_MINUTES = 5;
  private static final int DEFAULT_RETRY_COUNT = 0;

  public RetryProcessor(Lock lock) {
   super(lock);
  }

  @Override
  public void run() {
    if(!canExecute()) {
      LOGGER.trace("RetryProcessor.run() : Unable to acquire clustered lock, not running");
      return;
    }
    LOGGER.debug("RetryProcessor.run() : Successfully acquired RetryProcessor lock");

    Date d = new Date();

    long utcTimeInSeconds = d.getTime() / 1000L;
    int retryIntervalMin = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.RETRY_INTERVAL_MINUTES, DEFAULT_RETRY_INTERVAL_MINUTES);

    if (retryIntervalMin < MIN_RETRY_INTERVAL_MINUTES) {
      LOGGER.warn(String.format("Configured retry interval of [%d] minutes is less than the allowed minimum of [%d] " +
          "minutes. Using the allowed minimum value.", retryIntervalMin, MIN_RETRY_INTERVAL_MINUTES));
      retryIntervalMin = MIN_RETRY_INTERVAL_MINUTES;
    }

    int retryIntervalSeconds = retryIntervalMin * 60;

    int retryCount = MMXConfiguration.getConfiguration().getInt(MMXConfigKeys.RETRY_COUNT, DEFAULT_RETRY_COUNT);

    LOGGER.info("Processing retries at:" + d);
    if (LOGGER.isDebugEnabled()) {
      String template = "Retry count:%d and retry interval in minutes:%d";
      LOGGER.debug(String.format(template, retryCount, retryIntervalMin));
    }
    if (retryCount == 0) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Retry count is set to zero. Retries are disabled");
      }
      return;
    }

    MessageDAO messageDAO = getMessageDAO();
    //Note: We add a 1 to the retry count because we want to try retryCount times after the first attempt.
    List<MessageEntity> retryList = messageDAO.getMessagesForRetryProcessing(retryIntervalSeconds, utcTimeInSeconds, retryCount+1);

    if (retryList.isEmpty()) {
      LOGGER.info("Retry list is empty");
      return;
    }

    //for each of these add an entry in the WakeupQueue table
    AppDAO appDAO = getAppDAO();
    DeviceDAO deviceDAO = getDeviceDAO();

    for (MessageEntity entity : retryList) {
      String messageId = entity.getMessageId();
      String appId = entity.getAppId();
      String deviceId = entity.getDeviceId();
      DeviceEntity device = deviceDAO.getDeviceUsingId(appId, deviceId, DeviceStatus.ACTIVE);

      if (device == null || device.getClientToken() == null || device.getClientToken().isEmpty() || device.getPushStatus() == PushStatus.INVALID) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Skipping device with id:" + deviceId + " from retry processing");
        }
        //skip this device
        continue;
      }
      AppEntity appEntity = appDAO.getAppForAppKey(appId);
      if (appEntity == null) {
        continue;
      }
      queueWakeup(appEntity, device, messageId);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Queued a new wakeup for device:" + device + " for messageId:" + messageId);
      }
    }
  }

  protected MessageDAO getMessageDAO() {
    return new MessageDAOImpl(new OpenFireDBConnectionProvider());
  }

  protected DeviceDAO getDeviceDAO() {
    return new DeviceDAOImpl(new OpenFireDBConnectionProvider());
  }

  protected AppDAO getAppDAO() {
    return new AppDAOImpl(new OpenFireDBConnectionProvider());
  }

  protected WakeupEntityDAO getWakeupEntityDAO() {
    return new WakeupEntityDAOImpl(new OpenFireDBConnectionProvider());
  }

  protected void queueWakeup(AppEntity appEntity, DeviceEntity deviceEntity, String messageId) {
    WakeupEntity wakeupEntity = new WakeupEntity();
    wakeupEntity.setToken(deviceEntity.getClientToken());
    wakeupEntity.setDeviceId(deviceEntity.getDeviceId());
    wakeupEntity.setType(deviceEntity.getTokenType());
    wakeupEntity.setSenderIdentifier(appEntity.getGoogleAPIKey());
    /**
     * Set the payload based on the Device type.
     * The wakeup payloads are different for iOS and Android devices
     */
    if (deviceEntity.getTokenType() == PushType.APNS) {
      String payload = MMXPushAPNSPayloadBuilder.wakeupPayload();
      wakeupEntity.setPayload(payload);
    } else if (deviceEntity.getTokenType() == PushType.GCM) {
      String payload = MMXPushGCMPayloadBuilder.wakeupPayload();
      wakeupEntity.setPayload(payload);
    }
    wakeupEntity.setMessageId(messageId);
    wakeupEntity.setAppId(appEntity.getAppId());

    WakeupEntityDAO wakeupEntityDAO = getWakeupEntityDAO();
    wakeupEntityDAO.offer(wakeupEntity);
  }
}
