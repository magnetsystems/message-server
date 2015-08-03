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
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"totalItems", "maxItems", "publishedItems"})
public class MMXPubSubItemList {
  private int totalItems;
  private int maxItems;
  private List<MMXPubSubItem> publishedItems;

  public MMXPubSubItemList(int totalCount, int maxItems, List<MMXPubSubItem> publishedItems) {
    this.totalItems = totalCount;
    this.maxItems = maxItems;
    this.publishedItems = publishedItems;
  }

  public int getTotalItems() {
    return totalItems;
  }

  public void setTotalItems(int totalItems) {
    this.totalItems = totalItems;
  }

  public int getMaxItems() {
    return maxItems;
  }

  public void setMaxItems(int maxItems) {
    this.maxItems = maxItems;
  }

  public List<MMXPubSubItem> getPublishedItems() {
    return publishedItems;
  }

  public void setPublishedItems(List<MMXPubSubItem> publishedItems) {
    this.publishedItems = publishedItems;
  }
}
