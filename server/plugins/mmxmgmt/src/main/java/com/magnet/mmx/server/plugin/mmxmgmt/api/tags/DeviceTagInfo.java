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
package com.magnet.mmx.server.plugin.mmxmgmt.api.tags;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 */
@XmlRootElement
@JsonPropertyOrder({ "deviceId", "tags" })
public class DeviceTagInfo {
  private String deviceId;
  private List<String> tags;

  public DeviceTagInfo() {
  }

  public DeviceTagInfo(String deviceId, List<String> tags) {
    this.deviceId = deviceId;
    this.tags = tags;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return "DeviceTagList{" +
            "deviceId='" + deviceId + '\'' +
            ", tags=" + tags +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DeviceTagInfo)) return false;

    DeviceTagInfo that = (DeviceTagInfo) o;

    if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null) return false;
    if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = deviceId != null ? deviceId.hashCode() : 0;
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    return result;
  }
}
