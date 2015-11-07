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
package com.magnet.mmx.server.plugin.mmxmgmt.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 */
@XmlRootElement
@XmlType(propOrder = {"appId", "channels"})
@XmlAccessorType(XmlAccessType.FIELD)
public class MMXChannelSummaryResult {
  String appId;
  List<MMXChannelSummary> channels;

  public MMXChannelSummaryResult(String appId, List<MMXChannelSummary> channels) {
    this.appId = appId;
    this.channels = channels;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public List<MMXChannelSummary> getChannels() {
    return channels;
  }

  public void setChannels(List<MMXChannelSummary> channels) {
    this.channels = channels;
  }

  @Override
  public String toString() {
    return "MMXChannelSummaryResult{" +
            "appId='" + appId + '\'' +
            ", channels=" + channels +
            '}';
  }
}
