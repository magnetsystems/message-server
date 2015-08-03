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
import java.util.ArrayList;
import java.util.List;

/**
 */
@XmlRootElement
@JsonPropertyOrder({ "username", "tags" })
public class UserTagInfo {
  private String username;
  private List<String> tags = new ArrayList<String>();

  public UserTagInfo() {
  }

  public UserTagInfo(String username, List<String> tags) {
    this.username = username;
    this.tags = tags;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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
    if (!(o instanceof UserTagInfo)) return false;

    UserTagInfo that = (UserTagInfo) o;

    if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
    if (username != null ? !username.equals(that.username) : that.username != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = username != null ? username.hashCode() : 0;
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "UserTagList{" +
            "username='" + username + '\'' +
            ", tags=" + tags +
            '}';
  }
}
