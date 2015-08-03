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
package com.magnet.mmx.server.plugin.mmxmgmt.web;

import com.magnet.mmx.server.plugin.mmxmgmt.servlet.MessageCode;

/**
 * Validation for values that define a time range.
 * Value1 is the start time stamp
 * and Value 2 is the end time stamp
 */
public class MessageTimeRangeSearchValidator implements MessageSearchValidator {

  static final String MISSING_SEARCH_VALUE = "No value provided for either start time or end time.";
  static final String BAD_START_TS = "Invalid value provided for start time.";
  static final String BAD_END_TS = "Invalid value provided for end time.";

  @Override
  public ValidatorResult validate(String value1, String value2) {
    boolean isValid = true;
    if (value1 == null && value2 == null) {
      isValid = false;
      //we don't have either a start of end. That is no good
      String code = MessageCode.INVALID_SEARCH_VALUE.name();
      String message = MISSING_SEARCH_VALUE;
      return new BaseValidatorResult(isValid, code, message);
    }
    if (value1 != null) {
      try {
        long start = Long.parseLong(value1);
      } catch (NumberFormatException e) {
        isValid = false;
        String code = MessageCode.INVALID_SEARCH_VALUE.name();
        String message = BAD_START_TS;
        return new BaseValidatorResult(isValid, code, message);
      }
    }
    if (value2 != null) {
      try {
        long end = Long.parseLong(value2);
      } catch (NumberFormatException e) {
        isValid = false;
        String code = MessageCode.INVALID_SEARCH_VALUE.name();
        String message = BAD_END_TS;
        return new BaseValidatorResult(isValid, code, message);
      }
    }
    //params are valid
    return new BaseValidatorResult(true);
  }
}
