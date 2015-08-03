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

import com.google.common.base.Joiner;
import com.magnet.mmx.server.plugin.mmxmgmt.api.message.MessageResource;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.AppResource;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test methods in Helper.
 */
public class HelperTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(HelperTest.class);
  @Test
  public void testValidPhoneNumber() {
    String ph = "6507089000";
    boolean valid = Helper.checkPhoneNumber(ph);
    assertTrue("Validation error for:" + ph, valid);
  }

  @Test
  public void testValidInternationalPhoneNumber() {
    String ph = "914427334000";
    boolean valid = Helper.checkPhoneNumber(ph);
    assertTrue("Validation error for:" + ph, valid);
  }

  @Test
  public void testInvalidPhoneNumber2() {
    String ph = "800-230-0909";
    boolean valid = Helper.checkPhoneNumber(ph);
    assertFalse("Validation error for:" + ph, valid);
  }

  @Test
  public void testInvalidPhoneNumber3() {
    String ph = null;
    boolean valid = Helper.checkPhoneNumber(ph);
    assertFalse("Validation error for:" + ph, valid);
  }

  @Test
  public void testReverse1() {
    String input = "login3";
    String expected = "3nigol";
    String reversed = Helper.reverse(input);
    assertEquals("Doesn't match expected", expected, reversed);
  }

  @Test
  public void testReverse2() {
    String input = "with space 1234";
    String expected = "4321 ecaps htiw";
    String reversed = Helper.reverse(input);
    assertEquals("Doesn't match expected", expected, reversed);
  }

  @Test
  public void testRemoveSuffix1() {
    String input = "login3%AAABSNIBKOstQST7";
    String removed = Helper.removeSuffix(input, JIDUtil.APP_ID_DELIMITER);
    assertEquals("Doesn't match expected", "login3", removed);
  }

  @Test
  public void testRemoveSuffix2() {
    String input = "login3";
    String removed = Helper.removeSuffix(input, JIDUtil.APP_ID_DELIMITER);
    assertEquals("Doesn't match expected", "login3", removed);
  }

  @Test
  public void testRemoveSuffix3() {
    String input = "s%AAABSNIBKOstQST7";
    String removed = Helper.removeSuffix(input, JIDUtil.APP_ID_DELIMITER);
    assertEquals("Doesn't match expected", "s", removed);
  }

  @Test
  public void testGetStandardWakeupMessage() {
    String message = Helper.getStandardWakeupMessage();
    String expected = "mmx:w:retrieve\r\n";
    assertEquals("non matching push message", expected,message);
  }

  @Test
  public void testSimplify1() {
    String topicId = "/i223hxed420/john/com.magnet.geoloc";
    String appId = "i223hxed420";
    String simplified = Helper.simplify(appId, topicId);
    assertEquals("Non matching simplified id", "john/com.magnet.geoloc", simplified);
  }

  @Test
  public void testSimplify2() {
    String topicId = "/i223hxed420/*/com.magnet.os/ANDROID/_all_";
    String appId = "i223hxed420";
    String simplified = Helper.simplify(appId, topicId);
    assertEquals("Non matching simplified id", "com.magnet.os/ANDROID/_all_", simplified);
  }

  @Test
  public void testSimplify3() {
    String topicId = "com.magnet.os/iOS/ipad";
    String appId = "i223hxed420";
    String simplified = Helper.simplify(appId, topicId);
    assertEquals("Non matching simplified id", "com.magnet.os/iOS/ipad", simplified);
  }

  @Test
  public void testGetListFromCommaDelimitedString() {
    String[] deviceIds = {"1111", "2222", "3333", "4444", "aabb", "ccdd", "aa11"};
    String str = Joiner.on(",").join(Arrays.asList(deviceIds));
    ArrayList<String> list = Helper.getListFromCommaDelimitedString(str);
    assertEquals(list.size(), 7);
    for(int i=0; i < list.size(); i++) {
      assertEquals(deviceIds[i], list.get(i));
    }

    str = "asa, asa, aa , asdadsdasdasd, aaaaaa, aaaa,aaa, 1111122222, 331212121, null,,,,,,";
    list = Helper.getListFromCommaDelimitedString(str);
    assertEquals(list.size(), 10);
    assertEquals(list.get(0), "asa");
    assertEquals(list.get(9), "null");

    str = "";

    list=Helper.getListFromCommaDelimitedString(str);
    assertEquals(list.size(), 0);

    str = null;

    list=Helper.getListFromCommaDelimitedString(str);
    assertEquals(list.size(), 0);
  }

  @Ignore
  @Test
  public void testSimplify4() {
    String wakeup = Helper.getStandardWakeupMessage();
    assertEquals("Not matching", "wakeup", wakeup);
  }

  @Test
  public void testToString() {
    String[] resourceClasses = {MessageResource.class.getName(), AppResource.class.getName()};
    //String resources = Arrays.toString(resourceClasses);
    String resources = StringUtils.join(resourceClasses, ",");
    assertNotNull(resources);
  }
}
