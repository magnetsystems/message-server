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

import com.magnet.mmx.util.Utils;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.exceptions.NetworkIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Package private APNS Connection implementation.
 */
class APNSConnectionImpl implements APNSConnection {
  private static Logger LOGGER = LoggerFactory.getLogger(APNSConnectionImpl.class);

  private ApnsService apnsService;
  private APNSConnectionPoolImpl.APNSConnectionKey key;

  /**
   * Constructor.
   *
   * @param apnsService
   * @param key
   */
  APNSConnectionImpl(ApnsService apnsService, APNSConnectionPoolImpl.APNSConnectionKey key) {
    this.apnsService = apnsService;
    this.key = key;
  }

  void open() {
    LOGGER.info("open : Opening APNS Connection for key={}", new Object[]{key});
    apnsService.start();
  }

  void close() {
    LOGGER.info("close : Closing APNS Connection for key={}", new Object[]{key});
    apnsService.stop();
  }

  @Override
  public void send(String deviceToken, String payload) {
    send(deviceToken, payload, null);
  }

  @Override
  public void send(String deviceToken, String payload, Integer ttl) {
    Date expiry = null;
    if (ttl != null) {
      expiry = new Date(System.currentTimeMillis() + (ttl.intValue() * 1000L));
    }
    ApnsNotification notification = null;
    try {
      if (ttl != null) {
        notification = apnsService.push(deviceToken, payload, expiry);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Delivered notification:{} with expiry:{}", notification, Utils.buildISO8601DateFormat().format(expiry));
        }
      } else {
        notification = apnsService.push(deviceToken, payload);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Delivered notification:{} without expiry", notification);
        }
      }
    } catch (NetworkIOException e) {
      LOGGER.warn("NetworkIOException in pushing for key:{}", key, e);
      throw new APNSConnectionException(e.getMessage());
    } catch (RuntimeException e) {
      LOGGER.warn("Exception in pushing for key:{}", key, e);
      throw new APNSConnectionException(e);
    }
  }

  @Override
  public String getAppId() {
    return key.getAppId();
  }

  @Override
  public boolean isApnsProductionCert() {
    return key.isProduction();
  }

  @Override
  public List<String> getInactiveDeviceTokens() {
    List<String> rv;
    Map<String, Date> tokenMap = apnsService.getInactiveDevices();
    if (tokenMap != null && !tokenMap.isEmpty() ) {
      rv = new ArrayList<String>(tokenMap.keySet());
    } else {
      rv = Collections.emptyList();
    }
    return rv;
  }
}
