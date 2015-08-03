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
package com.magnet.mmx.server.plugin.mmxmgmt.push;

import com.magnet.mmx.protocol.OSType;
import com.magnet.mmx.protocol.PingPong;
import com.magnet.mmx.protocol.PushMessage;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DevicePushTokenInvalidator;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.APNSWakeupNotifierImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.GCMWakeupNotifierImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.NotificationResult;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.WakeupNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

/**
 */
public class PushMessageSender implements PushSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(PushMessageSender.class);

  private static final String SERVER_HOSTNAME;

  /**
   * compute the host name one time
   */
  static {
    String hostname = "localhost";
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (Throwable t) {
      LOGGER.warn("Exception when looking up hostname", t);
    }
    SERVER_HOSTNAME = hostname;
  }


  @Override
  public PushResult push(PushRequest request) {
    ConnectionProvider provider = getConnectionProvider();
    AppDAO appDAO = new AppDAOImpl(provider);
    DeviceDAO deviceDAO = new DeviceDAOImpl(provider);
    PushMessageDAO pushMessageDAO = new PushMessageDAOImpl(provider);

    ValidationResult validationResult = validate(request, appDAO, deviceDAO);

    if (!validationResult.isValid()) {
      LOGGER.info("Push request validation failed");
      PushResultImpl result = new PushResultImpl();
      result.setError(true);
      result.setMessage(validationResult.getFailureMessage());
      result.setStatus(PushConstants.PUSH_STATUS_ERROR);
      return result;
    }
    //generate id
    PushIdGenerator idGenerator = getIDGenerator();
    String messageId = idGenerator.generateId(request.getAppId(), request.getDeviceId());
    String callBackURL = CallbackUrlUtil.buildCallBackURL(messageId);
    //we have a valid request.
    PingPong pingMessage = new PingPong(SERVER_HOSTNAME, messageId, request.getText(), callBackURL);

    String payload = null;
    DeviceEntity deviceEntity = validationResult.getDeviceEntity();
    AppEntity appEntity = validationResult.getAppEntity();

    if (deviceEntity.getTokenType() == PushType.APNS) {
      payload = constructAPNSPayLoad(messageId, request.getText(), callBackURL);
    } else {
      payload = constructPayLoad(pingMessage);
    }

    WakeupNotifier notifier = getWakeupNotifier(deviceEntity);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("About to invoke sendNotification");
    }

    WakeupNotifier.NotificationSystemContext context = getNotificationSystemContext(appEntity, deviceEntity);
    List<NotificationResult> results = notifier.sendNotification(Collections.singletonList(deviceEntity.getClientToken())
        , payload, context);
    NotificationResult result = results.get(0);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("NotificationResult = " + result.name());
    }

    PushResultImpl presult = new PushResultImpl();
    if (result == NotificationResult.DELIVERED || result == NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER) {
      writePushMessage(pushMessageDAO, messageId, request.getAppId(), request.getDeviceId());
      presult.setError(false);
      presult.setStatus(PushConstants.PUSH_STATUS_OK);
    } else if (result == NotificationResult.DELIVERY_FAILED_INVALID_TOKEN) {
      if (deviceEntity.getTokenType() == PushType.GCM) {
        //invalidate the token. For APNS this is taken care of by the MMXAPNSDelegate.
        invalidateToken(deviceDAO, request.getAppId(), deviceEntity.getClientToken());
      }
      presult.setError(true);
      presult.setStatus(PushConstants.PUSH_STATUS_ERROR);
      presult.setMessage(result.name());
    } else {
      presult.setError(true);
      presult.setStatus(PushConstants.PUSH_STATUS_ERROR);
      presult.setMessage(result.name());
    }
    return presult;
  }

  protected WakeupNotifier getWakeupNotifier(DeviceEntity deviceEntity) {
    if (deviceEntity.getTokenType() == PushType.APNS) {
      return new APNSWakeupNotifierImpl();
    } else if (deviceEntity.getTokenType() == PushType.GCM) {
      return new GCMWakeupNotifierImpl();
    } else {
      throw new IllegalArgumentException("Bad token type");
    }
  }

  protected WakeupNotifier getWakeupNotifier(WakeupNotifier.NotificationSystemContext context) {
    if (context instanceof APNSWakeupNotifierImpl.APNSNotificationSystemContext) {
      return new APNSWakeupNotifierImpl();
    } else if (context instanceof GCMWakeupNotifierImpl.GCMNotificationSystemContext) {
      return new GCMWakeupNotifierImpl();
    } else {
      throw new IllegalArgumentException("Bad context");
    }
  }

  private ValidationResult validate(PushRequest request, AppDAO appDAO, DeviceDAO deviceDAO) {
    ValidationResult result = null;
    if (request == null) {
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_PUSH_REQUEST);
    }
    result = validate(request.getAppId(), request.getDeviceId(), appDAO, deviceDAO);
    return result;
  }

  private ValidationResult validate(String appId, String deviceId, AppDAO appDAO, DeviceDAO deviceDAO) {
    ValidationResult result = null;
    if (appId == null || appId.isEmpty()) {
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_APPID);
    }
    if (deviceId == null || deviceId.isEmpty()) {
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_DEVID);
    }
    AppEntity appEntity = appDAO.getAppForAppKey(appId);
    if (appEntity == null) {
      LOGGER.info("No app found for id:" + appId);
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_APPID);
    }
    DeviceEntity deviceEntity = deviceDAO.getDeviceUsingId(appId, deviceId, DeviceStatus.ACTIVE);
    if (deviceEntity == null) {
      LOGGER.info(String.format("No active device found for device id :%s for app id : %s", deviceId, appId));
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_DEVID);
    }
    //check we have the information necessary for sending the appropriate notification.
    if (deviceEntity.getTokenType() == PushType.APNS) {
      if (appEntity.getApnsCert() == null || appEntity.getApnsCertPassword() == null || appEntity.getApnsCertPassword().isEmpty()) {
        LOGGER.info("App doesn't have necessary APNs information:" + appId);
        return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_APPID);
      }
    } else if (deviceEntity.getTokenType() == PushType.GCM) {
      if (appEntity.getGoogleAPIKey() == null || appEntity.getGoogleAPIKey().isEmpty()) {
        LOGGER.info("App doesn't have a valid google api key:" + appId);
        result = new ValidationResult();
        result.setValid(false);
        result.setFailureMessage(PushConstants.ERROR_INVALID_APPID);
        return result;
      }
    } else if (deviceEntity.getTokenType() == null) {
      LOGGER.info(String.format("Token is null for Device with id :%s for app id : %s", deviceId, appId));
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_DEVID);
    }
    if (deviceEntity.getClientToken() == null || deviceEntity.getClientToken().isEmpty()) {
      LOGGER.info(String.format("Device with id :%s has token : %s which is invalid", deviceId, deviceEntity.getClientToken()));
      return ValidationResult.failedValidation(PushConstants.ERROR_INVALID_DEVID);
    }
    result = new ValidationResult();
    result.setValid(true);
    result.setAppEntity(appEntity);
    result.setDeviceEntity(deviceEntity);
    return result;
  }


  @Override
  public PushResult push(String appId, String clientToken, MMXPayload payload, WakeupNotifier.NotificationSystemContext context) {
    WakeupNotifier notifier = getWakeupNotifier(context);
    LOGGER.debug("push : payload=\n{}", payload);
    ConnectionProvider provider = getConnectionProvider();
    DeviceDAO deviceDAO = new DeviceDAOImpl(provider);
    List<NotificationResult> results = notifier.sendNotification(Collections.singletonList(clientToken)
        , payload.toString(), context);
    NotificationResult result = results.get(0);

    PushResultImpl presult = new PushResultImpl();
    if (result == NotificationResult.DELIVERED || result == NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER) {
      LOGGER.debug("push : succeeded result={}", result.name());
      presult.setError(false);
      presult.setStatus(PushConstants.PUSH_STATUS_OK);
    } else if (result == NotificationResult.DELIVERY_FAILED_INVALID_TOKEN) {
      if (context instanceof GCMWakeupNotifierImpl.GCMNotificationSystemContext) {
        //invalidate the token. For APNS this is taken care of by the MMXAPNSDelegate.
        invalidateToken(deviceDAO, appId, clientToken);
      }
      presult.setError(true);
      presult.setStatus(PushConstants.PUSH_STATUS_ERROR);
      presult.setMessage(result.name());
    } else {
      LOGGER.debug("push : {} failed status={}", result.name());
      presult.setError(true);
      presult.setStatus(PushConstants.PUSH_STATUS_ERROR);
      presult.setMessage(result.name());
    }
    return presult;
  }


  protected String constructPayLoad(PingPong pingMessage) {
    String constructed = PushMessage.encode(PushMessage.Action.WAKEUP, PushConstants.PUSH_MESSAGE_TYPE, pingMessage);
    return constructed;
  }

  protected String constructAPNSPayLoad(String pushMessageId, String body, String callBackUrl) {
    MMXPushAPNSPayloadBuilder payloadBuilder = new MMXPushAPNSPayloadBuilder();
    payloadBuilder.setBody(body);
    payloadBuilder.setCallBackURL(callBackUrl);
    payloadBuilder.setType(new MMXPushHeader(PushMessage.Action.PUSH.name(), PushConstants.PUSH_MESSAGE_TYPE));
    payloadBuilder.setId(pushMessageId);
    return payloadBuilder.build();
  }

  protected void writePushMessage(PushMessageDAO pushMessageDAO, String messageId, String appId, String deviceId) {
    PushMessageEntity message = new PushMessageEntity();
    message.setAppId(appId);
    message.setDeviceId(deviceId);
    message.setMessageId(messageId);
    message.setState(PushMessageEntity.PushMessageState.PUSHED);
    message.setType(PushMessageEntity.PushMessageType.CONSOLEPING);
    pushMessageDAO.add(message);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Push Message with id : " + messageId + " added.");
    }
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  protected PushIdGenerator getIDGenerator() {
    return new PushIdGeneratorImpl();
  }

  protected WakeupNotifier.NotificationSystemContext getNotificationSystemContext(AppEntity appEntity, DeviceEntity device) {
    WakeupNotifier.NotificationSystemContext context = null;
    if (device.getOsType() == OSType.ANDROID) {
      GCMWakeupNotifierImpl.GCMNotificationSystemContext gcmContext = new GCMWakeupNotifierImpl.GCMNotificationSystemContext(appEntity.getGoogleAPIKey());
      context = gcmContext;
    } else {
      APNSWakeupNotifierImpl.APNSNotificationSystemContext apnsContext = new APNSWakeupNotifierImpl.APNSNotificationSystemContext(appEntity.getAppId(),
           appEntity.isApnsCertProduction());
      context = apnsContext;
    }
    return context;
  }

  protected void invalidateToken (DeviceDAO dao, String appId, String token) {
    DevicePushTokenInvalidator invalidator = new DevicePushTokenInvalidator();
    invalidator.invalidateToken(appId, PushType.GCM, token);
  }

  /**
   * Private class for validation result encapsulation
   */
  public static class ValidationResult {

    private boolean valid;
    private AppEntity appEntity;
    private DeviceEntity deviceEntity;
    private String failureMessage;

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public AppEntity getAppEntity() {
      return appEntity;
    }

    public void setAppEntity(AppEntity appEntity) {
      this.appEntity = appEntity;
    }

    public DeviceEntity getDeviceEntity() {
      return deviceEntity;
    }

    public void setDeviceEntity(DeviceEntity deviceEntity) {
      this.deviceEntity = deviceEntity;
    }

    public String getFailureMessage() {
      return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
      this.failureMessage = failureMessage;
    }

    public static ValidationResult failedValidation (String message ) {
      ValidationResult failed = new ValidationResult();
      failed.setValid(false);
      failed.setFailureMessage(message);
      return failed;
    }

  }

  /**
   * Implementation of the PushResult interface.
   */
  public static class PushResultImpl implements PushResult {
    private String status;
    private String message;
    private boolean error;

    public void setStatus(String status) {
      this.status = status;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public void setError(boolean error) {
      this.error = error;
    }

    @Override
    public String getStatus() {
      return status;
    }

    @Override
    public boolean isError() {
      return error;
    }

    @Override
    public String getMessage() {
      return message;
    }
  }
}
