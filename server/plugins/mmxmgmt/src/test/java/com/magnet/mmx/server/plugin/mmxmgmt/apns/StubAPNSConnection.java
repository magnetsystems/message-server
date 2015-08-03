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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
*/
class StubAPNSConnection implements APNSConnection {
  private static Logger LOGGER = LoggerFactory.getLogger(StubAPNSConnection.class);
  private String appId;
  private boolean production;
  private long sleepTime;
  private List<String> invalidTokens = Collections.emptyList();

  StubAPNSConnection(String appId, boolean production, long sleepTime) {
    this (appId, production, sleepTime, new ArrayList<String>(0));
  }

  StubAPNSConnection(String appId, boolean production, long sleepTime, List<String> invalidTokens) {
    this.appId = appId;
    this.production = production;
    this.sleepTime = sleepTime;
    this.invalidTokens = invalidTokens;
  }

  @Override
  public void send(String deviceToken, String payload) {
    this.send(deviceToken, payload, null);
  }

  @Override
  public void send(String deviceToken, String payload, Integer ttl) {
    //pretend that it takes sleepTime seconds to send the payload
    try {
      Thread.sleep(sleepTime);
    } catch (InterruptedException e) {
      //e.printStackTrace();
    }
    LOGGER.info(String.format("Sending to:%s payload:%S", deviceToken, payload));
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public boolean isApnsProductionCert() {
    return production;
  }

  @Override
  public List<String> getInactiveDeviceTokens() {
    return invalidTokens;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("StubAPNSConnection{");
    sb.append("appId='").append(appId).append('\'');
    sb.append(", production=").append(production);
    sb.append('}');
    return sb.toString();
  }
}
