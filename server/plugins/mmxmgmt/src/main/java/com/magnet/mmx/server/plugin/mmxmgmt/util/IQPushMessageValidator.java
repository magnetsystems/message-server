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
import com.magnet.mmx.server.plugin.mmxmgmt.db.*;
import com.magnet.mmx.server.plugin.mmxmgmt.push.*;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.APNSWakeupNotifierImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.GCMWakeupNotifierImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.WakeupNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */

public class IQPushMessageValidator implements MMXPushMessageValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(IQPushMessageValidator.class);

  public MMXPushValidationResult validate(String appId, String ownerId, String deviceId) {

    AppDAO appDAO = new AppDAOImpl(new OpenFireDBConnectionProvider());
    DeviceDAO deviceDAO = new DeviceDAOImpl(new OpenFireDBConnectionProvider());

    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (appEntity == null) {
      LOGGER.info("validate : appId={} not found", appId);
      return getFailedResult(PushStatusCode.INVALID_APP_ID);
    }

    DeviceEntity deviceEntity = deviceDAO.getDeviceUsingId(appId, deviceId, DeviceStatus.ACTIVE);

    if(deviceEntity == null) {
      LOGGER.info("validate : appId : {}, deviceId : {} deviceId not found", appId, deviceId);
      return getFailedResult(PushStatusCode.INVALID_DEVICE_ID);
    }

    if(!ownerId.equals(deviceEntity.getOwnerId())) {
      LOGGER.info("validate : appId : {}, deviceId : {}, ownerId : {}", new Object[]{appId, deviceId, ownerId});
      return getFailedResult(PushStatusCode.INVALID_USER_DEVICE_PAIR);
    }

    if (deviceEntity.getTokenType() == PushType.GCM) {
      if (appEntity.getGoogleAPIKey() == null || appEntity.getGoogleAPIKey().isEmpty()) {
        LOGGER.info("validate : appId={} does nto have a valid api key", appId);
        return getFailedResult(PushStatusCode.INVALID_PUSH_SERVICE_KEY);
      }
      if (deviceEntity.getClientToken() == null) {
        LOGGER.info("validate : appId={}, deviceId={} ownerId={}", new Object[]{appId, deviceId, ownerId});
        return getFailedResult(PushStatusCode.INVALID_TOKEN_FOR_DEVICE);
      }
    } else if (deviceEntity.getTokenType() == PushType.APNS) {
      if (appEntity.getApnsCertPassword() == null || appEntity.getApnsCertPassword().isEmpty()) {
        LOGGER.info("validate : appId={} does nto have a valid apns cert password", appId);
        return getFailedResult(PushStatusCode.INVALID_PUSH_SERVICE_KEY);
      }
      if (appEntity.getApnsCert() == null) {
        LOGGER.info("validate : appId={} does nto have a valid apns cert", appId);
        return getFailedResult(PushStatusCode.INVALID_PUSH_SERVICE_KEY);
      }
    }
    if (deviceEntity.getPushStatus() == PushStatus.INVALID) {
      LOGGER.info("validate : appId={}, deviceId={} ownerId={}", new Object[]{appId, deviceId, ownerId});
      return getFailedResult(PushStatusCode.INVALID_TOKEN_FOR_DEVICE);
    }
    WakeupNotifier.NotificationSystemContext context = null;
    if (deviceEntity.getTokenType() == PushType.GCM) {
      context = new GCMWakeupNotifierImpl.GCMNotificationSystemContext(appEntity.getGoogleAPIKey());
    } else {
      context = new APNSWakeupNotifierImpl.APNSNotificationSystemContext(appEntity.getAppId(), appEntity.isApnsCertProduction());
    }
    return getSuccessResult(context);
  }

  private MMXPushValidationResult getFailedResult(PushStatusCode code) {
    MMXPushValidationResult failedResult = new MMXPushValidationFailure(code);
    return failedResult;
  }

  private MMXPushValidationResult getSuccessResult(WakeupNotifier.NotificationSystemContext context) {
    MMXPushValidationResult successResult = new MMXPushValidationSuccess(PushStatusCode.SUCCESSFUL, context);
    return successResult;
  }
}
