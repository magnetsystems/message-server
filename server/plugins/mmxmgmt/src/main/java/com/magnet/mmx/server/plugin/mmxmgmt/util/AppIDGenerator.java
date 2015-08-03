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

package com.magnet.mmx.server.plugin.mmxmgmt.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * This is a custom ID generator that is used for generating AppIDs.
 * AppIDs are constructed using a timestamp value and a random number
 * between 0 and Short.MAX_VALUE.
 * The two numbers are converted to String using Integer/Long.toString
 * with radix of 36.
 * The ID is then constructed by joining the two strings together.
 */
public class AppIDGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppIDGenerator.class);

  private static final Random random = new Random();
  private static final int RADIX = 36;

  /**
   * Generate an ID using a random short and current timestamp (number of 
   * milliseconds from epoch).
   * @return
   */
  public static String generate() {
    long timeValue = System.currentTimeMillis();
    int intValue =  random.nextInt(Short.MAX_VALUE);
    return generate(timeValue, intValue);
  }

  public static String generate(long timestamp, int number) {
    String generated = null;
    try {
      generated = Integer.toString(number,RADIX) + Long.toString(timestamp, RADIX);
    } catch (Exception e) {
      LOGGER.warn("Unsupported encoding exception in generating id", e);
    }
    return generated;
  }
}
