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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChannelTagInfo {
  private String channelName;
  private List<String> tags;

  public ChannelTagInfo() {
  }

  public ChannelTagInfo(String channelName, List<String> tags) {
    this.channelName = channelName;
    this.tags = tags;
  }

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ChannelTagInfo)) return false;

    ChannelTagInfo that = (ChannelTagInfo) o;

    if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
    if (channelName != null ? !channelName.equals(that.channelName) : that.channelName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = channelName != null ? channelName.hashCode() : 0;
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    return result;
  }
}
