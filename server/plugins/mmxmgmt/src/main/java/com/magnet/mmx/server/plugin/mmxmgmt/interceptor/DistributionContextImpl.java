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
package com.magnet.mmx.server.plugin.mmxmgmt.interceptor;

/**
 */
public class DistributionContextImpl implements MessageDistributor.DistributionContext {
  private String bareUserId;
  private String serverHostname;
  private String appId;
  private String messageId;

  public DistributionContextImpl(String bareUserId, String appId, String serverHostname, String messageId) {
    this.bareUserId = bareUserId;
    this.serverHostname = serverHostname;
    this.appId = appId;
    this.messageId = messageId;
  }

  @Override
  public String getBareUserId() {
    return bareUserId;
  }

  @Override
  public String getServerHostname() {
    return serverHostname;
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public String getMessageId() {
    return messageId;
  }
}
