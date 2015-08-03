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
import com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint.EndPointSearchOption;
import com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint.EndPointSortOption;
import com.magnet.mmx.server.plugin.mmxmgmt.web.ValueHolder;

/**
 * Interface that defines the API for retrieving EndPoint entity objects.
 */
public interface EndPointDAO {

  /**
   * Execute search to retrieve end points that match the supplied search criterion.
   * @param appId - id of the application for which we need to search endpoints
   * @param searchOption -- search option object encapsulating the search criterion
   * @param valueHolder -- value(s) for the criterion
   * @param sortOption -- information about how to sort the results.
   * @param info -- pagination information to limit the results
   * @return
   */
  public SearchResult<EndPointEntity> search(String appId, EndPointSearchOption searchOption, ValueHolder valueHolder,
                                             EndPointSortOption sortOption, PaginationInfo info);

}
