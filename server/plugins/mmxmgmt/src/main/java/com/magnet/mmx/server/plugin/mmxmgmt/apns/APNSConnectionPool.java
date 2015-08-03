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

/**
 * Interface for an APNS connection Pool
 */
public interface APNSConnectionPool {

  /**
   * Get an APNS Connection from the pool. If there are no available connections in the pool
   * this invocation will block.
   * @param appId
   * @param productionCert
   * @return
   */
  public APNSConnection getConnection(String appId, boolean productionCert);

  /**
   * Return a connection to the pool.
   * @param connection
   */
  public void returnConnection (APNSConnection connection);

  /**
   * Remove all APNS connections for the specified appId and flag indicating if it is a production cert.
   * @param appId
   * @param productionCert
   */
  public void remove (String appId, boolean productionCert);

}
