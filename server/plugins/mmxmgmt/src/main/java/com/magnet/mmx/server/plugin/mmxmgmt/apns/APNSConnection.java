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

import java.util.List;

/**
 * Interface that defines the API for an APNS Connection.
 */
public interface APNSConnection {

  /**
   * Send the supplied payload to the supplied device token
   * @param deviceToken
   * @param payload
   * @throws APNSConnectionException if a network/connection related issue
   * is encountered.
   */
  public void send (String deviceToken, String payload) throws APNSConnectionException;

  /**
   * Send the supplied payload to the supplied device token with the specified ttl.
   * @param deviceToken
   * @param payload
   * @param ttl number of seconds from the current time for expiry.
   * @throws APNSConnectionException if a network/connection related issue
   * is encountered.
   */
  public void send (String deviceToken, String payload, Integer ttl) throws APNSConnectionException;

  /**
   * Get the appId associated with this APNS Connection.
   * @return
   */
  public String getAppId();

  /**
   * Get the boolean flag indicating if this APNS Connection is using the production endpoint.
   * @return
   */
  public boolean isApnsProductionCert();

  /**
   * Get a list of device tokens that are reported by APNS service as being inactive/unreachable/app uninstalled.
   * @return
   */
  public List<String> getInactiveDeviceTokens();
}
