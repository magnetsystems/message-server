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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class MessageTimeRangeSearchValidatorTest {


  @Test
  public void test1Validate() throws Exception {
    String value1 = null;
    String value2 = null;

    MessageTimeRangeSearchValidator validator = new MessageTimeRangeSearchValidator();
    MessageSearchValidator.ValidatorResult result = validator.validate(value1, value2);
    assertFalse("Expected to be invalid", result.isValid());
  }

  @Test
  public void test2Validate() throws Exception {
    String value1 = null;
    String value2 = Long.toString(System.currentTimeMillis()/1000L);

    MessageTimeRangeSearchValidator validator = new MessageTimeRangeSearchValidator();
    MessageSearchValidator.ValidatorResult result = validator.validate(value1, value2);
    assertTrue("Expected to be valid", result.isValid());
  }
}
