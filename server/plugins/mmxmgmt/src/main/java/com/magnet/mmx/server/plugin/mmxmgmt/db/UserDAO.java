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
package com.magnet.mmx.server.plugin.mmxmgmt.db;

import com.magnet.mmx.server.plugin.mmxmgmt.search.PaginationInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.user.UserSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;

import java.util.List;

/**
 * For searching users.
 */
public interface UserDAO {

  /**
   * Search for users
   * @param appId
   * @param searchOption
   * @param valueHolder
   * @param sortOption
   * @param info
   * @return
   */
  public UserSearchResult searchUsers(String appId, UserSearchOption searchOption, ValueHolder valueHolder,
                                                 UserSortOption sortOption, PaginationInfo info);

  public UserEntity getUser(String username) throws DbInteractionException;

  /**
   * Retrieve a list of user using the QueryBuilderResult.
   * @param result
   * @return
   * @throws DbInteractionException
   */
  public List<UserEntity> getUsers(QueryBuilderResult result) throws DbInteractionException;

  /**
   * Get users using the QueryBuilder result.
   * @param result
   * @param pinfo
   * @return
   */
  public SearchResult<UserEntity> getUsersWithPagination (QueryBuilderResult result, PaginationInfo pinfo);
}
