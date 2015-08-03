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

package com.magnet.mmx.server.plugin.mmxmgmt.search.device;

import com.magnet.mmx.server.plugin.mmxmgmt.search.MinThreeCharValueValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.search.NotEmptyValueValidator;
import com.magnet.mmx.server.plugin.mmxmgmt.search.SearchValueValidator;

/**
 * Enum for encapsulating the search options for user
 * @author login3
 *
 */
public enum DeviceSearchOption {

  USERNAME ("username", new MinThreeCharValueValidator() ),
  PHONE("phone",new MinThreeCharValueValidator()),
  DATE_CREATED ("datecreated", new NotEmptyValueValidator()),
  DEVICE_ID ("deviceid", new MinThreeCharValueValidator())
  ;
  
  private String option;
  private SearchValueValidator validator;
  
  private DeviceSearchOption(String _option, SearchValueValidator validator) {
    this.option = _option;
    this.validator = validator;
  }
  
  public static DeviceSearchOption find (String option) {
    if (option == null) {
      return null;
    }
    DeviceSearchOption rv = null;
    for (DeviceSearchOption possible : DeviceSearchOption.values()) {
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
