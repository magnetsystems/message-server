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
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DevicePushTokenInvalidator;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntityDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntityDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.util.AppEntityDBLoadingEntityCache;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXClusterableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Wakeup processor picks items from the wakeup queue and then delivers notifications to them.
 * Items for which wakeup notification are sent are updated to have date sent set.
 */
public class WakeupProcessor extends MMXClusterableTask implements Runnable {
  private final Logger LOGGER = LoggerFactory.getLogger(WakeupProcessor.class);
  private final int WAKE_UP_CHUNK = 1000;
  private static final int CACHE_SIZE = 100;
  private static AppEntityDBLoadingEntityCache appCache =
      new AppEntityDBLoadingEntityCache(CACHE_SIZE, new AppEntityDBLoadingEntityCache.AppEntityDBLoader());

  public WakeupProcessor(Lock lock) {
    super(lock);
  }

  @Override
  public void run() {
    try {
      if (!canExecute()) {
        LOGGER.trace("WakeupProcessor.run() : Unable to acquire clustered lock, not running");
        return;
      }
      LOGGER.info("Start processing wakeup...");
      WakeupEntityDAO dao = getWakeupEntityDAO();
      MessageDAO messageDAO = getMessageDAO();

      List<WakeupEntity> wakeupList = retrievePendingList(dao);
      if (wakeupList.isEmpty()) {
        LOGGER.info("No pending wakeup entries found; nothing to process.");
        return;
      }

      long startTime = System.nanoTime();
      int count = 0;
      /**
       * initialize the notifiers
       */
      WakeupNotifier gcmNotifier = getGCMWakeupNotifier();
      WakeupNotifier apnsNotifier = getAPNSWakeupNotifier();
      List<WakeupEntity> completed = new LinkedList<WakeupEntity>();
      List<WakeupEntity> badWakeupEntity = new LinkedList<WakeupEntity>();

      for (WakeupEntity wkEntity : wakeupList) {
        String token = wkEntity.getToken();
        String appId = wkEntity.getAppId();
        if (token == null || appId == null) {
          LOGGER.info("Skipping (no token or appId) wakeup record:" + wkEntity.toString());
          badWakeupEntity.add(wkEntity);
          continue;
        }
        List<NotificationResult> results;
        if (wkEntity.getType() == PushType.GCM) {
          if (wkEntity.getGoogleApiKey() == null) {
            LOGGER.warn("Skipping (no apikey) wakeup record:" + wkEntity.toString());
            badWakeupEntity.add(wkEntity);
            continue;
          }
          WakeupNotifier.NotificationSystemContext context =
              new GCMWakeupNotifierImpl.GCMNotificationSystemContext(wkEntity.getGoogleApiKey());
          results = gcmNotifier.sendNotification(
              Collections.singletonList(token), wkEntity.getPayload(), context);
        } else if (wkEntity.getType() == PushType.APNS) {
          //handle APNS wake up notification
          AppEntity appEntity = getAppEntity(appId);
          if (appEntity == null) {
            LOGGER.warn("Skipping a wakeup record with invalid appId: "+appId);
            badWakeupEntity.add(wkEntity);
            continue;
          }
          WakeupNotifier.NotificationSystemContext context =
              new APNSWakeupNotifierImpl.APNSNotificationSystemContext(
                  appId, appEntity.isApnsCertProduction());
          results = apnsNotifier.sendNotification(
              Collections.singletonList(token), wkEntity.getPayload(), context);
        } else {
          // Unsupported type
          LOGGER.warn("Skipping wakeup record; unsupported type: " + wkEntity.getType());
          continue;
        }

        NotificationResult result0 = results.get(0);
        if (result0 == NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER ||
            result0 == NotificationResult.DELIVERY_IN_PROGRESS_REMIND_AGAIN) {
          completed.add(wkEntity);
          count++;
        } else if (result0 == NotificationResult.DELIVERY_FAILED_INVALID_TOKEN) {
          LOGGER.warn("Skipping and removing wakeup record with invalid "+
                      wkEntity.getType()+" token: "+token);
          //change PushStatus for the device to INVALID and remove it from wakeup
          DevicePushTokenInvalidator invalidator = new DevicePushTokenInvalidator();
          invalidator.invalidateToken(appId, wkEntity.getType(), token);
        } else {
          LOGGER.warn("Skipping wakeup record with "+wkEntity.getType()+
                      " because of "+result0);
          badWakeupEntity.add(wkEntity);
        }
      }

      //mark the processed items with a dateSent timestamp.
      Date dateSent = new Date();
      for (WakeupEntity entity : completed) {
        entity.setDateSent(dateSent.getTime() / 1000L);
        dao.complete(entity);
        // update the message state to wakeup sent
        messageDAO.wakeupSent(entity.getMessageId(), entity.getDeviceId());
      }
      /**
       * for wakeup entries that are identified as having bad api keys
       * delete the wakeup entries and change the message status to pending
       */
      for (WakeupEntity wk : badWakeupEntity) {
        LOGGER.warn("Removing wakeup record with bad data: "+wk.toString());
        messageDAO.changeStateToPending(wk.getAppId(), wk.getMessageId(), wk.getDeviceId());
        dao.remove(wk.getId());
      }

      long endTime = System.nanoTime();
      long delta = endTime - startTime;
      LOGGER.info("Completed processing wakeup chunk");
      String template = "Processed [%d] wakeup messages in [%d] milliseconds";
      LOGGER.info(String.format(template, count, TimeUnit.MILLISECONDS.convert(delta, TimeUnit.NANOSECONDS)));

    } catch (Throwable t) {
      //catching throwable here with the assumption that it is transient and subsequent runs will
      //be ok.
      LOGGER.warn("Throwable in WakeupProcessor is ignored; please handle it properly", t);
    }
  }

  protected List<WakeupEntity> retrievePendingList(WakeupEntityDAO dao) {
    return dao.poll(WAKE_UP_CHUNK);
  }

  protected WakeupEntityDAO getWakeupEntityDAO() {
    WakeupEntityDAO dao = new WakeupEntityDAOImpl(new OpenFireDBConnectionProvider());
    return dao;
  }

  protected MessageDAO getMessageDAO() {
    MessageDAO messageDAO = new MessageDAOImpl(new OpenFireDBConnectionProvider());
    return messageDAO;
  }

  protected DeviceDAO getDeviceDAO() {
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());
    return deviceDAO;
  }

  protected WakeupNotifier getGCMWakeupNotifier() {
    return new GCMWakeupNotifierImpl();
  }

  protected WakeupNotifier getAPNSWakeupNotifier() {
    return new APNSWakeupNotifierImpl();
  }

  protected AppEntity getAppEntity (String appId ) {
    return appCache.get(appId);
  }
}
