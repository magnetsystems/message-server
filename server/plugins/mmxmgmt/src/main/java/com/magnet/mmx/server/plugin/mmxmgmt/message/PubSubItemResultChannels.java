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

import java.util.List;

import com.magnet.mmx.server.api.v2.ChannelResource.MMXPubSubItemChannel2;

/**
 */
public class PubSubItemResultChannels {
  int totalCount;
  List<MMXPubSubItemChannel2> items;

  public PubSubItemResultChannels(int totalCount, List<MMXPubSubItemChannel2> list) {
    this.totalCount = totalCount;
    this.items = list;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public List<MMXPubSubItemChannel2> getItems() {
    return items;
  }
}
