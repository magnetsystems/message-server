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

package com.magnet.mmx.server.plugin.mmxmgmt.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author login3
 *
 */
public class SortInfo {
  
  private List<SortPair> sortPairs;
  private final static String DEFAULT_ORDER = "ascending";
  private final static String ESCAPED_COMMA = Pattern.quote(",");
  private SortInfo() {
    sortPairs = new ArrayList<SortPair>(10);
  }
  
  private void add(SortPair pair) {
    sortPairs.add(pair);
  }
  
  
  
  public List<SortPair> getSortPairs() {
    return sortPairs;
  }

  /**
   * Build a SortInfo object using the supplied sortBy and sortOrder clauses
   * @param sortBy
   * @param sortOrder
   * @return
   */
  public static SortInfo build(String sortBy, String sortOrder) {
    if (sortBy == null || sortBy.isEmpty()) {
      return new SortInfo();
    }
    String[] sortByList = sortBy.split(ESCAPED_COMMA);

    String[] sortOrderList = null;
    if (sortOrder == null || sortOrder.isEmpty()) {
      sortOrderList = new String[]{};
    } else {
      sortOrderList = sortOrder.split(ESCAPED_COMMA);
    }


    SortInfo info = new SortInfo();

    for (int i = 0; i < sortByList.length; i++) {
      String key = sortByList[i];
      String order = DEFAULT_ORDER;
      if (i < sortOrderList.length) {
        order = sortOrderList[i];
      }
      info.add(new SortPair(key, order));
    }
    return info;
  }
  
  
  public static class SortPair {
    final String key;
    final String order;
    /**
     * 
     * @param key
     * @param order
     */
    public SortPair(String key, String order) {
      super();
      this.key = key;
      this.order = order;
    }
    public String getKey() {
      return key;
    }
    public String getOrder() {
      return order;
    }
  }
}
