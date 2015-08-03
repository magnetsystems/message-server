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

import com.magnet.mmx.server.plugin.mmxmgmt.db.ConnectionProvider;

import java.util.List;

/**
 */
public class StubMMXAppAPNSFeedbackProcessor extends MMXAppAPNSFeedbackProcessor {

  private List<String> badTokens;


  public StubMMXAppAPNSFeedbackProcessor(ConnectionProvider provider, String appId, boolean productionCert, List<String> badTokens) {
    super(provider, appId, productionCert);
    this.badTokens = badTokens;
  }

  @Override
  protected APNSConnection getAPNSConnection(String appId, boolean production) {
    APNSConnection connection = new StubAPNSConnection(appId, production, 10L, badTokens);
    return connection;
  }

}
