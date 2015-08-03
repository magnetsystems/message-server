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

import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;

/**
 * @author login3
 *
 */
public enum SortOrder {
  ASCENDING,
  DESCENDING;
  
  public static SortOrder from(String sortOrder) {
    SortOrder order = null;
    
    SortOrder[] orders = SortOrder.values();
    
    for (SortOrder possible : orders) {
      if (possible.name().equalsIgnoreCase(sortOrder)) {
        order = possible;
        break;
      }
    }
    if (order == null && MMXServerConstants.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder))  {
      order = SortOrder.ASCENDING;
    }
    if (order == null && MMXServerConstants.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder)){
      order = SortOrder.DESCENDING;
    }
    return order;
  }
}
