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
package com.magnet.mmx.server.plugin.mmxmgmt.api.query;

/**
 */
public class DateRange {

  private Integer start;
  private Integer end;

  public DateRange() {
  }

  public DateRange(Integer start, Integer end) {
    this.start = start;
    this.end = end;
  }

  public Integer getStart() {
    return start;
  }

  public void setStart(Integer start) {
    this.start = start;
  }

  public Integer getEnd() {
    return end;
  }

  public void setEnd(Integer end) {
    this.end = end;
  }

  /**
   * Check if the DateRange object is valid.
   * @param dateRange
   * @return true if the passed date range object has either a start or end value
   */
  public static boolean isValid (DateRange dateRange) {
    boolean rv = false;
    if (dateRange.start != null || dateRange.end != null) {
      rv = true;
    }
    return rv;
  }
}
