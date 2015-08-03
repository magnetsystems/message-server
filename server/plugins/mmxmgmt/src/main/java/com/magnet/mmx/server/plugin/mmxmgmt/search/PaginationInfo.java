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
 * PaginationInfo class that holds the take and skip numbers and 
 * provides API for providing default values
 * @author login3
 *
 */
public class PaginationInfo {
  private int takeSize;
  private int skipSize;


  public PaginationInfo(int takeSize, int skipSize) {
    super();
    this.takeSize = takeSize;
    this.skipSize = skipSize;
  }
  
  public int getTakeSize() {
    return takeSize;
  }
  public int getSkipSize() {
    return skipSize;
  }
  
  /**
   * Build a paginationInfo object using the supplied size and offset values.
   * If the values are invalid/outofrange/null a PaginationInfo object with default
   * values is returned
   * @param size
   * @param offset
   * @return
   */
  public static PaginationInfo build (Integer size, Integer offset) {
    int takeSize;
    if (size == null || size.intValue() <= 0) {
      takeSize = MMXServerConstants.DEFAULT_PAGE_SIZE.intValue();
    } else {
      takeSize = size.intValue();
    }
    
    int skip;
    if (offset == null || offset.intValue() <= 0) {
      skip = MMXServerConstants.DEFAULT_OFFSET.intValue();
    } else {
      skip = offset.intValue();
    }
    return new PaginationInfo(takeSize, skip);
  }

  public static PaginationInfo build (String strSize, String strOffset) {
    Integer size = null;
    Integer offset = null;
    try {
      size = Integer.parseInt(strSize);
    } catch (NumberFormatException e) {
      //ignore
    }
    try {
      offset = Integer.parseInt(strOffset);
    } catch (NumberFormatException e) {
      //ignore
    }
    return build(size, offset);
   }
}
