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

package com.magnet.mmx.server.plugin.mmxmgmt.search.endpoint;

import com.magnet.mmx.server.plugin.mmxmgmt.search.MinThreeCharValueValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SearchValueValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.search.TimeRangeValueValidator;

/**
 * Enum for encapsulating the search options for endpoints
 *
 * @author login3
 */
public enum EndPointSearchOption {

  //  USERNAME ("username", new MinThreeCharValueValidator() ),
  /**
   * Name of the end point
   */
  ENDPOINT_NAME("epname", new MinThreeCharValueValidator()),
  /**
   * Date when the end point was created
   */
  ENDPOINT_DATE_CREATED("epdatecreated", new TimeRangeValueValidator()),
  /**
   * Status of the end point
   */
  ENDPOINT_STATUS("epstatus", new MinThreeCharValueValidator()),
  /**
   * Os Type of the endpoint
   */
  ENDPOINT_OSTYPE("epostype", new MinThreeCharValueValidator()),;

  private String option;
  private SearchValueValidator validator;

  private EndPointSearchOption(String _option, SearchValueValidator validator) {
    this.option = _option;
    this.validator = validator;
  }

  public static EndPointSearchOption find(String option) {
    if (option == null) {
      return null;
    }
    EndPointSearchOption rv = null;
    for (EndPointSearchOption possible : EndPointSearchOption.values()) {
      if (possible.option.equalsIgnoreCase(option)) {
        rv = possible;
        break;
      }
    }
    return rv;
  }

  public SearchValueValidator getValidator() {
    return validator;
  }
}
