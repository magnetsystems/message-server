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

import com.magnet.mmx.protocol.PushType;
import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.db.DevicePushTokenInvalidator;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.notnoop.apns.ApnsDelegate;
import com.notnoop.apns.ApnsDelegateAdapter;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.internal.Utilities;
import com.notnoop.exceptions.ApnsDeliveryErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Delegate that receives the callbacks from the APNS library to indicate message delivery and message
 * delivery problems.
 */
public class MMXAPNSDelegate extends ApnsDelegateAdapter implements ApnsDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(MMXAPNSDelegate.class);

  private APNSConnectionPoolImpl.APNSConnectionKey key = null;

  /**
   * Constructor with key for associated with this delegate.
   *
   * @param key not null key.
   */
  public MMXAPNSDelegate(APNSConnectionPoolImpl.APNSConnectionKey key) {
    this.key = key;
  }

  private static String fromBytes(byte[] bytes) {
    String rv = null;
    try {
      rv = new String(bytes, MMXServerConstants.UTF8_ENCODING);
    } catch (UnsupportedEncodingException e) {
      LOGGER.info("UnsupportedEncodingException when converting bytes to string", e);
    }
    return rv;
  }

  @Override
  public void messageSent(ApnsNotification message, boolean resent) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Delivered APNS message for key:{}", key);
    }
  }

  @Override
  public void messageSendFailed(ApnsNotification message, Throwable e) {
    LOGGER.info("APNS Message delivery failed for key:{}", key, e);
    if (message != null) {
      byte[] tokenByte = message.getDeviceToken();
      String deviceToken = Utilities.encodeHex(tokenByte);
      invalidateToken(deviceToken);
      byte[] payload = message.getPayload();
      String payloadString = fromBytes(payload);
      if (payloadString != null) {
        APNSPayloadInfo info = APNSPayloadInfo.parse(payloadString);
        if (info != null && info.getMessageId() != null) {
          Integer errorCode = null;
          if (e instanceof ApnsDeliveryErrorException) {
            errorCode = Integer.valueOf(((ApnsDeliveryErrorException) e).getDeliveryError().code());
          }
          //mark push message state as error
          updatePushMessageState(info.getMessageId(), errorCode);
        }
      }
    } else {
      LOGGER.warn("APNS Message delivery failed but ApnsNotification message is null and hence can't invalidate the token");
    }
  }

  ConnectionProvider getConnectionProvider() {
    return new OpenFireDBConnectionProvider();
  }

  void invalidateToken(String deviceToken) {
    LOGGER.info("Invalidating token:{} for appId:{}", deviceToken, key.getAppId());
    DevicePushTokenInvalidator invalidator = new DevicePushTokenInvalidator();
    invalidator.invalidateToken(key.getAppId(), PushType.APNS, deviceToken);
  }

  void updatePushMessageState(String messageId, Integer errorCode) {
    LOGGER.info("Need to updating message state for message with id:{}", messageId);
  }

}
