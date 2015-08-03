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

package com.magnet.mmx.server.plugin.mmxmgmt.search.user;


import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortInfo.SortPair;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SortOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author login3
 *
 */
public class UserSortOption {
  private UserSearchOption column;
  private SortOrder order;

  private UserSortOption(UserSearchOption column, SortOrder order) {
    super();
    this.column = column;
    this.order = order;
  }

  public UserSearchOption getColumn() {
    return column;
  }

  public SortOrder getOrder() {
    return order;
  }
  
  /**
   * Get SortOptions for a Message Entity using the sortBy and sortOrder values
   * supplied with the request
   * @param sortBy
   * @param sortOrder
   * @return
   */
  public static List<UserSortOption> build (String sortBy, String sortOrder) {
    SortInfo sortInfo = SortInfo.build(sortBy, sortOrder);
    List<UserSortOption> options = build(sortInfo);
    if (options.isEmpty()) {
      options.add(defaultSortOption());
    }
    return options;
  }
  
  /**
   * Build a list of MessageSortOption
   * @param sortInfo
   * @return
   */
  public static List<UserSortOption> build (SortInfo sortInfo) {
    List<UserSortOption> options = new ArrayList<UserSortOption>(5);
    
    List<SortPair> sortPairs = sortInfo.getSortPairs();
    
    for (SortPair pair : sortPairs) {
      UserSearchOption column = UserSearchOption.find(pair.getKey());
      SortOrder order = SortOrder.from(pair.getOrder());
      
      if (column != null) {
        if (order == null) {
          UserSortOption option = new UserSortOption(column, SortOrder.ASCENDING);
          options.add(option);
        } else {
          UserSortOption option = new UserSortOption(column, order);
          options.add(option);
        }
      }
    }
    return options;
  }
  
  /**
   * Get the default sort option for the message entity.
   * Clients should use this if the user supplied options are empty
   */
  public static UserSortOption defaultSortOption() {
    UserSortOption option = new UserSortOption(UserSearchOption.USERNAME, SortOrder.DESCENDING);
    return option;
  }
}
