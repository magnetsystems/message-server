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
import com.magnet.mmx.server.plugin.mmxmgmt.web.*;

/**
 * This validator requires that clients supply a start and an end value.
 */
public class TimeRangeValueValidator implements SearchValueValidator {
  static final String MISSING_SEARCH_VALUE = "Value not provided for either start time or end time.";
  static final String BAD_START_TS = "Invalid value provided for start time.";
  static final String BAD_END_TS = "Invalid value provided for end time.";


  @Override
  public ValidatorResult validate(String... value) {
    boolean isValid = true;
    int length = value != null ? value.length : 0;
    if (length < 2) {
      isValid = false;
      //we don't have either a start of end. That is no good
      String code = MessageCode.INVALID_SEARCH_VALUE.name();
      String message = MISSING_SEARCH_VALUE;
      return new BaseValidatorResult(isValid, code, message);
    }
    if (value[0] == null || value[1] == null) {
      isValid = false;
      //we don't have either a start or end. That is no good
      String code = MessageCode.INVALID_SEARCH_VALUE.name();
      String message = MISSING_SEARCH_VALUE;
      return new BaseValidatorResult(isValid, code, message);
    }
    if (value[0] != null) {
      try {
        long start = Long.parseLong(value[0]);
      } catch (NumberFormatException e) {
        isValid = false;
        String code = MessageCode.INVALID_SEARCH_VALUE.name();
        String message = BAD_START_TS;
        return new BaseValidatorResult(isValid, code, message);
      }
    }
    if (value[1] != null) {
      try {
        long end = Long.parseLong(value[1]);
      } catch (NumberFormatException e) {
        isValid = false;
        String code = MessageCode.INVALID_SEARCH_VALUE.name();
        String message = BAD_END_TS;
        return new BaseValidatorResult(isValid, code, message);
      }
    }
    //params are valid
    return new BaseValidatorResult(isValid);
  }
}
