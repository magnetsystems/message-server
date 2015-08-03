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

import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MessageCode;

/**
 * Validates that single value supplied is not null and not empty and has atleast 3 characters
 */
public class MinThreeCharValueValidator implements SearchValueValidator {
  static final String MISSING_SEARCH_VALUE = "Search value needs at least 3 characters.";

  @Override
  public ValidatorResult validate(String... value) {
    boolean isValid = true;

    if (value == null || value.length == 0 || value[0] == null || value[0].isEmpty() || value[0].length() < 3) {
      isValid = false;
      return new BaseValidatorResult(isValid, MessageCode.INVALID_SEARCH_VALUE.name(), MISSING_SEARCH_VALUE);
    }

    return new BaseValidatorResult(isValid);
  }
}
