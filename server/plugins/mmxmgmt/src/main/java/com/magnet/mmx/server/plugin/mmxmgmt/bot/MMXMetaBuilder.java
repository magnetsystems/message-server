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
package com.magnet.mmx.server.plugin.mmxmgmt.bot;

import com.google.gson.annotations.SerializedName;
import com.magnet.mmx.util.JSONifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MMXMetaBuilder {

  /**
   * Build the JSON string for the mmx internal meta data with the supplied userId and deviceId
   * @param userId
   * @param deviceId
   * @return
   */
  public static String build (String userId, String deviceId) {
    MetaTo metaTo = new MetaTo();
    MetaEntry entry = new MetaEntry();
    entry.setUserId(userId);
    entry.setDevId(deviceId);
    metaTo.setTo(Collections.singletonList(entry));
    return metaTo.toJson();
  }

  private static class MetaEntry extends JSONifiable {
    private String devId;
    private String userId;

    public String getDevId() {
      return devId;
    }

    public void setDevId(String devId) {
      this.devId = devId;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }
  }

  /**
   * Internal meta to entry
   */
  private static class MetaTo extends JSONifiable {
    @SerializedName("To")
    private List<MetaEntry> to = new ArrayList<MetaEntry>();


    public List<MetaEntry> getTo() {
      return to;
    }

    public void setTo(List<MetaEntry> to) {
      this.to = to;
    }
  }

}
