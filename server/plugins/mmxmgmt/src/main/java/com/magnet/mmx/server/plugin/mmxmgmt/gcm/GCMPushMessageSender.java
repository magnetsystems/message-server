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
package com.magnet.mmx.server.plugin.mmxmgmt.gcm;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Count;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Options;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushIdTuple;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushResult;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Unsent;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DevicePushTokenInvalidator;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.push.CallbackUrlUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushGCMPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGenerator;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGeneratorImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.wakeup.NotificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GCM Push message sender.
 */
public class GCMPushMessageSender {
  private static Logger LOGGER = LoggerFactory.getLogger(GCMPushMessageSender.class);
  private final int RETRY_COUNT = 1;
  private AppEntity appEntity;
  private Options options;
  private boolean hasOptions;
  private String googleApiKey;

  /**
   * Constructor
   * @param appEntity
   * @param options
   */
  public GCMPushMessageSender(AppEntity appEntity, Options options) {
    this.appEntity = appEntity;
    this.options = options;
    if (this.options != null) {
      hasOptions = true;
    }
    this.googleApiKey = appEntity.getGoogleAPIKey();
  }

  public GCMPushMessageSender(AppEntity appEntity) {
    this (appEntity, null);
  }


  public PushResult sendPush(List<DeviceEntity> deviceList, MMXPushGCMPayloadBuilder builder) {
    int sentCount = 0;
    int unsentCount = 0;
    int requested = deviceList.size();
    List<Unsent> unsentList = new ArrayList<Unsent>();
    List<PushIdTuple> sentList = new ArrayList<PushIdTuple>(deviceList.size());

    if (googleApiKey == null) {
      LOGGER.warn("Google API key for appId:{} is null", appEntity.getAppId());
      //fail all
      int code =  ErrorCode.GCM_INVALID_GOOGLE_API_KEY.getCode();
      String message =  ErrorMessages.ERROR_INVALID_GOOGLE_API_KEY;
      for (DeviceEntity de : deviceList) {
        String deviceId = de.getDeviceId();
        Unsent unsent = new Unsent(deviceId, code, message);
        unsentList.add(unsent);
        unsentCount++;
      }
    } else {
      Sender sender = getSender(googleApiKey);
      PushMessageDAO messageDAO = new PushMessageDAOImpl(getConnectionProvider());
      DeviceDAO deviceDAO = new DeviceDAOImpl(getConnectionProvider());
      PushIdGenerator generator = new PushIdGeneratorImpl();
      Integer ttl = null;
      if (hasOptions && options.getTtl() != null) {
        ttl =  options.getTtl();
      }
      String appId = appEntity.getAppId();
      for (DeviceEntity de : deviceList) {
        String token = de.getClientToken();
        String deviceId = de.getDeviceId();

        if (token == null || token.isEmpty()) {
          //no token
          Unsent unsent = new Unsent(deviceId, ErrorCode.DEVICE_MISSING_TOKEN.getCode(), ErrorMessages.ERROR_INVALID_DEVICE_TOKEN);
          unsentList.add(unsent);
          unsentCount++;
          continue;
        }
        if (de.getPushStatus() == PushStatus.INVALID) {
          Unsent unsent = new Unsent(deviceId, ErrorCode.APNS_INVALID_TOKEN.getCode(), ErrorMessages.ERROR_UNDELIVERABLE_TOKEN);
          unsentList.add(unsent);
          unsentCount++;
          continue;
        }
        NotificationResult nresult = null;
        String pushId = generator.generateId(appId, deviceId);
        String callBackURL = CallbackUrlUtil.buildCallBackURL(pushId);
        builder.setCallBackURL(callBackURL);
        builder.setId(pushId);
        String payload = builder.build();

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("sendPush: deviceId:{} payload:{}", deviceId, payload);
        }

        Message.Builder mb = new Message.Builder().addData("msg", payload);
        if (ttl != null) {
          mb.timeToLive(ttl.intValue());
        }

        Message gcmMessage = mb.build();
        try {
          MulticastResult mcResult = sender.send(gcmMessage, Collections.singletonList(token), RETRY_COUNT);
          if (mcResult.getFailure() == 0) {
            nresult = NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER;
          } else {
            String error = mcResult.getResults().get(0).getErrorCodeName();
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Delivery of payload:{} to device token:{} failed. Error:{}" , gcmMessage, token, error);
            }
            if (error.equals(Constants.ERROR_NOT_REGISTERED) || error.equals(Constants.ERROR_INVALID_REGISTRATION)) {
              nresult = NotificationResult.DELIVERY_FAILED_INVALID_TOKEN;
              //mark the device has having bad token
              invalidateToken(deviceDAO, appId, token);
            } else if (error.equals(Constants.ERROR_MESSAGE_TOO_BIG)) {
              //message too big
              nresult = NotificationResult.DELIVERY_FAILED_MESSAGE_TOO_BIG;
            } else {
              nresult = NotificationResult.DELIVERY_FAILED_PERMANENT;
            }
          }
        } catch (IOException e) {
          LOGGER.warn(String.format("Sending:%s to tokens:%s failed with an exception", payload, token), e);
          nresult = NotificationResult.DELIVERY_FAILED_PERMANENT;
        }
        if (nresult == NotificationResult.DELIVERY_IN_PROGRESS_ASSUME_WILL_EVENTUALLY_DELIVER ||
            nresult == NotificationResult.DELIVERED ||
            nresult == NotificationResult.DELIVERY_IN_PROGRESS_REMIND_AGAIN) {
          writePushMessage(messageDAO, pushId, appId, deviceId);
          sentCount++;
          PushIdTuple tuple = new PushIdTuple(deviceId, pushId);
          sentList.add(tuple);
        } else {
          LOGGER.info("Sending message to device id:{} failed", deviceId);
          int code;
          String message;
          if (nresult == NotificationResult.DELIVERY_FAILED_INVALID_TOKEN) {
            code = ErrorCode.GCM_INVALID_TOKEN.getCode();
            message = ErrorMessages.ERROR_INVALID_GCM_TOKEN;
          } else if (nresult == NotificationResult.DELIVERY_FAILED_MESSAGE_TOO_BIG) {
            code = ErrorCode.GCM_SIZE_EXCEEDED.getCode();
            message = ErrorMessages.ERROR_GCM_PAYLOAD_SIZE;
          } else {
            code = ErrorCode.GCM_SEND_FAILURE.getCode();
            message = ErrorMessages.ERROR_UNKNOWN_GCM_SEND_ISSUE;
          }
          Unsent unsent = new Unsent(deviceId, code, message);
          unsentList.add(unsent);
          unsentCount++;
        }
      }
    }
    Count resultCount = new Count(requested, sentCount, unsentCount);
    PushResult result = new PushResult();
    result.setCount(resultCount);
    result.setSentList(sentList);
    result.setUnsentList(unsentList);
    return result;
  }

  protected ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }


  protected void writePushMessage(PushMessageDAO pushMessageDAO, String messageId, String appId, String deviceId) {
    this.writePushMessage(pushMessageDAO, messageId, appId, deviceId, PushMessageEntity.PushMessageType.API_PUSH);
  }

  protected void writePushMessage(PushMessageDAO pushMessageDAO, String messageId, String appId, String deviceId, PushMessageEntity.PushMessageType type) {
    PushMessageEntity message = new PushMessageEntity();
    message.setAppId(appId);
    message.setDeviceId(deviceId);
    message.setMessageId(messageId);
    message.setState(PushMessageEntity.PushMessageState.PUSHED);
    message.setType(type);
    pushMessageDAO.add(message);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Push Message with id : " + messageId + " added.");
    }
  }

  protected Sender getSender (String googleApiKey) {
    return new Sender(googleApiKey);
  }


  protected void invalidateToken (DeviceDAO dao, String appId, String token) {
    DevicePushTokenInvalidator invalidator = new DevicePushTokenInvalidator();
    invalidator.invalidateToken(appId, PushType.GCM, token);
  }

}
