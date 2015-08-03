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
public class TopicTagInfo {
  private String topicName;
  private List<String> tags;

  public TopicTagInfo() {
  }

  public TopicTagInfo(String topicName, List<String> tags) {
    this.topicName = topicName;
    this.tags = tags;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
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
    if (!(o instanceof TopicTagInfo)) return false;

    TopicTagInfo that = (TopicTagInfo) o;

    if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
    if (topicName != null ? !topicName.equals(that.topicName) : that.topicName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = topicName != null ? topicName.hashCode() : 0;
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    return result;
  }
}
