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
package com.magnet.mmx.server.plugin.mmxmgmt.apns;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorMessages;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Count;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Options;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushIdTuple;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.PushResult;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Unsent;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushMessageEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.db.PushStatus;
import com.magnet.mmx.server.plugin.mmxmgmt.push.CallbackUrlUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.push.MMXPushAPNSPayloadBuilder;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PayloadSizeException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGenerator;
import com.magnet.mmx.server.plugin.mmxmgmt.push.PushIdGeneratorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Send Push messages to IOS Devices using APNS
 */
public class APNSPushMessageSender {
  private static Logger LOGGER = LoggerFactory.getLogger(APNSPushMessageSender.class);

  private AppEntity appEntity;
  private Options options;
  private boolean hasOptions;

  public APNSPushMessageSender(AppEntity appEntity, Options options) {
    this.appEntity = appEntity;
    this.options = options;
    if (this.options != null) {
      hasOptions = true;
    }
  }

  public APNSPushMessageSender(AppEntity appEntity) {
    this (appEntity, null);
  }

  public PushResult sendPush(List<DeviceEntity> deviceList, MMXPushAPNSPayloadBuilder builder) {

    APNSConnection connection = null;
    int sentCount = 0;
    int unsentCount = 0;
    int requested = deviceList.size();
    List<Unsent> unsentList = new ArrayList<Unsent>();
    List<PushIdTuple> sentList = new ArrayList<PushIdTuple>(deviceList.size());

    try {
       // retrieve the connection
      connection = getConnection(appEntity.getAppId(), appEntity.isApnsCertProduction());

      if (connection == null) {
        LOGGER.warn("Couldn't retrieve APNS Connection for appId:{} and apnsCertProduction:{}", appEntity.getAppId(), appEntity.isApnsCertProduction());
        //fail all
        for (DeviceEntity de : deviceList) {
          String deviceId = de.getDeviceId();
          int code =  ErrorCode.APNS_INVALID_CERTIFICATE.getCode();
          String message =  ErrorMessages.ERROR_INVALID_APNS_CERT;
          Unsent unsent = buildUnsent(deviceId, code, message);
          unsentList.add(unsent);
          unsentCount++;
        }
      } else {
        PushMessageDAO messageDAO = new PushMessageDAOImpl(getConnectionProvider());

        PushIdGenerator generator = new PushIdGeneratorImpl();
        String appId = appEntity.getAppId();
        for (DeviceEntity de : deviceList) {
          String deviceId = de.getDeviceId();
          String token = de.getClientToken();
          PushStatus pushStatus = de.getPushStatus();

          if (token == null || token.isEmpty()) {
            //no token
            Unsent unsent = buildUnsent(deviceId, ErrorCode.DEVICE_MISSING_TOKEN.getCode(), ErrorMessages.ERROR_INVALID_DEVICE_TOKEN);
            unsentList.add(unsent);
            unsentCount++;
            continue;
          }
          if (pushStatus == PushStatus.INVALID) {
            Unsent unsent = buildUnsent(deviceId, ErrorCode.APNS_INVALID_TOKEN.getCode(), ErrorMessages.ERROR_UNDELIVERABLE_TOKEN);
            unsentList.add(unsent);
            unsentCount++;
            continue;
          }

          String pushId = generator.generateId(appId, deviceId);
          String callBackURL = CallbackUrlUtil.buildCallBackURL(pushId);
          builder.setCallBackURL(callBackURL);
          builder.setId(pushId);
          try {
            String payload = builder.build();
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("sendPush: deviceId:{} payload:{}", deviceId, payload);
            }
            if (hasOptions) {
              connection.send(token, payload, options.getTtl());
            } else {
              connection.send(token, payload);
            }
            PushIdTuple tuple = new PushIdTuple(deviceId, pushId);
            sentList.add(tuple);
            writePushMessage(messageDAO, pushId, appId, deviceId);
            sentCount++;
          } catch (APNSConnectionException e) {
            LOGGER.info("APNSConnectionException", e);
            Unsent unsent = buildUnsent(deviceId, ErrorCode.APNS_CONNECTION_EXCEPTION.getCode(), ErrorMessages.ERROR_APNS_CONNECTION_PROBLEM);
            unsentList.add(unsent);
            unsentCount++;
          } catch (PayloadSizeException e) {
            LOGGER.info("Payload size exception", e);
            Unsent unsent = buildUnsent(deviceId, ErrorCode.APNS_SIZE_EXCEEDED.getCode(), ErrorMessages.ERROR_APNS_PAYLOAD_SIZE);
            unsentList.add(unsent);
            unsentCount++;
          } catch (Exception e) {
            LOGGER.warn("APNS send exception", e);
            Unsent unsent = buildUnsent(deviceId, ErrorCode.APNS_SEND_FAILURE.getCode(), ErrorMessages.ERROR_UNKNOWN_APNS_SEND_ISSUE);
            unsentList.add(unsent);
            unsentCount++;
          }
        }
      }
    } finally {
      if (connection != null) {
        returnConnection(connection);
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
    writePushMessage(pushMessageDAO, messageId, appId, deviceId, PushMessageEntity.PushMessageType.API_PUSH);
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

  protected Unsent buildUnsent (String deviceId, int code, String message) {
    Unsent unsent = new Unsent();
    unsent.setCode(code);
    unsent.setDeviceId(deviceId);
    unsent.setMessage(message);
    return unsent;
  }

  protected APNSConnection getConnection (String appId, boolean production) {
    APNSConnectionPool connectionPool = APNSConnectionPoolImpl.getInstance();
    APNSConnection connection = connectionPool.getConnection(appId, production);
    return connection;
  }


  protected void returnConnection (APNSConnection connection) {
    APNSConnectionPool connectionPool = APNSConnectionPoolImpl.getInstance();
    connectionPool.returnConnection(connection);
  }
}
