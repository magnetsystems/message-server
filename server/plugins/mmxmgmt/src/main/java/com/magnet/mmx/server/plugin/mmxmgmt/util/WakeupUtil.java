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
package com.magnet.mmx.server.plugin.mmxmgmt.util;

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppConfigurationCache;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.WakeupEntityDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MMXPacketExtension;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushAPNSPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushGCMPayloadBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Queuing up a wake-up (silent) or push notification for an ad-hoc message.
 */
public class WakeupUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(WakeupUtil.class);

  /**
   * Queue up a silent or push notification for a message.
   * @param appEntity
   * @param deviceEntity
   * @param messageId
   * @param pushConfigName An optional push config name, or null.
   */
  public static void queueWakeup(AppEntity appEntity, DeviceEntity deviceEntity,
                                 String messageId, String pushConfigName) {
    LOGGER.trace("queueWakeup : messageId={}", messageId);
    WakeupEntityDAO wakeupEntityDAO = DBUtil.getWakeupEntityDAO();
    AppConfigurationCache configurationCache = AppConfigurationCache.getInstance();
    String mutePeriodString = configurationCache.getString(appEntity.getAppId(), MMXConfigKeys.WAKEUP_MUTE_PERIOD_MINUTES);
    int mutePeriod = MMXServerConstants.WAKEUP_MUTE_PERIOD_MINUTES_DEFAULT;
    if (mutePeriodString == null || mutePeriodString.isEmpty()) {
      mutePeriod = MMXServerConstants.WAKEUP_MUTE_PERIOD_MINUTES_DEFAULT;
    } else {
      mutePeriod = Integer.parseInt(mutePeriodString);
    }
    List<WakeupEntity> queuedDuringMute = wakeupEntityDAO.retrieveOpenOrSentWakeup(appEntity.getAppId(),
                                            deviceEntity, mutePeriod);

    if (!queuedDuringMute.isEmpty()) {
      LOGGER.info("Device id:{} has wakeup queued or sent during mute period : {}. Not queueing a wakeup",
          deviceEntity.getDeviceId(), mutePeriod);
      return;
    }

    queueWakeup(wakeupEntityDAO, appEntity, deviceEntity, messageId);
  }

  /**
   * Persist a wakeup entity.
   * @param wakeupEntityDAO
   * @param appEntity
   * @param devEntity
   * @param messageId
   */
  public static void queueWakeup(WakeupEntityDAO wakeupEntityDAO, AppEntity appEntity,
                                DeviceEntity devEntity, String messageId) {
    WakeupEntity wakeupEntity = new WakeupEntity();
    wakeupEntity.setToken(devEntity.getClientToken());
    wakeupEntity.setDeviceId(devEntity.getDeviceId());
    wakeupEntity.setType(devEntity.getTokenType());
    wakeupEntity.setGoogleApiKey(appEntity.getGoogleAPIKey());

//    PubSubWakeupProvider.FmPushConfig pushConfig = new
//        PubSubWakeupProvider.FmPushConfig(devEntity.getOwnerId(),
//            appEntity.getAppId(), "", configName);
//    MessageNotification custom = null;
//    try {
//      Map<String, Object> context = pushConfig.buildContext(appEntity, null, 1, mmx);
//      if (pushConfig.eval(context) == null) {
//        return;
//      }
//      MMXid from = MMXid.fromMap((Map<String, String>) mmx.getMmxMeta().get(
//          MmxHeaders.FROM));
//      if (pushConfig.getPushType() == PushMessage.Action.PUSH) {
//        // Push notification payload
//        custom = new MessageNotification(mmx.getPayload().getSentTime(), from,
//            pushConfig.getTitle(), pushConfig.getBody(), pushConfig.getSound());
//      } else if (pushConfig.getPushType() == PushMessage.Action.WAKEUP) {
//        // Wakeup (silent) notification payload
//        custom = new MessageNotification(mmx.getPayload().getSentTime(), from,
//            pushConfig.getBody());
//      } else {
//        return;
//      }
//    } catch (MMXException e) {
//      e.printStackTrace();
//      return;
//    }

    /**
     * Set the payload based on the Device type.
     * The wakeup payloads are different for iOS and Android devices
     */
    if (devEntity.getTokenType() == PushType.APNS) {
      String payload = MMXPushAPNSPayloadBuilder.wakeupPayload();
      wakeupEntity.setPayload(payload);
    } else if (devEntity.getTokenType() == PushType.GCM) {
      String payload = MMXPushGCMPayloadBuilder.wakeupPayload();
      wakeupEntity.setPayload(payload);
    }
    wakeupEntity.setMessageId(messageId);
    wakeupEntity.setAppId(appEntity.getAppId());

    wakeupEntityDAO.offer(wakeupEntity);
  }
}
